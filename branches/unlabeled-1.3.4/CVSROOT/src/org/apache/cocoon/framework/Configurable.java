/*****************************************************************************
 * Copyright (C) 1999 The Apache Software Foundation.   All rights reserved. *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1,  a copy of wich has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/
package org.apache.cocoon.framework;

/**
 * The <code>Configurable</code> interface is implemented by those objects
 * whose operation depends on configuration parameters.
 *
 * @author <a href="mailto:fumagalli@exoffice.com">Pierpaolo Fumagalli</a>, 
 *         Exoffice Technologies, INC.</a>
 * @author Copyright 1999 &copy; <a href="http://www.apache.org">The Apache
 *         Software Foundation</a>. All rights reserved.
 * @version CVS $Revision: 1.3.4.1 $ $Date: 2000-02-07 15:35:38 $
 * @since Cocoon 2.0
 */
public interface Configurable {
    /**
     * Set the appropriate configurations for this component.
     *
     * @param conf The configuration parameters.
     * @exception ConfigurationException If this component cannot be configured
     *                                   properly.
     */
    public void configure(Configurations conf)
    throws ConfigurationException;
}
