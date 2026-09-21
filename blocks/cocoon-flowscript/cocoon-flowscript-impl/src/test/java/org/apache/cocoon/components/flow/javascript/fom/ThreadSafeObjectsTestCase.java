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
package org.apache.cocoon.components.flow.javascript.fom;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * Guards {@code FOM_JavaScriptInterpreter.CONTEXT_FACTORY} against being removed or weakened.
 *
 * <p><b>The defect.</b> Rhino reworked {@code ScriptableObject}'s slot storage after 1.7R5. Up
 * to 1.7R5 slot access was synchronized; from 1.7.15 onwards the map is a plain, non-thread-safe
 * {@code SlotMapContainer} unless {@code FEATURE_THREAD_SAFE_OBJECTS} is enabled on the factory
 * that created the Context. Flowscript's {@code fom_system.js} is a load-on-startup script, so
 * every new session scope re-executes it, and it writes four methods onto the <em>shared</em>
 * {@code FOM_Cocoon} prototype. The execution happens outside the {@code compiledScripts} lock,
 * so sessions starting together modify one slot map concurrently.
 *
 * <p><b>Why it is a startup problem.</b> Only the first execution <em>inserts</em> the
 * properties; later ones overwrite slots that already exist, which is safe. Measured here:
 * concurrent first inserts corrupt roughly a third of attempts, concurrent overwrites corrupt
 * none. That matches how it presents in production -- a container that comes up broken now and
 * then, and once broken stays broken until it is restarted, because the lost accessor never
 * comes back.
 *
 * <p><b>Why this is a unit test.</b> An integration test against a running container cannot
 * catch it: by the time any test runs, something has already executed the system script and
 * warmed the prototype, so the burst only overwrites. That was tried and it passed with the fix
 * reverted, which is worse than no test.
 */
public class ThreadSafeObjectsTestCase extends TestCase {

    private static final int THREADS = 16;
    private static final int ROUNDS = 40;

    /** What fom_system.js does to the shared prototype on every scope creation. */
    private static final String SYSTEM_JS =
            "FOM_Cocoon.prototype.sendPageAndWait = function(uri, biz, fun, ttl) { return 1; };\n"
            + "FOM_Cocoon.prototype.handleContinuation = function(k, wk) { return 2; };\n"
            + "FOM_Cocoon.prototype.createWebContinuation = function(ttl) { return 3; };\n"
            + "FOM_Cocoon.prototype.exit = function() { return 4; };\n";

    /** A few of the accessors FOM_Cocoon publishes; losing any of them breaks every request. */
    private static final String[] EXPECTED = {
        "sendPageAndWait", "handleContinuation", "createWebContinuation", "exit",
        "request", "response", "session", "context", "parameters", "log",
    };

    /**
     * The interpreter must enter its Contexts from a factory with thread-safe objects on. This
     * is the assertion that fails if someone reverts the factory or swaps in
     * {@code Context.enter()}.
     */
    public void testInterpreterFactoryEnablesThreadSafeObjects() throws Exception {
        assertEquals("concurrent session starts corrupted the shared FOM_Cocoon prototype; "
                + "FOM_JavaScriptInterpreter.CONTEXT_FACTORY must enable "
                + "FEATURE_THREAD_SAFE_OBJECTS",
                0, corruptedRounds(FOM_JavaScriptInterpreter.CONTEXT_FACTORY));
    }

    /**
     * Shows the test can actually fail: the same race on a stock factory. If this ever reports
     * zero, either Rhino has changed its defaults or the harness has stopped racing, and the
     * test above has quietly become vacuous.
     */
    public void testStockFactoryStillDemonstratesTheHazard() throws Exception {
        int corrupted = corruptedRounds(new ContextFactory());
        assertTrue("a stock ContextFactory no longer corrupts the prototype under concurrent "
                + "first inserts. Either Rhino made objects thread-safe by default -- in which "
                + "case CONTEXT_FACTORY can go -- or this harness stopped reproducing the race, "
                + "in which case testInterpreterFactoryEnablesThreadSafeObjects proves nothing.",
                corrupted > 0);
    }

    // ------------------------------------------------------------------

    /** Runs the cold-insert race and returns how many rounds lost a property. */
    private int corruptedRounds(final ContextFactory factory) throws Exception {
        int corrupted = 0;
        for (int round = 0; round < ROUNDS; round++) {
            final Scriptable root;
            final Script compiled;
            Context setup = factory.enterContext();
            try {
                root = setup.initStandardObjects();
                // The prototype has to be created under this factory too: the feature is read
                // when the object is built, not when it is written to.
                ScriptableObject.defineClass(root, FOM_Cocoon.class);
                compiled = setup.compileString(SYSTEM_JS, "fom_system.js", 1, null);
            } finally {
                Context.exit();
            }

            final CountDownLatch startLine = new CountDownLatch(1);
            final CountDownLatch done = new CountDownLatch(THREADS);
            for (int t = 0; t < THREADS; t++) {
                new Thread("session-start-" + t) {
                    public void run() {
                        Context cx = factory.enterContext();
                        try {
                            startLine.await();
                            compiled.exec(cx, root);
                        } catch (Throwable swallowed) {
                            // A thrown exception is itself a symptom, but the property check
                            // below is the assertion; racing threads may fail either way.
                        } finally {
                            Context.exit();
                            done.countDown();
                        }
                    }
                }.start();
            }
            startLine.countDown();
            assertTrue("round " + round + " did not finish", done.await(30, TimeUnit.SECONDS));

            if (!missingProperties(factory, root).isEmpty()) {
                corrupted++;
            }
        }
        return corrupted;
    }

    private List missingProperties(ContextFactory factory, Scriptable root) {
        Context cx = factory.enterContext();
        try {
            List missing = new ArrayList();
            Scriptable proto =
                    (Scriptable) ScriptableObject.getClassPrototype(root, "FOM_Cocoon");
            for (int i = 0; i < EXPECTED.length; i++) {
                if (!ScriptableObject.hasProperty(proto, EXPECTED[i])) {
                    missing.add(EXPECTED[i]);
                }
            }
            try {
                ((ScriptableObject) proto).getIds();
            } catch (Throwable t) {
                // the slot map's size counter disagreeing with its contents is corruption too
                missing.add("getIds threw " + t.getClass().getSimpleName());
            }
            return missing;
        } finally {
            Context.exit();
        }
    }
}
