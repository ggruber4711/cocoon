/*

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

 Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.

 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:

 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:  "This product includes  software
    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
    Alternately, this  acknowledgment may  appear in the software itself,  if
    and wherever such third-party acknowledgments normally appear.

 4. The names "Apache Cocoon" and  "Apache Software Foundation" must  not  be
    used to  endorse or promote  products derived from  this software without
    prior written permission. For written permission, please contact
    apache@apache.org.

 5. Products  derived from this software may not  be called "Apache", nor may
    "Apache" appear  in their name,  without prior written permission  of the
    Apache Software Foundation.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation and was  originally created by
 Stefano Mazzocchi  <stefano@apache.org>. For more  information on the Apache
 Software Foundation, please see <http://www.apache.org/>.

*/
package org.apache.cocoon.components.source;

import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.logger.Logger;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.ResourceNotFoundException;
import org.apache.cocoon.components.source.helpers.SourceCredential;
import org.apache.cocoon.environment.Environment;
import org.apache.cocoon.xml.IncludeXMLConsumer;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.ResourceIterator;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.XMLResource;
import org.xmldb.api.modules.XPathQueryService;

/**
 * This class implements the xmldb:// pseudo-protocol and allows to get XML
 * content from an XML:DB enabled XML database.
 *
 * @author <a href="mailto:gianugo@apache.org">Gianugo Rabellino</a>
 * @author <a href="mailto:vgritsenko@apache.org">Vadim Gritsenko</a>
 * @version CVS $Id: XMLDBSource.java,v 1.1 2003/03/09 00:06:48 pier Exp $
 */
public class XMLDBSource extends AbstractSAXSource {

    /** The driver implementation class */
    protected String driver;

    /** The connection status. */
    protected boolean connected = false;

    /** The requested URL */
    protected String url;

    /** The supplied user */
    protected String user = null;

    /** The supplied password */
    protected String password;

    /** The part of URL after # sign */
    protected String query = null;

    /** The System ID */
    protected String systemId;

    /** Static Strings used for XML Collection representation */

    protected static final String URI = "http://apache.org/cocoon/xmldb/1.0";
    // FIXME (VG): Should not be this more generic? Say, "xmldb"?
    protected static final String PREFIX = "collection";

    /** Root element &lt;collections&gt; */
    protected static final String COLLECTIONS  = "collections";
    protected static final String QCOLLECTIONS  = PREFIX + ":" + COLLECTIONS;
    protected static final String RESOURCE_COUNT_ATTR = "resources";
    protected static final String COLLECTION_COUNT_ATTR  = "collections";
//    protected static final String COLLECTION_BASE_ATTR  = "base";

    /** Element &lt;collection&gt; */
    protected static final String COLLECTION  = "collection";
    protected static final String QCOLLECTION  = PREFIX + ":" + COLLECTION;

    /** Element &lt;resource&gt; */
    protected static final String RESOURCE  = "resource";
    protected static final String QRESOURCE  = PREFIX + ":" + RESOURCE;
    protected static final String NAME_ATTR  = "name";

    /** Root element &lt;results&gt; */
    protected static final String RESULTSET = "results";
    protected static final String QRESULTSET = PREFIX + ":" + RESULTSET;
    protected static final String QUERY_ATTR = "query";
    protected static final String RESULTS_COUNT_ATTR = "resources";

    /** Element &lt;result&gt; */
    protected static final String RESULT = "result";
    protected static final String QRESULT = PREFIX + ":" + RESULT;
    protected static final String RESULT_DOCID_ATTR = "docid";
    protected static final String RESULT_ID_ATTR = "id";

    protected static final String CDATA  = "CDATA";

