/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cocoon.jakarta;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

/**
 * Guards the distribution against javax.* EE classes creeping back onto the runtime
 * classpath.
 *
 * <p>Two different problems are checked, because they fail in different ways:
 *
 * <ul>
 * <li><b>Containing</b> a spec class (for example a jar that ships its own copy of
 *     <code>javax/servlet/ServletContext.class</code>) is fatal: the container puts the
 *     real API on the classpath too, and whichever one loses the race produces
 *     <code>ClassCastException</code> or <code>LinkageError</code> at runtime. There is no
 *     legitimate reason for this, so the check is absolute.</li>
 * <li><b>Referencing</b> a spec class is usually harmless: several widely used libraries
 *     carry optional integrations that are only loaded if you configure them. Those are
 *     allow-listed by name below. The point of the check is not to reach zero, it is that
 *     a <em>new</em> entry has to be looked at by a human and consciously added.</li>
 * </ul>
 *
 * @see <a href="../../../../../../../plans/jakarta-ee10-fork.md">plans/jakarta-ee10-fork.md</a>, Phase 4
 */
public class ClasspathHasNoJavaxServletTest extends TestCase {

    /**
     * EE packages that were renamed to jakarta.* and must not appear as javax.* any more.
     *
     * <p><code>javax.annotation</code> is deliberately absent: the name is shared between
     * Jakarta Annotations (renamed) and JSR-305 / <code>javax.annotation.processing</code>
     * (not renamed, and still correct). Spring and Micrometer reference the latter. The
     * Jakarta ones are checked by type name in {@link #EE_ANNOTATIONS} instead.
     */
    private static final Pattern RENAMED_EE_PACKAGE = Pattern.compile(
            "javax/(servlet|mail|jms|activation|xml/soap|el|persistence|transaction|validation|ws|faces|portlet)/");

    /** The Jakarta Annotations types, which JSR-305 does not define. */
    private static final Pattern EE_ANNOTATIONS = Pattern.compile(
            "javax/annotation/(Resource|Resources|PostConstruct|PreDestroy|Priority|Generated|ManagedBean)\\b");

    /**
     * Jars allowed to <em>reference</em> a renamed javax.* package.
     *
     * <p>Every entry is an optional integration that is never loaded by Cocoon:
     * <ul>
     * <li><b>commons-jxpath</b> - JXPathServletContexts, for JXPath over servlet scopes.</li>
     * <li><b>commons-logging</b> - ServletContextCleaner, only used by containers that call it.</li>
     * <li><b>log4j</b> - JMSAppender and SMTPAppender, only if configured in log4j.properties.</li>
     * <li><b>ehcache</b> - bundles shaded caching filters and a JAX-RS agent.</li>
     * <li><b>excalibur-logger</b> - servlet/JMS/mail log targets, only if configured.</li>
     * <li><b>velocity</b> - its optional servlet view layer.</li>
     * <li><b>spring-context</b> - CommonAnnotationBeanPostProcessor keeps optional support
     *     for the legacy <code>javax.annotation.Resource</code> alongside the Jakarta one.
     *     It is a <code>ClassUtils.isPresent</code> / <code>forName</code> lookup, so it is
     *     inert when the class is absent, which is the case here.</li>
     * </ul>
     * These are references only: none of these jars <em>contains</em> a spec class, which is
     * what the other half of this test enforces.
     */
    private static final Set<String> MAY_REFERENCE_JAVAX = new HashSet<String>(Arrays.asList(
            "commons-jxpath",
            "commons-logging",
            "log4j",
            "ehcache",
            "excalibur-logger",
            "velocity",
            "spring-context"));

    /**
     * JAX-RPC is a special case and is not in {@link #RENAMED_EE_PACKAGE} at all.
     * It was dropped from Jakarta EE and never got a package rename: the artifact
     * <code>jakarta.xml.rpc:jakarta.xml.rpc-api</code> renamed only the Maven coordinates
     * and still ships <code>javax.xml.rpc</code> classes. So javax.xml.rpc on the
     * classpath is correct, not a leftover.
     */
    public void testJaxRpcIsDeliberatelyStillJavax() throws Exception {
        Class<?> stub = Class.forName("javax.xml.rpc.Stub");
        assertNotNull("jakarta.xml.rpc-api should still provide javax.xml.rpc", stub);
    }

