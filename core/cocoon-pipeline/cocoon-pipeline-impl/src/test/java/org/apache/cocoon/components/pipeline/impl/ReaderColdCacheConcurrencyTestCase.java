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
package org.apache.cocoon.components.pipeline.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.caching.Cache;
import org.apache.cocoon.caching.CachedResponse;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.AbstractEnvironment;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.reading.Reader;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.apache.excalibur.store.Store;
import org.apache.excalibur.store.StoreJanitor;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.xml.sax.SAXException;

import junit.framework.TestCase;

/**
 * Concurrency harness for {@link AbstractCachingProcessingPipeline#processReader}.
 *
 * <p><b>These tests pass, and that is the point.</b> They were written to reproduce the
 * failure seen in a running Cocoon when many requests hit the same, not yet cached,
 * resource at once:
 *
 * <pre>
 *   IllegalArgumentException: setContentLength(3653) when already written 7252
 * </pre>
 *
 * <p>where the "already written" figure is close to a multiple of the resource size,
 * because the response really does receive the body more than once. Roughly 15 of 20
 * concurrent first requests fail that way against the samples webapp; the same burst
 * against a warm cache is clean. It affects any reader implementing
 * {@link CacheableProcessingComponent}, including a plain {@code ResourceReader}, so it
 * is not specific to one reader.
 *
 * <p>Driving {@code processReader} directly does <em>not</em> reproduce it. That is a
 * useful result: it rules out the pipeline's own cache and lock protocol as the cause.
 * The three cases below cover
 *
 * <ul>
 * <li>a cold cache under concurrent load, with the lock protocol genuinely engaged --
 *     {@code generateLock} keys off {@code RequestContextHolder.getRequestAttributes()},
 *     so the harness installs per-thread request attributes; without them the lock is
 *     null and the whole protocol is inert and the test would pass vacuously;</li>
 * <li>the same burst against a warm cache;</li>
 * <li>one pipeline instance serving two requests without {@code recycle()} in between,
 *     which is what {@code PoolableProxyHandler} does when the Spring request scope
 *     destruction callback has not run yet.</li>
 * </ul>
 *
 * <p>All three write each response exactly once. Whatever causes the duplication
 * therefore lives above this layer, in the parts a real request passes through and this
 * harness does not: the servlet-service block dispatch, {@code HttpEnvironment} and
 * {@code HttpServletResponseBufferingWrapper}, and the call stack. That is where the
 * next attempt should start.
 *
 * <p>The harness is kept because it is faithful and it guards this layer against
 * regressions. See jakarta-verify/BOOT-SMOKE-TEST.md for the full write-up.
 */
public class ReaderColdCacheConcurrencyTestCase extends TestCase {

    /** Body served by the reader under test. */
    private static final byte[] PAYLOAD = new byte[3653];

    private static final int THREADS = 20;

    static {
        for (int i = 0; i < PAYLOAD.length; i++) {
            PAYLOAD[i] = (byte) ('a' + (i % 26));
        }
    }

    private Cache cache;
    private Store transientStore;

    protected void setUp() throws Exception {
        super.setUp();
        this.cache = new MapCache();
        this.transientStore = new MapStore();
    }

