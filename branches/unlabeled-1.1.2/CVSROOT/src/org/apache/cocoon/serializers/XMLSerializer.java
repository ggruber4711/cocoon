/*****************************************************************************
 * Copyright (C) 1999 The Apache Software Foundation.   All rights reserved. *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1,  a copy of wich has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/
package org.apache.cocoon.serializers;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Hashtable;
import org.apache.cocoon.sitemap.Request;
import org.apache.cocoon.sitemap.Response;
import org.apache.cocoon.sax.XMLConsumer;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 *
 * @author <a href="mailto:fumagalli@exoffice.com">Pierpaolo Fumagalli</a>, 
 *         Exoffice Technologies, INC.</a>
 * @author Copyright 1999 &copy; <a href="http://www.apache.org">The Apache
 *         Software Foundation</a>. All rights reserved.
 * @version CVS $Revision: 1.1.2.1 $ $Date: 2000-02-11 13:15:30 $
 * @since Cocoon 2.0
 */
public class XMLSerializer extends AbstractSerializer implements XMLConsumer {
    /** The PrintStream used for output */
    private OutputStreamWriter out=null;
    /** The current locator */
    private Locator loc=null;
    /** The namespaces URI-PREFIX table */
    private Hashtable namespacesUriPrefix=null;
    /** The namespaces PREFIX-URI reversed table */
    private Hashtable namespacesPrefixUri=null;
    /** A flag representing an open element (for "/>" or ">") */
    private boolean openElement=false;

    /**
     * Return a new instance of this <code>XMLSerializer</code>.
     */
    public XMLConsumer getXMLConsumer(Request req, Response res, OutputStream out)
    throws IOException {
        String c=this.configurations.getParameter("contentType","text/xml");
        String e=this.configurations.getParameter("encoding","us-ascii");
        res.setContentType(c);
        XMLSerializer s=new XMLSerializer();
        s.out=new OutputStreamWriter(new BufferedOutputStream(out),e);
        s.namespacesUriPrefix=new Hashtable();
        s.namespacesPrefixUri=new Hashtable();
        return(s);
    }

    /**
     * Receive an object for locating the origin of SAX document events.
     *
     * @param locator An object that can return the location of any SAX
     *                document event.
     */
    public void setDocumentLocator(Locator locator) {
        this.loc=locator;
    }

    /** Print a string */
    private void print(String s)
    throws SAXException {
        char data[]=s.toCharArray();
        this.print(data,0,data.length);
    }
    
    /** Print data from a character array */
    private void print(char data[], int start, int len)
    throws SAXException {
        try {
            this.out.write(data,start,len);
        } catch (IOException e) {
            throw new SAXException("IOException printing data",e);
        }
    }

    /** Print data from a character array */
    private void print(char c)
    throws SAXException {
        try {
            this.out.write(c);
        } catch (IOException e) {
            throw new SAXException("IOException printing data",e);
        }
    }

    /** Return the fully namespace qualified name */
    private String qualify(String uri, String loc, String raw)
    throws SAXException {
        if(uri.length()>0) {
            String pre=(String)this.namespacesUriPrefix.get(uri);
            if (pre==null) throw new SAXException("No prefix declared for "+
                                                  "namespace uri '"+uri+"'");
            if (pre.length()>0) return(pre+":"+loc);
            else return(loc);
        } else if(raw.length()>0) {
            return(raw);
        } else if(loc.length()>0) {
            return(loc);
        }
        throw new SAXException("Cannot qualify namespaced name");
    }

    private void closeElement()
    throws SAXException {
        if(!this.openElement) return;
        this.print('>');
        this.openElement=false;
    }


    /**
     * Receive notification of the beginning of a document.
     */
    public void startDocument()
    throws SAXException {
        this.closeElement();
        String encoding=this.out.getEncoding();
        if (encoding==null) this.print("<?xml version=\"1.0\"?>");
        else {
            this.print("<?xml version=\"1.0\" encoding=\"");
            this.print(encoding);
            this.print("\"?>");
        }
        this.print('\n');
    }

    /**
     * Receive notification of the end of a document.
     */
    public void endDocument()
    throws SAXException {
        this.closeElement();
        try {
            this.out.flush();
        } catch (IOException e) {
            throw new SAXException("IOException flushing stream",e);
        }
    }

    /**
     * Begin the scope of a prefix-URI Namespace mapping.
     *
     * @param p The Namespace prefix being declared.
     * @param uri The Namespace URI the prefix is mapped to.
     */
    public void startPrefixMapping(String p, String uri)
    throws SAXException {
        if (this.namespacesUriPrefix.put(uri,p)!=null)
            throw new SAXException("Namespace URI '"+uri+"' already declared");
        if (this.namespacesPrefixUri.put(p,uri)!=null)
            throw new SAXException("Namespace prefix '"+p+"' already declared");
    }

    /**
     * End the scope of a prefix-URI mapping.
     *
     * @param p The prefix that was being mapping.
     */
    public void endPrefixMapping(String p)
    throws SAXException {
        String uri=(String)this.namespacesPrefixUri.remove(p);
        if (uri==null)
            throw new SAXException("Namespace prefix '"+p+"' never declared");
        if (this.namespacesUriPrefix.remove(uri)==null)
            throw new SAXException("Namespace URI '"+uri+"' never declared");
    }

