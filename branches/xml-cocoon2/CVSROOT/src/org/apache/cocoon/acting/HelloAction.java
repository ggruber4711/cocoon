/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/
package org.apache.cocoon.acting;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletContext;

import org.apache.avalon.Component;
import org.apache.avalon.Configuration;
import org.apache.avalon.ConfigurationException;
import org.apache.avalon.Parameters;

import org.xml.sax.SAXException;
import org.xml.sax.EntityResolver;

import org.apache.cocoon.Constants;
/**
 *
 * @author <a href="mailto:Giacomo.Pati@pwr.ch">Giacomo Pati</a>
 * @version CVS $Revision: 1.1.2.7 $ $Date: 2000-12-15 20:35:09 $
 */
public class HelloAction extends ComposerAction {

    /**
     * Get the <code>Configuration</code> object for this <code>Component</code>
     */
    public void configure( Configuration configuration) throws ConfigurationException {
    }

    /**
     * A simple Action that logs if the <code>Session</code> object
     * has been created
     */
    public Map act (EntityResolver resolver, Map objectModel, String src, Parameters par) throws Exception {
        HttpServletRequest req = (HttpServletRequest) objectModel.get(Constants.REQUEST_OBJECT);
        if (req != null) {
            HttpSession session = req.getSession (false);
            ServletContext context = (ServletContext)objectModel.get(Constants.CONTEXT_OBJECT);
            if (context != null) {
                if (session != null) {
                    if (session.isNew()) {
                        super.log.debug("Session is new");
                        context.log("Session is new");
                    } else {
                        super.log.debug("Session is new");
                        context.log("Session is old");
                    }
                } else {
                    super.log.debug("A session object was not created");
                    context.log("A session object was not created");
                }
            } else {
                if (session != null) {
                    if (session.isNew()) {
                        super.log.debug("Session is new");
                    } else {
                        super.log.debug("Session is old");
                    }
                } else {
                    super.log.debug("A session object was not created");
                }
            }
        }
        return null;
    }
}