    /**
     * The constructor.
     *
     * @param environment the Cocoon Environment.
     * @param url the URL being queried.
     * @param driver the XML:DB driver class name.
     */
    public XMLDBSource(Environment environment,
                       ComponentManager manager,
                       Logger logger,
                       String driver,
                       SourceCredential credential,
                       String url) {

        super(environment, manager, logger);
        int start;

        this.driver = driver;

        this.user = credential.getPrincipal();
        this.password = credential.getPassword();

        if ((start = url.indexOf('#')) != -1) {
            this.url = url.substring(0, start);
            this.query = url.substring(start + 1);
        } else {
            this.url = url;
        }
    }

    /**
     * Initialize the XML:DB connection.
     *
     */
    public void connect()
    throws ProcessingException {

        if (log.isDebugEnabled()) {
            this.log.debug("Initializing XML:DB connection, using driver " + driver);
        }

        try {

            Class c = Class.forName(driver);
            DatabaseManager.registerDatabase((Database)c.newInstance());

        } catch (XMLDBException xde) {

            String error = "Unable to connect to the XMLDB database. Error "
                    + xde.errorCode + ": " + xde.getMessage();
            this.log.debug(error, xde);
            throw new ProcessingException(error, xde);

        } catch (Exception e) {

            throw new ProcessingException("Problem setting up the connection to XML:DB: "
                    + e.getMessage() + ". Make sure that your driver is available.", e);

        }

        this.connected = true;
    }


    /**
     * Stream SAX events to a given ContentHandler. If the requested
     * resource is a collection, build an XML view of it.
     *
     */
    public void toSAX(ContentHandler handler) throws SAXException {

        try {
            if (!connected) {
                this.connect();
            }
            if (url.endsWith("/"))
                this.collectionToSAX(handler);
            else
                this.resourceToSAX(handler);
        } catch (ProcessingException pe) {
            throw new SAXException("ProcessingException", pe);
        }

    }

    private void resourceToSAX(ContentHandler handler) throws SAXException, ProcessingException {

        final String col = url.substring(0, url.lastIndexOf('/'));
        final String res = url.substring(url.lastIndexOf('/') + 1);

        try {
            Collection collection = DatabaseManager.getCollection(col, user, password);
            if (collection == null) {
                throw new ResourceNotFoundException("Document " + url + " not found");
            }

            XMLResource xmlResource = (XMLResource) collection.getResource(res);
            if (xmlResource == null) {
                throw new ResourceNotFoundException("Document " + url + " not found");
            }

            if (query != null) {
                // Query resource
                if (log.isDebugEnabled()) {
                    this.log.debug("Querying resource " + res + " from collection " + url + "; query= " + this.query);
                }

                queryToSAX(handler, collection, res);
            } else {
                // Return entire resource
                if (log.isDebugEnabled()) {
                    this.log.debug("Obtaining resource " + res + " from collection " + col);
                }

                xmlResource.getContentAsSAX(handler);
            }

            collection.close();
        } catch (XMLDBException xde) {
            String error = "Unable to fetch content. Error "
                     + xde.errorCode + ": " + xde.getMessage();
            throw new SAXException(error, xde);
        }
    }