    /**
     * Receive notification of the beginning of an element.
     *
     * @param uri The Namespace URI, or the empty string if the element has no
     *            Namespace URI or if Namespace
     *            processing is not being performed.
     * @param loc The local name (without prefix), or the empty string if
     *            Namespace processing is not being performed.
     * @param raw The raw XML 1.0 name (with prefix), or the empty string if
     *            raw names are not available.
     * @param a The attributes attached to the element. If there are no
     *          attributes, it shall be an empty Attributes object.
     */
    public void startElement(String uri, String loc, String raw, Attributes a)
    throws SAXException {
        this.closeElement();
        this.print('<');

        this.print(this.qualify(uri,loc,raw));
        for (int x=0; x<a.getLength(); x++) {
            this.print(' ');
            this.print(this.qualify(a.getURI(x),a.getLocalName(x),
                                    a.getRawName(x)));
            this.print('=');
            this.print('\"');
            this.print(a.getValue(x));
            this.print('\"');
        }
        this.openElement=true;
    }
        

    /**
     * Receive notification of the end of an element.
     *
     * @param uri The Namespace URI, or the empty string if the element has no
     *            Namespace URI or if Namespace
     *            processing is not being performed.
     * @param loc The local name (without prefix), or the empty string if
     *            Namespace processing is not being performed.
     * @param raw The raw XML 1.0 name (with prefix), or the empty string if
     *            raw names are not available.
     */
    public void endElement (String uri, String loc, String raw)
    throws SAXException {
        if (this.openElement) {
            this.print('/');
            this.closeElement();
            return;
        }
        
        this.print('<');
        this.print('/');
        if (raw.length()>0) this.print(raw);
        else {
            // do all namespace stuff
        }
        this.print('>');
    }

    /**
     * Receive notification of character data.
     *
     * @param ch The characters from the XML document.
     * @param start The start position in the array.
     * @param len The number of characters to read from the array.
     */
    public void characters (char ch[], int start, int len)
    throws SAXException {
        this.closeElement();
        this.print(ch,start,len);
    }

    /**
     * Receive notification of ignorable whitespace in element content.
     *
     * @param ch The characters from the XML document.
     * @param start The start position in the array.
     * @param len The number of characters to read from the array.
     */
    public void ignorableWhitespace (char ch[], int start, int len)
    throws SAXException {
        this.closeElement();
        this.print(ch,start,len);
    }

    /**
     * Receive notification of a processing instruction.
     *
     * @param target The processing instruction target.
     * @param data The processing instruction data, or null if none was
     *             supplied.
     */
    public void processingInstruction (String target, String data)
    throws SAXException {
        this.closeElement();
        this.print("<?");
        this.print(target);
        this.print(' ');
        this.print(data);
        this.print("?>");
    }

    /**
     * Receive notification of a skipped entity.
     *
     * @param name The name of the skipped entity.  If it is a  parameter
     *             entity, the name will begin with '%'.
     */
    public void skippedEntity (String name)
    throws SAXException {
        this.closeElement();
        this.print('&');
        this.print(name);
        this.print(';');
    }

    /**
     * Report the start of DTD declarations, if any.
     *
     * @param name The document type name.
     * @param publicId The declared public identifier for the external DTD
     *                 subset, or null if none was declared.
     * @param systemId The declared system identifier for the external DTD
     *                 subset, or null if none was declared.
     */
    public void startDTD (String name, String publicId, String systemId)
    throws SAXException {
        this.closeElement();
        this.print("<!DOCTYPE ");
        this.print(name);
        if (publicId!=null) {
            this.print(" PUBLIC \"");
            this.print(publicId);
            this.print('\"');
            if (systemId!=null) {
                this.print(' ');
                this.print('\"');
                this.print(systemId);
                this.print('\"');
            }
        } else if (systemId!=null) {
            this.print(" SYSTEM \"");
            this.print(systemId);
            this.print('\"');
        }
        this.print('>');
        this.print('\n');
    }        
    
    /**
     * Report the end of DTD declarations.
     */
    public void endDTD ()
    throws SAXException {
        this.closeElement();
    }

    /**
     * Report the beginning of an entity.
     *
     * @param name The name of the entity. If it is a parameter entity, the
     *             name will begin with '%'.
     */
    public void startEntity (String name)
    throws SAXException {
        this.closeElement();
    }        

    /**
     * Report the end of an entity.
     *
     * @param name The name of the entity that is ending.
     */
    public void endEntity (String name)
    throws SAXException {
        this.closeElement();
    }        

    /**
     * Report the start of a CDATA section.
     */
    public void startCDATA ()
    throws SAXException {
        this.closeElement();
        this.print("<![CDATA[");
    }

    /**
     * Report the end of a CDATA section.
     */
    public void endCDATA ()
    throws SAXException {
        this.closeElement();
        this.print("]]>");
    }
    

    /**
     * Report an XML comment anywhere in the document.
     *
     * @param ch An array holding the characters in the comment.
     * @param start The starting position in the array.
     * @param len The number of characters to use from the array.
     */
    public void comment (char ch[], int start, int len)
    throws SAXException {
        this.closeElement();
        this.print("<!--");
        this.print(ch,start,len);
        this.print("-->");
    }
}
