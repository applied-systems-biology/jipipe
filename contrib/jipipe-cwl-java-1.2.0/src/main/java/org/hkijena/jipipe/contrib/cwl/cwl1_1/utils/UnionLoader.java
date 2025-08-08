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
import java.util.Arrays;
import java.util.List;

public class UnionLoader implements Loader<Object> {
  private final ArrayList<Loader> alternates;

  public UnionLoader(List<Loader> alternates) {
    this.alternates = new ArrayList<Loader>(alternates);
  }

  public UnionLoader(Loader[] alternates) {
    this(Arrays.asList(alternates));
  }

  public void addLoaders(List<Loader> loaders) {
    this.alternates.addAll(loaders);
  }

  public void addLoaders(Loader[] loaders) {
    this.addLoaders(Arrays.asList(loaders));
  }

  public Object load(
      final Object doc,
      final String baseUri,
      final LoadingOptions loadingOptions,
      final String docRoot) {
    final List<ValidationException> errors = new ArrayList();
    for (final Loader loader : this.alternates) {
      try {
        return loader.load(doc, baseUri, loadingOptions, docRoot);
      } catch (ValidationException e) {
        errors.add(e);
      }
    }
    throw new ValidationException("Failed to match union type", errors);
  }
}
