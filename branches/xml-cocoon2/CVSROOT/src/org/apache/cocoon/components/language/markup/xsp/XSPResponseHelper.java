/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/
package org.apache.cocoon.components.language.markup.xsp;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;

import org.apache.cocoon.environment.Response;

import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.SAXException;

import org.apache.cocoon.Constants;

import org.apache.log.LogKit;

/**
 * The XSP <code>Response</code> object helper
 *
 * @author <a href="mailto:ricardo@apache.org">Ricardo Rocha</a>
 * @version CVS $Revision: 1.1.2.13 $ $Date: 2001-04-19 13:11:41 $
 */
public class XSPResponseHelper extends XSPObjectHelper {
  /**
   * Assign values to the object's namespace uri and prefix
   */
  static {
    URI = Constants.XSP_RESPONSE_URI;
    PREFIX = Constants.XSP_RESPONSE_PREFIX;
  }

  /**
   * Set the content header for a given response
   *
   * @param objectModel The Map objectModel
   * @param name The header name
   * @param value The header value
   */
  public static void setHeader(Map objectModel, String name, String value) {
    Response response = (Response)objectModel.get(Constants.RESPONSE_OBJECT);
    response.setHeader(name, value);
  }

  /**
   * Set the content header for a given response
   *
   * @param objectModel The Map objectModel
   * @param name The parameter name
   * @param value The parameter value
   */
  public static void addHeader(
    Map objectModel,
    String name,
    String value
  ) {
    Response response = (Response)objectModel.get(Constants.RESPONSE_OBJECT);
    response.addHeader(name, value);
  }

  /**
   * Encode the URL
   *
   * @param objectModel The Map objectModel
   * @param name The input url string
   */
  public static String encodeURL(
    Map objectModel,
    String input
  ) {
      Response response = (Response)objectModel.get(Constants.RESPONSE_OBJECT);
      return response.encodeURL(input);
  }

}