    /**
     * Every one of a burst of concurrent first requests must receive the resource exactly
     * once, and none may fail.
     */
    public void testConcurrentFirstRequestsEachGetExactlyOneCopy() throws Exception {
        final CountDownLatch startLine = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(THREADS);
        final List environments = Collections.synchronizedList(new ArrayList());
        final List failures = Collections.synchronizedList(new ArrayList());

        for (int i = 0; i < THREADS; i++) {
            Thread t = new Thread("reader-" + i) {
                public void run() {
                    RecordingEnvironment env = new RecordingEnvironment();
                    environments.add(env);
                    try {
                        // generateLock() stores RequestContextHolder.getRequestAttributes()
                        // as the lock object and waitForLock() compares against it to avoid
                        // deadlocking with itself. Without per-request attributes the lock
                        // is null and the whole protocol is inert, so the contention this
                        // test exists to exercise never happens.
                        RequestContextHolder.setRequestAttributes(new PerRequestAttributes());
                        startLine.await();
                        // Each request gets its own pipeline instance, as the Avalon
                        // component pool hands out one per thread, but they share the
                        // cache and the transient store, as they do in a running Cocoon.
                        newPipeline().processReader(env);
                    } catch (Throwable e) {
                        failures.add(e);
                    } finally {
                        RequestContextHolder.resetRequestAttributes();
                        done.countDown();
                    }
                }
            };
            t.start();
        }

        startLine.countDown();
        assertTrue("threads did not finish within 30s", done.await(30, TimeUnit.SECONDS));

        StringBuffer report = new StringBuffer();
        for (int i = 0; i < failures.size(); i++) {
            report.append("\n  ").append(failures.get(i));
        }
        assertEquals("concurrent first requests failed:" + report + "\n",
                0, failures.size());

        int wrong = 0;
        StringBuffer sizes = new StringBuffer();
        for (int i = 0; i < environments.size(); i++) {
            RecordingEnvironment env = (RecordingEnvironment) environments.get(i);
            if (env.written() != PAYLOAD.length) {
                wrong++;
                sizes.append("\n  got ").append(env.written())
                     .append(" bytes, expected ").append(PAYLOAD.length);
            }
        }
        assertEquals("responses received the resource more than once (duplicated body):"
                + sizes + "\n", 0, wrong);
    }

    /** A warm cache must behave identically. This one passes even with the bug present. */
    public void testConcurrentRequestsAgainstAWarmCache() throws Exception {
        RecordingEnvironment warmUp = new RecordingEnvironment();
        newPipeline().processReader(warmUp);
        assertEquals(PAYLOAD.length, warmUp.written());

        testConcurrentFirstRequestsEachGetExactlyOneCopy();
    }

