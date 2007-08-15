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
package org.apache.cocoon.template;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.generation.ServiceableGenerator;
import org.apache.cocoon.objectmodel.ObjectModel;
import org.apache.cocoon.objectmodel.helper.ParametersMap;
import org.apache.cocoon.template.environment.ExecutionContext;
import org.apache.cocoon.template.environment.JXCacheKey;
import org.apache.cocoon.template.environment.JXSourceValidity;
import org.apache.cocoon.template.expression.Subst;
import org.apache.cocoon.template.script.Invoker;
import org.apache.cocoon.template.script.ScriptManager;
import org.apache.cocoon.template.script.event.Event;
import org.apache.cocoon.template.script.event.StartDocument;
import org.apache.cocoon.template.xml.AttributeAwareXMLConsumerImpl;
import org.apache.cocoon.xml.NamespacesTable;
import org.apache.cocoon.xml.RedundantNamespacesFilter;
import org.apache.cocoon.xml.XMLConsumer;
import org.apache.excalibur.source.SourceValidity;
import org.xml.sax.SAXException;

/**
 * @cocoon.sitemap.component.documentation Provides a generic page template with
 *                                         embedded JSTL and XPath expression
 *                                         substitution to access data sent by
 *                                         Cocoon Flowscripts.
 *
 * @cocoon.sitemap.component.name jx
 * @cocoon.sitemap.component.label content
 * @cocoon.sitemap.component.logger sitemap.generator.jx
 *
 * @cocoon.sitemap.component.pooling.max 16
 *
 *
 * @version $Id$
 */
public class JXTemplateGenerator
    extends ServiceableGenerator
    implements CacheableProcessingComponent {

    /** The namespace used by this generator */
    public final static String NS = "http://apache.org/cocoon/templates/jx/1.0";

    public final static String CACHE_KEY = "cache-key";
    public final static String VALIDITY = "cache-validity";

    private ObjectModel newObjectModel;
    private NamespacesTable namespaces;
    private ScriptManager scriptManager;

    private StartDocument startDocument;
    private Map definitions;

    public XMLConsumer getConsumer() {
        return this.xmlConsumer;
    }

    /**
     * @see org.apache.cocoon.generation.ServiceableGenerator#service(org.apache.avalon.framework.service.ServiceManager)
     */
    public void service(ServiceManager manager) throws ServiceException {
        super.service(manager);
        this.scriptManager = (ScriptManager) this.manager.lookup(ScriptManager.ROLE);
        this.newObjectModel = (ObjectModel) this.manager.lookup(ObjectModel.ROLE);
    }

    /**
     * @see org.apache.cocoon.generation.ServiceableGenerator#dispose()
     */
    public void dispose() {
        if (this.scriptManager != null) {
            this.manager.release(this.scriptManager);
            this.scriptManager = null;
        }
        if (this.newObjectModel != null) {
            this.manager.release(this.newObjectModel);
            this.newObjectModel = null;
        }
        super.dispose();
    }

    /**
     * @see org.apache.cocoon.generation.AbstractGenerator#recycle()
     */
    public void recycle() {
        this.startDocument = null;
        this.namespaces = null;
        this.definitions = null;
        super.recycle();
    }

    /**
     * @see org.apache.cocoon.generation.AbstractGenerator#setup(org.apache.cocoon.environment.SourceResolver, java.util.Map, java.lang.String, org.apache.avalon.framework.parameters.Parameters)
     */
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters parameters)
    throws ProcessingException, SAXException, IOException {

        super.setup(resolver, objectModel, src, parameters);
        // src can be null if this generator is triggered by the jxt transformer (through the TransformerAdapter)
        if ( src != null ) {
            this.startDocument = scriptManager.resolveTemplate(src);
        }

        this.namespaces = new NamespacesTable();
        this.definitions = new HashMap();
    }

    /**
     * @see org.apache.cocoon.generation.Generator#generate()
     */
    public void generate() throws IOException, SAXException, ProcessingException {
        performGeneration(this.startDocument, null);

        // no need to reference compiled script anymore
        this.startDocument = null;
    }

    public void performGeneration(Event startEvent, Event endEvent) throws SAXException {
        newObjectModel.markLocalContext();
        
        newObjectModel.putAt(ObjectModel.PARAMETERS_PATH, new ParametersMap(parameters));
        
        XMLConsumer consumer = new AttributeAwareXMLConsumerImpl(new RedundantNamespacesFilter(this.xmlConsumer));
        //not sure why this is needed
        newObjectModel.putAt("cocoon/consumer", consumer);
        Invoker.execute(consumer, this.newObjectModel, new ExecutionContext(this.definitions, this.scriptManager,
                this.manager), null, namespaces, startEvent, null);
        
        newObjectModel.cleanupLocalContext();
    }

    /**
     * @see org.apache.cocoon.caching.CacheableProcessingComponent#getKey()
     */
    public Serializable getKey() {
        Subst cacheKeyExpr = (Subst) this.startDocument
                .getTemplateProperty(JXTemplateGenerator.CACHE_KEY);
        if (cacheKeyExpr == null) {
            return null;
        }
        try {
            final Serializable templateKey = (Serializable) cacheKeyExpr.getValue(this.newObjectModel);
            if (templateKey != null) {
                return new JXCacheKey(this.startDocument.getUri(), templateKey);
            }
        } catch (Exception e) {
            getLogger().error("error evaluating cache key", e);
        }
        return null;
    }

    /**
     * @see org.apache.cocoon.caching.CacheableProcessingComponent#getValidity()
     */
    public SourceValidity getValidity() {
        Subst validityExpr = (Subst) this.startDocument
                .getTemplateProperty(JXTemplateGenerator.VALIDITY);
        if (validityExpr == null) {
            return null;
        }
        try {
            final SourceValidity sourceValidity = this.startDocument.getSourceValidity();
            final SourceValidity templateValidity = (SourceValidity) validityExpr.getValue(this.newObjectModel);
            if (sourceValidity != null && templateValidity != null) {
                return new JXSourceValidity(sourceValidity, templateValidity);
            }
        } catch (Exception e) {
            getLogger().error("error evaluating cache validity", e);
        }
        return null;
    }
}
