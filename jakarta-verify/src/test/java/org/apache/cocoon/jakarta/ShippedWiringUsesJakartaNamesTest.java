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
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

/**
 * Guards the class of migration bug that costs the most to find: configuration that
 * still names <code>javax.servlet.*</code> after the code moved to <code>jakarta.servlet.*</code>.
 *
 * <p>None of this is visible to the compiler, and only some of it even fails loudly:
 *
 * <ul>
 * <li>A Spring <code>ref="javax.servlet.ServletContext"</code> throws
 *     <code>NoSuchBeanDefinitionException</code> at startup - noisy, but only once the
 *     container actually boots.</li>
 * <li>An AspectJ pointcut <code>execution(* javax.servlet.Servlet.service(..))</code> simply
 *     stops matching. Nothing throws. The advice silently never runs.</li>
 * <li>A <code>&lt;bean-map type="javax.servlet.Servlet"&gt;</code> silently collects nothing.</li>
 * </ul>
 *
 * <p>The last two are why this is a test and not a code review item.
 *
 * @see <a href="../../../../../../../plans/jakarta-ee10-fork.md">plans/jakarta-ee10-fork.md</a>, Gap D
 */
public class ShippedWiringUsesJakartaNamesTest extends TestCase {

    private static final String[] CONFIG_SUFFIXES = { ".xml", ".xconf", ".xmap", ".properties", ".xsl", ".roles" };

    /**
     * The bean name Cocoon registers the servlet context under is derived from the class,
     * so the shipped XML has to agree with whatever the class is called today. This is the
     * invariant that broke: SettingsElementParser registers
     * <code>ServletContext.class.getName()</code> while the XML still asked for the javax name.
     */
    public void testServletContextBeanNameIsDerivedFromTheJakartaClass() {
        assertEquals("jakarta.servlet.ServletContext", ServletContext.class.getName());
        assertEquals("jakarta.servlet.http.HttpServletRequest", HttpServletRequest.class.getName());
        assertEquals("jakarta.servlet.http.HttpServletResponse", HttpServletResponse.class.getName());
    }

    public void testNoShippedConfigurationMentionsJavaxServlet() throws Exception {
        List<String> offenders = new ArrayList<String>();
        for (File jar : cocoonJars()) {
            JarFile jf = new JarFile(jar);
            try {
                for (Enumeration<JarEntry> e = jf.entries(); e.hasMoreElements();) {
                    JarEntry entry = e.nextElement();
                    if (!isConfiguration(entry.getName())) {
                        continue;
                    }
                    String text = read(jf, entry);
                    for (String line : text.split("\n")) {
                        if (line.contains("javax.servlet")) {
                            offenders.add(jar.getName() + "!" + entry.getName() + ": " + line.trim());
                        }
                    }
                }
            } finally {
                jf.close();
            }
        }
        assertEquals(
                "Shipped configuration still names javax.servlet. Spring refs fail at boot;\n"
                        + "AspectJ pointcuts and bean-map type filters fail silently and just stop\n"
                        + "matching anything:\n  " + join(offenders) + "\n",
                Collections.<String>emptyList(), offenders);
    }

    /**
     * Servlet context and request attribute names are part of the spec and were renamed
     * with the packages, so a literal "javax.servlet.context.tempdir" now resolves to
     * nothing and the lookup quietly returns null. Cocoon should be using the spec
     * constants; this catches a relapse to string literals.
     */
    public void testSpecAttributeNamesAreJakartaNames() {
        assertEquals("jakarta.servlet.context.tempdir", ServletContext.TEMPDIR);
        assertEquals("jakarta.servlet.include.request_uri",
                jakarta.servlet.RequestDispatcher.INCLUDE_REQUEST_URI);
    }

    // ------------------------------------------------------------------------

    private static boolean isConfiguration(String name) {
        for (String suffix : CONFIG_SUFFIXES) {
            if (name.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    /** Only Cocoon's own artifacts: third-party configuration is not ours to fix. */
    private static List<File> cocoonJars() {
        List<File> jars = new ArrayList<File>();
        for (String element : System.getProperty("java.class.path").split(File.pathSeparator)) {
            File f = new File(element);
            if (f.isFile() && f.getName().startsWith("cocoon-") && f.getName().endsWith(".jar")) {
                jars.add(f);
            }
        }
        assertFalse("no cocoon-*.jar on the test classpath - the guard would pass vacuously",
                jars.isEmpty());
        return jars;
    }

    private static String read(JarFile jf, JarEntry entry) throws IOException {
        InputStream in = jf.getInputStream(entry);
        try {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), "UTF-8");
        } finally {
            in.close();
        }
    }

    private static String join(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) {
                sb.append("\n  ");
            }
            sb.append(p);
        }
        return sb.toString();
    }

    /** Keeps the unused-import checker honest about why these are imported. */
    static final List<Class<?>> CHECKED_TYPES =
            Arrays.<Class<?>>asList(ServletContext.class, HttpServletRequest.class, HttpServletResponse.class);
}
