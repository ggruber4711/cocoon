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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.cocoon.forms.Constants;
import org.apache.cocoon.forms.FormContext;
import org.apache.cocoon.forms.event.WidgetEvent;
import org.apache.cocoon.xml.AttributesImpl;
import org.apache.cocoon.xml.XMLUtils;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * A repeater is a widget that repeats a number of other widgets.
 *
 * <p>Technically, the Repeater widget is a ContainerWidget whose children are
 * {@link RepeaterRow}s, and the RepeaterRows in turn are ContainerWidgets
 * containing the actual repeated widgets. However, in practice, you won't need
 * to use the RepeaterRow widget directly.
 *
 * <p>Using the methods {@link #getSize()} and {@link #getWidget(int, java.lang.String)}
 * you can access all of the repeated widget instances.
 * 
 * @version $Id: Repeater.java,v 1.12 2004/05/07 13:42:09 mpo Exp $
 */
public class Repeater extends AbstractWidget 
//implements ContainerWidget 
{
    private final RepeaterDefinition definition;
    private final List rows = new ArrayList();

    public Repeater(RepeaterDefinition repeaterDefinition) {
        this.definition = repeaterDefinition;
        // setup initial size
        removeRows();
    }

    protected WidgetDefinition getDefinition() {
        return definition;
    }

    public int getSize() {
        return rows.size();
    }

//    public void addWidget(Widget widget) {
//        throw new RuntimeException("Repeater.addWidget(): Please use addRow() instead.");
//    }

    public RepeaterRow addRow() {
        RepeaterRow repeaterRow = new RepeaterRow(definition);
        rows.add(repeaterRow);
        return repeaterRow;
    }
    
    public RepeaterRow addRow(int index) {
        RepeaterRow repeaterRow = new RepeaterRow(definition);
        if (index >= this.rows.size()) {
            rows.add(repeaterRow);
        } else {
            rows.add(index, repeaterRow);
        }
        return repeaterRow;
    }

    public RepeaterRow getRow(int index) {
        return (RepeaterRow)rows.get(index);
    }
    
    /**
     * Crawls up the parents of a widget up to finding a repeater row.
     * 
     * @param widget the widget whose row is to be found
     * @return the repeater row
     */
    public static RepeaterRow getParentRow(Widget widget) {
        Widget result = widget;
        while(result != null && ! (result instanceof Repeater.RepeaterRow)) {
            result = result.getParent();
        }
        
        if (result == null) {
            throw new RuntimeException("Could not find a parent row for widget " + widget);

        } else {
            return (Repeater.RepeaterRow)result;
        }
    }
    
    /**
     * Get the position of a row in this repeater.
     * @param row the row which we search the index for
     * @return the row position or -1 if this row is not in this repeater
     */
    public int indexOf(RepeaterRow row) {
        return this.rows.indexOf(row);
    }

    /**
     * @throws IndexOutOfBoundsException if the the index is outside the range of existing rows.
     */
    public void removeRow(int index) {
        rows.remove(index);
    }
    
    public void moveRowLeft(int index) {
        if (index == 0 || index >= this.rows.size()) {
            // do nothing
        } else {
            Object temp = this.rows.get(index-1);
            this.rows.set(index-1, this.rows.get(index));
            this.rows.set(index, temp);
        }
    }

    public void moveRowRight(int index) {
        if (index < 0 || index >= this.rows.size() - 1) {
            // do nothing
        } else {
            Object temp = this.rows.get(index+1);
            this.rows.set(index+1, this.rows.get(index));
            this.rows.set(index, temp);
        }
    }

    /**
     * Clears all rows from the repeater and go back to the initial size
     */
    public void removeRows() {
        rows.clear();
        
        // and reset to initial size
        for (int i = 0; i < this.definition.getInitialSize(); i++) {
            addRow();
        }
    }

    /**
     * Gets a widget on a certain row.
     * @param rowIndex startin from 0
     * @param id a widget id
     * @return null if there's no such widget
     */
    public Widget getWidget(int rowIndex, String id) {
        RepeaterRow row = (RepeaterRow)rows.get(rowIndex);
        return row.getWidget(id);
    }
//
//    public boolean hasWidget(String id) {
//        int row; 
//        try { 
//            row = Integer.parseInt(id);
//        } catch (NumberFormatException e) {
//            // TODO: Use i18n.
//            throw new RuntimeException("Repeater: Row id is not a valid integer: " + id);
//        }
//        return row >= 0 && row < rows.size();
//    }

    //TODO: consider removing when this method is removed 
    // from the Widget interface.
    public Widget getWidget(String id) {
        int row; 
        try { 
            row = Integer.parseInt(id);
        } catch (NumberFormatException e) {
            // TODO: Use i18n.
            throw new RuntimeException("Repeater: Row id is not a valid integer: " + id);
        }
        return (RepeaterRow)rows.get(row);
    }

    public void readFromRequest(FormContext formContext) {
        // read number of rows from request, and make an according number of rows
        String sizeParameter = formContext.getRequest().getParameter(getRequestParameterName() + ".size");
        if (sizeParameter != null) {
            int size = 0;
            try {
                size = Integer.parseInt(sizeParameter);
            } catch (NumberFormatException exc) {
                // do nothing
            }

            // some protection against people who might try to exhaust the server by supplying very large
            // size parameters
            if (size > 500)
                throw new RuntimeException("Client is not allowed to specify a repeater size larger than 500.");

            int currentSize = getSize();
            if (currentSize < size) {
                for (int i = currentSize; i < size; i++) {
                    addRow();
                }
            } else if (currentSize > size) {
                for (int i = currentSize - 1; i >= size; i--) {
                    removeRow(i);
                }
            }
        }

        // let the rows read their data from the request
        Iterator rowIt = rows.iterator();
        while (rowIt.hasNext()) {
            RepeaterRow row = (RepeaterRow)rowIt.next();
            row.readFromRequest(formContext);
        }
    }

    public boolean validate() {
        boolean valid = true;
        Iterator rowIt = rows.iterator();
        while (rowIt.hasNext()) {
            RepeaterRow row = (RepeaterRow)rowIt.next();
            valid = valid & row.validate();
        }
        return valid ? super.validate() : false;
    }


    private static final String REPEATER_EL = "repeater";
    private static final String HEADINGS_EL = "headings";
    private static final String HEADING_EL = "heading";
    private static final String LABEL_EL = "label";
    private static final String REPEATER_SIZE_EL = "repeater-size";
    

    /**
     * @return "repeater"
     */
    public String getXMLElementName() {
        return REPEATER_EL;
    }   
    
    
    
	/**
	 * Adds @size attribute
	 */
	public AttributesImpl getXMLElementAttributes() {
        AttributesImpl attrs = super.getXMLElementAttributes();
        attrs.addCDATAAttribute("size", String.valueOf(getSize()));
		return attrs;
	}
    
        
	public void generateDisplayData(ContentHandler contentHandler)
			throws SAXException {
        // the repeater's label
        contentHandler.startElement(Constants.INSTANCE_NS, LABEL_EL, Constants.INSTANCE_PREFIX_COLON + LABEL_EL, XMLUtils.EMPTY_ATTRIBUTES);
        generateLabel(contentHandler);
        contentHandler.endElement(Constants.INSTANCE_NS, LABEL_EL, Constants.INSTANCE_PREFIX_COLON + LABEL_EL);

        // heading element -- currently contains the labels of each widget in the repeater
        contentHandler.startElement(Constants.INSTANCE_NS, HEADINGS_EL, Constants.INSTANCE_PREFIX_COLON + HEADINGS_EL, XMLUtils.EMPTY_ATTRIBUTES);
        Iterator widgetDefinitionIt = definition.getWidgetDefinitions().iterator();
        while (widgetDefinitionIt.hasNext()) {
            WidgetDefinition widgetDefinition = (WidgetDefinition)widgetDefinitionIt.next();
            contentHandler.startElement(Constants.INSTANCE_NS, HEADING_EL, Constants.INSTANCE_PREFIX_COLON + HEADING_EL, XMLUtils.EMPTY_ATTRIBUTES);
            widgetDefinition.generateLabel(contentHandler);
            contentHandler.endElement(Constants.INSTANCE_NS, HEADING_EL, Constants.INSTANCE_PREFIX_COLON + HEADING_EL);
        }
        contentHandler.endElement(Constants.INSTANCE_NS, HEADINGS_EL, Constants.INSTANCE_PREFIX_COLON + HEADINGS_EL);
	}
    
    
    public void generateItemSaxFragment(ContentHandler contentHandler, Locale locale) throws SAXException {
        // the actual rows in the repeater
        Iterator rowIt = rows.iterator();
        while (rowIt.hasNext()) {
            RepeaterRow row = (RepeaterRow)rowIt.next();
            row.generateSaxFragment(contentHandler, locale);
        }
    }

    /**
     * Generates the label of a certain widget in this repeater.
     */
    public void generateWidgetLabel(String widgetId, ContentHandler contentHandler) throws SAXException {
        WidgetDefinition widgetDefinition = definition.getWidgetDefinition(widgetId);
        if (widgetDefinition == null)
            throw new SAXException("Repeater \"" + getRequestParameterName() + "\" at " + this.getLocation()
                                   + " contains no widget with id \"" + widgetId + "\".");
        widgetDefinition.generateLabel(contentHandler);
    }

    /**
     * Generates a repeater-size element with a size attribute indicating the size of this repeater.
     */
    public void generateSize(ContentHandler contentHandler) throws SAXException {
        AttributesImpl attrs = getXMLElementAttributes(); 
        contentHandler.startElement(Constants.INSTANCE_NS, REPEATER_SIZE_EL, Constants.INSTANCE_PREFIX_COLON + REPEATER_SIZE_EL, attrs);
        contentHandler.endElement(Constants.INSTANCE_NS, REPEATER_SIZE_EL, Constants.INSTANCE_PREFIX_COLON + REPEATER_SIZE_EL);
    }

    public class RepeaterRow extends AbstractContainerWidget {

        public RepeaterRow(AbstractWidgetDefinition definition) {
            ((ContainerDefinition)definition).createWidgets(this);
        }

//        public String getLocation() {
//            return Repeater.this.getLocation();
//        }
//        
        protected WidgetDefinition getDefinition() {
            return Repeater.this.definition;
        }

        public String getId() {
            // id of a RepeaterRow is the position of the row in the list of rows.
            return String.valueOf(rows.indexOf(this));
        }

        public Widget getParent() {
            return Repeater.this;
        }
        
        public Form getForm() {
            return Repeater.this.getForm();
        }

//        public String getNamespace() {
//            return getParent().getNamespace() + "." + getId();
//        }
//
//        public String getFullyQualifiedId() {
//            return getParent().getNamespace() + "." + getId();
//        }

        public void setParent(Widget widget) {
            throw new RuntimeException("Parent of RepeaterRow is fixed, and cannot be set.");
        }

        public boolean validate() {
            // Validate only child widtgets, as the definition's validators are those of the parent repeater
            return widgets.validate();
        }
        
        private static final String ROW_EL = "repeater-row";


        /**
         * @return "repeater-row"
         */
        public String getXMLElementName() {
            return ROW_EL;
        }

        public void generateLabel(ContentHandler contentHandler) throws SAXException {
            // this widget has its label generated in the context of the repeater
        }     
        
        public void generateDisplayData(ContentHandler contentHandler)
                throws SAXException {
            // this widget has its display-data generated in the context of the repeater
        }
                
//        //TODO: reuse available implementation on superclass       
//        public void generateSaxFragment(ContentHandler contentHandler, Locale locale) throws SAXException {
//            AttributesImpl rowAttrs = new AttributesImpl();
//            rowAttrs.addCDATAAttribute("id", getFullyQualifiedId());
//            contentHandler.startElement(Constants.INSTANCE_NS, ROW_EL, Constants.INSTANCE_PREFIX_COLON + ROW_EL, rowAttrs);
//            Iterator widgetIt = widgets.iterator();
//            while (widgetIt.hasNext()) {
//                Widget widget = (Widget)widgetIt.next();
//                widget.generateSaxFragment(contentHandler, locale);
//            }
//            contentHandler.endElement(Constants.INSTANCE_NS, ROW_EL, Constants.INSTANCE_PREFIX_COLON + ROW_EL);
//        }
        
        public void broadcastEvent(WidgetEvent event) {
            throw new UnsupportedOperationException("Widget " + this.getRequestParameterName() + " doesn't handle events.");
        }
    }
}
