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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.apache.cocoon.el.Expression;
import org.apache.cocoon.el.ExpressionCompiler;
import org.apache.cocoon.el.ExpressionException;
import org.apache.cocoon.el.impl.jexl.JexlCompiler;
import org.apache.cocoon.el.impl.objectmodel.ObjectModelImpl;
import org.apache.cocoon.el.objectmodel.ObjectModel;

/**
 * Covers the <code>jexl3</code> expression language.
 *
 * <p>The point of these tests is not that JEXL 3 works -- that is Commons' problem -- but that
 * adding it does not change what an expression means. Most of the assertions are therefore
 * comparisons against the <code>jexl</code> (JEXL 1) compiler running the same expression.
 *
 * @see Jexl3Compiler
 */
public class Jexl3TestCase extends TestCase {

    /** A plain application bean, of the kind a template reaches through ${...}. */
    public static class Person {

        private final String name;
        private final int age;

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return this.name;
        }

        public int getAge() {
            return this.age;
        }

        public String greet(String who) {
            return "hi " + who + ", I am " + this.name;
        }
    }

    private ExpressionCompiler jexl3;
    private ExpressionCompiler jexl1;

    protected void setUp() throws Exception {
        super.setUp();
        this.jexl3 = new Jexl3Compiler();
        this.jexl1 = new JexlCompiler();
    }

    private Object eval(String expression, ObjectModel objectModel) throws ExpressionException {
        return this.jexl3.compile("jexl3", expression).evaluate(objectModel);
    }

    private Object eval(String expression) throws ExpressionException {
        return eval(expression, new ObjectModelImpl());
    }

    // ---------------------------------------------------------------- the reason this exists

    /**
     * The conditional operator, which is the whole motivation. Under JEXL 1 this does not merely
     * evaluate oddly, it fails to parse: '?' is not a token in the JEXL 1 grammar.
     */
    public void testTernaryWorksAndIsRejectedByJexl1() throws Exception {
        ObjectModel objectModel = new ObjectModelImpl();
        objectModel.put("x", new Integer(5));

        assertEquals("big", eval("x > 3 ? 'big' : 'small'", objectModel));
        assertEquals("small", eval("x > 10 ? 'big' : 'small'", objectModel));

        try {
            this.jexl1.compile("jexl", "x > 3 ? 'big' : 'small'").evaluate(objectModel);
            fail("JEXL 1 was expected to reject the ternary operator; if this now passes, the "
                    + "jexl3 language may no longer be needed for conditionals");
        } catch (ExpressionException expected) {
            // JEXL 1 cannot lex '?'
        }
    }

    /** The null-coalescing form, also absent from JEXL 1. */
    public void testElvis() throws Exception {
        ObjectModel objectModel = new ObjectModelImpl();
        objectModel.put("present", "here");

        assertEquals("fallback", eval("missing ?: 'fallback'", objectModel));
        assertEquals("here", eval("present ?: 'fallback'", objectModel));
    }

    // ---------------------------------------------------------------- JEXL 1 parity

    /**
     * JEXL 3 is strict where JEXL 1 was silent, and its defaults would throw on every one of
     * these. {@link Jexl3Compiler} deliberately configures it back to JEXL 1 behaviour; this is
     * the test that says so, because a future change to those defaults would otherwise break
     * templates quietly rather than at build time.
     */
    public void testLenientSemanticsMatchJexl1() throws Exception {
        ObjectModel objectModel = new ObjectModelImpl();
        objectModel.put("x", new Integer(5));
        objectModel.put("nil", null);
        objectModel.put("list", Arrays.asList(new String[] { "a", "b", "c" }));

        assertNull("unknown variable should be null, not an error", eval("undefinedVar", objectModel));
        assertNull("property of unknown variable should be null", eval("undefinedVar.someProp", objectModel));
        assertNull("index out of range should be null", eval("list[99]", objectModel));
        assertEquals("null should coerce to zero in arithmetic",
                new Integer(1), eval("nil + 1", objectModel));
        assertEquals("division by zero should yield 0.0, as in JEXL 1",
                new Double(0.0), eval("x / 0", objectModel));
    }

    /**
     * Every one of these must agree between the two languages. Anything that disagrees is a
     * migration hazard, because the same template text would mean two different things.
     */
    public void testAgreesWithJexl1() throws Exception {
        String[] expressions = {
            "1 + 2", "'a' + 'b'", "x > 3", "x == 5", "nil == null", "!empty(s)",
            "empty(nil)", "size(list)", "list[1]", "s.length()", "x > 3 and s == 'bob'",
            "x * 2", "x - 10", "s + x",
        };

        List<String> disagreements = new ArrayList<String>();
        for (int i = 0; i < expressions.length; i++) {
            Object viaJexl1 = this.jexl1.compile("jexl", expressions[i]).evaluate(model());
            Object viaJexl3 = this.jexl3.compile("jexl3", expressions[i]).evaluate(model());
            // Compared as text: JEXL 1 widens integers to Long and JEXL 3 keeps Integer, which
            // is a known and documented difference in representation, not in value.
            if (!String.valueOf(viaJexl1).equals(String.valueOf(viaJexl3))) {
                disagreements.add(expressions[i] + ": jexl=" + viaJexl1 + " jexl3=" + viaJexl3);
            }
        }
        assertEquals("expressions that mean different things under jexl and jexl3: "
                + disagreements, 0, disagreements.size());
    }

    /**
     * The one difference that could not be configured away, pinned so that it is a known
     * quantity rather than a surprise. Only visible to Java that casts an expression result.
     */
    public void testIntegerWideningDiffersFromJexl1() throws Exception {
        Object viaJexl1 = this.jexl1.compile("jexl", "1 + 2").evaluate(new ObjectModelImpl());
        Object viaJexl3 = eval("1 + 2");

        assertEquals(new Long(3), viaJexl1);
        assertEquals(new Integer(3), viaJexl3);
        assertEquals("the numeric value is the same either way",
                ((Number) viaJexl1).intValue(), ((Number) viaJexl3).intValue());
    }

    private ObjectModel model() {
        ObjectModel objectModel = new ObjectModelImpl();
        objectModel.put("x", new Integer(5));
        objectModel.put("s", "bob");
        objectModel.put("nil", null);
        objectModel.put("list", Arrays.asList(new String[] { "a", "b", "c" }));
        return objectModel;
    }

    // ---------------------------------------------------------------- the sandbox trap

    /**
     * Guards the permissions default.
     *
     * <p>JEXL 3.3 added a sandbox and made {@code JexlPermissions.RESTRICTED} the default. Under
     * it, methods on application classes are denied, so <code>person.name</code> evaluates to
     * null. It does not throw -- which is what makes it dangerous, because
     * <code>person.age &gt; 30 ? 'senior' : 'junior'</code> then silently takes the wrong branch
     * rather than failing. JEXL 1 had no sandbox, so {@link Jexl3Compiler} defaults to
     * unrestricted. If that default is ever changed, this test fails first.
     */
    public void testApplicationBeansAreReachable() throws Exception {
        ObjectModel objectModel = new ObjectModelImpl();
        objectModel.put("person", new Person("ada", 36));

        assertEquals("ada", eval("person.name", objectModel));
        assertEquals(new Integer(36), eval("person.age", objectModel));
        assertEquals("ada", eval("person.getName()", objectModel));
        assertEquals("hi bob, I am ada", eval("person.greet('bob')", objectModel));
        assertEquals("a bean property behind a ternary must not silently take the wrong branch",
                "senior", eval("person.age > 30 ? 'senior' : 'junior'", objectModel));
    }

    /** The sandbox can still be switched on deliberately. */
    public void testRestrictedPermissionsCanBeOptedInto() throws Exception {
        Jexl3Compiler restricted = new Jexl3Compiler();
        restricted.setPermissions("restricted");

        ObjectModel objectModel = new ObjectModelImpl();
        objectModel.put("person", new Person("ada", 36));

        assertNull("restricted permissions should deny the application bean",
                restricted.compile("jexl3", "person.name").evaluate(objectModel));
    }

    // ---------------------------------------------------------------- basics and iteration

    public void testExpression() throws Exception {
        assertEquals(new Integer(3), eval("1+2"));
    }

    public void testContextExpression() throws Exception {
        ObjectModel objectModel = new ObjectModelImpl();
        objectModel.put("a", new Long(1));
        objectModel.put("b", new Long(2));
        assertEquals(new Long(3), eval("a+b", objectModel));
    }

    public void testIterator() throws Exception {
        ObjectModel objectModel = new ObjectModelImpl();
        objectModel.put("arr", new String[] { "foo" });

        Expression expression = this.jexl3.compile("jexl3", "arr");
        Iterator iter = expression.iterate(objectModel);
        assertTrue("hasNext", iter.hasNext());
        assertEquals("foo", iter.next());
        assertFalse("hasNext", iter.hasNext());
    }

    public void testIterateOverNullYieldsEmptyIterator() throws Exception {
        Iterator iter = this.jexl3.compile("jexl3", "nothing").iterate(new ObjectModelImpl());
        assertNotNull(iter);
        assertFalse("iterating an unset variable should be empty, not fail", iter.hasNext());
    }

    public void testAssignIsUnsupportedAsForJexl1() throws Exception {
        try {
            this.jexl3.compile("jexl3", "a").assign(new ObjectModelImpl(), "x");
            fail("assign should be unsupported, matching the jexl language");
        } catch (UnsupportedOperationException expected) {
            // as for jexl
        }
    }
}
