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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Guards against the cold-cache concurrency defect: several requests for the same
 * not-yet-cached resource arriving at once.
 *
 * <p>When that defect strikes, Cocoon either fails the requests with
 * {@code IllegalArgumentException: setContentLength(N) when already written M} (HTTP 500)
 * or serves a body containing the resource more than once. Once the cache is warm the same
 * burst is clean, which is why nothing catches it in normal use. It affects any reader
 * implementing {@code CacheableProcessingComponent}, including a plain
 * {@code ResourceReader}. It was reported downstream in 2015 against Jetty 6, where it
 * surfaced as {@code IOException: Closed}, and worked around by serving static resources
 * through a bundler. It is a decade older than the Jakarta migration and is still open.
 *
 * <p><b>This test currently passes, and that is not proof the defect is gone.</b> Forty
 * concurrent threads per URL inside one JVM have not reproduced it here, while the same
 * burst issued as separate processes does, reliably, against a freshly started container:
 *
 * <pre>
 *   ./cocoon.sh &amp;
 *   for i in $(seq 1 20); do curl -sS -o /dev/null -w "%{http_code} %{size_download}\n" \
 *       http://localhost:8888/cocoon-samples-style-default/images/cocoon-logo.jpg &amp; done; wait
 * </pre>
 *
 * <p>Roughly 15 of 20 come back 500. Why in-process threads do not trigger it is itself
 * unexplained and worth knowing: it may be connection pooling, or it may be a hint about
 * the timing the defect needs.
 *
 * <p>The test is kept because the property it asserts is the one that matters -- every
 * concurrent request for a resource gets the same, complete resource -- so it will catch a
 * gross regression even though it does not currently catch this one. The investigation is
 * written up in jakarta-verify/BOOT-SMOKE-TEST.md.
 */
public class ConcurrentColdCacheResourcesTest {

    /** Resources served by different blocks through the caching reader path. */
    private static final String[] RESOURCES = {
        "/cocoon-samples-style-default/styles/main.css",
        "/cocoon-samples-style-default/images/cocoon-logo.jpg",
        "/cocoon-forms-impl/resource/external/forms/js/forms-lib.js",
    };

    private static final int CONCURRENCY =
            Integer.getInteger("cocoon.it.concurrency", 40).intValue();

    @Test
    public void testConcurrentFirstRequestsAreNotCorrupted() throws Exception {
        // All three URLs at once: the window is while the block sitemaps are still
        // being compiled, so spreading the burst over sequential rounds misses it.
        final List<String> problems = Collections.synchronizedList(new ArrayList<String>());
        final CountDownLatch allDone = new CountDownLatch(RESOURCES.length);
        for (int i = 0; i < RESOURCES.length; i++) {
            final String path = RESOURCES[i];
            new Thread("hammer-" + path) {
                public void run() {
                    try {
                        problems.addAll(hammer(path));
                    } catch (Exception e) {
                        problems.add(path + " harness failed: " + e);
                    } finally {
                        allDone.countDown();
                    }
                }
            }.start();
        }
        assertTrue("harness did not finish", allDone.await(120, TimeUnit.SECONDS));

        StringBuilder report = new StringBuilder();
        for (int i = 0; i < problems.size(); i++) {
            report.append("\n  ").append(problems.get(i));
        }
        assertEquals("concurrent first requests were corrupted:" + report + "\n",
                0, problems.size());
    }

    /**
     * Fires {@link #CONCURRENCY} simultaneous requests at one URL and reports anything
     * that is not a clean, identical 200.
     */
    private List<String> hammer(final String path) throws Exception {
        final CountDownLatch startLine = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(CONCURRENCY);
        final List<int[]> statuses = Collections.synchronizedList(new ArrayList<int[]>());
        final List<byte[]> bodies = Collections.synchronizedList(new ArrayList<byte[]>());
        final List<String> problems = Collections.synchronizedList(new ArrayList<String>());

        for (int i = 0; i < CONCURRENCY; i++) {
            Thread t = new Thread("hammer-" + i) {
                public void run() {
                    try {
                        startLine.await();
                        HttpURLConnection c = (HttpURLConnection) new URL(baseUrl() + path).openConnection();
                        c.setRequestProperty("Connection", "close");
                        int status = c.getResponseCode();
                        InputStream in = status < 400 ? c.getInputStream() : c.getErrorStream();
                        byte[] body = readFully(in);
                        statuses.add(new int[] { status });
                        if (status == 200) {
                            bodies.add(body);
                        }
                        c.disconnect();
                    } catch (Exception e) {
                        problems.add(path + " threw " + e);
                    } finally {
                        done.countDown();
                    }
                }
            };
            t.start();
        }

        startLine.countDown();
        assertTrue("threads did not finish within 60s for " + path,
                done.await(60, TimeUnit.SECONDS));

        for (int i = 0; i < statuses.size(); i++) {
            int status = ((int[]) statuses.get(i))[0];
            if (status != 200) {
                problems.add(path + " returned HTTP " + status);
            }
        }

        // Every successful response for one URL must be byte-identical. A body that is a
        // multiple of the expected size is the silent form of this defect: the resource
        // written to the response more than once.
        if (!bodies.isEmpty()) {
            byte[] first = (byte[]) bodies.get(0);
            int shortest = first.length;
            for (int i = 0; i < bodies.size(); i++) {
                shortest = Math.min(shortest, ((byte[]) bodies.get(i)).length);
            }
            for (int i = 0; i < bodies.size(); i++) {
                byte[] b = (byte[]) bodies.get(i);
                if (b.length != shortest) {
                    problems.add(path + " returned " + b.length + " bytes where other "
                            + "responses returned " + shortest
                            + (b.length % shortest == 0
                                ? " (exactly " + (b.length / shortest) + " copies)" : ""));
                }
            }
        }
        return problems;
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
