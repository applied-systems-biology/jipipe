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

public enum LoadListingEnum {
  NO_LISTING("no_listing"),
  SHALLOW_LISTING("shallow_listing"),
  DEEP_LISTING("deep_listing");

  private static String[] symbols = new String[] {"no_listing", "shallow_listing", "deep_listing"};
  private String docVal;

  private LoadListingEnum(final String docVal) {
    this.docVal = docVal;
  }

  public static LoadListingEnum fromDocumentVal(final String docVal) {
    for(final LoadListingEnum val : LoadListingEnum.values()) {
      if(val.docVal.equals(docVal)) {
        return val;
      }
    }
    throw new ValidationException(String.format("Expected one of %s", LoadListingEnum.symbols, docVal));
  }
}
