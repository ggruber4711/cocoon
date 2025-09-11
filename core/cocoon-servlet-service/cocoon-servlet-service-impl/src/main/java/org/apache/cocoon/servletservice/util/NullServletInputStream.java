/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cocoon.servletservice.util;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ReadListener;

import java.io.IOException;

/**
 * Always empty stream.
 *
 * @version $Id: NullServletInputStream.java 608375 2008-01-03 08:33:00Z reinhard $
 * @since 1.0.0
 */
public class NullServletInputStream extends ServletInputStream {

    public static final NullServletInputStream INSTANCE = new NullServletInputStream();

    public int read() throws IOException {
        return -1;
    }

    @Override
    public boolean isFinished() {
        return true;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        // immediately signal data finished
        if (readListener != null) {
            try { readListener.onAllDataRead(); } catch (IOException ignored) {}
        }
    }

}
