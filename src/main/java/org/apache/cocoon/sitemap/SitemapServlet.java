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
package org.apache.cocoon.sitemap;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.cocoon.ProcessingUtil;
import org.apache.cocoon.Processor;
import org.apache.cocoon.components.LifecycleHelper;
import org.apache.cocoon.components.treeprocessor.TreeProcessor;
import org.apache.cocoon.core.Settings;
import org.apache.cocoon.core.osgi.CocoonSpringBeanRegistry;
import org.apache.cocoon.environment.Context;
import org.apache.cocoon.environment.Environment;
import org.apache.cocoon.environment.http.HttpContext;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.cocoon.environment.internal.EnvironmentHelper;
import org.osgi.service.component.ComponentContext;

/**
 * Use this servlet as entry point to Cocoon. It wraps the @link {@link TreeProcessor} and delegates
 * all requests to it.
 * 
 * @version $Id$
 */
public class SitemapServlet extends HttpServlet {

	private static final String DEFAULT_CONTAINER_ENCODING = "ISO-8859-1";
	private static final String DEFAULT_SITEMAP_PATH = "/COB-INF/sitemap.xmap";	
	private static final String SITEMAP_PATH_PROPERTY = "sitemapPath";

	private CocoonSpringBeanRegistry beanFactory;
	private Logger logger;
	private String sitemapPath = DEFAULT_SITEMAP_PATH;
    protected Context cocoonContext;
	private Processor processor;	
	private Settings settings;

	/**
	 * Initialize the servlet. The main purpose of this method is creating a configured @link {@link TreeProcessor}.
	 */
    public void init(ServletConfig config) throws ServletException {
    	super.init(config);
    	
    	// get components from the beanFactory
    	this.logger = (Logger) this.beanFactory.getBean("org.apache.avalon.framework.logger.Logger");
    	this.settings = (Settings) this.beanFactory.getBean(Settings.ROLE);
        ServiceManager serviceManager = (ServiceManager) 
    		this.beanFactory.getBean("org.apache.avalon.framework.service.ServiceManager");    	
    	
    	// create the Cocoon context out of the Servlet context
        this.cocoonContext = new HttpContext(config.getServletContext());  
        
        // get the Avalon context
        org.apache.avalon.framework.context.Context avalonContext = (org.apache.avalon.framework.context.Context) 
        	this.beanFactory.getBean(ProcessingUtil.CONTEXT_ROLE);
        
        // create the tree processor
        try {
			TreeProcessor treeProcessor =  new TreeProcessor();
            treeProcessor.setBeanFactory(this.beanFactory);			
            // TODO (DF/RP) The treeProcessor doesn't need to be a managed component at all. 
            this.processor = (Processor) LifecycleHelper.setupComponent(treeProcessor,
                    this.logger,
                    avalonContext,
                    serviceManager,
                    createTreeProcessorConfiguration());
		} catch (Exception e) {
            throw new ServletException(e);
		}
		
    }
    
    /**
     * Process the incoming request using the Cocoon tree processor.
     */
	protected void service(HttpServletRequest request, HttpServletResponse response) 
		throws ServletException, IOException {
		
		Environment environment = createCocoonEnvironment(request, response);
		try {
	        EnvironmentHelper.enterProcessor(this.processor, environment);			
			this.processor.process(environment);
		} catch (Exception e) {
			throw new ServletException(e);
		} finally { 
            EnvironmentHelper.leaveProcessor();	
	    } 
	}    
	
	/**
	 * This method takes the servlet request and response and creates a Cocoon 
	 * environment (@link {@link Environment}) out of it.
	 */
    protected Environment createCocoonEnvironment(HttpServletRequest req, 
    		HttpServletResponse res) throws IOException  {
    	
		String uri = req.getPathInfo();

		String formEncoding = req.getParameter("cocoon-form-encoding");
		if (formEncoding == null) {
			formEncoding = this.settings.getFormEncoding();
		}
		HttpEnvironment env = new HttpEnvironment(uri, req, res, this.getServletContext(),
				this.cocoonContext, DEFAULT_CONTAINER_ENCODING, formEncoding);
		
		env.enableLogging(this.logger);
		return env;
	}
    
    /**
     * Create an Avalon Configuration @link {@link Configuration} that configures the tree processor.
     */
	private Configuration createTreeProcessorConfiguration() {
		DefaultConfiguration treeProcessorConf = new DefaultConfiguration("treeProcessorConfiguration");
        treeProcessorConf.setAttribute("check-reload", true);
        treeProcessorConf.setAttribute("file", this.sitemapPath);
		return treeProcessorConf;
	}	    

	/**
	 * Get a Spring BeanFactory injected by OSGi declarative services.
	 */
	protected void setBeanFactory(CocoonSpringBeanRegistry beanFactory) {
		this.beanFactory = beanFactory;
	}
	
	/**
	 * Set the path of the sitemap
	 */
	public void setSitemapPath(String sitemapPath) {
		this.sitemapPath = sitemapPath;
	}	

	/**
	 * This Method is used in an OSGi environment to activate this servlet as bundle.
	 * 
	 * @param componentContext - The component context is made available and gives access to OSGi framework information
	 */
	protected void activate(ComponentContext componentContext) throws Exception {
		this.setSitemapPath((String) componentContext.getProperties().get(SITEMAP_PATH_PROPERTY));			
	}
	
}
