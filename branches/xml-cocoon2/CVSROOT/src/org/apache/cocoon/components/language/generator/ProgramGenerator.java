/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/
package org.apache.cocoon.components.language.generator;

import java.io.File;
import org.apache.avalon.framework.component.Component;
import org.xml.sax.EntityResolver;

/**
 * This interface defines a loader for programs automatically built from XML
 * documents writeen in a <code>MarkupLanguage</code>
 *
 * @author <a href="mailto:ricardo@apache.org">Ricardo Rocha</a>
 * @version CVS $Revision: 1.1.2.12 $ $Date: 2001-04-30 14:17:05 $
 */
public interface ProgramGenerator extends Component {
  /**
   * Load a program built from an XML document written in a
   * <code>MarkupLanguage</code>
   *
   * @param file The input document's <code>File</code>
   * @param markupLanguage The <code>MarkupLanguage</code> in which the input
   * document is written
   * @param programmingLanguage The <code>ProgrammingLanguage</code> in which
   * the program must be written
   * @return The loaded object
   * @exception Exception If an error occurs during generation or loading
   */
  CompiledComponent load(
    File file, String markupLanguage, String programmingLanguage,
    EntityResolver resolver
  ) throws Exception;

  /**
   * Release a program built from an XML document written in a
   * <code>MarkupLanguage</code>.
   *
   * @param CompiledSheet
   */
  void release(CompiledComponent component);
}
