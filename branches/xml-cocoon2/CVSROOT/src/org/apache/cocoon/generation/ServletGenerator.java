/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/
package org.apache.cocoon.generation;

import java.util.Map;
import java.io.IOException;

import javax.servlet.ServletContext;

import org.apache.avalon.configuration.Parameters;
import org.apache.avalon.Composer;

import org.apache.cocoon.Constants;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.http.HttpRequest;
import org.apache.cocoon.environment.http.HttpResponse;

import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;

/**
 *
 * @author <a href="mailto:Giacomo.Pati@pwr.ch">Giacomo Pati</a>
 * @version CVS $Revision: 1.1.2.12 $ $Date: 2001-03-23 13:48:53 $
 */
public abstract class ServletGenerator extends ComposerGenerator
implements Composer {

    protected HttpRequest request=null;
    protected HttpResponse response=null;
    protected ServletContext context=null;

    public void setup(EntityResolver resolver, Map objectModel, String src, Parameters par)
        throws ProcessingException, SAXException, IOException {

      super.setup(resolver, objectModel, src, par);
      this.request = (HttpRequest) objectModel.get(Constants.REQUEST_OBJECT);
      this.response = (HttpResponse) objectModel.get(Constants.RESPONSE_OBJECT);
      this.context = (ServletContext) objectModel.get(Constants.CONTEXT_OBJECT);
    }
}
