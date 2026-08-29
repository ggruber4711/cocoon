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
package org.apache.cocoon.servletservice.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import junit.framework.TestCase;

/**
 * Regression test for the session wrapper a servlet-service call sees.
 *
 * <p>Before the Servlet 6.0 migration this class had:
 *
 * <pre>
 *   public void removeAttribute(String name) { this.removeValue(name); }
 *   public void removeValue(String name)     { this.removeAttribute(name); }
 * </pre>
 *
 * <p>which is unbounded mutual recursion. Any attempt to remove a session attribute
 * during a servlet-service call ended in <code>StackOverflowError</code>, and no
 * attribute was ever removed. It survived because <code>removeValue</code> was a
 * deprecated Servlet 2.x method nobody called directly, and removing session attributes
 * inside a block call is rare.
 *
 * <p>Servlet 6.0 deletes <code>removeValue</code>, which forced the question and exposed
 * the bug. This test keeps it from coming back.
 *
 * <p>The session class is private, so it is reached reflectively rather than by widening
 * production visibility for a test.
 */
public class ServletServiceRequestSessionTestCase extends TestCase {

    private static final String SESSION_CLASS =
            "org.apache.cocoon.servletservice.util.ServletServiceRequest$Session";

    private Object session;
    private Map<?, ?> stored;
    private Method getAttribute;
    private Method setAttribute;
    private Method removeAttribute;

    protected void setUp() throws Exception {
        super.setUp();
        Class<?> sessionClass = Class.forName(SESSION_CLASS);
        Constructor<?> ctor = sessionClass.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        this.session = ctor.newInstance(new Object[ctor.getParameterTypes().length]);

        this.getAttribute = sessionClass.getMethod("getAttribute", String.class);
        this.setAttribute = sessionClass.getMethod("setAttribute", String.class, Object.class);
        this.removeAttribute = sessionClass.getMethod("removeAttribute", String.class);
        this.getAttribute.setAccessible(true);
        this.setAttribute.setAccessible(true);
        this.removeAttribute.setAccessible(true);

        // Attributes this session holds itself. Anything it does not hold is delegated to
        // the parent request's session, which needs a live servlet-service call to exist,
        // so the assertions below stay on the locally held map.
        Field values = Class.forName("org.apache.cocoon.servletservice.util.ServletServiceRequest$Values")
                .getDeclaredField("values");
        values.setAccessible(true);
        this.stored = (Map<?, ?>) values.get(this.session);
    }

    public void testRemoveAttributeActuallyRemovesAndDoesNotRecurse() throws Exception {
        this.setAttribute.invoke(this.session, "colour", "green");
        assertEquals("precondition: the attribute is held by this session",
                "green", this.getAttribute.invoke(this.session, "colour"));
        assertTrue(this.stored.containsKey("colour"));

        // Before the fix this call did not return at all:
        // removeAttribute -> removeValue -> removeAttribute -> StackOverflowError.
        this.removeAttribute.invoke(this.session, "colour");

        assertFalse("removeAttribute must actually remove the attribute",
                this.stored.containsKey("colour"));
    }

    public void testRemovingAnAbsentAttributeIsHarmless() throws Exception {
        this.removeAttribute.invoke(this.session, "never-set");
        assertFalse(this.stored.containsKey("never-set"));
    }

    /** The Servlet 2.x value API is gone in Servlet 6.0 and must not be reintroduced. */
    public void testDeprecatedValueApiIsAbsent() {
        for (String gone : new String[] { "getValueNames", "putValue", "removeValue", "getSessionContext" }) {
            for (Method m : HttpSession.class.getMethods()) {
                assertFalse("HttpSession." + gone + " was removed in Servlet 6.0",
                        m.getName().equals(gone));
            }
        }
    }
}
