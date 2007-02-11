/*
 * Copyright 1999-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cocoon.forms.formmodel;

import org.w3c.dom.Element;
import org.apache.cocoon.forms.Constants;
import org.apache.cocoon.forms.util.DomHelper;

/**
 * Builds {StructDefinition}s.
 *
 * @version $Id$
 */
public class StructDefinitionBuilder extends AbstractWidgetDefinitionBuilder {

    public WidgetDefinition buildWidgetDefinition(Element element) throws Exception {
        StructDefinition definition = new StructDefinition();
        setCommonProperties(element, definition);
        setDisplayData(element, definition);
        setValidators(element, definition);

        Element widgetsElement = DomHelper.getChildElement(element, Constants.DEFINITION_NS, "widgets", true);
        // All child elements of the widgets element are widgets
        Element[] widgetElements = DomHelper.getChildElements(widgetsElement, Constants.DEFINITION_NS);
        for (int i = 0; i < widgetElements.length; i++) {
            Element widgetElement = widgetElements[i];
            WidgetDefinition widgetDefinition = buildAnotherWidgetDefinition(widgetElement);
            definition.addWidgetDefinition(widgetDefinition);
        }

        return definition;
    }
}