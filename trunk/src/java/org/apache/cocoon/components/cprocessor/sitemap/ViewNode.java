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
package org.apache.cocoon.components.cprocessor.sitemap;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.cocoon.components.cprocessor.NamedContainerNode;
import org.apache.cocoon.components.cprocessor.ProcessingNode;

/**
 * 
 * @author <a href="mailto:unico@apache.org">Unico Hommes</a>
 * @version CVS $ID$
 * 
 * @avalon.component
 * @avalon.service type=ProcessingNode
 * @x-avalon.lifestyle type=singleton
 * @x-avalon.info name=view-node
 */
public class ViewNode extends NamedContainerNode implements ProcessingNode {
    
    /**
     * Pseudo-label for views <code>from-position="first"</code> (i.e. generator).
     */
    public static final String FIRST_POS_LABEL = "!first!";

    /**
     * Pseudo-label for views <code>from-position="last"</code> (i.e. serializer).
     */
    public static final String LAST_POS_LABEL = "!last!";
    
    private static final String FROM_LABEL_ATTR = "from-label";
    private static final String FROM_POSITION_ATTR = "from-position";
    
    private String m_label;
    
    public ViewNode() {
    }
    
    public void configure(Configuration config) throws ConfigurationException {
        super.configure(config);
        // Get the label or position (pseudo-label) of this view.
        m_label = config.getAttribute(FROM_LABEL_ATTR, null);
        
        if (m_label == null) {
            String position = config.getAttribute(FROM_POSITION_ATTR);
            if ("first".equals(position)) {
                m_label = FIRST_POS_LABEL;
            } else if ("last".equals(position)) {
                m_label = LAST_POS_LABEL;
            } else {
                String msg = "Bad value for 'from-position' at " + getLocation();
                throw new ConfigurationException(msg);
            }
        }
    }
    
    public String getLabel() {
        return m_label;
    }
}
