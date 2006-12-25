/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cocoon.core.container.spring;

/**
 * Some constants for the spring integration.
 * 
 * @version $Id$
 * @since 1.0
 */
public abstract class Constants {

    /** The default location of spring related configuration files. */
    public static final String DEFAULT_SPRING_CONFIGURATION_LOCATION = "classpath*:META-INF/cocoon/spring";
    public static final String DEFAULT_PROPERTIES_LOCATION = "classpath*:META-INF/cocoon/properties";

    /** The default location of spring related per sitemap configuration files. */
    public static final String DEFAULT_SITEMAP_SPRING_CONFIGURATION_LOCATION = "config/spring";
    public static final String DEFAULT_SITEMAP_PROPERTIES_LOCATION = "config/properties";
}
