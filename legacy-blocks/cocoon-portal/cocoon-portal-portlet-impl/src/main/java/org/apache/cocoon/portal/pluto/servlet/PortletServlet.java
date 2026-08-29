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
package org.apache.cocoon.portal.pluto.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.Portlet;
import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.UnavailableException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.pluto.core.CoreUtils;
import org.apache.pluto.core.InternalPortletRequest;
import org.apache.pluto.core.InternalPortletResponse;
import org.apache.pluto.factory.PortletObjectAccess;
import org.apache.pluto.om.ControllerObjectAccess;
import org.apache.pluto.om.portlet.PortletDefinition;
import org.apache.pluto.om.portlet.PortletDefinitionCtrl;

/**
 * The wrapper servlet.
 *
 * @version $Id$
 */
public class PortletServlet extends HttpServlet {

    public static final String PORTLET_DEFINITION = PortletServlet.class.getName() + "/portlet-definition";

    protected ServletConfig servletConfig;
    protected Map portlets = new HashMap();

    /**
     * @see jakarta.servlet.GenericServlet#init(jakarta.servlet.ServletConfig)
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.servletConfig = config;
    }

    protected synchronized void init(PortletDefinition pd, HttpServletRequest request)
    throws ServletException {
        final String guid = pd.getId().toString();
        PortletInfo info = (PortletInfo)this.portlets.get(guid);
        if ( info == null ) {
            info = new PortletInfo();
            try {
                info.portlet = (Portlet)Thread.currentThread().getContextClassLoader().loadClass(pd.getClassName()).newInstance();
            } catch (ClassNotFoundException e) {
                throw new ServletException(e);
            } catch (IllegalAccessException e) {
                throw new ServletException(e);
            } catch (InstantiationException e) {
                throw new ServletException(e);
            }
            // TBD - I should not use
            PortletDefinitionCtrl portletDefCtrl = (PortletDefinitionCtrl)ControllerObjectAccess.get(pd);
            portletDefCtrl.setPortletClassLoader(Thread.currentThread().getContextClassLoader());
    
            info.context = PortletObjectAccess.getPortletContext(this.servletConfig.getServletContext(),
                                                                   pd.getPortletApplicationDefinition());
            info.config = PortletObjectAccess.getPortletConfig(this.servletConfig, 
                                                               info.context,
                                                               pd);

            try {
                info.portlet.init(info.config);
            } catch (PortletException e) {
                throw new ServletException(e);
            }
            this.portlets.put(guid, info);
        }
    }

    /**
     * @see jakarta.servlet.http.HttpServlet#getLastModified(jakarta.servlet.http.HttpServletRequest)
     */
    protected long getLastModified(HttpServletRequest req) {
        return -1;
    }

    /**
     * @see jakarta.servlet.http.HttpServlet#service(jakarta.servlet.ServletRequest, jakarta.servlet.ServletResponse)
     */
    public final void service(ServletRequest request, ServletResponse response)
    throws ServletException, IOException {
        super.service(request,response);
    }

    /**
     * @see jakarta.servlet.http.HttpServlet#doGet(jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse)
     */
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {
        dispatch(req,resp);
    }

    /**
     * @see jakarta.servlet.http.HttpServlet#doPost(jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse)
     */
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {
        dispatch(req,resp);
    }

    /**
     * @see jakarta.servlet.http.HttpServlet#doPut(jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse)
     */
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {
        dispatch(req,resp);
    }

    /**
     * @see jakarta.servlet.http.HttpServlet#doDelete(jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse)
     */
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {
        super.doDelete(req,resp);
    }

    /**
     * @see jakarta.servlet.http.HttpServlet#doOptions(jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse)
     */
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {
        super.doOptions(req,resp);
    }

