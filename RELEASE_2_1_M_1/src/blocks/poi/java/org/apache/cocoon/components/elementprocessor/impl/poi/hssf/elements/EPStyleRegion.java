
/*

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

package org.apache.cocoon.components.elementprocessor.impl.poi.hssf.elements;

import java.util.Hashtable;

import org.apache.cocoon.components.elementprocessor.types.NumericConverter;
import org.apache.cocoon.components.elementprocessor.types.NumericResult;
import org.apache.cocoon.components.elementprocessor.ElementProcessor;
import org.apache.cocoon.components.elementprocessor.types.Attribute;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.util.Region;


import java.io.IOException;

/**
 * No-op implementation of ElementProcessor to handle the
 * "StyleRegion" tag
 *
 * This element is a container of other elements and has four
 * attributes that define the boundaries of the region.
 *
 * @author Marc Johnson (marc_johnson27591@hotmail.com)
 * @author Andrew C. Oliver (acoliver2@users.sourceforge.net)
 * @version CVS $Id: EPStyleRegion.java,v 1.2 2003/03/11 19:05:01 vgritsenko Exp $
 */
public class EPStyleRegion
    extends BaseElementProcessor
{
    private static final String _start_col_attribute = "startCol";
    private static final String _start_row_attribute = "startRow";
    private static final String _end_col_attribute   = "endCol";
    private static final String _end_row_attribute   = "endRow";
    private NumericResult       _start_col;
    private NumericResult       _start_row;
    private NumericResult       _end_col;
    private NumericResult       _end_row;

    private HSSFCellStyle       _style;
    private Hashtable           colorhash;

    private boolean             invalid;


    //kludge constant to fix gnumeric's love of declaring large stlye regions
    //for the blank sections of the sheet w/no apparent purpose that we can
    //
    private int MAX_AREA = 2001;

    /**
     * constructor
     */

    public EPStyleRegion()
    {
        super(null);
        _start_col = null;
        _start_row = null;
        _end_col   = null;
        _end_row   = null;
    }

    /**
     * Override of Initialize() implementation
     *
     * @param attributes the array of Attribute instances; may be
     *                   empty, will never be null
     * @param parent the parent ElementProcessor; may be null
     * @param filesystem the POIFSFileSystem object
     *
     * @exception IOException if anything is wrong
     */

    public void initialize(final Attribute [] attributes,
                           final ElementProcessor parent)
        throws IOException
    {
        super.initialize(attributes, parent);

        Region region = new Region(getStartRow(),
                                   (short)getStartCol(),
                                   getEndRow(),
                                   (short)getEndCol());

//        if (region.getRowFrom() == 0 &&
//            region.getColumnFrom() ==0)
//            getLogger().debug("got 0,0");

        getLogger().debug("region area is "+region.getArea());
        if(region.getArea() < MAX_AREA) {  //protect against stupid mega regions
                                       //of generally NOTHING and no real
                                       //puprose created by gnumeric
            getLogger().debug("region added");
            _style = getSheet().addStyleRegion(region
                                      ); //test
        } else {
            invalid = true;
        }

        colorhash = ((EPStyles)parent).getColorHash();
    }


    /**
     * @return start row
     *
     * @exception IOException
     */

    public int getStartRow()
        throws IOException
    {
        if (_start_row == null)
        {
            _start_row = NumericConverter
                .extractNonNegativeInteger(getValue(_start_row_attribute));
        }
        return _start_row.intValue();
    }

    /**
     * @return start column
     *
     * @exception IOException
     */

    public int getStartCol()
        throws IOException
    {
        if (_start_col == null)
        {
            _start_col = NumericConverter
                .extractNonNegativeInteger(getValue(_start_col_attribute));
        }
        return _start_col.intValue();
    }

    /**
     * @return end row
     *
     * @exception IOException
     */

    public int getEndRow()
        throws IOException
    {
        if (_end_row == null)
        {
            _end_row =
                NumericConverter
                    .extractNonNegativeInteger(getValue(_end_row_attribute));
        }
        return _end_row.intValue();
    }

    /**
     * @return end column
     *
     * @exception IOException
     */

    public int getEndCol()
        throws IOException
    {
        if (_end_col == null)
        {
            _end_col =
                NumericConverter
                    .extractNonNegativeInteger(getValue(_end_col_attribute));
        }
        return _end_col.intValue();
    }

    /**
     *  @return HSSFCellStyle associated with this style region.
     */
    public HSSFCellStyle getStyle() {
        return _style;
    }

    /**
     * @return instance created in the EPStyles instance from HSSFColor.getTripletHash();
     * @see org.apache.poi.hssf.util.HSSFColor#getTripletHash()
     */
    public Hashtable getColorHash() {
        return colorhash;
    }

    /**
     *
     * @return validity (used to determine whether this is a big wasteful region with
     *  no purpose (gnumeric does this
     */
    public boolean isValid () {
        return (!invalid);
    }

}   // end public class EPStyleRegion
