/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/
package org.apache.cocoon.transformation;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.StringCharacterIterator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Iterator;
import java.util.Map;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.Loggable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.Constants;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.Roles;
import org.apache.cocoon.caching.CacheValidity;
import org.apache.cocoon.caching.Cacheable;
import org.apache.cocoon.caching.CompositeCacheValidity;
import org.apache.cocoon.caching.ParametersCacheValidity;
import org.apache.cocoon.caching.TimeStampCacheValidity;
import org.apache.cocoon.components.browser.Browser;
import org.apache.cocoon.components.store.Store;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.util.HashUtil;
import org.apache.cocoon.util.TraxErrorHandler;
import org.apache.cocoon.xml.ContentHandlerWrapper;
import org.apache.cocoon.xml.XMLConsumer;
import org.apache.avalon.excalibur.pool.Poolable;
import org.apache.avalon.excalibur.pool.Recyclable;
import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

/**
 *
 * @author <a href="mailto:fumagalli@exoffice.com">Pierpaolo Fumagalli</a>
 *         (Apache Software Foundation, Exoffice Technologies)
 * @author <a href="mailto:dims@yahoo.com">Davanum Srinivas</a>
 * @author <a href="mailto:cziegeler@apache.org">Carsten Ziegeler</a>
 * @version CVS $Revision: 1.1.2.29 $ $Date: 2001-04-30 14:17:45 $
 */