    /**
     * @see jakarta.servlet.http.HttpServlet#doTrace(jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse)
     */
    protected void doTrace(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {
        super.doTrace(req,resp);
    }

    /**
     * @see jakarta.servlet.GenericServlet#destroy()
     */
    public void destroy() {
        synchronized (this.portlets) {
            final Iterator i = this.portlets.values().iterator();
            while (i.hasNext()) {
                final PortletInfo info = (PortletInfo)i.next();
                try {
                    info.portlet.destroy();
                } catch (Throwable ignore) {
                    // we just ignore this
                }
            }
            this.portlets.clear();
        }
        super.destroy();
    }

    /**
     * Dispatcher.
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    protected void dispatch(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        final PortletDefinition pd = (PortletDefinition)request.getAttribute(PORTLET_DEFINITION);
        final Integer method_id = (Integer)request.getAttribute(org.apache.pluto.Constants.METHOD_ID);
        if (method_id == org.apache.pluto.Constants.METHOD_NOOP ) {
            this.init(pd, request);
            return;
        }
        final PortletInfo info = (PortletInfo)this.portlets.get(pd.getId().toString());
        if (info == null) {
            throw new ServletException("The portlet '" + pd.getId() + "' is not initialized.");
        }
        try {
            // fill attribute, so that JSPs/servlets can access the config
            request.setAttribute(org.apache.pluto.Constants.PORTLET_CONFIG, info.config);

            if (method_id == org.apache.pluto.Constants.METHOD_RENDER) {
                RenderRequest renderRequest = (RenderRequest)request.getAttribute(org.apache.pluto.Constants.PORTLET_REQUEST);
                RenderResponse renderResponse = (RenderResponse)request.getAttribute(org.apache.pluto.Constants.PORTLET_RESPONSE);

                // prepare container objects to run in this webModule
                prepareRenderRequest(renderRequest, request);
                prepareRenderResponse(renderResponse, request, response);

                info.portlet.render(renderRequest,renderResponse);
            } else if (method_id==org.apache.pluto.Constants.METHOD_ACTION) {
                ActionRequest actionRequest = (ActionRequest)request.getAttribute(org.apache.pluto.Constants.PORTLET_REQUEST);
                ActionResponse actionResponse = (ActionResponse)request.getAttribute(org.apache.pluto.Constants.PORTLET_RESPONSE);

                // prepare container objects to run in this webModule
                prepareActionRequest(actionRequest, request);
                prepareActionResponse(actionResponse, request, response);

                info.portlet.processAction(actionRequest,actionResponse);
            }
        } catch (UnavailableException e) {
            // destroy isn't called by Tomcat, so we have to fix it
            this.portlets.remove(pd.getId().toString());
            try {
                info.portlet.destroy();
            } catch (Throwable t) {
                // don't care for Exception
            }

            // handle everything as permanently for now
            throw new jakarta.servlet.UnavailableException(e.getMessage());
        } catch (PortletException e) {
            throw new ServletException(e);
        } finally {
            request.removeAttribute(org.apache.pluto.Constants.PORTLET_CONFIG);
        }
    }

    protected void prepareActionRequest(ActionRequest portletRequest,
                                      HttpServletRequest servletRequest) {
        InternalPortletRequest internalPortletRequest = 
        CoreUtils.getInternalRequest(portletRequest);

        internalPortletRequest.lateInit(servletRequest);
    }

    protected void prepareRenderRequest(RenderRequest portletRequest,
                                      HttpServletRequest servletRequest) {
        InternalPortletRequest internalPortletRequest = 
        CoreUtils.getInternalRequest(portletRequest);

        internalPortletRequest.lateInit(servletRequest);
    }

    protected void prepareRenderResponse(RenderResponse portletResponse,
                                       HttpServletRequest servletRequest,
                                       HttpServletResponse servletResponse) {
        InternalPortletResponse internalPortletResponse = 
        CoreUtils.getInternalResponse(portletResponse);

        internalPortletResponse.lateInit(servletRequest, servletResponse);
    }

    protected void prepareActionResponse(ActionResponse portletResponse,
                                       HttpServletRequest servletRequest,
                                       HttpServletResponse servletResponse) {
        InternalPortletResponse internalPortletResponse = 
        CoreUtils.getInternalResponse(portletResponse);

        internalPortletResponse.lateInit(servletRequest, servletResponse);
    }

    protected final static class PortletInfo {
        public Portlet portlet;
        public PortletContext context;
        public PortletConfig config;
    }
}
