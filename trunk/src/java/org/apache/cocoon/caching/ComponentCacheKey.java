/*

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

 Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.

 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:

 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:  "This product includes  software
    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
    Alternately, this  acknowledgment may  appear in the software itself,  if
    and wherever such third-party acknowledgments normally appear.

 4. The names "Apache Cocoon" and  "Apache Software Foundation" must  not  be
    used to  endorse or promote  products derived from  this software without
    prior written permission. For written permission, please contact
    apache@apache.org.

 5. Products  derived from this software may not  be called "Apache", nor may
    "Apache" appear  in their name,  without prior written permission  of the
    Apache Software Foundation.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation and was  originally created by
 Stefano Mazzocchi  <stefano@apache.org>. For more  information on the Apache
 Software Foundation, please see <http://www.apache.org/>.

*/
package org.apache.cocoon.caching;

import java.io.Serializable;
/**
 * This is the cache key for one sitemap component.
 * It consists of three parts:<br/>
 * a.) The component type (generator, transformer etc.)<br/>
 * b.) The component identifier - a unique handle for the sitemap
 *      component<br/>
 * c.) The cache key - a key, generated by the component, which
 *      is unique inside the components space.
 *
 * @author <a href="mailto:cziegeler@apache.org">Carsten Ziegeler</a>
 * @version CVS $Id: ComponentCacheKey.java,v 1.2 2004/02/22 15:06:26 unico Exp $
 */
public final class ComponentCacheKey
    implements Serializable {

    public static final int ComponentType_Generator   = 1;
    public static final int ComponentType_Transformer = 3;
    public static final int ComponentType_Serializer  = 5;
    public static final int ComponentType_Reader      = 7;

    // Converts Generator / Transformer / Serializer / Reader constants above
    // into string.
    private static final String[] COMPONENTS = { "X", "G", "X", "T", "X", "S", "X", "R" };

    /** The component type */
    private int type;
    /** The component identifier */
    private String identifier;
    /** The unique key */
    private Serializable key;
    /** the hash code */
    private int hashCode = 0;
    /** cachePoint */
    private boolean cachePoint = false;

    /**
     * Constructor
     */
    public ComponentCacheKey(int          componentType,
                             String       componentIdentifier,
                             Serializable cacheKey) {
        this.type = componentType;
        this.identifier = componentIdentifier == null ? "" : componentIdentifier;
        this.key = cacheKey;

    }

    /**
     * alternate cachepoint Constructor
     */
    public ComponentCacheKey(int          componentType,
                             String       componentIdentifier,
                             Serializable cacheKey,
			     boolean cachePoint) {
        this.type = componentType;
        this.identifier = componentIdentifier;
        this.key = cacheKey;
        /** cachePoint */
        this.cachePoint = cachePoint;
    }

    /**
     * Compare
     */
    public boolean equals(Object object) {
        if (object instanceof ComponentCacheKey) {
            ComponentCacheKey ccp = (ComponentCacheKey)object;
            if (this.type == ccp.type
                && this.identifier.equals(ccp.identifier)
                && this.key.equals(ccp.key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * HashCode
     */
    public int hashCode() {
        // FIXME - this is not very safe
        if (this.hashCode == 0) {
            this.hashCode = this.type +
                            (this.identifier.length() << 3) +
                            (this.key.hashCode() << 10);
        }
        return this.hashCode;
    }

    private String toString;

    /**
     * toString
     * The FilesystemStore uses toString!
     */
    public String toString() {
        if (this.toString == null) {
            toString = COMPONENTS[this.type] + '-' + this.identifier + '-' + this.key.toString();
        }
        return toString;
    }

    /**
     * Check if we are a cachepoint 
     */
    public boolean isCachePoint() {
        return cachePoint;
    }
}
