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
package org.apache.cocoon.environment.mock;

import jakarta.servlet.ServletConnection;

import junit.framework.TestCase;

/**
 * Servlet 6.0 added three abstract methods to <code>ServletRequest</code>:
 * <code>getRequestId()</code>, <code>getProtocolRequestId()</code> and
 * <code>getServletConnection()</code>. They are implemented once, on
 * <code>AbstractRequest</code>, so that the whole Request hierarchy inherits them.
 *
 * <p>Cocoon's synthetic requests - the command-line environment, background processing,
 * internal pipeline calls, and this mock - have no network connection behind them, but
 * the spec still requires non-null answers. Returning null would surface as an NPE deep
 * inside a container or a logging filter, a long way from the cause, so the contract is
 * worth pinning down.
 *
 * <p>MockRequest is used here because it is the concrete AbstractRequest subclass that
 * already exists. That it still compiles is itself part of the check: if a later servlet
 * release adds another abstract method and AbstractRequest does not absorb it, every
 * concrete subclass in the tree breaks, and this one breaks first.
 */
public class MockRequestServlet6TestCase extends TestCase {

    private MockRequest request;

    protected void setUp() throws Exception {
        super.setUp();
        this.request = new MockRequest();
    }

    public void testRequestIdIsPresentAndStable() {
        String id = this.request.getRequestId();
        assertNotNull("getRequestId() must not return null", id);
        assertTrue("getRequestId() must not be blank", id.length() > 0);
        assertEquals("the id must be stable for the life of the request",
                id, this.request.getRequestId());
    }

    public void testDistinctRequestsGetDistinctIds() {
        assertFalse("two live requests must not share an id",
                this.request.getRequestId().equals(new MockRequest().getRequestId()));
    }

    /** The spec prescribes the empty string for protocols that do not multiplex requests. */
    public void testProtocolRequestIdIsEmptyForNonMultiplexedProtocols() {
        assertEquals("", this.request.getProtocolRequestId());
    }

    public void testServletConnectionIsPresentAndConsistent() {
        ServletConnection connection = this.request.getServletConnection();
        assertNotNull("getServletConnection() must not return null", connection);
        assertEquals("", connection.getProtocolConnectionId());
        assertEquals("the connection id should identify the request it belongs to",
                this.request.getRequestId(), connection.getConnectionId());
        assertEquals("a request that reports no protocol should fall back, not return null",
                this.request.getProtocol() == null ? "HTTP/1.1" : this.request.getProtocol(),
                connection.getProtocol());
        assertEquals(this.request.isSecure(), connection.isSecure());
    }

    public void testServletConnectionReportsTheRequestProtocol() {
        this.request.setProtocol("HTTP/2.0");
        assertEquals("HTTP/2.0", this.request.getServletConnection().getProtocol());
    }

    public void testServletConnectionNeverReportsANullProtocol() {
        this.request.setProtocol(null);
        assertEquals("HTTP/1.1", this.request.getServletConnection().getProtocol());
    }
}
