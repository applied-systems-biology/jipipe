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

import java.util.Optional;


public class OptionalLoader<T> implements Loader<Optional<T>> {
  private final Loader<T> itemLoader;

  public OptionalLoader(Loader<T> itemLoader) {
    this.itemLoader = itemLoader;
  }

  public Optional<T> load(
      final Object doc,
      final String baseUri,
      final LoadingOptions loadingOptions,
      final String docRoot) {
    if(doc == null) {
      return Optional.empty();
    }
    return Optional.of(itemLoader.load(doc, baseUri, loadingOptions, docRoot));
  }
}
