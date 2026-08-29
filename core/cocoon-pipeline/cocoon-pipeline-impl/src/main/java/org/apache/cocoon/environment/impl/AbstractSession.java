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
package org.apache.cocoon.environment.impl;

import java.util.Collections;
import java.util.Map;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSessionContext;

import org.apache.cocoon.environment.Session;

/**
 * Base class for any session
 *
 * @version $Id$
 * @deprecated This class implements deprecated interface and will be removed in the future.
 *             See {@link Session} interface for details. 
 */
public abstract class AbstractSession 
    implements Session {

    public Map getAttributes() {
	return new SessionMap(this);
    }

    public ServletContext getServletContext() {
        // TODO The method was added when Session was made extending HttpSession, implement the method
        throw new UnsupportedOperationException();
    }

    // SERVLET5-SHIM: the members below were removed from HttpSession in Servlet 6.0.
    // They are implemented here so that the whole AbstractSession hierarchy satisfies the
    // Servlet 5.0 interface during the EE 9 checkpoint. Phase 3 of
    // plans/jakarta-ee10-fork.md deletes every SERVLET5-SHIM block.

    /** @deprecated removed in Servlet 6.0; always returns null, as the spec required since 2.1. */
    public HttpSessionContext getSessionContext() {
        return null;
    }

    /** @deprecated removed in Servlet 6.0; use {@link #getAttribute(String)}. */
    public Object getValue(String name) {
        return getAttribute(name);
    }

    /** @deprecated removed in Servlet 6.0; use {@link #getAttributeNames()}. */
    public String[] getValueNames() {
        return Collections.list(getAttributeNames()).toArray(new String[0]);
    }

    /** @deprecated removed in Servlet 6.0; use {@link #setAttribute(String, Object)}. */
    public void putValue(String name, Object value) {
        setAttribute(name, value);
    }

    /** @deprecated removed in Servlet 6.0; use {@link #removeAttribute(String)}. */
    public void removeValue(String name) {
        removeAttribute(name);
    }
}
