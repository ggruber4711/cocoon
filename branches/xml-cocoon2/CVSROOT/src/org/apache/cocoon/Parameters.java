/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/
package org.apache.cocoon;

import java.util.Enumeration;
import java.util.Hashtable;
import org.apache.arch.config.Configuration;
import org.apache.arch.config.ConfigurationException;

/**
 *
 * @author <a href="mailto:fumagalli@exoffice.com">Pierpaolo Fumagalli</a>
 *         (Apache Software Foundation, Exoffice Technologies)
 * @version CVS $Revision: 1.1.2.3 $ $Date: 2000-02-27 07:11:35 $
 */
public class Parameters {
    private Hashtable parameters=null;

    /**
     * Create a new <code>Parameters</code> instance.
     */
    public Parameters() {
        super();
        this.parameters=new Hashtable();
    }

    /**
     * Set the <code>String</code> value of a specified parameter.
     * <br>
     * If the specified value is <b>null</b> the parameter is removed.
     *
     * @return The previous value of the parameter or <b>null</b>.
     */
    private String setParameter(String name, String value) {
        if (name==null) return(null);
        if (value==null) return((String)this.parameters.remove(name));
        return((String)this.parameters.put(name,value));
    }

    /**
     * Return an <code>Enumeration</code> view of all parameter names.
     */
    public Enumeration getParameterNames() {
        return(this.parameters.keys());
    }
    
    /**
     * Check if the specified parameter can be retrieved.
     */
    public boolean isParameter(String name) {
        return(this.parameters.containsKey(name));
    }

    /**
     * Retrieve the <code>String</code> value of the specified parameter.
     * <br>
     * If the specified parameter cannot be found, <b>null</b> is returned.
     */
    private String getParameter(String name) {
        if(name==null) return(null);
        return((String)this.parameters.get(name));
    }

    /**
     * Retrieve the <code>String</code> value of the specified parameter.
     * <br>
     * If the specified parameter cannot be found, <code>defaultValue</code>
     * is returned.
     */
    public String getParameter(String name, String defaultValue) {
        String value=this.getParameter(name);
        return(value==null ? defaultValue : value);
    }
    
    /**
     * Retrieve the <code>int</code> value of the specified parameter.
     * <br>
     * If the specified parameter cannot be found, <code>defaultValue</code>
     * is returned.
     */
    public int getParameterAsInteger(String name, int defaultValue) {
        String value=this.getParameter(name);
        if (value==null) return(defaultValue);
        try {
            if (value.startsWith("0x"))
                return(Integer.parseInt(value.substring(2),16));
            else if (value.startsWith("0o"))
                return(Integer.parseInt(value.substring(2),8));
            else if (value.startsWith("0b"))
                return(Integer.parseInt(value.substring(2),2));
            else return(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return(defaultValue);
        }
    }
    
    /**
     * Retrieve the <code>long</code> value of the specified parameter.
     * <br>
     * If the specified parameter cannot be found, <code>defaultValue</code>
     * is returned.
     */
    public long getParameterAsLong(String name, long defaultValue) {
        String value=this.getParameter(name);
        if (value==null) return(defaultValue);
        try {
            if (value.startsWith("0x"))
                return(Long.parseLong(value.substring(2),16));
            else if (value.startsWith("0o"))
                return(Long.parseLong(value.substring(2),8));
            else if (value.startsWith("0b"))
                return(Long.parseLong(value.substring(2),2));
            else return(Long.parseLong(value));
        } catch (NumberFormatException e) {
            return(defaultValue);
        }
    }
    
    /**
     * Retrieve the <code>float</code> value of the specified parameter.
     * <br>
     * If the specified parameter cannot be found, <code>defaultValue</code>
     * is returned.
     */
    public float getParameterAsFloat(String name, float defaultValue) {
        String value=this.getParameter(name);
        if (value==null) return(defaultValue);
        try {
            return(Float.parseFloat(value));
        } catch (NumberFormatException e) {
            return(defaultValue);
        }
    }
    
    /**
     * Retrieve the <code>boolean</code> value of the specified parameter.
     * <br>
     * If the specified parameter cannot be found, <code>defaultValue</code>
     * is returned.
     */
    public boolean getParameterAsBoolean(String name, boolean defaultValue) {
        String value=this.getParameter(name);
        if (value==null) return(defaultValue);
        if (value.equalsIgnoreCase("TRUE")) return(true);
        if (value.equalsIgnoreCase("FALSE")) return(false);
        return(defaultValue);
    }
    
    /**
     * Merge parameters from another <code>Parameters</code> instance
     * into this.
     *
     * @return This <code>Parameters</code> instance.
     */
    public Parameters merge(Parameters param) {
        Enumeration e=param.getParameterNames();
        while (e.hasMoreElements()) {
            String name=(String)e.nextElement();
            String value=param.getParameter(name);
            this.setParameter(name,value);
        }
        return(this);
    }
    
    /**
     * Create a <code>Parameters</code> object from a <code>Configuration</code>
     * object.
     */
    public static Parameters fromConfiguration(Configuration conf)
    throws ConfigurationException {
        Enumeration e=conf.getConfigurations("parameter");
        Parameters param=new Parameters();
        while (e.hasMoreElements()) {
            Configuration child=(Configuration)e.nextElement();
            String name=child.getAttribute("name");
            String value=child.getAttribute("value");
            param.setParameter(name,value);
        }
        return(param);
    }
}
