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

package org.apache.cocoon.components.elementprocessor.impl.poi.hssf.elements;

import org.apache.poi.hssf.util.CellRangeAddress;

import java.io.IOException;

/**
 * No-op implementation of ElementProcessor to handle the "Merge" tag.
 * This element is a container of other elements and has several attributes.
 *
 * @version $Id$
 */
public class EPMerge extends BaseElementProcessor {

    private String _cellRange;

    /**
     * constructor
     */
    public EPMerge() {
        super(null);
        _cellRange = null;
    }

    public String getCellRange() {
        if (this._cellRange == null) {
            //pulls in the content
            _cellRange = this.getData();
        }
        return this._cellRange;
    }

    /**
     * Setup the merged cellRangeAddresses
     * @exception IOException
     */
    public void endProcessing() throws IOException {
        // POI 3.10 removed org.apache.poi.hssf.util.RangeAddress. CellRangeAddress.valueOf
        // replaces it for parsing and is already zero-based, so the "subtract one" the old
        // code needed (RangeAddress counted from 1,1) is gone. Verified to produce identical
        // coordinates to the old parse, multi-letter columns included; see EPMergeTestCase.
        //
        // The parse returns the ss.util type while Sheet still speaks the hssf.util subtype,
        // so the result is copied across rather than widening Sheet's signature.
        org.apache.poi.ss.util.CellRangeAddress parsed =
                org.apache.poi.ss.util.CellRangeAddress.valueOf(getCellRange());
        CellRangeAddress cellRangeAddress = new CellRangeAddress(
                parsed.getFirstRow(), parsed.getLastRow(),
                parsed.getFirstColumn(), parsed.getLastColumn());
        Sheet sheet = this.getSheet();

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Merging Range: Row (" + cellRangeAddress.getFirstRow()
                    + ") Col (" + cellRangeAddress.getFirstColumn() + ")"
                    + " to Row (" + cellRangeAddress.getLastRow()
                    + ") Col (" + cellRangeAddress.getLastColumn() + ")");
        }
        sheet.addMergedRegion(cellRangeAddress);
    }

} // end public class EPMerge
