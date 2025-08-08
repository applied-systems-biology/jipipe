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
* Auto-generated interface for <I>https://w3id.org/cwl/salad#UnionSchema</I><BR>This interface is implemented by {@link UnionSchemaImpl}<BR>
 */
public interface UnionSchema extends Saveable {
  /**
   * Getter for property <I>https://w3id.org/cwl/salad#names</I><BR>
   * <BLOCKQUOTE>
   * Defines the type of the union elements.   * </BLOCKQUOTE>
   */

  Object getNames();
  /**
   * Getter for property <I>https://w3id.org/cwl/salad#type</I><BR>
   * <BLOCKQUOTE>
   * Must be `union`   * </BLOCKQUOTE>
   */

  Union_name getType();
}
