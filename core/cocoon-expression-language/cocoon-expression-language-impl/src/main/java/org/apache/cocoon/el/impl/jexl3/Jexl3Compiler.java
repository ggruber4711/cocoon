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

import org.apache.cocoon.el.Expression;
import org.apache.cocoon.el.ExpressionCompiler;
import org.apache.cocoon.el.ExpressionException;
import org.apache.commons.jexl3.JexlArithmetic;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.apache.commons.jexl3.introspection.JexlUberspect;
import org.apache.commons.logging.LogFactory;

/**
 * Compiles expressions with Commons JEXL 3, registered as the <code>jexl3</code> language.
 *
 * <p>This exists alongside {@link org.apache.cocoon.el.impl.jexl.JexlCompiler}, which stays on
 * JEXL 1 and is unchanged. The two libraries live in different packages
 * (<code>org.apache.commons.jexl</code> and <code>org.apache.commons.jexl3</code>), so both can
 * be on the classpath at once and a template can move from <code>jexl</code> to
 * <code>jexl3</code> one expression at a time.
 *
 * <p><b>Why bother:</b> JEXL 1 has no conditional operator. <code>a ? b : c</code> is not merely
 * unsupported, the <code>?</code> character is not a token in its grammar, so the expression
 * fails to parse. Anything conditional has to be written as <code>&lt;jx:choose&gt;</code> in the
 * surrounding template. JEXL 2 introduced the ternary and the null-coalescing
 * <code>a ?: b</code>; this brings both.
 *
 * <h3>The defaults deliberately weaken JEXL 3</h3>
 *
 * Out of the box JEXL 3 is strict where JEXL 1 was silent, and it would change the meaning of
 * existing templates rather than just extending them. The defaults here restore JEXL 1 behaviour,
 * verified expression by expression:
 *
 * <table border="1">
 * <caption>measured against commons-jexl 1.1</caption>
 * <tr><th>expression</th><th>JEXL 1.1</th><th>JEXL 3 default</th><th>here</th></tr>
 * <tr><td><code>undefinedVar</code></td><td>null</td><td>throws</td><td>null</td></tr>
 * <tr><td><code>nil + 1</code></td><td>1</td><td>throws</td><td>1</td></tr>
 * <tr><td><code>x / 0</code></td><td>0.0</td><td>throws</td><td>0.0</td></tr>
 * <tr><td><code>list[99]</code></td><td>null</td><td>throws</td><td>null</td></tr>
 * </table>
 *
 * <p>One difference could not be papered over: integer arithmetic widens differently.
 * <code>5 + 1</code> is a <code>Long</code> under JEXL 1 and an <code>Integer</code> here. That is
 * invisible in rendered output but visible to Java that casts the result.
 *
 * <h3>Permissions</h3>
 *
 * JEXL 3.3 introduced a sandbox, and {@link JexlPermissions#RESTRICTED} is its default. That
 * default is unusable for Cocoon: it denies method access on application classes, so
 * <code>${bean.property}</code> resolves to null. It fails silently rather than loudly -- a
 * <code>${p.age &gt; 30 ? 'senior' : 'junior'}</code> quietly takes the wrong branch. JEXL 1 had
 * no sandbox at all, so {@link JexlPermissions#UNRESTRICTED} is the faithful equivalent and is
 * the default here. Set {@link #setPermissions(String)} to <code>restricted</code> to opt in to
 * the sandbox, but expect to allow-list the packages your templates touch.
 *
 * @see JSUberspect
 * @since 2.3.1
 */
public class Jexl3Compiler implements ExpressionCompiler {

    private volatile JexlEngine engine;

    private boolean strict;
    private boolean silent = true;
    private boolean safe = true;
    private boolean lenientArithmetic = true;
    private String permissions = "unrestricted";
    private int cacheSize = 512;

    /**
     * @see org.apache.cocoon.el.ExpressionCompiler#compile(java.lang.String, java.lang.String)
     */
    public Expression compile(String language, String expression) throws ExpressionException {
        return new Jexl3Expression(language, expression, getEngine());
    }

    /**
     * The engine is immutable and shared; it is built on first use so that Spring has finished
     * setting properties, and so that a directly constructed instance works in a unit test
     * without an afterPropertiesSet callback.
     */
    private JexlEngine getEngine() {
        JexlEngine result = this.engine;
        if (result == null) {
            synchronized (this) {
                result = this.engine;
                if (result == null) {
                    result = this.engine = buildEngine();
                }
            }
        }
        return result;
    }

    private JexlEngine buildEngine() {
        JexlPermissions perms = "restricted".equalsIgnoreCase(this.permissions)
                ? JexlPermissions.RESTRICTED
                : JexlPermissions.UNRESTRICTED;

        // The uberspect has to be built with the same permissions the engine runs under,
        // otherwise the sandbox is bypassed for every non-Rhino object.
        JexlUberspect uberspect = new JSUberspect(
                LogFactory.getLog(JSUberspect.class), JexlUberspect.JEXL_STRATEGY, perms);

        return new JexlBuilder()
                .uberspect(uberspect)
                .permissions(perms)
                .strict(this.strict)
                .silent(this.silent)
                .safe(this.safe)
                .arithmetic(new JexlArithmetic(!this.lenientArithmetic))
                .cache(this.cacheSize)
                .create();
    }

    // ---------------------------------------------------------------- configuration

    /** Throw on unknown variables and null operands instead of returning null. Default false. */
    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    /** Swallow evaluation errors and return null. Default true, matching JEXL 1. */
    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    /** Treat dereferencing null as null rather than an error. Default true, matching JEXL 1. */
    public void setSafe(boolean safe) {
        this.safe = safe;
    }

    /** Coerce null to zero in arithmetic and yield 0.0 for division by zero. Default true. */
    public void setLenientArithmetic(boolean lenientArithmetic) {
        this.lenientArithmetic = lenientArithmetic;
    }

    /** Either <code>unrestricted</code> (default) or <code>restricted</code>; see the class comment. */
    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

    /** Size of the parsed-expression cache. */
    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }
}
