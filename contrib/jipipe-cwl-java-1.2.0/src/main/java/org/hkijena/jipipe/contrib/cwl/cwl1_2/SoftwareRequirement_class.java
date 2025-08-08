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

public enum SoftwareRequirement_class {
  SOFTWAREREQUIREMENT("SoftwareRequirement");

  private static String[] symbols = new String[] {"SoftwareRequirement"};
  private String docVal;

  private SoftwareRequirement_class(final String docVal) {
    this.docVal = docVal;
  }

  public static SoftwareRequirement_class fromDocumentVal(final String docVal) {
    for(final SoftwareRequirement_class val : SoftwareRequirement_class.values()) {
      if(val.docVal.equals(docVal)) {
        return val;
      }
    }
    throw new ValidationException(String.format("Expected one of %s", SoftwareRequirement_class.symbols, docVal));
  }
}
