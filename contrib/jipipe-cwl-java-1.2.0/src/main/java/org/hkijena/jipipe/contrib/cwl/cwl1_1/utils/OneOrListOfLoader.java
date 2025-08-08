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

package org.hkijena.jipipe.contrib.cwl.cwl1_1.utils;

import java.util.ArrayList;
import java.util.List;


public class OneOrListOfLoader<T> implements Loader<OneOrListOf<T>> {
  private final Loader<T> oneLoader;
  private final Loader<List<T>> listLoader;

  public OneOrListOfLoader(Loader<T> oneLoader, Loader<List<T>> listLoader) {
      this.oneLoader = oneLoader;
      this.listLoader = listLoader;
  }

  public OneOrListOf<T> load(
      final Object doc,
      final String baseUri,
      final LoadingOptions loadingOptions,
      final String docRoot) {
    final List<ValidationException> errors = new ArrayList();
    try {
      return OneOrListOf.oneOf(this.oneLoader.load(doc, baseUri, loadingOptions, docRoot));
    } catch (ValidationException e) {
      errors.add(e);
    }
    try {
      return OneOrListOf.listOf(this.listLoader.load(doc, baseUri, loadingOptions, docRoot));
    } catch (ValidationException e) {
      errors.add(e);
    }
    throw new ValidationException("Failed to one or list of of type", errors);
  }
}
