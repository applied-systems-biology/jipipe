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

public enum PrimitiveType {
  NULL("null"),
  BOOLEAN("boolean"),
  INT("int"),
  LONG("long"),
  FLOAT("float"),
  DOUBLE("double"),
  STRING("string");

  private static String[] symbols = new String[] {"null", "boolean", "int", "long", "float", "double", "string"};
  private String docVal;

  private PrimitiveType(final String docVal) {
    this.docVal = docVal;
  }

  public static PrimitiveType fromDocumentVal(final String docVal) {
    for(final PrimitiveType val : PrimitiveType.values()) {
      if(val.docVal.equals(docVal)) {
        return val;
      }
    }
    throw new ValidationException(String.format("Expected one of %s", PrimitiveType.symbols, docVal));
  }
}
