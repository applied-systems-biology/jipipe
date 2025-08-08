/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.contrib.cwl.cwl1_2;

import org.hkijena.jipipe.contrib.cwl.cwl1_2.utils.Saveable;

/**
* Auto-generated interface for <I>https://w3id.org/cwl/salad#RecordField</I><BR>This interface is implemented by {@link RecordFieldImpl}<BR> <BLOCKQUOTE>
 A field of a record. </BLOCKQUOTE>
 */
public interface RecordField extends Documented, Saveable {
  /**
   * Getter for property <I>https://w3id.org/cwl/salad#RecordField/name</I><BR>
   * <BLOCKQUOTE>
   * The name of the field
   *    * </BLOCKQUOTE>
   */

  String getName();
  /**
   * Getter for property <I>https://w3id.org/cwl/salad#Documented/doc</I><BR>
   * <BLOCKQUOTE>
   * A documentation string for this object, or an array of strings which should be concatenated.   * </BLOCKQUOTE>
   */

  Object getDoc();
  /**
   * Getter for property <I>https://w3id.org/cwl/salad#type</I><BR>
   * <BLOCKQUOTE>
   * The field type. If it is an array, it indicates
   * that the field type is a union type of its elements.
   * Its elements may be duplicated.
   *    * </BLOCKQUOTE>
   */

  Object getType();
}
