/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

 Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.

 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:

 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:  "This product includes  software
    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
    Alternately, this  acknowledgment may  appear in the software itself,  if
    and wherever such third-party acknowledgments normally appear.

 4. The names "Apache Cocoon" and  "Apache Software Foundation" must  not  be
    used to  endorse or promote  products derived from  this software without
    prior written permission. For written permission, please contact
    apache@apache.org.

 5. Products  derived from this software may not  be called "Apache", nor may
    "Apache" appear  in their name,  without prior written permission  of the
    Apache Software Foundation.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation and was  originally created by
 Stefano Mazzocchi  <stefano@apache.org>. For more  information on the Apache
 Software Foundation, please see <http://www.apache.org/>.

*/
package org.apache.cocoon.generation;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.excalibur.source.Source;
import org.apache.excalibur.source.SourceException;
import org.apache.cocoon.components.source.SourceUtil;
import org.apache.cocoon.environment.Environment;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.components.flow.WebContinuation;
import org.apache.cocoon.xml.*;
import java.io.*;
import java.util.*;
import org.xml.sax.*;
import org.xml.sax.ext.*;
import org.xml.sax.helpers.*;
import org.apache.commons.jxpath.*;
import org.apache.commons.jexl.*;
import org.apache.commons.jexl.util.*;
import org.apache.commons.jexl.util.introspection.*;
import org.mozilla.javascript.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.beans.PropertyDescriptor;
/**
 *
 *  <p>Provides a tag library with embedded JSTL and XPath expression substitution
 *  to access data sent by Cocoon flowscripts.</p>
 *  The embedded expression language allows a page author to access an 
 *  object using a simplified syntax such as
 *  <p><pre>
 *  &lt;site signOn="${accountForm.signOn}"&gt;
 *  </pre></p>
 * <p>Embedded Jexl expressions are contained in <code>${}</code>.</p>
 * <p>Embedded XPath expressions are contained in <code>#{}</code>.</p>
 * <p>Note that since this generator uses <a href="http://jakarta.apache.org/commons/jxpath">Apache JXPath</a> and <a href="http://jakarta.apache.org/commons/jexl">Apache Jexl</a>, the referenced 
 * objects may be Java Beans, DOM, JDOM, or JavaScript objects from a 
 * Flowscript. The current Web Continuation from the Flowscript 
 * is also available as an variable named <code>continuation</code>. You would 
 * typically access its <code>id</code>:
 * <p><pre>
 *    &lt;form action="${continuation.id}"&gt;
 * </pre></p>
 * <p>You can also reach previous continuations by using the <code>getContinuation()</code> function:</p>
 * <p><pre>
 *     &lt;form action="${continuation.getContinuation(1).id}" >
 * </pre></p>
 * <p>The <code>if</code> tag allows the conditional execution of its body 
 * according to value of a <code>test</code> attribute:</p>
 * <p><pre>
 *   &lt;if test="Expression"&gt;
 *       body
 *   &lt;/if&gt;
 * </pre></p>
 * <p>The <code>choose</code> tag performs conditional block execution by the 
 * embedded <code>when</code> sub tags. It renders the body of the first 
 * <code>when</code> tag whose <code>test</code> condition evaluates to true. 
 * If none of the <code>test</code> conditions of nested <code>when</code> tags
 * evaluate to <code>true</code>, then the body of an <code>otherwise</code> 
 * tag is evaluated, if present:</p>
 * <p><pre>
 *  &lt;choose&gt;
 *    &lt;when test="Expression"&gt;
 *       body
 *    &lt;/when&gt;
 *    &lt;otherwise&gt;
 *       body
 *    &lt;/otherwise&gt;
 *  &lt;/choose&gt;
 * </pre></p>
 * <p>The <code>out</code> tag evaluates an expression and outputs 
 * the result of the evaluation:</p>
 * <p><pre>
 * &lt;out value="Expression"/&gt;
 * </pre></p>
 * <p>The <code>forEach</code> tag allows you to iterate over a collection 
 * of objects:<p>
 * <p><pre>
 *   &lt;forEach var="name" items="Expression" begin="n" end="n" step="n"&gt;
 *     body
 *  &lt;/forEach&gt;
 *   &lt;forEach select="XPathExpression" begin="n" end="n" step="n"&gt;
 *     body
 *  &lt;/forEach&gt;
 * </pre></p>
 *
 *
 */

public class JexlTemplate extends AbstractGenerator {

    private static final JXPathContextFactory 
        jxpathContextFactory = JXPathContextFactory.newInstance();

    /**
     * Jexl Introspector that supports Rhino JavaScript objects
     * as well as Java Objects
     */
    static public class JSIntrospector extends UberspectImpl {
        
        public static class JSMethod implements VelMethod {
            
            Scriptable scope;
            String name;
            
            public JSMethod(Scriptable scope, String name) {
                this.scope = scope;
                this.name = name;
            }
            
            public Object invoke(Object thisArg, Object[] args)
                throws Exception {
                Context cx = Context.enter();
                try {
                    Object result; 
                    Scriptable thisObj;
                    if (!(thisArg instanceof Scriptable)) {
                        thisObj = Context.toObject(thisArg, scope);
                    } else {
                        thisObj = (Scriptable)thisArg;
                    }
                    result = ScriptableObject.getProperty(thisObj, name);
                    Object[] newArgs = null;
                    if (args != null) {
                        newArgs = new Object[args.length];
                        for (int i = 0; i < args.length; i++) {
                            newArgs[i] = args[i];
                            if (args[i] != null && 
                                !(args[i] instanceof Number) &&
                                !(args[i] instanceof Boolean) &&
                                !(args[i] instanceof String) &&
                                !(args[i] instanceof Scriptable)) {
                                newArgs[i] = Context.toObject(args[i], scope);
                            }
                        }
                    }
                    result = ScriptRuntime.call(cx, result, thisObj, 
                                                newArgs, scope);
                    if (result == Undefined.instance ||
                        result == ScriptableObject.NOT_FOUND) {
                        result = null;
                    } else while (result instanceof Wrapper) {
                        result = ((Wrapper)result).unwrap();
                    }
                    return result;
                } catch (JavaScriptException e) {
                    throw new java.lang.reflect.InvocationTargetException(e);
                } finally {
                    Context.exit();
                }
            }
            
            public boolean isCacheable() {
                return false;
            }
            
            public String getMethodName() {
                return name;
            }
            
            public Class getReturnType() {
                return Object.class;
            }
            
        }
        
        public static class JSPropertyGet implements VelPropertyGet {
            
            Scriptable scope;
            String name;
            
            public JSPropertyGet(Scriptable scope, String name) {
                this.scope = scope;
                this.name = name;
            }
            
            public Object invoke(Object thisArg) throws Exception {
                Context.enter();
                try {
                    Scriptable thisObj;
                    if (!(thisArg instanceof Scriptable)) {
                        thisObj = Context.toObject(thisArg, scope);
                    } else {
                        thisObj = (Scriptable)thisArg;
                    }
                    Object result = ScriptableObject.getProperty(thisObj, name);
                    if (result == Undefined.instance || 
                        result == ScriptableObject.NOT_FOUND) {
                        result = null;
                    } else while (result instanceof Wrapper) {
                        result = ((Wrapper)result).unwrap();
                    }
                    return result;
                } finally {
                    Context.exit();
                }
            }
            
            public boolean isCacheable() {
                return false;
            }
            
            public String getMethodName() {
                return name;
            }
            
        }
        
        public static class JSPropertySet implements VelPropertySet {
            
            Scriptable scope;
            String name;
            
            public JSPropertySet(Scriptable scope, String name) {
                this.scope = scope;
                this.name = name;
            }
            
