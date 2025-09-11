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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.io.InputStream;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.*;
import java.util.EventListener;
import java.util.Map;
import java.util.Set;

import org.apache.cocoon.environment.impl.AbstractContext;

public class MockContext extends AbstractContext {

    private Hashtable attributes = new Hashtable();
    private Hashtable resources = new Hashtable();
    private Hashtable mappings = new Hashtable();
    private Hashtable initparameters = new Hashtable();

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    public Enumeration getAttributeNames() {
        return attributes.keys();
    }

    public void setResource(String path, URL url) {
        resources.put(path, url);
    }

    public URL getResource(String path) throws MalformedURLException {
        return (URL)resources.get(path);
    }

    public String getRealPath(String path) {
      return path;
    }

    public String getMimeType(String file) {
        return (String)mappings.get(file.substring(file.lastIndexOf(".")+1));
    }

    public boolean setInitParameter(String name, String value) {
        initparameters.put(name, value);
        return true;
    }

    public String getInitParameter(String name) {
        return (String)initparameters.get(name);
    }

    public InputStream getResourceAsStream(String path) {
        return null;
    }

    public void reset() {
        attributes.clear();
        resources.clear();
        mappings.clear();
        initparameters.clear();
    }

    public void log(Exception arg0, String arg1) {
        System.err.println("log");
    }

    public void log(String arg0, Throwable arg1) {
        System.err.println("log");
    }

    public void log(String arg0) {
        System.err.println("log");
    }

    // Servlet 3.0+/3.1 minimal support
    public String getVirtualServerName() { return "mock"; }
    public int getEffectiveMajorVersion() { return getMajorVersion(); }
    public int getEffectiveMinorVersion() { return getMinorVersion(); }
    public ClassLoader getClassLoader() { return this.getClass().getClassLoader(); }
    public JspConfigDescriptor getJspConfigDescriptor() { return null; }
    public <T extends EventListener> T createListener(Class<T> c) throws ServletException { try { return c.newInstance(); } catch (Exception e) { throw new ServletException(e); } }
    public void addListener(String className) {}
    public void addListener(EventListener t) {}
    public void addListener(Class<? extends EventListener> listenerClass) {}
    public ServletRegistration.Dynamic addServlet(String servletName, String className) { return null; }
    public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) { return null; }
    public ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) { return null; }
    public <T extends Servlet> T createServlet(Class<T> c) throws ServletException { try { return c.newInstance(); } catch (Exception e) { throw new ServletException(e); } }
    public ServletRegistration getServletRegistration(String servletName) { return null; }
    public Map<String, ? extends ServletRegistration> getServletRegistrations() { return java.util.Collections.emptyMap(); }
    public FilterRegistration.Dynamic addFilter(String filterName, String className) { return null; }
    public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) { return null; }
    public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) { return null; }
    public <T extends Filter> T createFilter(Class<T> c) throws ServletException { try { return c.newInstance(); } catch (Exception e) { throw new ServletException(e); } }
    public FilterRegistration getFilterRegistration(String filterName) { return null; }
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() { return java.util.Collections.emptyMap(); }
    public SessionCookieConfig getSessionCookieConfig() { return null; }
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {}
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() { return java.util.Collections.emptySet(); }
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() { return java.util.Collections.emptySet(); }
}
