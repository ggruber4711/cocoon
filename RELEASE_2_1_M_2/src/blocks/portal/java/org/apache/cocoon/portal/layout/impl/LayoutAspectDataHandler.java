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
package org.apache.cocoon.portal.layout.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cocoon.portal.aspect.AspectDataHandler;
import org.apache.cocoon.portal.aspect.Aspectalizable;

/**
 *
 * @author <a href="mailto:cziegeler@s-und-n.de">Carsten Ziegeler</a>
 * 
 * @version CVS $Id: LayoutAspectDataHandler.java,v 1.2 2003/05/19 13:16:34 cziegeler Exp $
 */
public class LayoutAspectDataHandler 
    implements AspectDataHandler {

    protected DefaultLayoutDescription description;
    
    // TODO - Implement persistence
    
    protected Map stati;
    
    /**
     * Constructor
     */
    public LayoutAspectDataHandler(DefaultLayoutDescription desc) {
        this.description = desc;
        this.stati = new HashMap();
    }

    /* (non-Javadoc)
     * @see org.apache.cocoon.portal.aspect.AspectDataHandler#getAspectData(org.apache.cocoon.portal.aspect.Aspectalizable, java.lang.String)
     */
    public Object getAspectData(Aspectalizable owner, String aspectName) {
        // is this aspect allowed?
        DefaultLayoutAspectDescription aspectDesc = (DefaultLayoutAspectDescription)this.description.getAspect( aspectName );
        if ( aspectDesc == null ) return null;
        
        Map datas = (Map)this.stati.get(owner);
        if ( datas == null ) {
            datas = new HashMap();
            this.stati.put( owner, datas );
        }
        Object data = datas.get( aspectName );
        if ( data == null ) {
            data = aspectDesc.createNewInstance();
            datas.put( aspectName, data );
        }
        return data;
    }

    /* (non-Javadoc)
     * @see org.apache.cocoon.portal.aspect.AspectDataHandler#getAspectDatas(org.apache.cocoon.portal.aspect.Aspectalizable)
     */
    public List getAspectDatas(Aspectalizable owner) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.cocoon.portal.aspect.AspectDataHandler#setAspectData(org.apache.cocoon.portal.aspect.Aspectalizable, java.lang.String, java.lang.Object)
     */
    public void setAspectData(Aspectalizable owner,
                               String aspectName,
                               Object data) {
        // is this aspect allowed?
        DefaultLayoutAspectDescription aspectDesc = (DefaultLayoutAspectDescription)this.description.getAspect( aspectName );
        if ( aspectDesc == null ) return;

        Map datas = (Map)this.stati.get(owner);
        if ( datas == null ) {
            datas = new HashMap();
            this.stati.put( owner, datas );
        }
        datas.put( aspectName, data );
        
    }

    /* (non-Javadoc)
     * @see org.apache.cocoon.portal.aspect.AspectDataHandler#getDescription()
     */
    public Object getDescription() {
        return this.description;
    }

}