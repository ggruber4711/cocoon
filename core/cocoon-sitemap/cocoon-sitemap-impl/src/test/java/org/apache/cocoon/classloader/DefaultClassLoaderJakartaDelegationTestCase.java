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
package org.apache.cocoon.classloader;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

/**
 * The reloading class loader must never define servlet API classes itself.
 *
 * <p>If it does, a reloaded block ends up with its own
 * <code>jakarta.servlet.http.HttpServletRequest</code>, distinct from the one the
 * container created the request with, and the first assignment across that boundary
 * throws <code>ClassCastException</code> - at runtime, in production, with a message
 * that names the same class twice and explains nothing.
 *
 * <p>The filter used to list only <code>javax.servlet</code>. The namespace moved and the
 * filter did not, which is invisible to the compiler and to every functional test that
 * does not exercise block reloading.
 */
public class DefaultClassLoaderJakartaDelegationTestCase extends TestCase {

    private ProbeableClassLoader classLoader;

    protected void setUp() throws Exception {
        super.setUp();
        this.classLoader = new ProbeableClassLoader(
                new URL[0],
                Collections.EMPTY_LIST,
                Collections.EMPTY_LIST,
                getClass().getClassLoader());
    }

    public void testServletApiIsAlwaysLoadedByTheParent() {
        List<String> mustDelegate = Arrays.asList(
                "jakarta.servlet.Servlet",
                "jakarta.servlet.ServletContext",
                "jakarta.servlet.ServletConnection",
                "jakarta.servlet.http.HttpServletRequest",
                "jakarta.servlet.http.HttpServletResponse",
                "jakarta.servlet.http.HttpSession");
        for (String name : mustDelegate) {
            assertFalse(name + " must be loaded by the parent, never defined by the reloading "
                    + "class loader", this.classLoader.probe(name));
        }
    }

    public void testJdkClassesAreAlwaysLoadedByTheParent() {
        assertFalse(this.classLoader.probe("java.lang.String"));
        assertFalse(this.classLoader.probe("java.util.Map"));
    }

    /**
     * The pre-Jakarta namespace stays in the filter so that a consumer still mid-migration,
     * or a container that exposes both, keeps the same protection.
     */
    public void testLegacyJavaxServletIsStillDelegated() {
        assertFalse(this.classLoader.probe("javax.servlet.http.HttpServletRequest"));
    }

    public void testOrdinaryApplicationClassesAreLoadedLocally() {
        assertTrue(this.classLoader.probe("org.apache.cocoon.something.Whatever"));
        assertTrue(this.classLoader.probe("com.example.BlockComponent"));
    }

    /** Exposes the protected delegation decision. */
    private static final class ProbeableClassLoader extends DefaultClassLoader {
        ProbeableClassLoader(URL[] urls, List includes, List excludes, ClassLoader parent) {
            super(urls, includes, excludes, parent);
        }

        boolean probe(String name) {
            return tryClassHere(name);
        }
    }
}
