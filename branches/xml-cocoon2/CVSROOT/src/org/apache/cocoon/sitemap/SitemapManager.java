/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/
package org.apache.cocoon.sitemap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException; 
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.avalon.ComponentManager;
import org.apache.avalon.Composer;
import org.apache.avalon.Configurable;
import org.apache.avalon.Configuration;

import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.Environment;
import org.apache.cocoon.sitemap.SitemapHandler;

import org.xml.sax.SAXException;

/**
 * This class manages all sub <code>Sitemap</code>s of a <code>Sitemap</code>
 * Invokation of sub sitemaps will be done by this instance as well 
 * checking regeneration of the sub <code>Sitemap</code>
 *
 * @author <a href="mailto:Giacomo.Pati@pwr.ch">Giacomo Pati</a>
 * @version CVS $Revision: 1.1.2.4 $ $Date: 2000-07-27 21:49:05 $
 */
public class SitemapManager implements Configurable, Composer {

    /** The vectors of sub sitemaps */
    private Hashtable sitemaps = new Hashtable();

    /** The configuration */
    private Configuration conf = null;

    /** The component manager */
    private ComponentManager manager = null;

    public SitemapManager () {
    }

    public void setConfiguration (Configuration conf) {
        this.conf = conf;
    }

    public void setComponentManager (ComponentManager manager) {
        this.manager = manager;
System.out.println ("SitemapManager.setComponentManager: manager is "
+((this.manager == null)?"null":"set"));
    }

    public boolean invoke (Environment environment, String uri_prefix, 
                           String source, boolean check_reload, OutputStream out) 
    throws Exception {
        SitemapHandler sitemapHandler = (SitemapHandler) sitemaps.get (source);
        System.out.println ("SitemapManager.invoke(\""+uri_prefix+"\",\""+source+"\")");
        if (sitemapHandler != null) {
            System.out.println ("SitemapManager.invoke: SitemapHandler found");
            sitemapHandler.throwError();
            if (sitemapHandler.available()) {
                if (check_reload 
                 && sitemapHandler.hasChanged()
                 && !sitemapHandler.isRegenerating()) {
                    sitemapHandler.regenerateAsynchroniously(environment);
                }
                environment.changeContext (uri_prefix, source);
                return sitemapHandler.process (environment, out);
            } else {
                sitemapHandler.regenerate(environment);
            }
            System.out.println ("SitemapManager.invoke: setting uri prefix");
            environment.changeContext (uri_prefix, source);
            return sitemapHandler.process (environment, out);
        } else {
            System.out.println ("SitemapManager.invoke: instantiating SitemapHandler");
            sitemapHandler = new SitemapHandler(source, check_reload);
            if (sitemapHandler instanceof Composer) sitemapHandler.setComponentManager (this.manager);
            if (sitemapHandler instanceof Configurable) sitemapHandler.setConfiguration (this.conf); 
            sitemaps.put(source, sitemapHandler);
            sitemapHandler.regenerate(environment); 
            System.out.println ("SitemapManager.invoke: setting uri prefix");
            environment.changeContext (uri_prefix, source);
            return sitemapHandler.process (environment, out);
        }
    }

    public boolean hasChanged () {
        System.out.print("SitemapManager.hasChanged() = ");
        SitemapHandler sitemapHandler = null;
        Enumeration enum = sitemaps.elements();
        while (enum.hasMoreElements()) {
            sitemapHandler = (SitemapHandler) enum.nextElement ();
            if (sitemapHandler != null) {
                System.out.println((sitemapHandler.hasChanged()?"true":"false"));
                if (sitemapHandler.hasChanged())
                    return true;
            }
        }
        System.out.println("true");
        return true;
    }
}
