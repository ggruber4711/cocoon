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

import org.apache.cocoon.el.ExpressionException;
import org.apache.cocoon.el.impl.AbstractExpression;
import org.apache.cocoon.el.objectmodel.ObjectModel;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;

/**
 * A single expression compiled by {@link Jexl3Compiler}.
 *
 * @see Jexl3Compiler
 * @since 2.3.1
 */
public class Jexl3Expression extends AbstractExpression {

    private final JexlEngine engine;
    private final JexlExpression compiledExpression;

    public Jexl3Expression(String language, String expression, JexlEngine engine)
            throws ExpressionException {
        super(language, expression);
        this.engine = engine;
        try {
            this.compiledExpression = engine.createExpression(expression);
        } catch (Exception e) {
            throw new ExpressionException("Couldn't create expression " + expression, e);
        }
    }

    public Object evaluate(ObjectModel objectModel) throws ExpressionException {
        try {
            return this.compiledExpression.evaluate(new ObjectModelContext(objectModel));
        } catch (Exception e) {
            throw new ExpressionException("Couldn't evaluate expression " + getExpression(), e);
        }
    }

    public Iterator iterate(ObjectModel objectModel) throws ExpressionException {
        Object result = evaluate(objectModel);
        Iterator iter = null;
        if (result != null) {
            try {
                iter = this.engine.getUberspect().getIterator(result);
            } catch (Exception e) {
                throw new ExpressionException(
                        "Couldn't get an iterator from expression " + getExpression(), e);
            }
        }
        return iter == null ? EMPTY_ITER : iter;
    }

    /**
     * Not supported, exactly as for the <code>jexl</code> language. JEXL 3 can assign, but the
     * <code>jexl</code> compiler has always thrown here, so nothing that runs under either
     * language can depend on it; implementing it only on <code>jexl3</code> would make the two
     * behave differently for no gain.
     */
    public void assign(ObjectModel objectModel, Object value) throws ExpressionException {
        throw new UnsupportedOperationException("Assign is not implemented for jexl3");
    }

    public Object getNode(ObjectModel objectModel) throws ExpressionException {
        return evaluate(objectModel);
    }

    /**
     * Adapts Cocoon's {@link ObjectModel} to a JEXL 3 context.
     *
     * <p>JEXL 1 required the whole variable map up front via <code>JexlContext.getVars()</code>.
     * JEXL 3 asks for one name at a time, which suits {@link ObjectModel} better: it is a
     * layered, scoped map whose full contents are more expensive to materialise than a lookup.
     */
    private static class ObjectModelContext implements JexlContext {

        private final ObjectModel objectModel;

        ObjectModelContext(ObjectModel objectModel) {
            this.objectModel = objectModel;
        }

        public Object get(String name) {
            return this.objectModel.get(name);
        }

        public void set(String name, Object value) {
            this.objectModel.put(name, value);
        }

        public boolean has(String name) {
            return this.objectModel.containsKey(name);
        }
    }
}
