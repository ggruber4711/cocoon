/*
 * Copyright 1999-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cocoon.components.validation.impl;

import org.apache.cocoon.components.validation.Schema;
import org.apache.excalibur.source.SourceValidity;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * <p>A simple implementation of the {@link Schema} interface.</p>
 *
 * @author <a href="mailto:pier@betaversion.org">Pier Fumagalli</a>
 */
public abstract class AbstractSchema implements Schema {
    
    /** <p>The {@link SourceValidity} of this {@link Schema} instance.</p> */
    private final SourceValidity validity;

    /**
     * <p>Create a new {@link AbstractSchema} instance.</p>
     */
    public AbstractSchema(SourceValidity validity) {
        this.validity = validity;
    }

    /**
     * <p>Return the {@link SourceValidity} associated with this {@link Schema}.</p>
     * 
     * @return a {@link SourceValidity} instance or <b>null</b> if not known.
     */
    public SourceValidity getValidity() {
        return this.validity;
    }

    /**
     * <p>Return a new {@link ContentHandler} instance that can be used to send SAX
     * events to for proper validation.</p>
     *
     * <p>By default, this method will create a {@link ContentHandler} failing on the
     * first occurrence of an warning, error or fatal error . If this behavior is
     * not suitable, use the {@link #newValidator(ErrorHandler)} method instead and
     * specify an {@link ErrorHandler} suitable to your needs.</p>
     *
     * <p>Once used, the returned {@link ContentHandler} <b>can't</b> be reused.</p> 
     * 
     * @return a <b>non-null</b> {@link ContentHandler} instance.
     */
    public ContentHandler newValidator() {
        ErrorHandler handler = new ErrorHandler() {
            public void warning(SAXParseException e) throws SAXException {
                throw e;
            }
            public void error(SAXParseException e) throws SAXException {
                throw e;
            }
            public void fatalError(SAXParseException e) throws SAXException {
                throw e;
            }
        };
        return this.newValidator(handler);
    }
}
