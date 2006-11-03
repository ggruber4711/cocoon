/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.cocoon.classloader;

import java.io.File;
import java.net.URL;
import java.util.List;

import javax.servlet.ServletContext;

import org.apache.cocoon.classloader.fam.SitemapMonitor;
import org.apache.commons.jci.listeners.ReloadingListener;
import org.apache.commons.jci.stores.ResourceStore;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @see AbstractClassLoaderFactory
 * @version $Id$
 * @since 2.2
 */
public class ReloadingClassLoaderFactory extends AbstractClassLoaderFactory {

    protected ClassLoaderConfiguration config;
    private final static Log log = LogFactory.getLog(ReloadingListener.class);
    
    public ClassLoader createClassLoader(ClassLoader parent,
            ClassLoaderConfiguration config, ServletContext servletContext)
            throws Exception {
        this.config = config;
        return super.createClassLoader(parent, config, servletContext);
         }

    protected ClassLoader createClassLoader(URL[] urls, List includePatterns,
            List excludePatterns, ClassLoader parent) {

        org.apache.commons.jci.ReloadingClassLoader jciClassLoader = new org.apache.commons.jci.ReloadingClassLoader(
                new DefaultClassLoader(urls, includePatterns,
                        excludePatterns, Thread.currentThread()
                                .getContextClassLoader()));


        SitemapMonitor fam = this.config.getSitemapMonitor();
        
        for (int i = 0; i < urls.length; i++) {
            URL url = urls[i];
            final ResourceStore store = (ResourceStore)this.config.getStore(url.getFile());

            final ReloadingListener listener = createReloadingListener(url,
                    store, this.config);
            jciClassLoader.addListener(listener);
            fam.subscribe(listener);
            try {
                listener.waitForFirstCheck();
            } catch (Exception e) {
                log.error("Timeout error configuring JCI Listener for url "
                        + url + " having store " + store);
            }
            log.debug("ReloadingClassLoaderFactory - Subscriber SitemapMonitor listener for url "
                            + url + " having store " + store);
        }
        return jciClassLoader;
    }


    private ReloadingListener createReloadingListener(final URL dir,
            final ResourceStore store, ClassLoaderConfiguration configuration) {
        final File repository = new File(dir.getFile());

        if (store instanceof PatternMatcherResourceStore) {
            PatternMatcherResourceStore mstore = (PatternMatcherResourceStore) store;
            mstore.setExcludes(configuration.getExcludes());
            mstore.setIncludes(configuration.getIncludes());
        }
        return new ReloadingListener(repository, store);
    }
}