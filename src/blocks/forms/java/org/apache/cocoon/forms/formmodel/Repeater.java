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
import org.apache.cocoon.forms.validation.ValidationError;
import org.apache.cocoon.forms.validation.ValidationErrorAware;
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
 * @version $Id$
 */
public class Repeater extends AbstractWidget implements ValidationErrorAware //, ContainerWidget 
{
    private final RepeaterDefinition definition;
    private final List rows = new ArrayList();
    protected ValidationError validationError;

    public Repeater(RepeaterDefinition repeaterDefinition) {
        super(repeaterDefinition);
        this.definition = repeaterDefinition;
        // Setup initial size. Do not call addRow() as it will call initialize()
        // on the newly created rows, which is not what we want here.
        for (int i = 0; i < this.definition.getInitialSize(); i++) {
            rows.add(new RepeaterRow(definition));
        }
    }

    public WidgetDefinition getDefinition() {
        return definition;
    }
    
    public void initialize() {
        for (int i = 0; i < this.rows.size(); i++) {
            ((RepeaterRow)rows.get(i)).initialize();
        }
        super.initialize();
    }

    public int getSize() {
        return rows.size();
    }

    public RepeaterRow addRow() {
        RepeaterRow repeaterRow = new RepeaterRow(definition);
        rows.add(repeaterRow);
        repeaterRow.initialize();
        getForm().addWidgetUpdate(this);
        return repeaterRow;
    }
    
    public RepeaterRow addRow(int index) {
        RepeaterRow repeaterRow = new RepeaterRow(definition);
        if (index >= this.rows.size()) {
            rows.add(repeaterRow);
        } else {
            rows.add(index, repeaterRow);
        }
        repeaterRow.initialize();
        getForm().addWidgetUpdate(this);
        return repeaterRow;
    }

    public RepeaterRow getRow(int index) {
        return (RepeaterRow)rows.get(index);
    }
    
    /**
     * Overrides {@link AbstractWidget#getChild(String)} to return the 
     * repeater-row indicated by the index in 'id'
     * 
     * @param id index of the row as a string-id
     * @return the repeater-row at the specified index
     */
    public Widget getChild(String id) {
        int rowIndex = -1;
        try {
            rowIndex = Integer.parseInt(id);
        } catch (NumberFormatException nfe) {
            // Not a number
            return null;
        }
        if (rowIndex < 0 || rowIndex >= getSize()) 
            return null;
        return getRow(rowIndex);
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

        }
        return (Repeater.RepeaterRow)result;
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
        getForm().addWidgetUpdate(this);
    }
    
    public void moveRowLeft(int index) {
        if (index == 0 || index >= this.rows.size()) {
            // do nothing
        } else {
            Object temp = this.rows.get(index-1);
            this.rows.set(index-1, this.rows.get(index));
            this.rows.set(index, temp);
        }
        getForm().addWidgetUpdate(this);
    }

    public void moveRowRight(int index) {
        if (index < 0 || index >= this.rows.size() - 1) {
            // do nothing
        } else {
            Object temp = this.rows.get(index+1);
            this.rows.set(index+1, this.rows.get(index));
            this.rows.set(index, temp);
        }
        getForm().addWidgetUpdate(this);
    }
    
    /**
     * @deprecated {@see #clear()}
     *
     */
    public void removeRows() {
        clear();
        getForm().addWidgetUpdate(this);
    }
    
    /**
     * Clears all rows from the repeater and go back to the initial size
     */
    public void clear() {
        rows.clear();
        
        // and reset to initial size
        for (int i = 0; i < this.definition.getInitialSize(); i++) {
            addRow();
        }
        getForm().addWidgetUpdate(this);
    }

    /**
     * Gets a widget on a certain row.
     * @param rowIndex startin from 0
     * @param id a widget id
     * @return null if there's no such widget
     */
    public Widget getWidget(int rowIndex, String id) {
        RepeaterRow row = (RepeaterRow)rows.get(rowIndex);
        return row.getChild(id);
    }
    
    public void readFromRequest(FormContext formContext) {
        if (!getCombinedState().isAcceptingInputs())
            return;

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

    /**
     * @see org.apache.cocoon.forms.formmodel.Widget#validate()
     */
    public boolean validate() {
        if (!getCombinedState().isValidatingValues()) {
            this.wasValid = true;
            return true;
        }
        boolean valid = true;
        Iterator rowIt = rows.iterator();
        while (rowIt.hasNext()) {
            RepeaterRow row = (RepeaterRow)rowIt.next();
            valid = valid & row.validate();
        }
        this.wasValid = (valid ? super.validate() : false) && this.validationError == null;
        return this.wasValid;
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
    
    /**
     * Set a validation error on this field. This allows repeaters be externally marked as invalid by
     * application logic.
     *
     * @return the validation error
     */
    public ValidationError getValidationError() {
        return this.validationError;
    }

    /**
     * set a validation error
     */
    public void setValidationError(ValidationError error) {
        this.validationError = error;
    }

    public class RepeaterRow extends AbstractContainerWidget {

        public RepeaterRow(RepeaterDefinition definition) {
            super(definition);
            setParent(Repeater.this);
            ((ContainerDefinition)definition).createWidgets(this);
        }

//        public String getLocation() {
//            return Repeater.this.getLocation();
//        }
//        
        public WidgetDefinition getDefinition() {
            return Repeater.this.definition;
        }

        public String getId() {
            // id of a RepeaterRow is the position of the row in the list of rows.
            return String.valueOf(rows.indexOf(this));
        }

        public Form getForm() {
            return Repeater.this.getForm();
        }
        
        public void initialize() {
            // Initialize children but don't call super.initialize() that would call the repeater's
            // on-create handlers for each row.
            // FIXME(SW): add an 'on-create-row' handler?
            Iterator it = this.getChildren();
            while(it.hasNext()) {
              ((Widget)it.next()).initialize();
            }
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
        
        public void broadcastEvent(WidgetEvent event) {
            throw new UnsupportedOperationException("Widget " + this.getRequestParameterName() + " doesn't handle events.");
        }
    }

}
