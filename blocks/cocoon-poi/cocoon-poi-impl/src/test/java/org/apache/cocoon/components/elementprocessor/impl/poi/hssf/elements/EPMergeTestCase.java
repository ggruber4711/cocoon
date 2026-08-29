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

import junit.framework.TestCase;

import org.apache.poi.ss.util.CellRangeAddress;

/**
 * Pins the cell-range parsing that {@link EPMerge} relies on.
 *
 * <p>POI 3.10 removed <code>org.apache.poi.hssf.util.RangeAddress</code>, which EPMerge used to
 * turn a Gnumeric merge range such as "B3:D7" into coordinates. The replacement,
 * {@link CellRangeAddress#valueOf(String)}, differs in one respect that matters: RangeAddress
 * counted from 1,1 and the old code subtracted one from every coordinate, while valueOf is
 * already zero-based. Getting that wrong would shift every merged region by one row and column
 * -- a silent, plausible-looking corruption of generated spreadsheets rather than a failure.
 *
 * <p>The expectations below were taken from the POI 3.2 implementation before the change, so
 * this test is a genuine before/after comparison rather than a restatement of the new code.
 */
public class EPMergeTestCase extends TestCase {

    /** range, firstColumn, firstRow, lastColumn, lastRow -- as POI 3.2 RangeAddress produced. */
    private static final Object[][] EXPECTED = {
        { "A1:B2", new int[] { 0, 0, 1, 1 } },
        { "A1:A1", new int[] { 0, 0, 0, 0 } },
        { "B3:D7", new int[] { 1, 2, 3, 6 } },
        { "AA10:AB20", new int[] { 26, 9, 27, 19 } },
        { "C5:C5", new int[] { 2, 4, 2, 4 } },
    };

    public void testRangeParsingMatchesThePoi32Behaviour() {
        for (int i = 0; i < EXPECTED.length; i++) {
            String range = (String) EXPECTED[i][0];
            int[] want = (int[]) EXPECTED[i][1];

            CellRangeAddress actual = CellRangeAddress.valueOf(range);

            assertEquals(range + " first column", want[0], actual.getFirstColumn());
            assertEquals(range + " first row", want[1], actual.getFirstRow());
            assertEquals(range + " last column", want[2], actual.getLastColumn());
            assertEquals(range + " last row", want[3], actual.getLastRow());
        }
    }

    /** Multi-letter columns are where an off-by-one in the column maths would show up. */
    public void testMultiLetterColumns() {
        assertEquals(26, CellRangeAddress.valueOf("AA1:AA1").getFirstColumn());
        assertEquals(51, CellRangeAddress.valueOf("AZ1:AZ1").getFirstColumn());
        assertEquals(52, CellRangeAddress.valueOf("BA1:BA1").getFirstColumn());
    }
}
