/*
 * Copyright 1999-2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cocoon.matching;

import java.util.Map;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.SitemapComponentTestCase;


public class ParameterMatcherTestCase extends SitemapComponentTestCase {
    
    public ParameterMatcherTestCase(String name) {
        super(name);
    }
    
    /**
     * Run this test suite from commandline
     *
     * @param args commandline arguments (ignored)
     */
    public static void main( String[] args ) {
        TestRunner.run(suite());
    }
    
    /** Create a test suite.
     * This test suite contains all test cases of this class.
     * @return the Test object containing all test cases.
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(ParameterMatcherTestCase.class);
        return suite;
    }
    
    /**
     * A simple parameter matcher test
     */
    public void testParameterMatch() throws Exception {
        // create a parameter attribute
        
        Parameters parameters = new Parameters();
        final String parameterName = "paramterMatchTestCase";
        final String parameterValue = "parameterMatchTestCaseValue";
        parameters.setParameter( parameterName, parameterValue );

        Map result = match("parameter", parameterName, parameters);
        System.out.println(result);
        assertNotNull("Test if parameter entry exists", result);
        assertEquals("Test for parameter " + parameterName + " having value " + parameterValue, parameterValue, result.get("1"));
    }
    
    /**
     * A simple parameter matcher test
     */
    public void testParameterMatchFails() throws Exception {        
        Parameters parameters = new Parameters();
        
        final String parameterNameDoesNotExist = "parameterNameDoesNotExist";
        
        // create a parameter attribute
        final String parameterName = "paramterMatchTestCase";
        final String parameterValue = "parameterMatchTestCaseValue";
        parameters.setParameter( parameterName, parameterValue );

        Map result = match("parameter", parameterNameDoesNotExist, parameters);
        assertNull( "Test if parameter entry " + parameterNameDoesNotExist + " does not exist", result );
    }
}