public class TraxTransformer extends ContentHandlerWrapper
implements Transformer, Composable, Recyclable, Configurable, Cacheable, Disposable {
    private static String FILE = "file:/";

    /** The store service instance */
    private Store store = null;

    /** The Browser service instance */
    private Browser browser = null;

    /** The trax TransformerFactory */
    private SAXTransformerFactory tfactory = null;

    /** The trax TransformerHandler */
    private TransformerHandler transformerHandler = null;

    /** Is the store turned on? (default is on) */
    private boolean useStore = true;

    private ComponentManager manager;

    /** The InputSource */
    private InputSource inputSource;
    private String systemID;
    private String xsluri;
    private Parameters par;
    private Map objectModel;
    private EntityResolver resolver;

    TransformerHandler getTransformerHandler(EntityResolver resolver)
      throws SAXException, ProcessingException, IOException, TransformerConfigurationException
    {
        Templates templates = getTemplates(systemID, xsluri);
        if(templates == null) {
            getLogger().debug("Creating new Templates in " + this + " for" + systemID + ":" + xsluri);
            templates = getTransformerFactory().newTemplates(new SAXSource(new InputSource(systemID)));
            putTemplates (systemID, xsluri, templates);
        } else {
            getLogger().debug("Reusing Templates in " + this + " for" + systemID + ":" + xsluri);
        }

        TransformerHandler handler = getTransformerFactory().newTransformerHandler(templates);
        if(handler == null) {
            /* If there is a problem in getting the handler, try using a brand new Templates object */
            getLogger().debug("Re-creating new Templates in " + this + " for" + systemID + ":" + xsluri);
            templates = getTransformerFactory().newTemplates(new SAXSource(new InputSource(systemID)));
            putTemplates (systemID, xsluri, templates);
            handler = getTransformerFactory().newTransformerHandler(templates);
        }

        handler.getTransformer().setErrorListener(new TraxErrorHandler(getLogger()));
        return handler;
    }

    private Templates getTemplates (String systemID, String xsluri)
       throws IOException
    {
        Templates templates = null;
        if (this.useStore == false)
            return null;

        // Only local files are checked for modification for compatibility reasons!
        // Using the entity resolver we get the filename of the current file:
        // The systemID if such a resource starts with file://.

        // Is this a local file
        if (systemID.startsWith(FILE) == true) {
            // Stored is an array of the template and the caching time
            if (store.containsKey(xsluri) == true) {
                Object[] templateAndTime = (Object[])store.get(xsluri);
                if(templateAndTime != null && templateAndTime[1] != null) {
                    File xslFile = new File(systemID.substring(FILE.length()));
                    long storedTime = ((Long)templateAndTime[1]).longValue();
                    if (storedTime < xslFile.lastModified()) {
                        templates = null;
                    } else {
                        templates = (Templates)templateAndTime[0];
                    }
                }
            }
        } else {
            // only the template is stored
            if (store.containsKey(xsluri) == true) {
               templates = (Templates)store.get(xsluri);
            }
        }
        return templates;
    }

    private void putTemplates (String systemID, String xsluri, Templates templates)
       throws IOException
    {
        if (this.useStore == false)
            return;

        // Is this a local file
        if (systemID.startsWith(FILE) == true) {
            // Stored is an array of the template and the current time
            Object[] templateAndTime = new Object[2];
            templateAndTime[0] = templates;
            templateAndTime[1] = new Long(System.currentTimeMillis());
            store.hold(xsluri, templateAndTime);
        } else {
            store.hold(xsluri,templates);
        }
    }

    /**
     * Helper for TransformerFactory.
     */
    private synchronized SAXTransformerFactory getTransformerFactory()
    {
        if(tfactory == null)  {
            tfactory = (SAXTransformerFactory) TransformerFactory.newInstance();
            tfactory.setErrorListener(new TraxErrorHandler(getLogger()));
        }
        return tfactory;
    }

    /**
     * Configure this transformer.
     */
    public void configure(Configuration conf)
    throws ConfigurationException {
        if (conf != null) {
            Configuration child = conf.getChild("use-store");
            this.useStore = child.getValueAsBoolean(true);
            getLogger().debug("Use store is " + this.useStore + " for " + this);
        }
    }

    /**
     * Set the current <code>ComponentManager</code> instance used by this
     * <code>Composable</code>.
     */
    public void compose(ComponentManager manager) {
        try {
            this.manager = manager;
            getLogger().debug("Looking up " + Roles.STORE);
            this.store = (Store) manager.lookup(Roles.STORE);
            getLogger().debug("Looking up " + Roles.BROWSER);
            this.browser = (Browser) manager.lookup(Roles.BROWSER);
        } catch (Exception e) {
            getLogger().error("Could not find component", e);
        }
    }

    /**
     * Set the <code>EntityResolver</code>, the <code>Map</code> with
     * the object model, the source and sitemap
     * <code>Parameters</code> used to process the request.
     */
    public void setup(EntityResolver resolver, Map objectModel, String src, Parameters par)
    throws SAXException, ProcessingException, IOException {

        // Check the stylesheet uri
        this.xsluri = src;
        if (this.xsluri == null) {
            throw new ProcessingException("Stylesheet URI can't be null");
        }
        this.par = par;
        this.objectModel = objectModel;
        this.inputSource = resolver.resolveEntity(null, this.xsluri);
        this.systemID = inputSource.getSystemId();
        this.resolver = resolver;
    }

    /**
     * Generate the unique key.
     * This key must be unique inside the space of this component.
     *
     * @return The generated key hashes the src
     */
    public long generateKey() {
        if (this.systemID.startsWith("file:") == true) {
            return HashUtil.hash(this.xsluri);
        } else {
            return 0;
        }
    }

    /**
     * Generate the validity object.
     *
     * @return The generated validity object or <code>null</code> if the
     *         component is currently not cacheable.
     */
    public CacheValidity generateValidity() {
        HashMap map = getLogicSheetParameters();

        if (this.systemID.startsWith("file:") == true) {
            File xslFile = new File(this.systemID.substring("file:".length()));
            return new CompositeCacheValidity(
                                    new ParametersCacheValidity(map),
                                    new TimeStampCacheValidity(xslFile.lastModified())
                                    );
        }
        return null;
    }

    /**
     * Set the <code>XMLConsumer</code> that will receive XML data.
     * <br>
     * This method will simply call <code>setContentHandler(consumer)</code>
     * and <code>setLexicalHandler(consumer)</code>.
     */
    public void setConsumer(XMLConsumer consumer) {

        /** Get a Transformer Handler */
        try {
            transformerHandler = getTransformerHandler(resolver);
        } catch (TransformerConfigurationException e){
            getLogger().error("Problem in getTransformer:", e);
            throw new RuntimeException("Problem in getTransformer:" + e.getMessage());
        } catch (SAXException e){
            getLogger().error("Problem in getTransformer:", e);
            throw new RuntimeException("Problem in getTransformer:" + e.getMessage());
        } catch (IOException e){
            getLogger().error("Problem in getTransformer:", e);
            throw new RuntimeException("Problem in getTransformer:" + e.getMessage());
        } catch (ProcessingException e){
            getLogger().error("Problem in getTransformer:", e);
            throw new RuntimeException("Problem in getTransformer:" + e.getMessage());
        }

        HashMap map = getLogicSheetParameters();
        Iterator iterator = map.keySet().iterator();
        while(iterator.hasNext()) {
            String name = (String)iterator.next();
            transformerHandler.getTransformer().setParameter(name,map.get(name));
        }

        super.setContentHandler(transformerHandler);
        if(transformerHandler instanceof Loggable) {
            ((Loggable)transformerHandler).setLogger(getLogger());
        }
        if(transformerHandler instanceof org.xml.sax.ext.LexicalHandler)
            this.setLexicalHandler((org.xml.sax.ext.LexicalHandler)transformerHandler);

        this.setContentHandler(consumer);
    }

    private HashMap getLogicSheetParameters()
    {
        HashMap map = new HashMap();

        /** The Request object */
        Request request = (Request) objectModel.get(Constants.REQUEST_OBJECT);

        if (request != null) {
            Enumeration parameters = request.getParameterNames();
            if ( parameters != null ) {
                while (parameters.hasMoreElements()) {
                    String name = (String) parameters.nextElement();
                    if (isValidXSLTParameterName(name)) {
                        String value = request.getParameter(name);
                        map.put(name,value);
                    }
                }
            }
        }

        if (par != null) {
            Iterator params = par.getParameterNames();
            while (params.hasNext()) {
                String name = (String) params.next();
                if (isValidXSLTParameterName(name)) {
                    String value = par.getParameter(name,null);
                    if (value != null) {
                        map.put(name,value);
                    }
                }
            }
        }

// FIXME(DIMS): Commented out pending discussion.
//        try
//        {
//            /* Get the accept header; it's needed to get the browser type. */
//            String accept = request.getParameter("accept");
//            if (accept == null)
//                accept = request.getHeader("accept");
//
//            /* Get the user agent; it's needed to get the browser type. */
//            String agent = request.getParameter("user-Agent");
//            if (agent == null)
//                agent = request.getHeader("user-Agent");
//
//            /* add the accept param */
//            transformerHandler.getTransformer().setParameter("accept", accept);
//
//            /* add the user agent param */
//            transformerHandler.getTransformer().setParameter("user-agent", java.net.URLEncoder.encode(agent));
//
//            /* add the map param */
//            HashMap map = browser.getBrowser(agent, accept);
//            transformerHandler.getTransformer().setParameter("browser",map);
//
//            /* add the media param */
//            String browserMedia = browser.getMedia(map);
//            transformerHandler.getTransformer().setParameter("browser-media",map);
//
//            /* add the uaCapabilities param */
//            org.w3c.dom.Document uaCapabilities = browser.getUACapabilities(map);
//            transformerHandler.getTransformer().setParameter("ua-capabilities", uaCapabilities);
//        } catch (Exception e) {
//            /** FIXME - i don't have a logger
//                (GP) now you have one :) */
//            getLogger().error("Error setting Browser info", e);
//        }

        return map;
    }

    /**
     * Set the <code>ContentHandler</code> that will receive XML data.
     * <br>
     * Subclasses may retrieve this <code>ContentHandler</code> instance
     * accessing the protected <code>super.contentHandler</code> field.
     */
    public void setContentHandler(ContentHandler content) {
        transformerHandler.setResult(new SAXResult(content));
    }

    /**
     * Set the <code>LexicalHandler</code> that will receive XML data.
     * <br>
     * Subclasses may retrieve this <code>LexicalHandler</code> instance
     * accessing the protected <code>super.lexicalHandler</code> field.
     *
     * @exception IllegalStateException If the <code>LexicalHandler</code> or
     *                                  the <code>XMLConsumer</code> were
     *                                  already set.
     */
    public void setLexicalHandler(LexicalHandler lexical) {
    }

    // FIXME (SM): this method may be a hotspot for requests with many
    //             parameters we should try to optimize it further
    static boolean isValidXSLTParameterName(String name) {
        StringCharacterIterator iter = new StringCharacterIterator(name);
        char c = iter.first();
        if (!(Character.isLetter(c) || c == '_')) {
            return false;
        } else {
            c = iter.next();
        }
        while (c != iter.DONE) {
            if (!(Character.isLetterOrDigit(c) ||
                c == '-' ||
                c == '_' ||
                c == '.')) {
                return false;
            } else {
                c = iter.next();
            }
        }

        return true;
    }

    public void dispose()
    {
        if(this.store != null)
            this.manager.release((Component)this.store);
        if(this.browser != null)
            this.manager.release((Component)this.browser);
    }

    public void recycle()
    {
        //FIXME: Patch for Xalan2J, to stop transform threads if
        //       there is a failure in the pipeline.
        try {
            Class clazz =
                Class.forName("org.apache.xalan.stree.SourceTreeHandler");
            Class  paramTypes[] =
                    new Class[]{ Exception.class };
            Object params[] =
                    new Object[] { new SAXException("Dummy Exception") };
            if(clazz.isInstance(transformerHandler)) {
                Method method = clazz.getMethod("setExceptionThrown",paramTypes);
                method.invoke(transformerHandler,params);
            }
        } catch (Exception e){
            getLogger().debug("Exception in recycle:", e);
        }
        this.transformerHandler = null;
        this.objectModel = null;
        this.inputSource = null;
        this.par = null;
        this.systemID = null;
        this.xsluri = null;
        this.resolver = null;
        super.recycle();
    }
}
