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
package org.apache.cocoon.it;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Exercises a flowscript continuation end to end, which is the one thing the move from Rhino
 * 1.6 to 1.7 could break invisibly.
 *
 * <p>Rhino 1.7 removed {@code org.mozilla.javascript.continuations.Continuation} in favour of
 * {@code NativeContinuation}. Flowscript is built on continuations: {@code sendPageAndWait}
 * captures one, and the follow-up request casts it back to resume the script. Compiling against
 * the new class is not evidence that either half works, because the cast and the scope restore
 * only happen at runtime, on the second request.
 *
 * <p>So this test does both halves. It asks for a page that suspends a flowscript, checks a
 * continuation was actually created, then posts back to it and checks the script resumed. A
 * broken port fails on the second request, not the first -- which is exactly the kind of thing
 * a smoke test that only fetches pages would miss.
 */
public class FlowscriptContinuationTest {

    /** A form driven by flowscript, as opposed to the plain sitemap-driven ones. */
    private static final String FLOW_PAGE = "/samples/forms/form1.flow";

    /** Cocoon renders the continuation as a 40-character hash followed by ".continue". */
    private static final Pattern CONTINUATION =
            Pattern.compile("([a-f0-9]{40}\\.continue)");

    @Test
    public void testContinuationIsCreatedAndCanBeResumed() throws Exception {
        String page = get(FLOW_PAGE);

        Matcher m = CONTINUATION.matcher(page);
        assertTrue("no continuation id in " + FLOW_PAGE + ", so the flowscript never suspended; "
                + "sendPageAndWait did not capture a continuation", m.find());
        String continuation = m.group(1);

        // Resume it. The field values do not matter: the form will fail validation and be
        // redisplayed, and that redisplay is itself proof the script resumed, because it is
        // the flowscript's own loop that renders it again.
        String resumed = post("/samples/forms/" + continuation,
                "email=someone%40example.com&submit=submit");

        assertFalse("resuming the continuation produced an error page, which is what a bad "
                + "NativeContinuation cast looks like from outside",
                resumed.indexOf("org.apache.cocoon.ProcessingException") >= 0
                        || resumed.indexOf("ClassCastException") >= 0);
        assertTrue("the resumed response is not a Cocoon Forms page, so the flowscript did not "
                + "pick up where it left off", resumed.indexOf("continuation-id") >= 0
                        || CONTINUATION.matcher(resumed).find());
    }

    /** The sitemap-driven form is the control: it must work whether or not continuations do. */
    @Test
    public void testNonFlowscriptFormStillWorks() throws Exception {
        String page = get("/samples/forms/form1");
        assertTrue("the plain form1 page did not render",
                page.indexOf("</html>") > 0 || page.indexOf("</HTML>") > 0);
    }

    // ------------------------------------------------------------------

    private static String get(String path) throws Exception {
        HttpURLConnection c = open(path);
        c.setRequestMethod("GET");
        return read(c, path);
    }

    private static String post(String path, String body) throws Exception {
        HttpURLConnection c = open(path);
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        OutputStream out = c.getOutputStream();
        try {
            out.write(body.getBytes("UTF-8"));
        } finally {
            out.close();
        }
        return read(c, path);
    }

    private static HttpURLConnection open(String path) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(baseUrl() + path).openConnection();
        c.setInstanceFollowRedirects(true);
        return c;
    }

    private static String read(HttpURLConnection c, String path) throws Exception {
        try {
            int status = c.getResponseCode();
            InputStream in = status < 400 ? c.getInputStream() : c.getErrorStream();
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            if (in != null) {
                byte[] chunk = new byte[8192];
                int n;
                while ((n = in.read(chunk)) > 0) {
                    buf.write(chunk, 0, n);
                }
                in.close();
            }
            String body = new String(buf.toByteArray(), "UTF-8");
            assertEquals(path + " returned HTTP " + status + ": "
                    + body.substring(0, Math.min(300, body.length())).replaceAll("\\s+", " "),
                    200, status);
            return body;
        } finally {
            c.disconnect();
        }
    }

    private static void assertFalse(String message, boolean condition) {
        assertTrue(message, !condition);
    }

    private static String baseUrl() {
        String base = System.getProperty("htmlunit.base-url");
        return base != null ? base : "http://localhost:8888";
    }
}
