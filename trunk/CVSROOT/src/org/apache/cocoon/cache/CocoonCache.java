/*-- $Id: CocoonCache.java,v 1.8 2000-11-14 22:02:57 greenrd Exp $ --

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

 Copyright (C) @year@ The Apache Software Foundation. All rights reserved.

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

 4. The names "Cocoon" and  "Apache Software Foundation"  must not be used to
    endorse  or promote  products derived  from this  software without  prior
    written permission. For written permission, please contact
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
package org.apache.cocoon.cache;

import java.util.*;
import javax.servlet.http.*;
import org.apache.cocoon.*;
import org.apache.cocoon.store.*;
import org.apache.cocoon.framework.*;

/**
 * This is the dynamic cocoon cache implementation which is
 * able to cache all instances of generated documents, both
 * statically and dynamically generated.
 *
 * @author <a href="stefano@apache.org">Stefano Mazzocchi</a>
 * @version $Revision: 1.8 $Date: 2000/11/01 20:12:40 $
 */
public class CocoonCache implements Cache, Status {

    private Store store;

    public void init(Director director) {
        this.store = (Store) director.getActor("store");
    }

    /**
     * This method retrieves a page from the store
     * and checks if its changeable points have changed.
     * Only if all the changeable points haven't changed
     * the page is returned, otherwise null is returned.
     */
    public Page getPage(HttpServletRequest request) {
        String encoded = Utils.encode(request);
        Page page = (Page) store.get(encoded);

        if (page == null) {
            return null;
        }

        boolean changed = false;
        Enumeration e = page.getChangeables();
        while (e.hasMoreElements()) {
            Changeable c = (Changeable) e.nextElement();
            changed = c.hasChanged(request);
            if (changed) {
                break;
            }
        }

        if (changed) store.remove (encoded);

        return (changed) ? null : page;
    }

    /**
     * Get the time that this request was added to the cache.
     * If the request is no longer in the cache (maybe it was
     * cleared due to low memory), just returns the current time.
     */
    public long getLastModified(HttpServletRequest request) {
      try {
        return store.getTime (request);
      }
      catch (NullPointerException ex) {
        return System.currentTimeMillis ();
      }
    }

    /**
     * This method inserts the page in cache and associates it
     * with the given request.
     */
    public void setPage(Page page, HttpServletRequest request) {
        if (!page.isCached()) {
            page.setCached(true);
            this.store.hold(Utils.encode(request), page);
        }
    }

    /**
     * Flushes the cache and forces an additional cache cleanup. This is
     * normally used when the system requires additional memory.
     */
    public void flush() {
        this.store.free();
    }

    public String getStatus() {
        return "Cocoon Dynamic Cache System";
    }
}
