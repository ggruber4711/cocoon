/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/
package org.apache.cocoon.components.saxconnector;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.Stack;
import org.apache.avalon.activity.Disposable;
import org.apache.avalon.component.Component;
import org.apache.avalon.component.ComponentException;
import org.apache.avalon.component.ComponentManager;
import org.apache.avalon.component.Composable;
import org.apache.avalon.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.Roles;
import org.apache.cocoon.components.pipeline.EventPipeline;
import org.apache.cocoon.components.pipeline.StreamPipeline;
import org.apache.cocoon.components.url.URLFactory;
import org.apache.cocoon.environment.Environment;
import org.apache.cocoon.sitemap.Manager;
import org.apache.cocoon.xml.AbstractXMLPipe;
import org.apache.cocoon.xml.IncludeXMLConsumer;
import org.apache.cocoon.xml.XMLProducer;
import org.apache.excalibur.pool.Recyclable;
import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Copy of code from XIncludeTransformer as a starting point for XIncludeSAXConnector.
 * @author <a href="dims@yahoo.com">Davanum Srinivas</a>
 * @version CVS $Revision: 1.1.2.9 $ $Date: 2001-04-26 21:12:07 $
 */
public class XIncludeSAXConnector extends AbstractXMLPipe implements Composable, Recyclable, SAXConnector, Disposable {

    /** Stacks namespaces during processing */
    private ArrayList currentNS = new ArrayList();

    /** The current <code>ComponentManager</code>. */
    protected ComponentManager manager = null;

    public static final String XINCLUDE_NAMESPACE_URI = "http://www.w3.org/1999/XML/xinclude";
    public static final String XINCLUDE_INCLUDE_ELEMENT = "include";
    public static final String XINCLUDE_INCLUDE_ELEMENT_SRC_ATTRIBUTE = "src";
    public static final String XINCLUDE_INCLUDE_ELEMENT_ELEMENT_ATTRIBUTE = "element";
    public static final String XINCLUDE_INCLUDE_ELEMENT_NS_ATTRIBUTE = "ns";
    public static final String XINCLUDE_INCLUDE_ELEMENT_PREFIX_ATTRIBUTE = "prefix";

    /** The current <code>Environment</code>. */
    protected Environment environment;

    public void setup(EntityResolver resolver, Map objectModel,
                      String source, Parameters parameters)
            throws ProcessingException, SAXException, IOException {
        environment = (Environment) resolver;
    }

    public void compose(ComponentManager manager) {
        this.manager = manager;
    }

    public void startElement(String uri, String name, String raw, Attributes attr) throws SAXException {
        String value;

        if (uri != null && name != null && uri.equals(XINCLUDE_NAMESPACE_URI) && name.equals(XINCLUDE_INCLUDE_ELEMENT)) {
            String src = attr.getValue("",XINCLUDE_INCLUDE_ELEMENT_SRC_ATTRIBUTE);
            String element = attr.getValue("",XINCLUDE_INCLUDE_ELEMENT_ELEMENT_ATTRIBUTE);
            String ns = attr.getValue("",XINCLUDE_INCLUDE_ELEMENT_NS_ATTRIBUTE);
            String prefix = attr.getValue("",XINCLUDE_INCLUDE_ELEMENT_PREFIX_ATTRIBUTE);
            try {
                processXIncludeElement(src, element, ns, prefix);
            } catch (MalformedURLException e) {
                getLogger().debug("XIncludeSAXConnector", e);
                throw new SAXException(e);
            } catch (IOException e) {
                getLogger().debug("XIncludeSAXConnector", e);
                throw new SAXException(e);
            }
            return;
        }

        if (uri == null || uri.equals("")) {
            uri = (String)this.getNS();
        }
        this.pushNS(uri);
        super.startElement(uri, name, raw, attr);
    }

    public void endElement(String uri, String name, String raw) throws SAXException {
        if (uri != null && name != null && uri.equals(XINCLUDE_NAMESPACE_URI) && name.equals(XINCLUDE_INCLUDE_ELEMENT)) {
            return;
        }
        super.endElement((String)this.popNS(),name,raw);
    }

    private void startElem(String namespaceURI, String prefix, String name) throws SAXException {
        this.pushNS(namespaceURI);
        AttributesImpl attrs = new AttributesImpl();
        String qname = name;
        if (!namespaceURI.equals("")) {
            super.startPrefixMapping(prefix, namespaceURI);
            if (prefix!= null && !prefix.equals("")) {
                attrs.addAttribute("", prefix, "xmlns:" + prefix, "CDATA", namespaceURI);
            } else {
                attrs.addAttribute("", "xmlns", "xmlns", "CDATA", namespaceURI);
            }
        }
        super.startElement(namespaceURI, name, name, attrs);
    }

    private void endElem(String prefix, String name) throws SAXException {
        String ns = this.popNS();
        super.endElement(ns, name, name);
        if (!ns.equals("")) {
            super.endPrefixMapping(prefix);
        }
    }

    private String pushNS(String ns) {
        currentNS.add(ns);
        return ns;
    }

    private String popNS() {
        int last = currentNS.size()-1;
        String ns = (String)currentNS.get(last);
        currentNS.remove(last);
        return ns;
    }

    private String getNS() {
        int last = currentNS.size()-1;
        String ns = "";
        if (last >= 0)
            ns = (String)currentNS.get(last);
        return ns;
    }

    public void setDocumentLocator(Locator locator) {
        super.setDocumentLocator(locator);
    }

    protected void processXIncludeElement(String src, String element, String ns, String prefix)
        throws SAXException,MalformedURLException,IOException {

        Manager sitemap = null;
        EventPipeline eventPipeline = null;
        StreamPipeline pipeline = null;
        if (element == null) element="";
        if (ns == null) ns="";
        if (prefix == null) prefix="";

        try {
            sitemap = (Manager) manager.lookup(Roles.SITEMAP_MANAGER);
            getLogger().debug("Processing XInclude element: src=" + src
                                + ", sitemap=" + sitemap
                                + ", element=" + element
                                + ", ns=" + ns
                                + ", prefix=" + prefix);

            eventPipeline = (EventPipeline)this.manager.lookup(Roles.EVENT_PIPELINE);
            pipeline = (StreamPipeline)this.manager.lookup(Roles.STREAM_PIPELINE);
            pipeline.setEventPipeline(eventPipeline);

            IncludeXMLConsumer consumer = new IncludeXMLConsumer(this);

            if (!"".equals(element))
                this.startElem(ns, prefix, element);

            ((XMLProducer)eventPipeline).setConsumer(consumer);

            this.environment.pushURI(src);
            sitemap.invoke(this.environment, "", src, true, true, pipeline, eventPipeline);
            //sitemap.process(this.environment, pipeline, eventPipeline);
            eventPipeline.process(this.environment);
            this.environment.popURI();

            if (!"".equals(element))
                this.endElem(prefix, element);
        } catch (Exception e) {
            getLogger().error("Error selecting sitemap",e);
        } finally {
            if (sitemap != null)
                this.manager.release((Component)sitemap);
            if(eventPipeline != null)
                this.manager.release((Component)eventPipeline);
            if(pipeline != null)
                this.manager.release((Component)pipeline);
        }
    }

    public void dispose() {
    }

    /**
     * Recycle the producer by removing references
     */
    public void recycle () {
        this.currentNS.clear();
    }
}
