/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.cocoon.sitemap;

import org.apache.avalon.ComponentManager;
import org.apache.avalon.Configuration;

import org.apache.avalon.Poolable;
import org.apache.avalon.ThreadSafe;
import org.apache.avalon.SingleThreaded;

import org.apache.cocoon.util.ClassUtils;
/**
 * This factory instantiate the corresponding ComponentHolder according to the
 * interfaces the passed component implements.
 *
 * @author <a href="mailto:Giacomo.Pati@pwr.ch">Giacomo Pati</a>
 * @version CVS $Revision: 1.1.2.2 $ $Date: 2000-10-09 09:30:12 $
 */
public class ComponentHolderFactory {

    public static ComponentHolder getComponentHolder (String componentName, Configuration configuration, ComponentManager manager)
    throws Exception {
        if (ClassUtils.implementsInterface (componentName, Poolable.class.getName())) {
            return new PoolableComponentHolder (componentName, configuration, manager);
        } else if (ClassUtils.implementsInterface (componentName, SingleThreaded.class.getName())) {
            return new DefaultComponentHolder (componentName, configuration, manager);
        } else if (ClassUtils.implementsInterface (componentName, ThreadSafe.class.getName())) {
            return new ThreadSafeComponentHolder (componentName, configuration, manager);
        } else  {
            return new DefaultComponentHolder (componentName, configuration, manager);
        }
    }
}