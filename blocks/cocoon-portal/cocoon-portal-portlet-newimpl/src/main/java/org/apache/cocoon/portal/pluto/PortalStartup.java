/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cocoon.portal.pluto;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pluto.PortletContainer;
import org.apache.pluto.PortletContainerException;
import org.apache.pluto.PortletContainerFactory;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * @version $Id$
 */
public class PortalStartup extends AbstractLogEnabled
    implements ServletContextAware {

    protected ServletContext servletContext;

    protected String uniqueContainerName = "cocoon-portal";

    public static final String CONTAINER_KEY = PortalStartup.class.getName() + "/Container";

    /**
     */
    public void init() {
        if ( this.getLogger().isInfoEnabled() ) {
            this.getLogger().info("Starting up Pluto Portal Driver...");
        }
        this.initContainer();
        if ( this.getLogger().isInfoEnabled() ) {
            this.getLogger().info("********** Pluto Portal Driver Started **********\n\n");
        }
    }

    /**
     */
    public void destroy() {
        if (this.getLogger().isInfoEnabled()) {
            this.getLogger().info("Shutting down Pluto Portal Driver...");
        }
        this.destroyContainer();
        if (this.getLogger().isInfoEnabled()) {
            this.getLogger().info("********** Pluto Portal Driver Shut Down **********\n\n");
        }
    }
    
    /**
     * Initializes the portlet container. This method constructs and initializes
     * the portlet container, and saves it to the servlet context scope.
     * @param servletContext  the servlet context.
     */
    private void initContainer() {
        
    	// Retrieve the driver configuration from servlet context.
    	//DriverConfiguration driverConfig = (DriverConfiguration)
        //		servletContext.getAttribute(DRIVER_CONFIG_KEY);
        
        try {
        	
        	// Create portal context.
            if (this.getLogger().isDebugEnabled()) {
           //     this.getLogger().debug("Creating portal context ["
           //     		+ driverConfig.getPortalName() + "/"
           //             + driverConfig.getPortalVersion() + "]...");
            }
           // PortalContextImpl portalContext =
           // 		new PortalContextImpl(driverConfig);
            
            // Create container services.
            if (this.getLogger().isDebugEnabled()) {
                this.getLogger().debug("Creating container services...");
            }
          //  ContainerServicesImpl containerServices =
          //  		new ContainerServicesImpl(portalContext, driverConfig);
            
            // Create portlet container.
            if (this.getLogger().isDebugEnabled()) {
                this.getLogger().debug("Creating portlet container...");
            }
            PortletContainerFactory factory =
         		PortletContainerFactory.getInstance();
            PortletContainer container = factory.createContainer(
                    this.uniqueContainerName,
                    null,
                    null);
            
            // Initialize portlet container.
            if (this.getLogger().isDebugEnabled()) {
                this.getLogger().debug("Initializing portlet container...");
            }
            container.init(servletContext);
            
            // Save portlet container to the servlet context scope.
            servletContext.setAttribute(CONTAINER_KEY, container);
            if (this.getLogger().isInfoEnabled()) {
                this.getLogger().info("Pluto portlet container started.");
            }
            
        } catch (PortletContainerException ex) {
            this.getLogger().error("Unable to start up portlet container: "
            		+ ex.getMessage(), ex);
        }
    }
    
    
    // Private Destruction Methods ---------------------------------------------
    
    /**
     * Destroyes the portlet container and removes it from servlet context.
     * @param servletContext  the servlet context.
     */
    private void destroyContainer() {
        if (this.getLogger().isInfoEnabled()) {
            this.getLogger().info("Shutting down Pluto Portal Driver...");
        }
        PortletContainer container = (PortletContainer)
                servletContext.getAttribute(CONTAINER_KEY);
        if (container != null) {
            try {
                container.destroy();
                if (this.getLogger().isInfoEnabled()) {
                    this.getLogger().info("Pluto Portal Driver shut down.");
                }
            } catch (PortletContainerException ex) {
                this.getLogger().error("Unable to shut down portlet container: "
                        + ex.getMessage(), ex);
            } finally {
                servletContext.removeAttribute(CONTAINER_KEY);
            }
        }
    }

    /**
     * @see org.springframework.web.context.ServletContextAware#setServletContext(javax.servlet.ServletContext)
     */
    public void setServletContext(ServletContext context) {
        this.servletContext = context;
    }
}

