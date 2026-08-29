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

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;
import java.util.Map;
import java.util.EventListener;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.descriptor.JspConfigDescriptor;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.SessionTrackingMode;

/**
 * @version $Id: ServletContextWrapper.java 608375 2008-01-03 08:33:00Z reinhard $
 * @since 1.0.0
 */
public class ServletContextWrapper implements ServletContext {

    protected ServletContext servletContext;

    /**
     * @param servletContext The servletContext to set.
     */
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public ServletContext getContext(String uripath) {
        return this.servletContext.getContext(uripath);
    }

    public int getMajorVersion() {
        return this.servletContext.getMajorVersion();
    }

    public int getMinorVersion() {
        return this.servletContext.getMinorVersion();
    }

    public String getMimeType(String file) {
        return this.servletContext.getMimeType(file);
    }

    public Set getResourcePaths(String paths) {
        return this.servletContext.getResourcePaths(paths);
    }

    public URL getResource(String path) throws MalformedURLException {
        return this.servletContext.getResource(path);
    }

    public InputStream getResourceAsStream(String path) {
        return this.servletContext.getResourceAsStream(path);
    }

    public RequestDispatcher getRequestDispatcher(String path) {
        return this.servletContext.getRequestDispatcher(path);
    }

    public RequestDispatcher getNamedDispatcher(String name) {
        return this.servletContext.getNamedDispatcher(name);
    }




    public void log(String msg) {
        this.servletContext.log(msg);
    }


    public void log(String msg, Throwable throwable) {
        this.servletContext.log(msg, throwable);
    }

    public String getRealPath(String path) {
        return this.servletContext.getRealPath(path);
    }

    public String getServerInfo() {
        return this.servletContext.getServerInfo();
    }

    public String getInitParameter(String path) {
        return this.servletContext.getInitParameter(path);
    }

    public Enumeration getInitParameterNames() {
        return this.servletContext.getInitParameterNames();
    }

    public Object getAttribute(String name) {
        return this.servletContext.getAttribute(name);
    }

    public Enumeration getAttributeNames() {
        return this.servletContext.getAttributeNames();
    }

    public void setAttribute(String name, Object value) {
        this.servletContext.setAttribute(name, value);
    }

    public void removeAttribute(String name) {
        this.servletContext.removeAttribute(name);
    }

    public String getServletContextName() {
        return this.servletContext.getServletContextName();
    }

    // Servlet 3.0+/3.1 delegations
    public String getContextPath() { return this.servletContext.getContextPath(); }
    public String getVirtualServerName() { return this.servletContext.getVirtualServerName(); }
    public boolean setInitParameter(String name, String value) { return this.servletContext.setInitParameter(name, value); }
    public int getEffectiveMajorVersion() { return this.servletContext.getEffectiveMajorVersion(); }
    public int getEffectiveMinorVersion() { return this.servletContext.getEffectiveMinorVersion(); }
    public ClassLoader getClassLoader() { return this.servletContext.getClassLoader(); }
    public JspConfigDescriptor getJspConfigDescriptor() { return this.servletContext.getJspConfigDescriptor(); }
    public <T extends EventListener> T createListener(Class<T> c) throws ServletException { return this.servletContext.createListener(c); }
    public void addListener(String className) { this.servletContext.addListener(className); }
    public void addListener(EventListener t) { this.servletContext.addListener(t); }
    public void addListener(Class<? extends EventListener> listenerClass) { this.servletContext.addListener(listenerClass); }
    public ServletRegistration.Dynamic addServlet(String servletName, String className) { return this.servletContext.addServlet(servletName, className); }
    public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) { return this.servletContext.addServlet(servletName, servlet); }
    public ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) { return this.servletContext.addServlet(servletName, servletClass); }
    public <T extends Servlet> T createServlet(Class<T> c) throws ServletException { return this.servletContext.createServlet(c); }
    public ServletRegistration getServletRegistration(String servletName) { return this.servletContext.getServletRegistration(servletName); }
    public Map<String, ? extends ServletRegistration> getServletRegistrations() { return this.servletContext.getServletRegistrations(); }
    public FilterRegistration.Dynamic addFilter(String filterName, String className) { return this.servletContext.addFilter(filterName, className); }
    public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) { return this.servletContext.addFilter(filterName, filter); }
    public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) { return this.servletContext.addFilter(filterName, filterClass); }
    public <T extends Filter> T createFilter(Class<T> c) throws ServletException { return this.servletContext.createFilter(c); }
    public FilterRegistration getFilterRegistration(String filterName) { return this.servletContext.getFilterRegistration(filterName); }
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() { return this.servletContext.getFilterRegistrations(); }
    public SessionCookieConfig getSessionCookieConfig() { return this.servletContext.getSessionCookieConfig(); }
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) { this.servletContext.setSessionTrackingModes(sessionTrackingModes); }
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() { return this.servletContext.getDefaultSessionTrackingModes(); }
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() { return this.servletContext.getEffectiveSessionTrackingModes(); }
    public void declareRoles(String... roleNames) { this.servletContext.declareRoles(roleNames); }

    // Servlet 4.0 additions.
    public ServletRegistration.Dynamic addJspFile(String servletName, String jspFile) { return this.servletContext.addJspFile(servletName, jspFile); }
    public int getSessionTimeout() { return this.servletContext.getSessionTimeout(); }
    public void setSessionTimeout(int sessionTimeout) { this.servletContext.setSessionTimeout(sessionTimeout); }
    public String getRequestCharacterEncoding() { return this.servletContext.getRequestCharacterEncoding(); }
    public void setRequestCharacterEncoding(String encoding) { this.servletContext.setRequestCharacterEncoding(encoding); }
    public String getResponseCharacterEncoding() { return this.servletContext.getResponseCharacterEncoding(); }
    public void setResponseCharacterEncoding(String encoding) { this.servletContext.setResponseCharacterEncoding(encoding); }

}
