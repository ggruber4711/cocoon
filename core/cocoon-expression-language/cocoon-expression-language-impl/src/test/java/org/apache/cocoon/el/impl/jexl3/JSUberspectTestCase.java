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
package org.apache.cocoon.el.impl.jexl3;

import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.cocoon.el.ExpressionCompiler;
import org.apache.cocoon.el.impl.objectmodel.ObjectModelImpl;
import org.apache.cocoon.el.objectmodel.ObjectModel;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * Covers {@link JSUberspect}: reaching Rhino JavaScript objects from a jexl3 expression.
 *
 * <p>This is what makes jexl3 usable from flowscript, where the variables placed in the object
 * model are Rhino {@link Scriptable}s rather than Java beans. Without the uberspect a Scriptable
 * is introspected as an ordinary Java object, and <code>${user.name}</code> finds Rhino's own
 * class members instead of the script object's properties -- silently, yielding null.
 */
public class JSUberspectTestCase extends TestCase {

    private ExpressionCompiler jexl3;
    private Context context;
    private Scriptable scope;

    protected void setUp() throws Exception {
        super.setUp();
        this.jexl3 = new Jexl3Compiler();
        this.context = Context.enter();
        this.scope = this.context.initStandardObjects();
    }

    protected void tearDown() throws Exception {
        Context.exit();
        super.tearDown();
    }

    /** Evaluates a snippet of JavaScript and returns the resulting object. */
    private Object js(String source) {
        return this.context.evaluateString(this.scope, source, "test", 1, null);
    }

    private Object eval(String expression, Object jsValue) throws Exception {
        ObjectModel objectModel = new ObjectModelImpl();
        objectModel.put("o", jsValue);
        return this.jexl3.compile("jexl3", expression).evaluate(objectModel);
    }

    public void testReadsJavaScriptObjectProperties() throws Exception {
        Object obj = js("({name: 'ada', age: 36})");

        assertEquals("ada", eval("o.name", obj));
        // Rhino numbers are doubles.
        assertEquals(36.0, ((Number) eval("o.age", obj)).doubleValue(), 0.0);
    }

    /** The ternary over a script object is the combination that motivated all of this. */
    public void testTernaryOverJavaScriptObject() throws Exception {
        Object obj = js("({age: 36})");
        assertEquals("senior", eval("o.age > 30 ? 'senior' : 'junior'", obj));
    }

    public void testCallsJavaScriptMethods() throws Exception {
        Object obj = js("({greet: function(who) { return 'hi ' + who; }})");
        assertEquals("hi bob", eval("o.greet('bob')", obj));
    }

    /** A get-prefixed function is used as a property, mirroring the JEXL 1 introspector. */
    public void testFallsBackToJavaBeanStyleAccessor() throws Exception {
        Object obj = js("({getTitle: function() { return 'doctor'; }})");
        assertEquals("doctor", eval("o.title", obj));
    }

    public void testMissingPropertyIsNull() throws Exception {
        Object obj = js("({name: 'ada'})");
        assertNull(eval("o.nope", obj));
    }

    /** Java objects handed to script and back must come out unwrapped, not as Rhino wrappers. */
    public void testUnwrapsJavaObjects() throws Exception {
        Object obj = js("({})");
        ObjectModel objectModel = new ObjectModelImpl();
        objectModel.put("o", obj);
        org.mozilla.javascript.ScriptableObject.putProperty(
                (Scriptable) obj, "wrapped", Context.javaToJS("plain", this.scope));

        assertEquals("plain", this.jexl3.compile("jexl3", "o.wrapped").evaluate(objectModel));
    }

    public void testIteratesNativeArray() throws Exception {
        Object arr = js("['a', 'b', 'c']");
        ObjectModel objectModel = new ObjectModelImpl();
        objectModel.put("o", arr);

        Iterator iter = this.jexl3.compile("jexl3", "o").iterate(objectModel);
        StringBuffer seen = new StringBuffer();
        while (iter.hasNext()) {
            seen.append(iter.next());
        }
        assertEquals("abc", seen.toString());
    }

    public void testIteratesScriptableProperties() throws Exception {
        Object obj = js("({a: 1, b: 2})");
        ObjectModel objectModel = new ObjectModelImpl();
        objectModel.put("o", obj);

        Iterator iter = this.jexl3.compile("jexl3", "o").iterate(objectModel);
        int count = 0;
        while (iter.hasNext()) {
            iter.next();
            count++;
        }
        assertEquals(2, count);
    }

    /**
     * The uberspect must only intervene for Scriptables; plain Java objects still go through
     * JEXL's own introspector, honouring the engine's permissions.
     */
    public void testPlainJavaObjectsStillWork() throws Exception {
        ObjectModel objectModel = new ObjectModelImpl();
        objectModel.put("o", new Jexl3TestCase.Person("ada", 36));
        assertEquals("ada", this.jexl3.compile("jexl3", "o.name").evaluate(objectModel));
    }
}
