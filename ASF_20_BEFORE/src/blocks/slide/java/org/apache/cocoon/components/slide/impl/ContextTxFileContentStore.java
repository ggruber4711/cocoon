/*

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

 Copyright (C) 1999-2002 The Apache Software Foundation. All rights reserved.

 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:

 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:   "This product includes software
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
package org.apache.cocoon.components.slide.impl;

import java.io.File;
import java.util.Hashtable;

import org.apache.slide.common.Domain;
import org.apache.slide.common.ServiceParameterErrorException;
import org.apache.slide.common.ServiceParameterMissingException;
import org.apache.slide.store.txfile.TxFileContentStore;

/**
 * Specialized version of the TxFileContentStore from the
 * Jakarta Slide project, which respects the context path and work directory.
 *
 * @author <a href="mailto:stephan@apache.org">Stephan Michels</a>
 * @version CVS $Id: ContextTxFileContentStore.java,v 1.1 2004/01/20 11:13:59 unico Exp $
 */
public class ContextTxFileContentStore extends TxFileContentStore {
    
    public ContextTxFileContentStore() {
    }
    
    public void setParameters(Hashtable parameters)
        throws ServiceParameterErrorException, ServiceParameterMissingException {
        
        // resolve the rootpath parameter relative to the webapp context path
        String rootpath = (String) parameters.get(STORE_DIR_PARAMETER);
        rootpath = new File(Domain.getParameter("contextpath"),rootpath).toString();
        parameters.put(STORE_DIR_PARAMETER,rootpath);
        
        // resolve the workpath parameter relative to the cocoon work directory
        String workpath = (String) parameters.get(WORK_DIR_PARAMETER);
        workpath = new File(Domain.getParameter("workdir"),workpath).toString();
        parameters.put(WORK_DIR_PARAMETER,workpath);
        
        super.setParameters(parameters);
    }

}
