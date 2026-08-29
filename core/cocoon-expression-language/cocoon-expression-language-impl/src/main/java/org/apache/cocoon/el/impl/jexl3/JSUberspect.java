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

import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlOperator;
import org.apache.commons.jexl3.internal.introspection.Uberspect;
import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.apache.commons.jexl3.introspection.JexlPropertyGet;
import org.apache.commons.jexl3.introspection.JexlPropertySet;
import org.apache.commons.jexl3.introspection.JexlUberspect;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaClass;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.Wrapper;

/**
 * A JEXL 3 uberspect that can walk Rhino JavaScript objects as well as Java ones.
 *
 * <p>This is the JEXL 3 port of {@link org.apache.cocoon.el.impl.jexl.JSIntrospector}, and it is
 * what makes a <code>jexl3</code> expression able to reach a variable produced by flowscript.
 * Without it, a {@link Scriptable} is an opaque Java bean and <code>${user.name}</code> resolves
 * against Rhino's own class members instead of the script object's properties.
 *
 * <p>Two things improved in the port, both consequences of the JEXL 3 API rather than choices:
 *
 * <ul>
 * <li>JEXL 1 offered no way to install an uberspect. The JEXL 1 code reflects into a private
 *     static field on <code>Introspector</code> from a static initialiser, with the comment
 *     "Hack: there's no _nice_ way to add my introspector to Jexl right now". It is global,
 *     order-dependent, and reports failure by printing a stack trace. JEXL 3 takes the uberspect
 *     as a {@link org.apache.commons.jexl3.JexlBuilder} argument, so the hack is gone and two
 *     engines can hold different uberspects.</li>
 * <li>The permissions the engine runs under are passed to {@code super}, so the sandbox still
 *     applies to everything that is not a {@link Scriptable}.</li>
 * </ul>
 *
 * <p>The Rhino calls used here have identical signatures in Rhino 1.6R7 (which this build uses)
 * and 1.7R5, so the class compiles and behaves the same against either. That matters because
 * applications embedding Cocoon commonly override the Rhino version.
 *
 * @see Jexl3Compiler
 * @since 2.3.1
 */
public class JSUberspect extends Uberspect {

    public JSUberspect(Log logger, JexlUberspect.ResolverStrategy strategy, JexlPermissions permissions) {
        super(logger, strategy, permissions);
    }

    // ------------------------------------------------------------------ dispatch

    @Override
    public Iterator<?> getIterator(Object obj) {
        if (!(obj instanceof Scriptable)) {
            // Enumeration and Iterator are handled by the base class too, but it logs a long
            // warning about them not being resettable every time. Cocoon passes both routinely.
            if (obj instanceof Enumeration) {
                return asIterator((Enumeration<?>) obj);
            }
            if (obj instanceof Iterator) {
                return (Iterator<?>) obj;
            }
            return super.getIterator(obj);
        }
        if (obj instanceof NativeArray) {
            return new NativeArrayIterator((NativeArray) obj);
        }
        return new ScriptableIterator((Scriptable) obj);
    }

    @Override
    public JexlMethod getMethod(Object obj, String methodName, Object... args) {
        return obj instanceof Scriptable
                ? new JSMethod((Scriptable) obj, methodName)
                : super.getMethod(obj, methodName, args);
    }

    @Override
    public JexlPropertyGet getPropertyGet(Object obj, Object identifier) {
        return obj instanceof Scriptable
                ? new JSPropertyGet((Scriptable) obj, toName(identifier))
                : super.getPropertyGet(obj, identifier);
    }

    /**
     * JEXL calls this overload when it has a resolver list for the operator in play, so it has to
     * be intercepted as well; overriding only the two-argument form would let property access on
     * a {@link Scriptable} fall through to the base introspector on those paths.
     */
    @Override
    public JexlPropertyGet getPropertyGet(List<PropertyResolver> resolvers, Object obj, Object identifier) {
        return obj instanceof Scriptable
                ? new JSPropertyGet((Scriptable) obj, toName(identifier))
                : super.getPropertyGet(resolvers, obj, identifier);
    }

    @Override
    public JexlPropertySet getPropertySet(Object obj, Object identifier, Object arg) {
        return obj instanceof Scriptable
                ? new JSPropertySet((Scriptable) obj, toName(identifier))
                : super.getPropertySet(obj, identifier, arg);
    }

    @Override
    public JexlPropertySet getPropertySet(List<PropertyResolver> resolvers, Object obj, Object identifier, Object arg) {
        return obj instanceof Scriptable
                ? new JSPropertySet((Scriptable) obj, toName(identifier))
                : super.getPropertySet(resolvers, obj, identifier, arg);
    }

    @Override
    public List<PropertyResolver> getResolvers(JexlOperator op, Object obj) {
        return super.getResolvers(op, obj);
    }

    // ------------------------------------------------------------------ members

    static class JSMethod implements JexlMethod {

        private final Scriptable scope;
        private final String name;

        JSMethod(Scriptable scope, String name) {
            this.scope = scope;
            this.name = name;
        }

        public Object invoke(Object thisArg, Object... args) throws Exception {
            Context cx = Context.enter();
            try {
                Scriptable thisObj = thisArg instanceof Scriptable
                        ? (Scriptable) thisArg
                        : Context.toObject(thisArg, this.scope);
                Object result = ScriptableObject.getProperty(thisObj, this.name);
                Object[] newArgs = null;
                if (args != null) {
                    newArgs = new Object[args.length];
                    for (int i = 0; i < args.length; i++) {
                        newArgs[i] = toJs(args[i], this.scope);
                    }
                }
                result = ScriptRuntime.call(cx, result, thisObj, newArgs, this.scope);

                return unwrap(result);
            } catch (JavaScriptException e) {
                throw new java.lang.reflect.InvocationTargetException(e);
            } finally {
                Context.exit();
            }
        }

