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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import java.util.EventListener;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.SessionTrackingMode;
import jakarta.servlet.descriptor.JspConfigDescriptor;

import org.apache.cocoon.environment.Context;

/**
 * Base class for any context
 *
 * @version $Id$
 */
public abstract class AbstractContext 
    implements Context, ServletContext {

    /** Attributes. */
    protected final Map attributes = new HashMap();

    /**
     * @see jakarta.servlet.ServletContext#getAttribute(java.lang.String)
     */
    public Object getAttribute(String key) {
        return this.attributes.get(key);
    }

    /**
     * @see jakarta.servlet.ServletContext#getAttributeNames()
     */
    public Enumeration getAttributeNames() {
        return Collections.enumeration(this.attributes.keySet());
    }

    /**
     * @see jakarta.servlet.ServletContext#getContext(java.lang.String)
     */
    public ServletContext getContext(String arg0) {
        return this;
    }

    /**
     * @see jakarta.servlet.ServletContext#getInitParameter(java.lang.String)
     */
    public String getInitParameter(String arg0) {
        return null;
    }

    /**
     * @see jakarta.servlet.ServletContext#getInitParameterNames()
     */
    public Enumeration getInitParameterNames() {
        return Collections.enumeration(Collections.EMPTY_LIST);
    }

    /**
     * @see jakarta.servlet.ServletContext#getMajorVersion()
     */
    public int getMajorVersion() {
        return 2;
    }

    /**
     * @see jakarta.servlet.ServletContext#getMimeType(java.lang.String)
     */
    public String getMimeType(String arg0) {
        return null;
    }

    /**
     * @see jakarta.servlet.ServletContext#getMinorVersion()
     */
    public int getMinorVersion() {
        return 3;
    }

    /**
     * @see jakarta.servlet.ServletContext#getNamedDispatcher(java.lang.String)
     */
    public RequestDispatcher getNamedDispatcher(String arg0) {
        return null;
    }

    /**
     * @see jakarta.servlet.ServletContext#getRealPath(java.lang.String)
     */
    public String getRealPath(String arg0) {
        return null;
    }

    /**
     * @see jakarta.servlet.ServletContext#getRequestDispatcher(java.lang.String)
     */
    public RequestDispatcher getRequestDispatcher(String arg0) {
        return null;
    }

    /**
     * @see jakarta.servlet.ServletContext#getResource(java.lang.String)
     */
    public URL getResource(String arg0) throws MalformedURLException {
        return null;
    }

    /**
     * Get access to the resource as @link {@link InputStream}. If there is any problem,
     * <code>null</code> is returned.
     * 
     * @see jakarta.servlet.ServletContext#getResourceAsStream(java.lang.String)
     */
    public InputStream getResourceAsStream(String path) {
    	URL resourceURL = null;
		try {
			resourceURL = this.getResource(path);
		} catch (MalformedURLException e) {
			return null;
		}
    	if(resourceURL != null) {
    		try {
				return resourceURL.openStream();
			} catch (IOException e) {
				return null;
			}
    	}
        return null;
    }

    /**
     * @see jakarta.servlet.ServletContext#getResourcePaths(java.lang.String)
     */
    public Set getResourcePaths(String arg0) {
        return null;
    }

    /**
     * @see jakarta.servlet.ServletContext#getServerInfo()
     */
    public String getServerInfo() {
        return null;
    }

    /**
     * @see jakarta.servlet.ServletContext#getServlet(java.lang.String)
     */
    public Servlet getServlet(String arg0) throws ServletException {
        return null;
    }

    /**
     * @see jakarta.servlet.ServletContext#getServletContextName()
     */
    public String getServletContextName() {
        return null;
    }

    /**
     * @see jakarta.servlet.ServletContext#getServletNames()
     */
    public Enumeration getServletNames() {
        return null;
    }

    /**
     * @see jakarta.servlet.ServletContext#getServlets()
     */
    public Enumeration getServlets() {
        return null;
    }

    /**
     * @see jakarta.servlet.ServletContext#removeAttribute(java.lang.String)
     */
    public void removeAttribute(String key) {
        this.attributes.remove(key);
    }

    /**
     * @see jakarta.servlet.ServletContext#setAttribute(java.lang.String, java.lang.Object)
     */
    public void setAttribute(String key, Object object) {
        this.attributes.put(key, object);
    }

    /**
     * @see org.apache.cocoon.environment.Context#getAttributes()
     */
    public Map getAttributes() {
	    return new ContextMap(this);
    }

    // Servlet 4.0 additions. Cocoon's Context abstractions are read-only views over a
    // container context, so the mutating ones are not supported; the getters report
    // "unset" rather than throwing, which is what callers expect.

    public String getVirtualServerName() {
        return null;
    }

    public jakarta.servlet.ServletRegistration.Dynamic addJspFile(String servletName, String jspFile) {
        throw new UnsupportedOperationException();
    }

    public int getSessionTimeout() {
        return 0;
    }

    public void setSessionTimeout(int sessionTimeout) {
        throw new UnsupportedOperationException();
    }

    public String getRequestCharacterEncoding() {
        return null;
    }

    public void setRequestCharacterEncoding(String encoding) {
        throw new UnsupportedOperationException();
    }

    public String getResponseCharacterEncoding() {
        return null;
    }

    public void setResponseCharacterEncoding(String encoding) {
        throw new UnsupportedOperationException();
    }

    // ------------------------------------------------------------------------
    // Servlet 3.0 / 3.1 surface.
    //
    // Cocoon's Context implementations are read-only adapters over a container
    // context (or a synthetic one, as in the CLI and background environments).
    // They cannot register servlets, filters or listeners, so the programmatic
    // registration API is unsupported; the accessors report "nothing configured"
    // instead of throwing, so that generic callers can probe safely.
    //
    // These live here rather than in each concrete subclass so that adding a
    // method to the servlet spec is a one-file change. See section 3.3 of
    // plans/jakarta-ee10-fork.md.
    // ------------------------------------------------------------------------

    public String getContextPath() {
        return null;
    }

    public int getEffectiveMajorVersion() {
        return getMajorVersion();
    }

    public int getEffectiveMinorVersion() {
        return getMinorVersion();
    }

    public boolean setInitParameter(String name, String value) {
        return false;
    }

    public ClassLoader getClassLoader() {
        return getClass().getClassLoader();
    }

    public JspConfigDescriptor getJspConfigDescriptor() {
        return null;
    }

    public void log(String message) {
    }

    public void log(String message, Throwable throwable) {
    }

    public ServletRegistration.Dynamic addServlet(String servletName, String className) {
        throw new UnsupportedOperationException();
    }

    public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
        throw new UnsupportedOperationException();
    }

    public ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
        throw new UnsupportedOperationException();
    }

    public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
        throw new UnsupportedOperationException();
    }

    public ServletRegistration getServletRegistration(String servletName) {
        return null;
    }

    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return Collections.emptyMap();
    }

    public FilterRegistration.Dynamic addFilter(String filterName, String className) {
        throw new UnsupportedOperationException();
    }

    public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        throw new UnsupportedOperationException();
    }

    public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
        throw new UnsupportedOperationException();
    }

    public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
        throw new UnsupportedOperationException();
    }

    public FilterRegistration getFilterRegistration(String filterName) {
        return null;
    }

    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return Collections.emptyMap();
    }

    public void addListener(String className) {
        throw new UnsupportedOperationException();
    }

    public <T extends EventListener> void addListener(T listener) {
        throw new UnsupportedOperationException();
    }

    public void addListener(Class<? extends EventListener> listenerClass) {
        throw new UnsupportedOperationException();
    }

    public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
        throw new UnsupportedOperationException();
    }

    public SessionCookieConfig getSessionCookieConfig() {
        return null;
    }

    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
        throw new UnsupportedOperationException();
    }

    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return Collections.emptySet();
    }

    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return Collections.emptySet();
    }

    public void declareRoles(String... roleNames) {
        throw new UnsupportedOperationException();
    }
}