    /**
     * The Avalon component pool hands a pipeline to a thread and returns it only when the
     * Spring request scope is destroyed (see PoolableProxyHandler, which parks it in a
     * ThreadLocal). If that callback does not run, the next request on the same thread
     * gets the same instance with the previous request's state still on it. Serving two
     * requests from one instance must still write each response exactly once.
     */
    public void testPipelineInstanceReusedWithoutRecycleWritesEachResponseOnce() throws Exception {
        RequestContextHolder.setRequestAttributes(new PerRequestAttributes());
        try {
            TestPipeline reused = newPipeline();

            RecordingEnvironment first = new RecordingEnvironment();
            reused.processReader(first);
            assertEquals("first response", PAYLOAD.length, first.written());

            // Same instance, no recycle() in between, fresh reader as the sitemap would
            // set up, and now a warm cache.
            reused.useReader(new FixedReader());
            RecordingEnvironment second = new RecordingEnvironment();
            reused.processReader(second);
            assertEquals("second response on a reused pipeline instance",
                    PAYLOAD.length, second.written());
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    private TestPipeline newPipeline() {
        TestPipeline pipeline = new TestPipeline();
        pipeline.init(this.cache, this.transientStore, new FixedReader());
        return pipeline;
    }

    // ------------------------------------------------------------------------

    /** Exposes processReader with collaborators injected rather than looked up. */
    static final class TestPipeline extends CachingProcessingPipeline {

        void init(Cache cache, Store transientStore, Reader reader) {
            this.cache = cache;
            this.transientStore = transientStore;
            this.reader = reader;
            this.readerRole = Reader.ROLE;
            this.outputBufferSize = 1024 * 1024;
            this.lockTimeout = 5000;
        }

        void useReader(Reader reader) {
            this.reader = reader;
        }

        public boolean processReader(org.apache.cocoon.environment.Environment env)
                throws ProcessingException {
            return super.processReader(env);
        }
    }

    /** Serves a fixed body and reports itself cacheable, like ResourceReader does. */
    static final class FixedReader implements Reader, CacheableProcessingComponent {

        private OutputStream out;

        public void setup(SourceResolver resolver, Map objectModel, String src, Parameters par)
                throws ProcessingException, SAXException, IOException {
        }

        public void setOutputStream(OutputStream out) {
            this.out = out;
        }

        public void generate() throws IOException, ProcessingException {
            this.out.write(PAYLOAD);
            this.out.flush();
        }

        public String getMimeType() {
            return "text/css";
        }

        public boolean shouldSetContentLength() {
            return false;
        }

        public long getLastModified() {
            return 0;
        }

        public Serializable getKey() {
            return "the-one-contended-resource";
        }

        public SourceValidity getValidity() {
            return NOPValidity.SHARED_INSTANCE;
        }
    }

    /**
     * Collects the response body and enforces the container's rule that Content-Length
     * cannot be set once content has been written, which is how this fault announces
     * itself in Jetty.
     */
    static final class RecordingEnvironment extends AbstractEnvironment {

        private final ByteArrayOutputStream body = new ByteArrayOutputStream();

        RecordingEnvironment() {
            super("", null, "");
            this.outputStream = this.body;
            this.objectModel = new HashMap();
        }

        int written() {
            return this.body.size();
        }

        public void setContentLength(int length) {
            if (this.body.size() > 0) {
                throw new IllegalArgumentException(
                        "setContentLength(" + length + ") when already written " + this.body.size());
            }
        }

        public void setContentType(String contentType) {
        }

        public String getContentType() {
            return "text/css";
        }

        public void setStatus(int statusCode) {
        }

        public boolean isExternal() {
            return true;
        }

        public void redirect(String url, boolean global, boolean permanent) {
        }
    }

    // ------------------------------------------------------------------------

    static final class MapCache implements Cache {
        private final Map entries = new ConcurrentHashMap();

        public void store(Serializable key, CachedResponse response) {
            this.entries.put(key, response);
        }

        public CachedResponse get(Serializable key) {
            return (CachedResponse) this.entries.get(key);
        }

        public void remove(Serializable key) {
            this.entries.remove(key);
        }

        public void clear() {
            this.entries.clear();
        }

        public boolean containsKey(Serializable key) {
            return this.entries.containsKey(key);
        }
    }

    /** Minimal Store; only the handful of methods the lock protocol uses do anything. */
    static final class MapStore implements Store {
        private final Map entries = new HashMap();
        private final AtomicInteger ignored = new AtomicInteger();

        public synchronized Object get(Object key) {
            return this.entries.get(key);
        }

        public synchronized void store(Object key, Object value) throws IOException {
            this.entries.put(key, value);
        }

        public synchronized void free() {
            this.ignored.incrementAndGet();
        }

        public synchronized void remove(Object key) {
            this.entries.remove(key);
        }

        public synchronized void clear() {
            this.entries.clear();
        }

        public synchronized boolean containsKey(Object key) {
            return this.entries.containsKey(key);
        }

        public synchronized Enumeration keys() {
            return Collections.enumeration(new ArrayList(this.entries.keySet()));
        }

        public synchronized int size() {
            return this.entries.size();
        }
    }

    /** Stand-in for the Spring request scope the lock protocol keys off. */
    static final class PerRequestAttributes implements RequestAttributes {
        private final Map attrs = new HashMap();

        public Object getAttribute(String name, int scope) { return this.attrs.get(name); }
        public void setAttribute(String name, Object value, int scope) { this.attrs.put(name, value); }
        public void removeAttribute(String name, int scope) { this.attrs.remove(name); }
        public String[] getAttributeNames(int scope) {
            return (String[]) this.attrs.keySet().toArray(new String[0]);
        }
        public void registerDestructionCallback(String name, Runnable callback, int scope) { }
        public Object resolveReference(String key) { return null; }
        public String getSessionId() { return "test-session"; }
        public Object getSessionMutex() { return this; }
    }

    /** Unused, present so the imports document what a real pipeline is wired to. */
    interface Unused extends StoreJanitor {
    }
}
