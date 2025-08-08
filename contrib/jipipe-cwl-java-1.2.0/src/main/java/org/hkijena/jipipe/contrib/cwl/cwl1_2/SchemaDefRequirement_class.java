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

public enum SchemaDefRequirement_class {
  SCHEMADEFREQUIREMENT("SchemaDefRequirement");

  private static String[] symbols = new String[] {"SchemaDefRequirement"};
  private String docVal;

  private SchemaDefRequirement_class(final String docVal) {
    this.docVal = docVal;
  }

  public static SchemaDefRequirement_class fromDocumentVal(final String docVal) {
    for(final SchemaDefRequirement_class val : SchemaDefRequirement_class.values()) {
      if(val.docVal.equals(docVal)) {
        return val;
      }
    }
    throw new ValidationException(String.format("Expected one of %s", SchemaDefRequirement_class.symbols, docVal));
  }
}
