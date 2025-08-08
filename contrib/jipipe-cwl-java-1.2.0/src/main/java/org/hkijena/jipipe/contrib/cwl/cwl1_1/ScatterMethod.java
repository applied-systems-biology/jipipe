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

import org.hkijena.jipipe.contrib.cwl.cwl1_1.utils.ValidationException;

public enum ScatterMethod {
  DOTPRODUCT("dotproduct"),
  NESTED_CROSSPRODUCT("nested_crossproduct"),
  FLAT_CROSSPRODUCT("flat_crossproduct");

  private static String[] symbols = new String[] {"dotproduct", "nested_crossproduct", "flat_crossproduct"};
  private String docVal;

  private ScatterMethod(final String docVal) {
    this.docVal = docVal;
  }

  public static ScatterMethod fromDocumentVal(final String docVal) {
    for(final ScatterMethod val : ScatterMethod.values()) {
      if(val.docVal.equals(docVal)) {
        return val;
      }
    }
    throw new ValidationException(String.format("Expected one of %s", ScatterMethod.symbols, docVal));
  }
}
