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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapLoader<T> implements Loader<Map<String, T>> {
  private final Loader<T> valueLoader;
  private final String container;
  private final Boolean noLinkCheck;

  public MapLoader(Loader<T> valueLoader, final String container, final Boolean noLinkCheck) {
    this.valueLoader = valueLoader;
    this.container = container;
    this.noLinkCheck = noLinkCheck;
  }

  public Map<String, T> load(
      final Object doc,
      final String baseUri,
      final LoadingOptions loadingOptions,
      final String docRoot) {
    final Map<String, Object> docMap = (Map<String, Object>) Loader.validateOfJavaType(Map.class, doc);
    LoadingOptions innerLoadingOptions = loadingOptions;
    if (this.container != null || this.noLinkCheck != null) {
      LoadingOptionsBuilder builder = new LoadingOptionsBuilder().copiedFrom(loadingOptions);
      if (this.container != null) {
        builder.setContainer(this.container);
      }
      if (this.noLinkCheck != null) {
        builder.setNoLinkCheck(this.noLinkCheck);
      }
      innerLoadingOptions = builder.build();
    }
    final Map<String, T> r = new HashMap();
    final List<ValidationException> errors = new ArrayList();
    for (final Map.Entry<String, Object> entry : docMap.entrySet()) {
      try {
        final Object loadedField = this.valueLoader.loadField(entry.getValue(), baseUri, innerLoadingOptions);
        r.put(entry.getKey(), (T) loadedField);
      } catch (final ValidationException e) {
        errors.add(e);
      }
    }
    if (!errors.isEmpty()) {
      throw new ValidationException("", errors);
    }
    return r;
  }
}
