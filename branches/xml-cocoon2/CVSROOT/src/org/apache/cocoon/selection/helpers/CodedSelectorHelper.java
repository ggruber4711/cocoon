/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/
package org.apache.cocoon.selection.helpers;

import java.util.Map;
import org.apache.cocoon.Constants;
import org.apache.cocoon.environment.Context;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.environment.Session;

/**
 * This abstract class provides the basis for testing the environment with
 * developer specified java code via the CodedSelectorFactory. This class also
 * provides the environments context, request, response and session
 * variables for developer ease.
 *
 * @author <a href="mailto:Marcus.Crafter@osa.de">Marcus Crafter</a>
 * @version CVS $Revision: 1.1.2.7 $ $Date: 2001-04-25 17:08:22 $
 */
public abstract class CodedSelectorHelper
{
    public abstract boolean evaluate(Map objectModel);

    protected CodedSelectorHelper() { }

    protected void initialize(Map objectModel)
    {
        context = (Context)
                  objectModel.get(Constants.CONTEXT_OBJECT);
        request = (Request)
                  objectModel.get(Constants.REQUEST_OBJECT);
        response = (Response)
                   objectModel.get(Constants.RESPONSE_OBJECT);
        session = request.getSession(false);
    }

    protected Context context;
    protected Request request;
    protected Response response;
    protected Session session;
}
