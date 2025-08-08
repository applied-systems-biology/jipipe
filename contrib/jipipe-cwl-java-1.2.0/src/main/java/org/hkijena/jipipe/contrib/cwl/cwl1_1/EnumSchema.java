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

package org.hkijena.jipipe.contrib.cwl.cwl1_1;

import org.hkijena.jipipe.contrib.cwl.cwl1_1.utils.Saveable;

/**
* Auto-generated interface for <I>https://w3id.org/cwl/salad#EnumSchema</I><BR>This interface is implemented by {@link EnumSchemaImpl}<BR> <BLOCKQUOTE>
 Define an enumerated type.
  </BLOCKQUOTE>
 */
public interface EnumSchema extends Saveable {
  /**
   * Getter for property <I>https://w3id.org/cwl/salad#EnumSchema/name</I><BR>

   */

  java.util.Optional<String> getName();
  /**
   * Getter for property <I>https://w3id.org/cwl/salad#symbols</I><BR>
   * <BLOCKQUOTE>
   * Defines the set of valid symbols.   * </BLOCKQUOTE>
   */

  java.util.List<String> getSymbols();
  /**
   * Getter for property <I>https://w3id.org/cwl/salad#type</I><BR>
   * <BLOCKQUOTE>
   * Must be `enum`   * </BLOCKQUOTE>
   */

  Enum_name getType();
}
