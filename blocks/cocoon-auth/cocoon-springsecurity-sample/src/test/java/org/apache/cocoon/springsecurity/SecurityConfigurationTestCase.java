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
package org.apache.cocoon.springsecurity;

import junit.framework.TestCase;

import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;

/**
 * Loads this sample's Spring Security configuration.
 *
 * <p>The sample is XML only -- it has no Java -- so nothing about it was ever checked at build
 * time. That is how it came to be silently unusable: the Acegi configuration it used to carry
 * referenced <code>org.acegisecurity</code> classes from a jar that cannot run on Jakarta EE 10,
 * and it used <code>&lt;ref local="..."/&gt;</code>, which Spring removed in 4.0. Neither shows
 * up in a build that only copies resources, and the sample is not deployed in the demo webapp,
 * so nothing ever loaded it.
 *
 * <p>This test exists so that the replacement cannot rot the same way: it actually builds the
 * application context, which is what proves the namespace parses, the classes exist and the
 * bean wiring resolves.
 */
public class SecurityConfigurationTestCase extends TestCase {

    private GenericWebApplicationContext context;

    protected void setUp() throws Exception {
        super.setUp();
        // <security:http> contributes a servlet filter chain, so the context has to be a web
        // one; a plain ClassPathXmlApplicationContext cannot instantiate it.
        this.context = new GenericWebApplicationContext(new MockServletContext());
        new XmlBeanDefinitionReader(this.context).loadBeanDefinitions(
                "classpath:META-INF/cocoon/spring/cocoon-springsecurity.xml");
        this.context.refresh();
    }

    protected void tearDown() throws Exception {
        if (this.context != null) {
            this.context.close();
        }
        super.tearDown();
    }

    /**
     * The filter chain must be registered under exactly this name: the DelegatingFilterProxy
     * added to web.xml by springsecurity-filter-patch.xweb looks the bean up by the filter's name.
     */
    public void testFilterChainIsRegisteredUnderTheNameWebXmlExpects() {
        assertTrue("springSecurityFilterChain is missing, so the DelegatingFilterProxy in "
                + "web.xml would fail at startup",
                this.context.containsBean("springSecurityFilterChain"));
    }

    /** The three users Acegi declared through InMemoryDaoImpl's userMap. */
    public void testDeclaredUsersCanAuthenticate() {
        assertAuthenticates("cocoon", "cocoon");
        assertAuthenticates("guest", "guest");
        assertAuthenticates("other", "other");
    }

    public void testWrongPasswordIsRejected() {
        try {
            authenticate("cocoon", "wrong");
            fail("a wrong password should not authenticate");
        } catch (AuthenticationException expected) {
            // as it should
        }
    }

    /**
     * Guards the <code>{noop}</code> prefixes in the user declarations. Spring Security 5 made
     * a PasswordEncoder mandatory; without the prefix every login fails at runtime with
     * "There is no PasswordEncoder mapped for the id null" -- which nothing but an actual
     * authentication attempt would reveal.
     */
    public void testPasswordsAreStoredWithAnEncoderPrefix() {
        Authentication result = authenticate("cocoon", "cocoon");
        assertTrue("authentication did not succeed, which usually means a {noop} prefix is "
                + "missing from a user declaration", result.isAuthenticated());
    }

    /** The role split the sample's sitemap depends on: only cocoon is a supervisor. */
    public void testSupervisorRoleIsOnlyGrantedToCocoon() {
        assertTrue("cocoon should be a supervisor",
                hasAuthority(authenticate("cocoon", "cocoon"), "ROLE_SUPERVISOR"));
        assertFalse("guest should not be a supervisor",
                hasAuthority(authenticate("guest", "guest"), "ROLE_SUPERVISOR"));
        assertTrue("guest should still be a user",
                hasAuthority(authenticate("guest", "guest"), "ROLE_USER"));
    }

    // ------------------------------------------------------------------

    /**
     * Looked up by name, not by type. The security namespace registers three
     * AuthenticationManager beans -- the global one plus factory beans for the
     * &lt;security:http&gt; element's own child manager -- so a by-type lookup is ambiguous.
     * This name is the one &lt;security:authentication-manager&gt; publishes.
     */
    private Authentication authenticate(String user, String password) {
        AuthenticationManager manager = (AuthenticationManager)
                this.context.getBean("org.springframework.security.authenticationManager");
        return manager.authenticate(new UsernamePasswordAuthenticationToken(user, password));
    }

    private void assertAuthenticates(String user, String password) {
        assertTrue(user + " should authenticate", authenticate(user, password).isAuthenticated());
    }

    private static boolean hasAuthority(Authentication authentication, String authority) {
        for (GrantedAuthority granted : authentication.getAuthorities()) {
            if (authority.equals(granted.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
