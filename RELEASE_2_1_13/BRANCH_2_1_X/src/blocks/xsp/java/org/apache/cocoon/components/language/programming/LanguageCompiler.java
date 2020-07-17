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
package org.apache.cocoon.components.language.programming;

import org.apache.avalon.framework.component.Component;

import java.io.IOException;
import java.util.List;

/**
 * This interface defines a compiler's functionality for all
 * (Java-based) compiled languages
 *
 * @author <a href="mailto:stefano@apache.org">Stefano Mazzocchi</a>
 * @version CVS $Id$
 * @since 2.0
 */
public interface LanguageCompiler extends Component {

    /**
     * Set the name of the file containing the source program
     *
     * @param file The name of the file containing the source program
     */
    void setFile(String file);

    /**
     * Set the name of the directory containing the source program file
     *
     * @param srcDir The name of the directory containing the source program file
     */
    void setSource(String srcDir);

    /**
     * Set the name of the directory to contain the resulting object program file
     *
     * @param destDir The name of the directory to contain the resulting object
     * program file
     */
    void setDestination(String destDir);

    /**
     * Set the classpath to be used for this compilation
     *
     * @param classpath The classpath to be used for this compilation
     */
    void setClasspath(String classpath);

    /**
     * Set the encoding of the input source file or <code>null</code> to use the
     * platform's default encoding
     *
     * @param encoding The encoding of the input source file or <code>null</code>
     * to use the platform's default encoding
     */
    void setEncoding(String encoding);

    /**
     * Set the version of the java source code to be compiled
     *
     * @param level The version of the JVM for wich the code was written.
     * i.e: Posible level's values are:
     * 130 = for Java 1.3, 140 = for Java 1.4 and 150 = for Java 1.5
     * 
     * @since 2.1.7
     */
    void setCompilerComplianceLevel(int level);
    
    /**
     * Compile a source file yielding a loadable program file.
     *
     * @exception IOException If an error occurs during compilation
     */
    boolean compile() throws IOException;

    /**
     * Return the list of errors generated by this compilation
     *
     * @return The list of errors generated by this compilation
     * @exception IOException If an error occurs during message collection
     */
    List getErrors() throws IOException;
}
