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

package org.hkijena.jipipe.contrib.cwl.cwl1_2.utils;

import java.util.ArrayList;
import java.util.List;

public class ArrayLoader<T> implements Loader<List<T>> {
  private final Loader<T> itemLoader;

  public ArrayLoader(Loader<T> itemLoader) {
    this.itemLoader = itemLoader;
  }

  public List<T> load(
      final Object doc,
      final String baseUri,
      final LoadingOptions loadingOptions,
      final String docRoot) {
    final List<Object> docList = (List<Object>) Loader.validateOfJavaType(List.class, doc);
    final List<T> r = new ArrayList();
    final List<Loader> loaders = new ArrayList<Loader>();
    loaders.add(this);
    loaders.add(this.itemLoader);
    final UnionLoader unionLoader = new UnionLoader(loaders);
    final List<ValidationException> errors = new ArrayList();
    for (final Object el : docList) {
      try {
        final Object loadedField = unionLoader.loadField(el, baseUri, loadingOptions);
        final boolean flatten = !"@list".equals(loadingOptions.container);
        if (flatten && loadedField instanceof List) {
          r.addAll((List<T>) loadedField);
        } else {
          r.add((T) loadedField);
        }
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
