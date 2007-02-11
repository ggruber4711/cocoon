/*
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache Cocoon" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.cocoon.components.source.impl;

import org.apache.excalibur.xml.xpath.XPathProcessor;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.parameters.Parameterizable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.avalon.framework.thread.ThreadSafe;
import org.apache.cocoon.components.source.SourceInspector;
import org.apache.cocoon.components.source.helpers.SourceProperty;
import org.apache.excalibur.source.Source;
import org.apache.excalibur.source.SourceException;
import org.apache.excalibur.xml.dom.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * This source inspector inspects XML files with a xpath expression
 *
 * @author <a href="mailto:stephan@apache.org">Stephan Michels</a>
 * @version CVS $Id: XPathSourceInspector.java,v 1.4 2003/05/19 10:35:03 stephan Exp $
 */
public class XPathSourceInspector extends AbstractLogEnabled implements 
    SourceInspector, ThreadSafe, Parameterizable {

    private String propertynamespace = "http://apache.org/cocoon/inspector/xpath/1.0";
    private String propertyname = "result";
    private String extension = null;
    private String xpath = null;

    private ComponentManager manager = null;

    public void compose(ComponentManager manager) {
        this.manager = manager;
    }

    public void parameterize(Parameters params)  {
        this.propertynamespace = params.getParameter("namespace", 
            "http://apache.org/cocoon/inspector/xpath/1.0");
        this.propertyname = params.getParameter("name", "result");
        this.extension = params.getParameter("extension", ".xml");
        this.xpath = params.getParameter("xpath", "/*");
    }

    public SourceProperty getSourceProperty(Source source, String namespace, String name) 
        throws SourceException {

        if ((namespace.equals(propertynamespace)) && (name.equals(propertyname)) && 
            (source.getURI().endsWith(extension))) {

                DOMParser parser = null;
            Document doc = null;
            try { 
                parser = (DOMParser)manager.lookup(DOMParser.ROLE);

                doc = parser.parseDocument(new InputSource(source.getInputStream()));
            } catch (SAXException se) {
                this.getLogger().error(source.getURI()
                                        + " is not a valid XML file");
            } catch (IOException ioe) {
                this.getLogger().error("Could not read file", ioe);
            } catch (ComponentException ce) {
                this.getLogger().error("Could not retrieve component", ce);
            } finally {
                if (parser!=null)
                    this.manager.release((Component)parser);
            }

            if (doc != null) {

                XPathProcessor processor = null;
                try {
                    processor = (XPathProcessor)manager.lookup(XPathProcessor.ROLE);

                    NodeList nodelist = processor.selectNodeList(doc.getDocumentElement(), this.xpath);

                    SourceProperty property = new SourceProperty(this.propertynamespace, this.propertyname);
                    property.setValue(nodelist);

                    return property;
                } catch (ComponentException ce) {
                    this.getLogger().error("Could not retrieve component", ce);
                } finally {
                    if ((processor!=null) && (processor instanceof Component))
                        this.manager.release((Component)processor);
                }
            }
        } 
        return null;  
    }

    public SourceProperty[] getSourceProperties(Source source) throws SourceException {

        SourceProperty property = getSourceProperty(source, this.propertynamespace, this.propertyname);
        if (property!=null)
            return new SourceProperty[]{property};
        return null;
    }

}
