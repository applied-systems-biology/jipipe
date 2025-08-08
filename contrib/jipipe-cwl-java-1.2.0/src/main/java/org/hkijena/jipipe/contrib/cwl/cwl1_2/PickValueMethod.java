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

import org.hkijena.jipipe.contrib.cwl.cwl1_2.utils.ValidationException;

public enum PickValueMethod {
  FIRST_NON_NULL("first_non_null"),
  THE_ONLY_NON_NULL("the_only_non_null"),
  ALL_NON_NULL("all_non_null");

  private static String[] symbols = new String[] {"first_non_null", "the_only_non_null", "all_non_null"};
  private String docVal;

  private PickValueMethod(final String docVal) {
    this.docVal = docVal;
  }

  public static PickValueMethod fromDocumentVal(final String docVal) {
    for(final PickValueMethod val : PickValueMethod.values()) {
      if(val.docVal.equals(docVal)) {
        return val;
      }
    }
    throw new ValidationException(String.format("Expected one of %s", PickValueMethod.symbols, docVal));
  }
}
