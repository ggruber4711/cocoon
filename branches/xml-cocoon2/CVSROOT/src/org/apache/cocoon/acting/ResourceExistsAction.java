/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/
package org.apache.cocoon.acting;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.Constants;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.Roles;
import org.apache.cocoon.components.url.URLFactory;
import org.apache.cocoon.environment.Request;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * This action simply checks to see if a given resource exists. It takes a
 * single parameter named 'url' and returns an empty map if it exists and
 * null otherwise. It has only been tested with context urls.
 *
 * @author <a href="mailto:balld@apache.org">Donald Ball</a>
 * @version CVS $Revision: 1.1.2.4 $ $Date: 2001-04-30 14:16:59 $
 */
public class ResourceExistsAction extends ComposerAction {

    protected URLFactory urlFactory;

    public void configure(Configuration conf) throws ConfigurationException {
        try {
            urlFactory = (URLFactory)this.manager.lookup(Roles.URL_FACTORY);
        } catch (ComponentException e) {
            throw new ConfigurationException("Could not lookup url factory",e);
        }
    }

    public Map act(EntityResolver resolver, Map objectModel, String source, Parameters parameters) throws Exception {
        HashMap results = new HashMap();
        String urlstring = parameters.getParameter("url",null);
        try {
            URL url = urlFactory.getURL(urlstring);
            url.getContent();
        } catch (Exception e) {
            getLogger().debug("ResourceExistsAction: exception: ",e);
            return null;
        }
        return Collections.unmodifiableMap(results);
    }

}
