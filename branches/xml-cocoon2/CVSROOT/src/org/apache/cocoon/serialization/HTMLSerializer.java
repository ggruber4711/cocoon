/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.cocoon.serialization;

import java.io.OutputStream;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.excalibur.pool.Poolable;

/**
 * @author <a href="mailto:stefano@apache.org">Stefano Mazzocchi</a>
 * @version CVS $Revision: 1.1.2.22 $ $Date: 2001-04-30 14:17:38 $
 */

public class HTMLSerializer extends AbstractTextSerializer implements Poolable {

    private TransformerHandler handler;

    public HTMLSerializer() {
    }

    public void setOutputStream(OutputStream out) {
        try {
            super.setOutputStream(out);
            handler = getTransformerFactory().newTransformerHandler();
            format.put(OutputKeys.METHOD,"html");
            handler.setResult(new StreamResult(out));
            handler.getTransformer().setOutputProperties(format);
            this.setContentHandler(handler);
            this.setLexicalHandler(handler);
        } catch (Exception e) {
            getLogger().error("HTMLSerializer.setOutputStream()", e);
            throw new RuntimeException(e.toString());
        }
    }

    /**
     * Recyce the serializer. GC instance variables
     */
    public void recycle() {
        super.recycle();
        this.handler = null;
    }
}