            public Object invoke(Object thisArg, Object rhs) throws Exception {
                Context.enter();
                try {
                    Scriptable thisObj;
                    Object arg = rhs;
                    if (!(thisArg instanceof Scriptable)) {
                        thisObj = Context.toObject(thisArg, scope);
                    } else {
                        thisObj = (Scriptable)thisArg;
                    }
                    if (arg != null && 
                        !(arg instanceof Number) &&
                        !(arg instanceof Boolean) &&
                        !(arg instanceof String) &&
                        !(arg instanceof Scriptable)) {
                        arg = Context.toObject(arg, scope);
                    }
                    ScriptableObject.putProperty(thisObj, name, arg);
                    return rhs;
                } finally {
                    Context.exit();
                }
            }
            
            public boolean isCacheable() {
                return false;
            }
            
            public String getMethodName() {
                return name;        
            }
        }
        
        public static class NativeArrayIterator implements Iterator {
            
            NativeArray arr;
            int index;
            
            public NativeArrayIterator(NativeArray arr) {
                this.arr = arr;
                this.index = 0;
            }
            
            public boolean hasNext() {
                return index < (int)arr.jsGet_length();
            }
            
            public Object next() {
                Context.enter();
                try {
                    Object result = arr.get(index++, arr);
                    if (result == Undefined.instance ||
                        result == ScriptableObject.NOT_FOUND) {
                        result = null;
                    } else while (result instanceof Wrapper) {
                        result = ((Wrapper)result).unwrap();
                    }
                    return result;
                } finally {
                    Context.exit();
                }
            }
            
            public void remove() {
                arr.delete(index);
            }
        }
        
        public static class ScriptableIterator implements Iterator {
            
            Scriptable scope;
            Object[] ids;
            int index;
            
            public ScriptableIterator(Scriptable scope) {
                this.scope = scope;
                this.ids = scope.getIds();
                this.index = 0;
            }
            
            public boolean hasNext() {
                return index < ids.length;
            }
            
            public Object next() {
                Context.enter();
                try {
                    Object result = 
                        ScriptableObject.getProperty(scope, 
                                                     ids[index++].toString());
                    if (result == Undefined.instance ||
                        result == ScriptableObject.NOT_FOUND) {
                        result = null;
                    } else while (result instanceof Wrapper) {
                        result = ((Wrapper)result).unwrap();
                    }
                    return result;
                } finally {
                    Context.exit();
                }
            }
            
            public void remove() {
                Context.enter();
                try {
                    scope.delete(ids[index].toString());
                } finally {
                    Context.exit();
                }
            }
        }
        
        public Iterator getIterator(Object obj, Info i)
            throws Exception {
            if (!(obj instanceof Scriptable)) {
                return super.getIterator(obj, i);
            }
            if (obj instanceof NativeArray) {
                return new NativeArrayIterator((NativeArray)obj);
            }
            return new ScriptableIterator((Scriptable)obj);
        }
        
        public VelMethod getMethod(Object obj, String methodName, 
                                   Object[] args, Info i)
            throws Exception {
            if (!(obj instanceof Scriptable)) {
                return super.getMethod(obj, methodName, args, i);
            }
            return new JSMethod((Scriptable)obj, methodName);
        }
        
        public VelPropertyGet getPropertyGet(Object obj, String identifier, 
                                             Info i)
            throws Exception {
            if (!(obj instanceof Scriptable)) {
                return super.getPropertyGet(obj, identifier, i);
            }
            return new JSPropertyGet((Scriptable)obj, identifier);
        }
        
        public VelPropertySet getPropertySet(Object obj, String identifier, 
                                             Object arg, Info i)
            throws Exception {
            if (!(obj instanceof Scriptable)) {
                return super.getPropertySet(obj, identifier, arg, i);
            }
            return new JSPropertySet((Scriptable)obj, identifier);
        }
    }

    static class MyJexlContext 
        extends HashMap implements JexlContext {
        public Map getVars() {
            return this;
        }
        public void setVars(Map map) {
            putAll(map);
        }
        public Object get(Object key) {
            Object result = super.get(key);
            if (result != null) {
                return result;
            }
            MyJexlContext c = closure;
            for (; c != null; c = c.closure) {
                result = c.get(key);
                if (result != null) {
                    return result;
                }
            }
            return result;
        }
        MyJexlContext closure;
        MyJexlContext() {
        }
        MyJexlContext(MyJexlContext closure) {
            this.closure = closure;
        }
    }

    static class MyVariables implements Variables {
        Map localVariables = new HashMap();

        static final String[] VARIABLES = new String[] {
            "continuation",
            "flowContext",
            "request",
            "response",
            "context",
            "session",
            "parameters"
        };

        Object bean, kont, request, response,
            session, context, parameters;

        MyVariables(Object bean, WebContinuation kont,
                    Request request, Response response,
                    org.apache.cocoon.environment.Context context,
                    Parameters parameters) {
            this.bean = bean;
            this.kont = kont;
            this.request = request;
            this.session = request.getSession(false);
            this.response = response;
            this.context = context;
            this.parameters = parameters;
        }

        public boolean isDeclaredVariable(String varName) {
            for (int i = 0; i < VARIABLES.length; i++) {
                if (varName.equals(VARIABLES[i])) {
                    return true;
                }
            }
            return localVariables.containsKey(varName);
        }
        
        public Object getVariable(String varName) {
            if (varName.equals("continuation")) {
                return kont;
            } else if (varName.equals("flowContext")) {
                return bean;
            } else if (varName.equals("request")) {
                return request;
            } else if (varName.equals("response")) {
                return response;
            } else if (varName.equals("session")) {
                return session;
            } else if (varName.equals("context")) {
                return context;
            } else if (varName.equals("parameters")) {
                return parameters;
            }
            return localVariables.get(varName);
        }
        
        public void declareVariable(String varName, Object value) {
            localVariables.put(varName, value);
        }
        
        public void undeclareVariable(String varName) {
            localVariables.remove(varName);
        }
    }

