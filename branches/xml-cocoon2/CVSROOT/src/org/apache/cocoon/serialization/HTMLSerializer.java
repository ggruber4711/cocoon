/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.cocoon.serialization;

import java.io.OutputStream;

import org.apache.avalon.Configuration;
import org.apache.avalon.configuration.ConfigurationException;
import org.apache.avalon.Poolable;

import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.OutputKeys;

/**
 * @author <a href="mailto:stefano@apache.org">Stefano Mazzocchi</a>
 * @version CVS $Revision: 1.1.2.16 $ $Date: 2001-03-12 04:39:01 $
 */

public class HTMLSerializer extends AbstractTextSerializer implements Poolable {

    private TransformerHandler handler;

    public HTMLSerializer() {
    }

    public void setOutputStream(OutputStream out) {
        try {
            super.setOutputStream(out);
            handler = factory.newTransformerHandler();
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
}
