/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/
package org.apache.cocoon.components.saxconnector;

import java.io.IOException;
import java.util.Map;
import org.apache.avalon.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.sitemap.Sitemap;
import org.apache.cocoon.xml.AbstractXMLPipe;
import org.apache.excalibur.pool.Poolable;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;

/**
 * Null implementation of the SAXConnector. Simply sends events on to the next component in the pipeline.
 * @author <a href="prussell@apache.org">Paul Russell</a>
 * @version CVS $Revision: 1.1.2.7 $ $Date: 2001-04-25 18:20:37 $
 */
public class NullSAXConnector extends AbstractXMLPipe implements Poolable, SAXConnector {

    /** Setup this SAXConnector.
     */
    public void setup(EntityResolver resolver, Map objectModel, String src, Parameters params)
    throws ProcessingException, SAXException, IOException {
        // do nothing.
    }
}
