/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.arch;

/**
 * This abstract class is an utility class that provides default values
 * for component naming and versioning. Each Component implementation should
 * override the methods it needs to express.
 *
 * @author <a href="mailto:scoobie@betaversion.org">Federico Barbieri</a>
 * @author <a href="mailto:pier@apache.org">Pierpaolo Fumagalli</a>
 * @author <a href="mailto:stefano@apache.org">Stefano Mazzocchi</a>
 * @version $Revision: 1.1.2.1 $ $Date: 1999-12-11 23:28:46 $
 */
public abstract class AbstractComponent implements Component {

	/**
	 * Return a string identifying the component author.
	 */
    public String getVersion() {
        return null;
    }
    
	/**
	 * Return a string identifying the component name. (i.e. "SuperCheeser")
	 */
    public String getName() {
        return null;
    }
    
	/**
	 * Return a string identifying the component author. (i.e. "Cheese Inc.")
	 */
    public String getAuthor() {
        return null;
    }

	/**
	 * Return a string identifying the component type. (i.e. "Cheese Producer")
	 */
    public String getType() {
        return null;
    }
    
    /*
     * Return a string identifying the component status 
     * (configurations, statistics, performance)
     *
     * <p><strong>FIXME (SM):</strong>
     * should this method return a Status class instead? This would
     * allow to return something different from a string and also to control
     * what DTD (if any) is used for the string markup. This follows the MVC
     * design pattern where Component is the "controller/model" and Status is 
     * the "view".
     */
    public String getStatus() {
        return null;
    }
}