    private void collectionToSAX(ContentHandler handler) throws SAXException, ProcessingException {

        AttributesImpl attributes = new AttributesImpl();

        try {
            Collection collection = DatabaseManager.getCollection(url, user, password);
            if (collection == null) {
                throw new ResourceNotFoundException("Collection " + url +
                        " not found");
            }

            if (query != null) {
                // Query collection
                if (log.isDebugEnabled()) {
                    this.log.debug("Querying collection " + url + "; query= " + this.query);
                }

                queryToSAX(handler, collection, null);
            } else {
                // List collection
                if (log.isDebugEnabled()) {
                    this.log.debug("Listing collection " + url);
                }

                final String ncollections = Integer.toString(collection.getChildCollectionCount());
                final String nresources = Integer.toString(collection.getResourceCount());
                attributes.addAttribute("", RESOURCE_COUNT_ATTR,
                        RESOURCE_COUNT_ATTR, "CDATA", nresources);
                attributes.addAttribute("", COLLECTION_COUNT_ATTR,
                        COLLECTION_COUNT_ATTR, "CDATA", ncollections);
//                attributes.addAttribute("", COLLECTION_BASE_ATTR,
//                        COLLECTION_BASE_ATTR, "CDATA", url);

                handler.startDocument();
                handler.startPrefixMapping(PREFIX, URI);
                handler.startElement(URI, COLLECTIONS, QCOLLECTIONS, attributes);

                // Print child collections
                String[] collections = collection.listChildCollections();
                for (int i = 0; i < collections.length; i++) {
                    attributes.clear();
                    attributes.addAttribute("", NAME_ATTR, NAME_ATTR, CDATA, collections[i]);
                    handler.startElement(URI, COLLECTION,
                            QCOLLECTION, attributes);
                    handler.endElement(URI, COLLECTION, COLLECTION);
                }

                // Print child resources
                String[] resources = collection.listResources();
                for (int i = 0; i < resources.length; i++) {
                    attributes.clear();
                    attributes.addAttribute("", NAME_ATTR, NAME_ATTR, CDATA, resources[i]);
                    handler.startElement(URI, RESOURCE,
                            QRESOURCE, attributes);
                    handler.endElement(URI, RESOURCE, RESOURCE);
                }

                handler.endElement(URI, COLLECTIONS, QCOLLECTIONS);
                handler.endPrefixMapping(PREFIX);
                handler.endDocument();
            }

            collection.close();
        } catch (XMLDBException xde) {
            String error = "Collection listing failed. Error " + xde.errorCode + ": " + xde.getMessage();
            throw new SAXException(error, xde);
        }
    }

    private void queryToSAX(ContentHandler handler, Collection collection, String resource) throws SAXException {

        AttributesImpl attributes = new AttributesImpl();

        try {
            XPathQueryService service =
                (XPathQueryService) collection.getService("XPathQueryService", "1.0");
            ResourceSet resultSet = (resource == null) ?
                    service.query(query) : service.queryResource(resource, query);

            attributes.addAttribute("", QUERY_ATTR, QUERY_ATTR, "CDATA", query);
            attributes.addAttribute("", RESULTS_COUNT_ATTR,
                    RESULTS_COUNT_ATTR, "CDATA", Long.toString(resultSet.getSize()));

            handler.startDocument();
            handler.startPrefixMapping(PREFIX, URI);
            handler.startElement(URI, RESULTSET, QRESULTSET, attributes);

            IncludeXMLConsumer includeHandler = new IncludeXMLConsumer(handler);

            // Print search results
            ResourceIterator results = resultSet.getIterator();
            while (results.hasMoreResources()) {
                XMLResource result = (XMLResource)results.nextResource();

                final String id = result.getId();
                final String documentId = result.getDocumentId();

                attributes.clear();
                if (id != null) {
                    attributes.addAttribute("", RESULT_ID_ATTR, RESULT_ID_ATTR,
                        CDATA, id);
                }
                if (documentId != null) {
                    attributes.addAttribute("", RESULT_DOCID_ATTR, RESULT_DOCID_ATTR,
                        CDATA, documentId);
                }
                handler.startElement(URI, RESULT, QRESULT, attributes);

                result.getContentAsSAX(includeHandler);

                handler.endElement(URI, RESULT, RESULT);
            }

            handler.endElement(URI, RESULTSET, QRESULTSET);
            handler.endPrefixMapping(PREFIX);
            handler.endDocument();
        } catch (XMLDBException xde) {
            String error = "Query failed. Error " + xde.errorCode + ": " + xde.getMessage();
            throw new SAXException(error, xde);
        }
    }

    public void recycle() {
        this.driver = null;
        this.log = null;
        this.manager = null;
        this.url = null;
        this.query = null;
    }

    public String getSystemId() {
        return url;
    }
}
