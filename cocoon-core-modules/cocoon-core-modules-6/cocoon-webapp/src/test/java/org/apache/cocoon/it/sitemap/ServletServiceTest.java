package org.apache.cocoon.it.sitemap;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.cocoon.tools.it.HtmlUnitTestCase;

/**
 * Test the usage of servlet services in Cocoon sitemaps.
 */
public class ServletServiceTest extends HtmlUnitTestCase {

    /**
     * A parameter is passed to an XSLT transformer.
     */
    public void testSimplePipelineParameterPassingToTransformer() throws Exception {
        this.loadXmlPage("/cocoon-it/ssf/local");
        assertTrue(this.response.getStatusCode() == 200);
        assertEquals("text/xml", this.response.getContentType());
        assertXPath("/html/body/p", "3");
    }

}