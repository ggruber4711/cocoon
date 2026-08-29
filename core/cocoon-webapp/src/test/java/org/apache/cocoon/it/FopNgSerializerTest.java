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
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Exercises {@code FOPNGSerializer} against a running container, after its port from the
 * FOP 1.0 API to FOP 2.x.
 *
 * <p>Unit tests are not much use for this component: it does nothing except assemble a
 * {@code FopFactory} and hand FOP a SAX stream, so almost everything that can break is a
 * wiring or classpath problem that only appears once a real FO document is rendered by a real
 * container. All three parts of the port fail in ways a compile does not catch:
 *
 * <ul>
 * <li>{@code FopFactory} is now built through {@code FopFactoryBuilder} in {@code configure},
 *     rather than being a field initialiser. If that is not reached, the factory is null.</li>
 * <li>The user config is parsed with FOP's own configuration classes, since FOP 2.7 forked
 *     Avalon's. A mismatch there throws only when a config is actually supplied.</li>
 * <li>{@code setURIResolver} was replaced by a {@code ResourceResolver}. If that adapter is
 *     wrong, FOP still returns a valid PDF -- just one with the images missing. This is the
 *     failure worth guarding, because nothing about the response says anything went wrong;
 *     see {@link #testImagesAreFetchedThroughCocoon()}.</li>
 * </ul>
 */
public class FopNgSerializerTest {

    private static final String BLOCK = "/cocoon-fop-ng-sample";

    /** A PDF must be a real PDF, not an error page served with the wrong content type. */
    @Test
    public void testRendersPdf() throws Exception {
        byte[] pdf = get(BLOCK + "/misc/minimal.pdf", "application/pdf");

        assertTrue("response does not start with the PDF magic bytes: "
                + head(pdf), startsWith(pdf, "%PDF-"));
        assertTrue("PDF is not terminated by %%EOF, so it was truncated",
                new String(pdf, "ISO-8859-1").trim().endsWith("%%EOF"));
    }

    /**
     * The PDF records its producer, which is the most direct evidence available that FOP 2 is
     * the thing doing the rendering. If a FOP 1.x jar were still on the classpath, or the
     * serializer had silently fallen back to the legacy block, this is what would notice.
     */
    @Test
    public void testRenderedByFop2() throws Exception {
        String pdf = new String(get(BLOCK + "/misc/minimal.pdf", "application/pdf"), "ISO-8859-1");

        int at = pdf.indexOf("/Producer");
        assertTrue("PDF declares no /Producer", at >= 0);
        String producer = pdf.substring(at, Math.min(at + 60, pdf.length()));
        assertTrue("expected an Apache FOP 2.x producer, got: " + producer,
                producer.matches("(?s)/Producer \\(Apache FOP Version 2\\..*"));
    }

    /**
     * The one that actually tests the ResourceResolver port.
     *
     * <p>{@code resolver-test.fo.xml} references its image as
     * {@code blockcontext:/cocoon-samples-style-default/...}. FOP cannot fetch that on its own;
     * only {@code CocoonResourceResolver}, handing the URI to Cocoon's {@code SourceResolver},
     * can. When the adapter is missing or broken FOP does not fail the request -- it logs an
     * event and lays out the page without the image, returning a perfectly valid 200 PDF. So
     * the assertion has to be about the image being *in* the document, not about the status.
     */
    @Test
    public void testImagesAreFetchedThroughCocoon() throws Exception {
        byte[] withImage = get(BLOCK + "/resolver-test.pdf", "application/pdf");

        assertTrue("no image XObject in the PDF, so FOP could not fetch an image addressed "
                + "through a Cocoon protocol -- the ResourceResolver is not doing its job",
                countImageXObjects(withImage) > 0);
    }

    /** PostScript and PNG go through different FOP renderers, so they are separate wiring. */
    @Test
    public void testRendersPostScript() throws Exception {
        byte[] ps = get(BLOCK + "/hello.ps", "application/postscript");
        assertTrue("not a PostScript document: " + head(ps), startsWith(ps, "%!PS"));
    }

    @Test
    public void testRendersPng() throws Exception {
        byte[] png = get(BLOCK + "/hello.png", "image/png");
        assertTrue("not a PNG: " + head(png),
                png.length > 8 && (png[0] & 0xff) == 0x89 && png[1] == 'P'
                        && png[2] == 'N' && png[3] == 'G');
    }

    /**
     * The FO source is reachable in its own right. This is the pipeline step before
     * serialization, so when it fails the PDF assertions above are misleading.
     */
    @Test
    public void testFoSourceIsServed() throws Exception {
        byte[] fo = get(BLOCK + "/misc/minimal.fo.xml", "text/xml");
        assertTrue("FO source does not look like XSL-FO",
                new String(fo, "UTF-8").indexOf("http://www.w3.org/1999/XSL/Format") > 0);
    }

    // ------------------------------------------------------------------

    /** GETs a URL, asserting the status and content type, and returns the body. */
    private static byte[] get(String path, String expectedContentType) throws Exception {
        URL url = new URL(baseUrl() + path);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        try {
            int status = c.getResponseCode();
            byte[] body = readFully(status < 400 ? c.getInputStream() : c.getErrorStream());
            assertEquals(path + " returned HTTP " + status + ": " + head(body), 200, status);

            String contentType = c.getContentType();
            assertTrue(path + " has content type " + contentType + ", expected "
                    + expectedContentType,
                    contentType != null && contentType.startsWith(expectedContentType));
            assertTrue(path + " returned an empty body", body.length > 0);
            return body;
        } finally {
            c.disconnect();
        }
    }

    /**
     * Counts embedded images. A PDF image is an XObject whose subtype is /Image; FOP writes the
     * dictionary entries on one line, but the two tokens may be separated by whitespace.
     */
    private static int countImageXObjects(byte[] pdf) throws Exception {
        String text = new String(pdf, "ISO-8859-1");
        int count = 0;
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("/Subtype\\s*/Image").matcher(text);
        while (m.find()) {
            count++;
        }
        return count;
    }

    private static boolean startsWith(byte[] body, String prefix) throws Exception {
        return body.length >= prefix.length()
                && new String(body, 0, prefix.length(), "ISO-8859-1").equals(prefix);
    }

    /** First line-ish of a response, for failure messages. */
    private static String head(byte[] body) {
        int n = Math.min(body.length, 200);
        try {
            return new String(body, 0, n, "ISO-8859-1").replaceAll("\\s+", " ");
        } catch (Exception e) {
            return "<" + body.length + " bytes>";
        }
    }

    private static String baseUrl() {
        String base = System.getProperty("htmlunit.base-url");
        return base != null ? base : "http://localhost:8888";
    }

    private static byte[] readFully(InputStream in) throws Exception {
        if (in == null) {
            return new byte[0];
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }
}
