/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/
package org.apache.cocoon.generation;

import java.io.IOException;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.sitemap.SitemapModelComponent;
import org.apache.cocoon.xml.XMLProducer;
import org.xml.sax.SAXException;

/**
 *
 * @author <a href="mailto:fumagalli@exoffice.com">Pierpaolo Fumagalli</a>
 *         (Apache Software Foundation, Exoffice Technologies)
 * @version CVS $Revision: 1.1.2.6 $ $Date: 2001-04-25 17:07:39 $
 */
public interface Generator extends XMLProducer, SitemapModelComponent {

    void generate()
    throws IOException, SAXException, ProcessingException;
}