    public void testNoJarContainsARenamedEeSpecClass() throws Exception {
        List<String> offenders = new ArrayList<String>();
        for (File jar : classpathJars()) {
            Set<String> found = packagesContainedIn(jar);
            if (!found.isEmpty()) {
                offenders.add(jar.getName() + " contains " + found);
            }
        }
        assertEquals(
                "Jars shipping their own copy of a renamed EE spec package. These collide with "
                        + "the container's API and fail at runtime, not at build time:\n  "
                        + join(offenders, "\n  ") + "\n",
                Collections.<String>emptyList(), offenders);
    }

    public void testOnlyKnownJarsReferenceRenamedEePackages() throws Exception {
        List<String> unexpected = new ArrayList<String>();
        for (File jar : classpathJars()) {
            if (MAY_REFERENCE_JAVAX.contains(baseArtifactName(jar))) {
                continue;
            }
            Set<String> found = packagesReferencedBy(jar);
            if (!found.isEmpty()) {
                unexpected.add(jar.getName() + " references " + found);
            }
        }
        assertEquals(
                "Jars referencing a renamed EE package that are not on the reviewed allow-list.\n"
                        + "Either the jar needs a Jakarta release / an Eclipse Transformer shim (see\n"
                        + "jakarta-shims/), or it is another harmless optional integration and belongs\n"
                        + "in MAY_REFERENCE_JAVAX with a note saying why:\n  "
                        + join(unexpected, "\n  ") + "\n",
                Collections.<String>emptyList(), unexpected);
    }

    // ------------------------------------------------------------------------

    private static Set<String> packagesContainedIn(File jar) throws IOException {
        Set<String> found = new HashSet<String>();
        JarFile jf = new JarFile(jar);
        try {
            for (java.util.Enumeration<JarEntry> e = jf.entries(); e.hasMoreElements();) {
                String name = e.nextElement().getName();
                if (!name.endsWith(".class")) {
                    continue;
                }
                // lookingAt() anchors at position 0, which is the point: only a class at
                // the root of the jar is actually on the classpath. ehcache, for one,
                // ships a whole servlet stack under "rest-management-private-classpath/"
                // precisely so that it is not visible to the normal class loader. Matching
                // anywhere in the entry name would flag that as a collision when it is not.
                Matcher m = RENAMED_EE_PACKAGE.matcher(name);
                if (m.lookingAt()) {
                    found.add(m.group());
                }
            }
        } finally {
            jf.close();
        }
        return found;
    }

    private static Set<String> packagesReferencedBy(File jar) throws IOException {
        Set<String> found = new HashSet<String>();
        JarFile jf = new JarFile(jar);
        try {
            for (java.util.Enumeration<JarEntry> e = jf.entries(); e.hasMoreElements();) {
                JarEntry entry = e.nextElement();
                if (!entry.getName().endsWith(".class")) {
                    continue;
                }
                InputStream in = jf.getInputStream(entry);
                try {
                    // Class references live in the constant pool as plain UTF-8 with '/'
                    // separators, so a scan of the raw bytes finds them without a parser.
                    String bytes = new String(readFully(in), "ISO-8859-1");
                    Matcher m = RENAMED_EE_PACKAGE.matcher(bytes);
                    while (m.find()) {
                        found.add(m.group());
                    }
                    Matcher a = EE_ANNOTATIONS.matcher(bytes);
                    while (a.find()) {
                        found.add(a.group());
                    }
                } finally {
                    in.close();
                }
            }
        } finally {
            jf.close();
        }
        return found;
    }

    private static List<File> classpathJars() {
        List<File> jars = new ArrayList<File>();
        for (String element : System.getProperty("java.class.path").split(File.pathSeparator)) {
            File f = new File(element);
            if (f.isFile() && f.getName().endsWith(".jar")) {
                jars.add(f);
            }
        }
        assertFalse("no jars found on the test classpath - the guard would pass vacuously",
                jars.isEmpty());
        return jars;
    }

    /** "commons-jxpath-1.3.jar" -&gt; "commons-jxpath". */
    private static String baseArtifactName(File jar) {
        return jar.getName().replaceFirst("-\\d[^/]*\\.jar$", "").replaceFirst("\\.jar$", "");
    }

    private static byte[] readFully(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    private static String join(List<String> parts, String sep) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) {
                sb.append(sep);
            }
            sb.append(p);
        }
        return sb.toString();
    }
}