    static {
        // Hack: there's no _nice_ way to add my introspector to Jexl right now
        try {
            Field field = 
                org.apache.commons.jexl.util.Introspector.class.getDeclaredField("uberSpect");
            field.setAccessible(true);
            field.set(null, new JSIntrospector());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    final static String JEXL_NS = 
        "http://cocoon.apache.org/transformation/jexl/1.0";

    final static String TEMPLATE = "template";
    final static String FOR_EACH = "forEach";
    final static String IF = "if";
    final static String CHOOSE = "choose";
    final static String WHEN = "when";
    final static String OTHERWISE = "otherwise";
    final static String OUT = "out";
    final static String IMPORT = "import";
    final static String DEFINE = "define";


    /**
     * Compile a single Jexl expr (contained in ${}) or XPath expression
     * (contained in #{}) 
     */

    private Object compileExpr(String inStr) throws Exception {
        try {
            if (inStr == null) return null;
            StringReader in = new StringReader(inStr.trim());
            int ch;
            StringBuffer expr = new StringBuffer();
            boolean xpath = false;
            boolean inExpr = false;
            while ((ch = in.read()) != -1) {
                char c = (char)ch;
                if (inExpr) {
                    if (c == '}') {
                        String str = expr.toString();
                        return compile(str, xpath);
                    } else if (c == '\\') {
                        ch = in.read();
                        if (ch == -1) {
                            expr.append('\\');
                        } else {
                            expr.append((char)ch);
                        }
                    } else {
                        expr.append(c);
                    }
                } else {
                    if (c == '$' || c == '#') {
                        ch = in.read();
                        if (ch == '{') {
                            inExpr = true;
                            xpath = c == '#';
                            continue;
                        }
                    } 
                    // hack: invalid expression?
                    // just return the original and swallow exception
                    return inStr;
                }
            }
        } catch (IOException ignored) {
            ignored.printStackTrace();
        }
        return inStr;
    }

    private Object compile(final String variable, boolean xpath) 
        throws Exception {
        if (xpath) {
            return JXPathContext.compile(variable);
        } else {
            return ExpressionFactory.createExpression(variable);
        }
    }

    private Object getValue(Object expr, JexlContext jexlContext,
                            JXPathContext jxpathContext) 
        throws Exception {
        if (expr instanceof CompiledExpression) {
            CompiledExpression e = (CompiledExpression)expr;
            return e.getValue(jxpathContext);
        } else {
            org.apache.commons.jexl.Expression e = 
                (org.apache.commons.jexl.Expression)expr;
            return e.evaluate(jexlContext);
        }
    }

    class Event {
        final Locator location;
        Event next;
        Event(Locator location) {
            this.location = new LocatorImpl(location);
        }

        public String locationString() {
            String result = "";
            String systemId = location.getSystemId();
            if (systemId != null) {
                result += systemId + ", ";
            }
            result += "Line " + location.getLineNumber();
            int col = location.getColumnNumber();
            if (col > 0) {
                result += "." + col;
            }
            return result;
        }
    }

    class TextEvent extends Event {
        TextEvent(Locator location, 
                  char[] chars, int start, int length) 
            throws SAXException {
            super(location);
            StringBuffer buf = new StringBuffer();
            CharArrayReader in = new CharArrayReader(chars, start, length);
            int ch;
            boolean inExpr = false;
            boolean xpath = false;
            try {
                top: while ((ch = in.read()) != -1) {
                    char c = (char)ch;
                    if (inExpr) {
                        if (c == '}') {
                            String str = buf.toString();
                            Object compiledExpression;
                            try {
                                if (xpath) {
                                    compiledExpression = 
                                        JXPathContext.compile(str);
                                } else {
                                    compiledExpression = 
                                        ExpressionFactory.createExpression(str);
                                }
                            } catch (Exception exc) {
                                throw new SAXParseException(exc.getMessage(),
                                                            location,
                                                            exc);
                            }
                            substitutions.add(compiledExpression);
                            buf.setLength(0);
                            inExpr = false;
                        } else if (c == '\\') {
                            ch = in.read();
                            if (ch == -1) {
                                buf.append('\\');
                            } else {
                                buf.append((char)ch);
                            } 
                        } else {
                            buf.append(c);
                        }
                    } else {
                        if (c == '\\') {
                            ch = in.read();
                            if (ch == -1) {
                                buf.append('\\');
                            } else {
                                buf.append((char)ch);
                            }
                        } else if (c == '$' || c == '#') {
                            while (c == '$' || c == '#') {
                                ch = in.read();
                                if (ch == '{') {
                                    xpath = c == '#';
                                    inExpr = true;
                                    if (buf.length() > 0) {
                                        char[] charArray = 
                                            new char[buf.length()];
                                        
                                        buf.getChars(0, buf.length(),
                                                     charArray, 0);
                                        substitutions.add(charArray);
                                        buf.setLength(0);
                                    }
                                    continue top;
                                } else if (ch == -1) {
                                    break;
                                }
                                buf.append(c);
                                c = (char)ch;
                            }
                        } else {
                            buf.append(c);
                        }
                    }
                }
            } catch (IOException ignored) {
                ignored.printStackTrace();
            }
            if (buf.length() > 0) {
                char[] charArray = 
                    new char[buf.length()];
                buf.getChars(0, buf.length(), charArray, 0);
                substitutions.add(charArray);
            } else if (substitutions.size() == 0) {
                substitutions.add(EMPTY_CHARS);
            }
        }

        final List substitutions = new LinkedList();
    }

    class Characters extends TextEvent {
        Characters(Locator location, 
                   char[] chars, int start, int length) 
            throws SAXException {
            super(location, chars, start, length);
        }
    }

    class StartDocument extends Event {
        StartDocument(Locator location) {
            super(location);
        }
        long compileTime;
        EndDocument endDocument; // null if document fragment
    }

    class EndDocument extends Event {
        EndDocument(Locator location) {
            super(location);
        }
    }

    class EndElement extends Event {
        EndElement(Locator location, 
                   StartElement startElement) {
            super(location);
            this.startElement = startElement;
        }
        final StartElement startElement;
    }

    class EndPrefixMapping extends Event {
        EndPrefixMapping(Locator location, String prefix) {
            super(location);
            this.prefix = prefix;
        }
        final String prefix;
    }
    
    class IgnorableWhitespace extends TextEvent {
        IgnorableWhitespace(Locator location, 
                            char[] chars, int start, int length) 
            throws SAXException {
            super(location, chars, start, length);
        }
    }

    class ProcessingInstruction extends Event {
        ProcessingInstruction(Locator location,
                              String target, String data) {
            super(location);
            this.target = target;
            this.data = data;
        }
        final String target;
        final String data;
    }

    class SkippedEntity extends Event {
        SkippedEntity(Locator location, String name) {
            super(location);
            this.name = name;
        }
        final String name;
    }

    abstract class AttributeEvent {
        AttributeEvent(String namespaceURI, String localName, String raw,
                       String type) {
            this.namespaceURI = namespaceURI;
            this.localName = localName;
            this.raw = raw;
            this.type = type;
        }
        final String namespaceURI;
        final String localName;
        final String raw;
        final String type;
    }
    
    class CopyAttribute extends AttributeEvent {
        CopyAttribute(String namespaceURI, 
                      String localName,
                      String raw,
                      String type, String value) {
            super(namespaceURI, localName, raw, type);
            this.value = value;
        }
        final String value;
    }
    
    class Subst {
    }
    
    class Literal extends Subst {
        Literal(String val) {
            this.value = val;
        }
        final String value;
    }
    
    class Expression extends Subst {
        Expression(Object expr) {
            this.compiledExpression = expr;
        }
        final Object compiledExpression;
    }

    class SubstituteAttribute extends AttributeEvent {
        SubstituteAttribute(String namespaceURI,
                            String localName,
                            String raw,
                            String type, List substs) {
            super(namespaceURI, localName, raw, type);
            this.substitutions = substs;
        }
        final List substitutions;
    }

    class StartElement extends Event {
        StartElement(Locator location, String namespaceURI,
                     String localName, String raw,
                     Attributes attrs) 
            throws SAXException {
            super(location);
            this.namespaceURI = namespaceURI;
            this.localName = localName;
            this.raw = raw;
            this.qname = "{"+namespaceURI+"}"+localName;
            StringBuffer buf = new StringBuffer();
            for (int i = 0, len = attrs.getLength(); i < len; i++) {
                String uri = attrs.getURI(i);
                String local = attrs.getLocalName(i);
                String qname = attrs.getQName(i);
                String type = attrs.getType(i);
                String value = attrs.getValue(i);
                StringReader in = new StringReader(value);
                int ch;
                buf.setLength(0);
                boolean inExpr = false;
                List substEvents = new LinkedList();
                boolean xpath = false;
                try {
                    top: while ((ch = in.read()) != -1) {
                        char c = (char)ch;
                        if (inExpr) {
                            if (c == '}') {
                                String str = buf.toString();
                                Object compiledExpression;
                                try {
                                    compiledExpression =
                                        compile(str, xpath);
                                } catch (Exception exc) {
                                    throw new SAXParseException(exc.getMessage(),
                                                                location,
                                                                exc);
                                                                
                                } 
                                substEvents.add(new Expression(compiledExpression));
                                buf.setLength(0);
                                inExpr = false;
                            } else if (c == '\\') {
                                ch = in.read();
                                if (ch == -1) {
                                    buf.append('\\');
                                } else {
                                    buf.append((char)ch);
                                }
                            } else {
                                buf.append(c);
                            }
                        } else {
                            if (c == '\\') {
                                ch = in.read();
                                if (ch == -1) {
                                    buf.append('\\');
                                } else {
                                    buf.append((char)ch);
                                }
                            } else {
                                if (c == '$' || c == '#') {
                                    while (c == '$' || c == '#') {
                                        ch = in.read();
                                        if (ch == '{') {
                                            if (buf.length() > 0) {
                                                substEvents.add(new Literal(buf.toString()));
                                                buf.setLength(0);
                                            }
                                            inExpr = true;
                                            xpath = c == '#';
                                            continue top;
                                        } else if (ch == -1) {
                                            break;
                                        }
                                        buf.append(c);
                                        c = (char)ch;
                                    }
                                } else {
                                    buf.append(c);
                                }
                            }
                        }
                    }
                } catch (IOException ignored) {
                    ignored.printStackTrace();
                }
                if (buf.length() > 0) {
                    if (substEvents.size() == 0) {
                        attributeEvents.add(new CopyAttribute(uri,
                                                              local,
                                                              qname,
                                                              type,
                                                              value));
                    } else {
                        substEvents.add(new Literal(buf.toString()));
                        attributeEvents.add(new SubstituteAttribute(uri,
                                                                    local,
                                                                    qname,
                                                                    type,
                                                                    substEvents));
                    }
                } else {
                    if (substEvents.size() > 0) {
                        attributeEvents.add(new SubstituteAttribute(uri,
                                                                    local,
                                                                    qname,
                                                                    type,
                                                                    substEvents));
                    } else {
                        attributeEvents.add(new CopyAttribute(uri, local,
                                                              qname, type,
                                                               ""));
                    }
                }
            }
        }
        final String namespaceURI;
        final String localName;
        final String raw;
        final String qname;
        final List attributeEvents = new LinkedList();
        EndElement endElement;
    }

    class StartForEach extends Event {
        StartForEach(Locator location, Object items, String var,
                     int begin, int end, int step) {
            super(location);
            this.items = items;
            this.var = var;
            this.begin = begin;
            this.end = end;
            this.step = step;
        }
        final Object items;
        final String var;
        final int begin;
        final int end;
        final int step;
        EndForEach endForEach;
    }
    
    class EndForEach extends Event {
        EndForEach(Locator location) {
            super(location);
        }
    }

    class StartIf extends Event {
        StartIf(Locator location, Object test) {
            super(location);
            this.test = test;
        }
        final Object test;
        EndIf endIf;
    }

    class EndIf extends Event {
        EndIf(Locator location) {
            super(location);
        }
    }

    class StartChoose extends Event {
        StartChoose(Locator location) {
            super(location);
        }
        StartWhen firstChoice;
        StartOtherwise otherwise;
        EndChoose endChoose;
    }

    class EndChoose extends Event {
        EndChoose(Locator location) {
            super(location);
        }
    }

    class StartWhen extends Event {
        StartWhen(Locator location, Object test) {
            super(location);
            this.test = test;
        }
        final Object test;
        StartWhen nextChoice;
        EndWhen endWhen;
    }

    class EndWhen extends Event {
        EndWhen(Locator location) {
            super(location);
        }
    }

    class StartOtherwise extends Event {
        StartOtherwise(Locator location) {
            super(location);
        }
        EndOtherwise endOtherwise;
    }

    class EndOtherwise extends Event {
        EndOtherwise(Locator location) {
            super(location);
        }
    }

    class StartPrefixMapping extends Event {
        StartPrefixMapping(Locator location, String prefix,
                           String uri) {
            super(location);
            this.prefix = prefix;
            this.uri = uri;
        }
        final String prefix;
        final String uri;
    }

    class Comment extends TextEvent {
        Comment(Locator location, char[] chars,
                int start, int length)
            throws SAXException {
            super(location, chars, start, length);
        }
    }

    class EndCDATA extends Event {
        EndCDATA(Locator location) {
            super(location);
        }
    }

    class EndDTD extends Event {
        EndDTD(Locator location) {
            super(location);
        }
    }

    class EndEntity extends Event {
        EndEntity(Locator location, String name) {
            super(location);
            this.name = name;
        }
        final String name;
    }

    class StartCDATA extends Event {
        StartCDATA(Locator location) {
            super(location);
        }
    }

    class StartDTD extends Event {
        StartDTD(Locator location, String name, 
                 String publicId, String systemId) {
            super(location);
            this.name = name;
            this.publicId = publicId;
            this.systemId = systemId;
        }
        final String name;
        final String publicId;
        final String systemId;
    }
    
    class StartEntity extends Event {
        public StartEntity(Locator location, String name) {
            super(location);
            this.name = name;
        }
        final String name;
    }

    class StartOut extends Event {
        StartOut(Locator location, Object expr) {
            super(location);
            this.compiledExpression = expr;
        }
        final Object compiledExpression;
    }

    class EndOut extends Event {
        EndOut(Locator location) {
            super(location);
        }
    }

    class StartImport extends Event {
        StartImport(Locator location, AttributeEvent uri, 
                    Object select) {
            super(location);
            this.uri = uri;
            this.select = select;
        }
        final AttributeEvent uri;
        final Object select;
        EndImport endImport;
    }

    class EndImport extends Event {
        EndImport(Locator location) {
            super(location);
        }
    }

    class StartTemplate extends Event {
        StartTemplate(Locator location) {
            super(location);
        }
        EndTemplate endTemplate;
    }

    class EndTemplate extends Event {
        EndTemplate(Locator location) {
            super(location);
        }
    }

    class StartDefine extends Event {
        StartDefine(Locator location, String namespace, String name,
                    Map formalParameters) {
            super(location);
            this.namespace = namespace;
            this.name = name;
            this.qname = "{"+namespace+"}"+name;
            this.parameters = formalParameters;
        }
        final String namespace;
        final String name;
        final String qname;
        final Map parameters;
        EndDefine endDefine;
    }

    class EndDefine extends Event {
        EndDefine(Locator location) {
            super(location);
        }
    }


    class Parser implements LexicalHandler, ContentHandler {

        StartDocument startEvent;
        Event lastEvent;
        Stack stack = new Stack();
        Locator locator;
        Locator charLocation;
        StringBuffer charBuf;

        StartDocument getStartEvent() {
            return startEvent;
        }
        
        private void addEvent(Event ev) throws SAXException {
            if (ev == null) {
                throw new NullPointerException("null event");
            }
            if (charBuf != null) {
                char[] chars = new char[charBuf.length()];
                charBuf.getChars(0, charBuf.length(), chars, 0);
                Characters charEvent = new Characters(charLocation,
                                                      chars, 0, chars.length);
                                                      
                lastEvent.next = charEvent;
                lastEvent = charEvent;
                charLocation = null;
                charBuf = null;
            }
            if (lastEvent == null) {
                lastEvent = startEvent = new StartDocument(locator);
            }
            lastEvent.next = ev;
            lastEvent = ev;
        }

        public void characters(char[] ch, int start, int length) 
            throws SAXException {
            if (charBuf == null) {
                charBuf = new StringBuffer();
                charLocation = new LocatorImpl(locator);
            }
            charBuf.append(ch, start, length);
        }

        public void endDocument() throws SAXException {
            StartDocument startDoc = (StartDocument)stack.pop();
            EndDocument endDoc = new EndDocument(locator);
            startDoc.endDocument = endDoc;
            addEvent(endDoc);
        }

        public void endElement(String namespaceURI,
                               String localName,
                               String raw) 
            throws SAXException {
            Event start = (Event)stack.pop();
            Event newEvent = null;
            if (JEXL_NS.equals(namespaceURI)) {
                if (start instanceof StartForEach) {
                    StartForEach startForEach = 
                        (StartForEach)start;
                    newEvent = startForEach.endForEach = 
                        new EndForEach(locator);
                    
                } else if (start instanceof StartIf) {
                    StartIf startIf = (StartIf)start;
                    newEvent = startIf.endIf = 
                        new EndIf(locator);
                } else if (start instanceof StartWhen) {
                    StartWhen startWhen = (StartWhen)start;
                    StartChoose startChoose = (StartChoose)stack.peek();
                    if (startChoose.firstChoice != null) {
                        StartWhen w = startChoose.firstChoice;
                        while (w.nextChoice != null) {
                            w = w.nextChoice;
                        }
                        w.nextChoice = startWhen;
                    } else {
                        startChoose.firstChoice = startWhen;
                    }
                    newEvent = startWhen.endWhen = 
                        new EndWhen(locator);
                } else if (start instanceof StartOtherwise) {
                    StartOtherwise startOtherwise = 
                        (StartOtherwise)start;
                    StartChoose startChoose = (StartChoose)stack.peek();
                    newEvent = startOtherwise.endOtherwise = 
                        new EndOtherwise(locator);
                    startChoose.otherwise = startOtherwise;
                } else if (start instanceof StartOut) {
                    newEvent = new EndOut(locator);
                } else if (start instanceof StartChoose) {
                    StartChoose startChoose = (StartChoose)start;
                    newEvent = 
                        startChoose.endChoose = new EndChoose(locator);
                } else if (start instanceof StartImport) {
                    StartImport startImport = (StartImport)start;
                    newEvent = 
                        startImport.endImport = new EndImport(locator);
                } else if (start instanceof StartTemplate) {
                    StartTemplate startTemplate = (StartTemplate)start;
                    newEvent =
                        startTemplate.endTemplate = new EndTemplate(locator);
                } else if (start instanceof StartDefine) {
                    StartDefine startDefine = (StartDefine)start;
                    newEvent = 
                        startDefine.endDefine = new EndDefine(locator);
                } else {
                    throw new SAXParseException("unrecognized tag: " + localName, locator, null);
                }
            } else {
                StartElement startElement = (StartElement)start;
                newEvent = startElement.endElement = 
                    new EndElement(locator, startElement);
            }
            addEvent(newEvent);
        }
        
        public void endPrefixMapping(String prefix) throws SAXException {
            EndPrefixMapping endPrefixMapping = 
                new EndPrefixMapping(locator, prefix);
            addEvent(endPrefixMapping);
        }

        public void ignorableWhitespace(char[] ch, int start, int length) 
            throws SAXException {
            Event ev = new IgnorableWhitespace(locator, ch, start, length);
            addEvent(ev);
        }

        public void processingInstruction(String target, String data) 
            throws SAXException {
            Event pi = new ProcessingInstruction(locator, target, data);
            addEvent(pi);
        }

        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        public void skippedEntity(String name) throws SAXException {
            addEvent(new SkippedEntity(locator, name));
        }

        public void startDocument() {
            startEvent = new StartDocument(locator);
            lastEvent = startEvent;
            stack.push(lastEvent);
        }

        public void startElement(String namespaceURI,
                                 String localName,
                                 String raw,
                                 Attributes attrs) 
            throws SAXException {
            Event newEvent = null;
            if (JEXL_NS.equals(namespaceURI)) {
                if (localName.equals(FOR_EACH)) {
                    String items = attrs.getValue("items");
                    String select = attrs.getValue("select");
                    String s = attrs.getValue("begin");
                    int begin = s == null ? -1 : Integer.parseInt(s);
                    s = attrs.getValue("end");
                    int end = s == null ? -1 : Integer.parseInt(s);
                    s = attrs.getValue("step");
                    int step = s == null ? 1 : Integer.parseInt(s);
                    if (step < 1) {
                        throw new SAXParseException("forEach: \"step\" must be a positive integer", locator, null);
                    }
                    String var = attrs.getValue("var");
                    if (items == null) {
                        if (select == null && (begin == -1 || end == -1)) {
                            throw new SAXParseException("forEach: \"select\", \"items\", or both \"begin\" and \"end\" must be specified", locator, null);
                        }
                    } else if (select != null) {
                        throw new SAXParseException("forEach: only one of \"select\" or \"items\" may be specified", locator, null);
                    }
                    begin = begin == -1 ? 0 : begin;
                    end = end == -1 ? Integer.MAX_VALUE: end;
                    Object expr;
                    try {
                        expr = compileExpr(items == null ? select : items);
                    } catch (Exception exc) {
                        throw new SAXParseException(exc.getMessage(),
                                                    locator, exc);
                    }
                    StartForEach startForEach = 
                        new StartForEach(locator, expr, 
                                         var, begin, end, step);
                    newEvent = startForEach;
                } else if (localName.equals(CHOOSE)) {
                    StartChoose startChoose = new StartChoose(locator);
                    newEvent = startChoose;
                } else if (localName.equals(WHEN)) {
                    if (stack.size() == 0 ||
                        !(stack.peek() instanceof StartChoose)) {
                        throw new SAXParseException("<when> must be within <choose>", locator, null);
                    }
                    String test = attrs.getValue("test");
                    if (test == null) {
                        throw new SAXParseException("when: \"test\" is required", locator, null);
                    }
                    Object expr;
                    try {
                        expr = compileExpr(test);
                    } catch (Exception e) {
                        throw new SAXParseException("when: \"test\": " + e.getMessage(), locator, null);
                    }
                    StartWhen startWhen = new StartWhen(locator, expr);
                    newEvent = startWhen;
                } else if (localName.equals(OUT)) {
                    String value = attrs.getValue("value");
                    if (value == null) {
                        throw new SAXParseException("out: \"value\" is required", locator, null);
                    }
                    Object expr;
                    try {
                        expr = compileExpr(value);
                    } catch (Exception e) {
                        throw new SAXParseException("out: \"value\": " + e.getMessage(), locator, null);
                    }
                    newEvent = new StartOut(locator, expr);
                } else if (localName.equals(OTHERWISE)) {
                    if (stack.size() == 0 ||
                        !(stack.peek() instanceof StartChoose)) {
                        throw new SAXParseException("<otherwise> must be within <choose>", locator, null);
                    }
                    StartOtherwise startOtherwise = 
                        new StartOtherwise(locator);
                    newEvent = startOtherwise;
                } else if (localName.equals(IF)) {
                    String test = attrs.getValue("test");
                    if (test == null) {
                        throw new SAXParseException("if: \"test\" is required", locator, null);
                    }
                    Object expr;
                    try {
                        expr = compileExpr(test);
                    } catch (Exception e) {
                        throw new SAXParseException("if: \"test\": " + e.getMessage(), locator, null);
                    }
                    StartIf startIf = 
                        new StartIf(locator, expr);
                    newEvent = startIf;
                } else if (localName.equals(DEFINE)) {
                    // <define namespace="a" name="b">
                    // body
                    // </define>
                    String namespace = attrs.getValue(JEXL_NS, "namespace");
                    if (namespace == null) {
                        namespace = "";
                    }
                    String name = attrs.getValue(JEXL_NS, "name");
                    if (name == null) {
                        throw new SAXParseException("define: template \"name\" is required", locator, null);
                    }
                    Map formalParams = new HashMap();
                    for (int i = 0, len = attrs.getLength(); i < len; i++) {
                        String uri = attrs.getURI(i);
                        if (uri.equals("")) {
                            String n = attrs.getLocalName(i);
                            String v = attrs.getValue(i);
                            if (v == null) v = "";
                            formalParams.put(n, v);
                        }
                    }
                    StartDefine startDefine = 
                        new StartDefine(locator, namespace, name, 
                                        formalParams);
                    newEvent = startDefine;
                } else if (localName.equals(IMPORT)) {
                    // <import uri="${root}/foo/bar.xml" select="{.}"/>
                    // Allow expression substituion in "uri" attribute
                    AttributeEvent uri = null;
                    StartElement startElement = 
                        new StartElement(locator, namespaceURI,
                                         localName, raw, attrs);
                    Iterator iter = startElement.attributeEvents.iterator();
                    while (iter.hasNext()) {
                        AttributeEvent e = (AttributeEvent)iter.next();
                        if (e.localName.equals("uri")) {
                            uri = e;
                            break;
                        }
                    }
                    if (uri == null) {
                        throw new SAXParseException("import: \"uri\" is required", locator, null);
                    }
                    // If "select" is present then its value will be used
                    // as the context object in the imported template
                    String select = attrs.getValue("select");
                    Object expr = null;
                    if (select != null) {
                        try {
                            expr = compileExpr(select);
                        } catch (Exception e) {
                            throw new SAXParseException("import: \"select\": " + e.getMessage(), locator, null);
                        }
                    }
                    StartImport startImport = 
                        new StartImport(locator, uri, expr);
                    newEvent = startImport;
                } else if (localName.equals(TEMPLATE)) {
                    StartTemplate startTemplate =
                        new StartTemplate(locator);
                    newEvent = startTemplate;
                } else {
                    throw new SAXParseException("unrecognized tag: " + localName, locator, null);
                }
            } else {
                StartElement startElem = 
                    new StartElement(locator, namespaceURI,
                                     localName, raw, attrs);
                newEvent = startElem;
            }
            stack.push(newEvent);
            addEvent(newEvent);
        }
        
        public void startPrefixMapping(String prefix, String uri) 
            throws SAXException {
            addEvent(new StartPrefixMapping(locator, prefix, uri));
        }

        public void comment(char ch[], int start, int length) 
            throws SAXException {
            addEvent(new Comment(locator, ch, start, length));
        }

        public void endCDATA() throws SAXException {
            addEvent(new EndCDATA(locator));
        }

        public void endDTD() throws SAXException {
            addEvent(new EndDTD(locator));
        }

        public void endEntity(String name) throws SAXException {
            addEvent(new EndEntity(locator, name));
        }

        public void startCDATA() throws SAXException {
            addEvent(new StartCDATA(locator));
        }

        public void startDTD(String name, String publicId, String systemId) 
            throws SAXException {
            addEvent(new StartDTD(locator, name, publicId, systemId));
        }
        
        public void startEntity(String name) throws SAXException {
            addEvent(new StartEntity(locator, name));
        }
    }

    private XMLConsumer consumer;
    private JXPathContext jxpathContext;
    private MyJexlContext globalJexlContext;
    private Variables variables;
    private static Map cache = new HashMap();
    private Source inputSource;
    private Map definitions;

    public void recycle() {
        super.recycle();
        consumer = null;
        jxpathContext = null;
        globalJexlContext = null;
        variables = null;
        inputSource = null;
        definitions = null;
    }

    public void setup(SourceResolver resolver, Map objectModel,
                      String src, Parameters parameters)
        throws ProcessingException, SAXException, IOException {

        super.setup(resolver, objectModel, src, parameters);
        if (src != null) {
            try {
                this.inputSource = resolver.resolveURI(src);
            } catch (SourceException se) {
                throw SourceUtil.handle("Error during resolving of '" + src + "'.", se);
            }
        }
        long lastMod = inputSource.getLastModified();
        String uri = inputSource.getURI();
        synchronized (cache) {
            StartDocument startEvent = (StartDocument)cache.get(uri);
            if (startEvent != null &&
                lastMod > startEvent.compileTime) {
                cache.remove(uri);
            }
        }
        // FIX ME: When we decide proper way to pass "bean" and "kont"
        Object bean = ((Environment)resolver).getAttribute("bean-dict");
        WebContinuation kont =
            (WebContinuation)((Environment)resolver).getAttribute("kont");
        setContexts(bean, kont,
                    ObjectModelHelper.getRequest(objectModel),
                    ObjectModelHelper.getResponse(objectModel),
                    ObjectModelHelper.getContext(objectModel),
                    parameters);
        definitions = new HashMap();
    }

    private void setContexts(Object contextObject,
                             WebContinuation kont,
                             Request request,
                             Response response,
                             org.apache.cocoon.environment.Context app,
                             Parameters parameters) {
        if (variables == null) {
            variables = new MyVariables(contextObject,
                                        kont,
                                        request,
                                        response,
                                        app,
                                        parameters);
        }
        Map map;
        if (contextObject instanceof Map) {
            map = (Map)contextObject;
        } else {
            // Hack: I use jxpath to populate the context object's properties
            // in the jexl context
            final JXPathBeanInfo bi = 
                JXPathIntrospector.getBeanInfo(contextObject.getClass());
            map = new HashMap();
            if (bi.isDynamic()) {
                Class cl = bi.getDynamicPropertyHandlerClass();
                try {
                    DynamicPropertyHandler h = (DynamicPropertyHandler) cl.newInstance();
                    String[] result = h.getPropertyNames(contextObject);
                    for (int i = 0; i < result.length; i++) {
                        try {
                            map.put(result[i], h.getProperty(contextObject, result[i]));
                        } catch (Exception exc) {
                            exc.printStackTrace();
                        }
                    }
                } catch (Exception ignored) {
                    ignored.printStackTrace();
                }
            } else {
                PropertyDescriptor[] props =  bi.getPropertyDescriptors();
                for (int i = 0; i < props.length; i++) {
                    try {
                        Method read = props[i].getReadMethod();
                        if (read != null) {
                            map.put(props[i].getName(), 
                                    read.invoke(contextObject, null));
                        }
                    } catch (Exception ignored) {
                        ignored.printStackTrace();
                    }
                }
            }
        }
        jxpathContext = jxpathContextFactory.newContext(null, contextObject);
        jxpathContext.setVariables(variables);
        globalJexlContext = new MyJexlContext();
        globalJexlContext.setVars(map);
        map = globalJexlContext.getVars();
        map.put("flowContext", contextObject);
        map.put("continuation", kont);
        map.put("request", request);
        map.put("response", response);
        map.put("context", app);
        map.put("session", request.getSession(false));
    }

    public void setConsumer(XMLConsumer consumer) {
        this.consumer = consumer;
    }

    public void generate() 
        throws IOException, SAXException, ProcessingException {
        StartDocument startEvent;
        synchronized (cache) {
            startEvent = (StartDocument)cache.get(inputSource.getURI());
        }
        if (startEvent == null) {
            long compileTime = inputSource.getLastModified();
            Parser parser = new Parser();
            this.resolver.toSAX(this.inputSource, parser);
            startEvent = parser.getStartEvent();
            startEvent.compileTime = compileTime;
            synchronized (cache) {
                cache.put(inputSource.getURI(), startEvent);
            }
        }
        execute(globalJexlContext, jxpathContext, startEvent, null);
    }

    final static char[] EMPTY_CHARS = "".toCharArray();
    
    interface CharHandler {
        public void characters(char[] ch, int offset, int length)
            throws SAXException;
    }

    private void characters(JexlContext jexlContext,
                            JXPathContext jxpathContext, 
                            TextEvent event,
                            CharHandler handler) throws SAXException {
        Iterator iter = event.substitutions.iterator();
        while (iter.hasNext()) {
            Object subst = iter.next();
            char[] chars;
            if (subst instanceof char[]) {
                chars = (char[])subst;
            } else {
                Object expr = subst;
                try {
                    Object val = getValue(expr, jexlContext, jxpathContext);
                    if (val != null) {
                        chars = val.toString().toCharArray();
                    } else {
                        chars = EMPTY_CHARS;
                    }
                } catch (Exception e) {
                    throw new SAXParseException(e.getMessage(),
                                                event.location,
                                                e);
                }
            }
            handler.characters(chars, 0, chars.length);
        }
    }

    private void execute(MyJexlContext jexlContext,
                         JXPathContext jxpathContext,
                         Event startEvent, Event endEvent) 
        throws SAXException {
        Event ev = startEvent;
        while (ev != endEvent) {
            consumer.setDocumentLocator(ev.location);
            if (ev instanceof Characters) {
                TextEvent text = (TextEvent)ev;
                characters(jexlContext, 
                           jxpathContext, 
                           text, 
                           new CharHandler() {
                               public void characters(char[] ch, int offset,
                                                      int len) 
                                   throws SAXException {
                                   consumer.characters(ch, offset, len);
                               }
                           });
            } else if (ev instanceof EndDocument) {
                consumer.endDocument();
            } else if (ev instanceof EndElement) {
                EndElement endElement = (EndElement)ev;
                StartElement startElement = 
                    (StartElement)endElement.startElement;
                consumer.endElement(startElement.namespaceURI,
                                    startElement.localName,
                                    startElement.raw);
            } else if (ev instanceof EndPrefixMapping) {
                EndPrefixMapping endPrefixMapping = 
                    (EndPrefixMapping)ev;
                consumer.endPrefixMapping(endPrefixMapping.prefix);
            } else if (ev instanceof IgnorableWhitespace) {
                TextEvent text = (TextEvent)ev;
                characters(jexlContext, 
                           jxpathContext, 
                           text, 
                           new CharHandler() {
                               public void characters(char[] ch, int offset,
                                                      int len) 
                                   throws SAXException {
                                   consumer.ignorableWhitespace(ch, offset, len);
                               }
                           });
            } else if (ev instanceof ProcessingInstruction) {
                ProcessingInstruction pi = (ProcessingInstruction)ev;
                consumer.processingInstruction(pi.target, pi.data);
            } else if (ev instanceof SkippedEntity) {
                SkippedEntity skippedEntity = (SkippedEntity)ev;
                consumer.skippedEntity(skippedEntity.name);
            } else if (ev instanceof StartDocument) {
                StartDocument startDoc = (StartDocument)ev;
                if (startDoc.endDocument != null) {
                    // if this isn't a document fragment
                    consumer.startDocument();
                }
            } else if (ev instanceof StartIf) {
                StartIf startIf = (StartIf)ev;
                Object val;
                try {
                    val = getValue(startIf.test, jexlContext, jxpathContext);
                } catch (Exception e) {
                    throw new SAXParseException(e.getMessage(),
                                                ev.location,
                                                e);
                }
                boolean result = false;
                if (val instanceof Boolean) {
                    result = ((Boolean)val).booleanValue();
                }
                if (!result) {
                    ev = startIf.endIf.next;
                    continue;
                }
            } else if (ev instanceof StartForEach) {
                StartForEach startForEach = (StartForEach)ev;
                Object items = startForEach.items;
                Iterator iter;
                boolean xpath = false;
                try {
                    if (items == null) {
                        iter = new Iterator() {
                                public boolean hasNext() {
                                    return true;
                                }
                                public Object next() {
                                    return null;
                                }
                                public void remove() {
                                }
                            };
                    } else if (items instanceof CompiledExpression) {
                        CompiledExpression compiledExpression = 
                            (CompiledExpression)items;
                        iter = 
                            compiledExpression.iteratePointers(jxpathContext);
                        xpath = true;
                    } else {
                        org.apache.commons.jexl.Expression e = 
                            (org.apache.commons.jexl.Expression)items;
                        Object result = e.evaluate(jexlContext);
                        iter =
                            org.apache.commons.jexl.util.Introspector.getUberspect().getIterator(result, null);
                    }
                } catch (Exception exc) {
                    throw new SAXParseException(exc.getMessage(),
                                                ev.location,
                                                exc);
                }
                int i;
                int begin = startForEach.begin;
                int end = startForEach.end;
                int step = startForEach.step;
                MyJexlContext localJexlContext = 
                    new MyJexlContext(jexlContext);
                for (i = 0; i < begin && iter.hasNext(); i++) {
                    iter.next();
                }
                for (; i < end && iter.hasNext(); i++) {
                    Object value;
                    if (xpath) {
                        Pointer ptr = (Pointer)iter.next();
                        Object contextObject;
                        try {
                            contextObject = ptr.getNode();
                        } catch (Exception exc) {
                            throw new SAXParseException(exc.getMessage(),
                                                        ev.location,
                                                        exc);
                        }
                        value = ptr.getNode();
                    } else {
                        value = iter.next();
                    }
                    JXPathContext localJXPathContext = 
                        jxpathContextFactory.newContext(null, value);
                    localJXPathContext.setVariables(variables);
                    if (startForEach.var != null) {
                        localJexlContext.put(startForEach.var, value);
                    }
                    execute(localJexlContext,
                            localJXPathContext,
                            startForEach.next,
                            startForEach.endForEach);
                    for (int skip = step-1; 
                         skip > 0 && iter.hasNext(); --skip) {
                        iter.next();
                    }
                }
                ev = startForEach.endForEach.next;
                continue;
            } else if (ev instanceof StartChoose) {
                StartChoose startChoose = (StartChoose)ev;
                StartWhen startWhen = startChoose.firstChoice; 
                for (;startWhen != null; startWhen = startWhen.nextChoice) {
                    Object val;
                    try {
                        val = getValue(startWhen.test, jexlContext,
                                       jxpathContext);
                    } catch (Exception e) {
                        throw new SAXParseException(e.getMessage(),
                                                    ev.location,
                                                    e);
                    }
                    boolean result = false;
                    if (val instanceof Boolean) {
                        result = ((Boolean)val).booleanValue();
                    }
                    if (result) {
                        execute(jexlContext, jxpathContext,
                                startWhen.next, startWhen.endWhen);
                        break;
                    }
                }
                if (startWhen == null) {
                    if (startChoose.otherwise != null) {
                        execute(jexlContext, jxpathContext,
                                startChoose.otherwise.next,
                                startChoose.otherwise.endOtherwise);
                    }
                }
                ev = startChoose.endChoose.next;
                continue;
            } else if (ev instanceof StartElement) {
                StartElement startElement = (StartElement)ev;
                StartDefine def = 
                    (StartDefine)definitions.get(startElement.qname);
                if (def != null) {
                    MyVariables vars = 
                        (MyVariables)jxpathContext.getVariables();
                    MyJexlContext local = new MyJexlContext(globalJexlContext);
                    final Map localVariables = vars.localVariables;
                    vars.localVariables = new HashMap();
                    Iterator i = startElement.attributeEvents.iterator();
                    while (i.hasNext()) {
                        AttributeEvent attrEvent = (AttributeEvent)i.next();
                        Object defVal;
                        if (attrEvent.namespaceURI.length() == 0) {
                           defVal = def.parameters.get(attrEvent.localName);
                           if (defVal == null) continue;
                        }
                        if (attrEvent instanceof CopyAttribute) {
                            CopyAttribute copy =
                                (CopyAttribute)attrEvent;
                            vars.declareVariable(attrEvent.localName, 
                                                 copy.value);
                            local.put(attrEvent.localName,
                                      copy.value);
                        } else if (attrEvent instanceof 
                                   SubstituteAttribute) {
                            SubstituteAttribute substEvent = 
                                (SubstituteAttribute)attrEvent;
                            if (substEvent.substitutions.size() == 1) {
                                Subst subst = 
                                    (Subst)substEvent.substitutions.get(0);
                                if (subst instanceof Expression) {
                                    Object val;
                                    Expression expr = (Expression)subst;
                                    try {
                                        val = 
                                            getValue(expr.compiledExpression,
                                                     jexlContext,
                                                     jxpathContext);
                                    } catch (Exception e) {
                                        throw new SAXParseException(e.getMessage(),
                                                                    ev.location,
                                                                    e);
                                    }
                                    vars.declareVariable(attrEvent.localName,
                                                         val);
                                    local.put(attrEvent.localName, val);
                                    continue;
                                }
                            }
                            StringBuffer buf = new StringBuffer();
                            Iterator ii = substEvent.substitutions.iterator();
                            while (ii.hasNext()) {
                                Subst subst = (Subst)ii.next();
                                if (subst instanceof Literal) {
                                    Literal lit = (Literal)subst;
                                    buf.append(lit.value);
                                } else if (subst instanceof Expression) {
                                    Expression expr = (Expression)subst;
                                    Object val;
                                    try {
                                        val = 
                                            getValue(expr.compiledExpression,
                                                     jexlContext,
                                                     jxpathContext);
                                    } catch (Exception e) {
                                        throw new SAXParseException(e.getMessage(),
                                                                    ev.location,
                                                                    e);
                                    }
                                    if (val == null) {
                                        val = "";
                                    }
                                    buf.append(val.toString());
                                }
                            }
                            vars.declareVariable(attrEvent.localName,
                                                 buf.toString());
                            local.put(attrEvent.localName,
                                      buf.toString());
                        }
                    }
                    execute(local, jxpathContext, 
                            def.next, def.endDefine);
                    vars.localVariables = localVariables;
                    ev = startElement.endElement.next;
                    continue;
                }
                Iterator i = startElement.attributeEvents.iterator();
                AttributesImpl attrs = new AttributesImpl();
                while (i.hasNext()) {
                    AttributeEvent attrEvent = (AttributeEvent)
                        i.next();
                    if (attrEvent instanceof CopyAttribute) {
                        CopyAttribute copy =
                            (CopyAttribute)attrEvent;
                        attrs.addAttribute(copy.namespaceURI,
                                           copy.localName,
                                           copy.raw,
                                           copy.type,
                                           copy.value);
                    } else if (attrEvent instanceof 
                               SubstituteAttribute) {
                        StringBuffer buf = new StringBuffer();
                        SubstituteAttribute substEvent =
                            (SubstituteAttribute)attrEvent;
                        Iterator ii = substEvent.substitutions.iterator();
                        while (ii.hasNext()) {
                            Subst subst = (Subst)ii.next();
                            if (subst instanceof Literal) {
                                Literal lit = (Literal)subst;
                                buf.append(lit.value);
                            } else if (subst instanceof Expression) {
                                Expression expr = (Expression)subst;
                                Object val;
                                try {
                                    val = 
                                        getValue(expr.compiledExpression,
                                                 jexlContext,
                                                 jxpathContext);
                                } catch (Exception e) {
                                    throw new SAXParseException(e.getMessage(),
                                                                ev.location,
                                                                e);
                                }
                                if (val == null) {
                                    val = "";
                                }
                                buf.append(val.toString());
                            }
                        }
                        attrs.addAttribute(attrEvent.namespaceURI,
                                           attrEvent.localName,
                                           attrEvent.raw,
                                           attrEvent.type,
                                           buf.toString());
                    }
                }
                consumer.startElement(startElement.namespaceURI,
                                      startElement.localName,
                                      startElement.raw,
                                      attrs); 
            } else if (ev instanceof StartPrefixMapping) {
                StartPrefixMapping startPrefixMapping = 
                    (StartPrefixMapping)ev;
                consumer.startPrefixMapping(startPrefixMapping.prefix, 
                                            startPrefixMapping.uri);
            } else if (ev instanceof Comment) {
                TextEvent text = (TextEvent)ev;
                final StringBuffer buf = new StringBuffer();
                characters(jexlContext, 
                           jxpathContext, 
                           text, 
                           new CharHandler() {
                               public void characters(char[] ch, int offset,
                                                      int len) 
                                   throws SAXException {
                                   buf.append(ch, offset, len);
                               }
                           });
                char[] chars = new char[buf.length()];
                buf.getChars(0, chars.length, chars, 0);
                consumer.comment(chars, 0, chars.length);
             } else if (ev instanceof EndCDATA) {
                consumer.endCDATA();
            } else if (ev instanceof EndDTD) {
                consumer.endDTD();
            } else if (ev instanceof EndEntity) {
                consumer.endEntity(((EndEntity)ev).name);
            } else if (ev instanceof StartCDATA) {
                consumer.startCDATA();
            } else if (ev instanceof StartDTD) {
                StartDTD startDTD = (StartDTD)ev;
                consumer.startDTD(startDTD.name,
                                  startDTD.publicId,
                                  startDTD.systemId);
            } else if (ev instanceof StartEntity) {
                consumer.startEntity(((StartEntity)ev).name);
            } else if (ev instanceof StartOut) {
                StartOut startOut = (StartOut)ev;
                Object val;
                try {
                    val = getValue(startOut.compiledExpression,
                                   jexlContext,
                                   jxpathContext);
                } catch (Exception e) {
                    throw new SAXParseException(e.getMessage(),
                                                ev.location,
                                                e);
                }
                if (val == null) {
                    val = "";
                }
                char[] ch = val.toString().toCharArray();
                consumer.characters(ch, 0, ch.length);
            } else if (ev instanceof StartTemplate) {
                // no action
            } else if (ev instanceof StartDefine) {
                StartDefine startDefine = (StartDefine)ev;
                definitions.put(startDefine.qname, startDefine);
                ev = startDefine.endDefine.next;
            } else if (ev instanceof StartImport) {
                String uri;
                StartImport startImport = (StartImport)ev;
                AttributeEvent e = startImport.uri;
                if (e instanceof CopyAttribute) {
                    CopyAttribute copy = (CopyAttribute)e;
                    uri = copy.value;
                } else {
                    StringBuffer buf = new StringBuffer();
                    SubstituteAttribute substAttr = (SubstituteAttribute)e;
                    Iterator i = substAttr.substitutions.iterator();
                    while (i.hasNext()) {
                        Subst subst = (Subst)i.next();
                        if (subst instanceof Literal) {
                            Literal lit = (Literal)subst;
                            buf.append(lit.value);
                        } else if (subst instanceof Expression) {
                            Expression expr = (Expression)subst;
                            Object val;
                            try {
                                val = 
                                    getValue(expr.compiledExpression,
                                             jexlContext,
                                             jxpathContext);
                            } catch (Exception exc) {
                                throw new SAXParseException(exc.getMessage(),
                                                            ev.location,
                                                            exc);
                            }
                            if (val == null) {
                                val = "";
                            }
                            buf.append(val.toString());
                        }
                    }
                    uri = buf.toString();
                    
                }
                Source input;
                try {
                    input = resolver.resolveURI(uri);
                } catch (Exception exc) {
                    throw new SAXParseException(exc.getMessage(),
                                                ev.location,
                                                exc);
                }
                long lastMod = input.getLastModified();
                StartDocument doc;
                synchronized (cache) {
                    doc = (StartDocument)cache.get(input.getURI());
                    if (doc != null) {
                        if (doc.compileTime < lastMod) {
                            doc = null; // recompile
                        }
                    }
                }
                if (doc == null) {
                    try {
                        Parser parser = new Parser();
                        this.resolver.toSAX(input, parser);
                        doc = parser.getStartEvent();
                        doc.compileTime = lastMod;
                    } catch (Exception exc) {
                        throw new SAXParseException(exc.getMessage(),
                                                    ev.location,
                                                    exc);
                    }
                    synchronized (cache) {
                        cache.put(input.getURI(), doc);
                    }
                }
                JXPathContext select = jxpathContext;
                if (startImport.select != null) {
                    try {
                        Object obj = getValue(startImport.select,
                                              jexlContext,
                                              jxpathContext);
                        select = jxpathContextFactory.newContext(null, obj);
                        select.setVariables(variables);
                    } catch (Exception exc) {
                        throw new SAXParseException(exc.getMessage(),
                                                    ev.location,
                                                    exc);
                    }
                }
                execute(jexlContext, select, doc.next, null);
                ev = startImport.endImport.next;
                continue;
            }
            ev = ev.next;
        }
    }

}