        public Object tryInvoke(String name, Object obj, Object... args) {
            // Not cacheable, so JEXL never takes the fast path; decline it explicitly.
            return JexlEngine.TRY_FAILED;
        }

        public boolean tryFailed(Object rval) {
            return rval == JexlEngine.TRY_FAILED;
        }

        public boolean isCacheable() {
            return false;
        }

        public Class<?> getReturnType() {
            return Object.class;
        }
    }

    static class JSPropertyGet implements JexlPropertyGet {

        private final Scriptable scope;
        private final String name;

        JSPropertyGet(Scriptable scope, String name) {
            this.scope = scope;
            this.name = name;
        }

        public Object invoke(Object thisArg) throws Exception {
            Context cx = Context.enter();
            try {
                Scriptable thisObj = thisArg instanceof Scriptable
                        ? (Scriptable) thisArg
                        : Context.toObject(thisArg, this.scope);
                Object result = ScriptableObject.getProperty(thisObj, this.name);
                if (result == Scriptable.NOT_FOUND) {
                    // Fall back to a JavaBean-style accessor defined on the script object,
                    // so that ${o.foo} also finds a function named getFoo.
                    result = ScriptableObject.getProperty(thisObj, "get" + StringUtils.capitalize(this.name));
                    if (result != Scriptable.NOT_FOUND && result instanceof Function) {
                        result = ((Function) result).call(
                                cx, ScriptableObject.getTopLevelScope(thisObj), thisObj, new Object[] {});
                    }
                }

                return unwrap(result);
            } finally {
                Context.exit();
            }
        }

        public Object tryInvoke(Object obj, Object key) {
            return JexlEngine.TRY_FAILED;
        }

        public boolean tryFailed(Object rval) {
            return rval == JexlEngine.TRY_FAILED;
        }

        public boolean isCacheable() {
            return false;
        }
    }

    static class JSPropertySet implements JexlPropertySet {

        private final Scriptable scope;
        private final String name;

        JSPropertySet(Scriptable scope, String name) {
            this.scope = scope;
            this.name = name;
        }

        public Object invoke(Object thisArg, Object rhs) throws Exception {
            Context.enter();
            try {
                Scriptable thisObj = thisArg instanceof Scriptable
                        ? (Scriptable) thisArg
                        : Context.toObject(thisArg, this.scope);
                ScriptableObject.putProperty(thisObj, this.name, toJs(rhs, this.scope));
                return rhs;
            } finally {
                Context.exit();
            }
        }

        public Object tryInvoke(Object obj, Object key, Object value) {
            return JexlEngine.TRY_FAILED;
        }

        public boolean tryFailed(Object rval) {
            return rval == JexlEngine.TRY_FAILED;
        }

        public boolean isCacheable() {
            return false;
        }
    }

    // ------------------------------------------------------------------ iterators

    public static class NativeArrayIterator implements Iterator<Object> {

        private final NativeArray arr;
        private int index;

        public NativeArrayIterator(NativeArray arr) {
            this.arr = arr;
        }

        public boolean hasNext() {
            return this.index < (int) this.arr.getLength();
        }

        public Object next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Context.enter();
            try {
                return unwrap(this.arr.get(this.index++, this.arr));
            } finally {
                Context.exit();
            }
        }

        public void remove() {
            this.arr.delete(this.index);
        }
    }

    static class ScriptableIterator implements Iterator<Object> {

        private final Scriptable scope;
        private final Object[] ids;
        private int index;

        ScriptableIterator(Scriptable scope) {
            this.scope = scope;
            this.ids = scope.getIds();
        }

        public boolean hasNext() {
            return this.index < this.ids.length;
        }

        public Object next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Context.enter();
            try {
                return unwrap(ScriptableObject.getProperty(this.scope, this.ids[this.index++].toString()));
            } finally {
                Context.exit();
            }
        }

        public void remove() {
            Context.enter();
            try {
                this.scope.delete(this.ids[this.index].toString());
            } finally {
                Context.exit();
            }
        }
    }

    private static Iterator<Object> asIterator(final Enumeration<?> e) {
        return new Iterator<Object>() {

            public boolean hasNext() {
                return e.hasMoreElements();
            }

            public Object next() {
                return e.nextElement();
            }

            public void remove() {
                // no action
            }
        };
    }

    // ------------------------------------------------------------------ conversion

    /**
     * JEXL 3 passes a property identifier as an Object, because it may be an array index or a map
     * key rather than a name. Rhino property access needs a String.
     */
    private static String toName(Object identifier) {
        return identifier == null ? null : identifier.toString();
    }

    /** Wraps a Java value for Rhino, leaving values Rhino already understands alone. */
    private static Object toJs(Object value, Scriptable scope) {
        if (value == null
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof String
                || value instanceof Scriptable) {
            return value;
        }
        return Context.toObject(value, scope);
    }

    private static Object unwrap(Object result) {
        if (result == Undefined.instance || result == Scriptable.NOT_FOUND) {
            return null;
        }

        if (!(result instanceof NativeJavaClass)) {
            Object value;
            while (result instanceof Wrapper) {
                value = ((Wrapper) result).unwrap();
                if (value == result) {
                    break;
                }
                result = value;
            }
        }

        return result;
    }
}
