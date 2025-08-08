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

public class UriLoader<T> implements Loader<T> {
  private final Loader<T> innerLoader;
  private final boolean scopedId;
  private final boolean vocabTerm;
  private final Integer scopedRef;
  private final Boolean noLinkCheck;

  public UriLoader(
      final Loader<T> innerLoader,
      final boolean scopedId,
      final boolean vocabTerm,
      final Integer scopedRef,
      final Boolean noLinkCheck) {
    this.innerLoader = innerLoader;
    this.scopedId = scopedId;
    this.vocabTerm = vocabTerm;
    this.scopedRef = scopedRef;
    this.noLinkCheck = noLinkCheck;
  }

  private Object expandUrl(
      final Object object, final String baseUri, final LoadingOptions loadingOptions) {
    if (object instanceof String) {
      return loadingOptions.expandUrl(
          (String) object, baseUri, this.scopedId, this.vocabTerm, this.scopedRef);
    } else {
      return object;
    }
  }

  public T load(
      final Object doc_,
      final String baseUri,
      final LoadingOptions loadingOptions,
      final String docRoot) {
    LoadingOptions innerLoadingOptions = loadingOptions;
    if (this.noLinkCheck != null) {
      innerLoadingOptions = new LoadingOptionsBuilder().copiedFrom(loadingOptions).setNoLinkCheck(this.noLinkCheck).build();
    }
    Object doc = doc_;
    if (doc instanceof List) {
      List<Object> docList = (List<Object>) doc;
      List<Object> docWithExpansion = new ArrayList<Object>();
      for (final Object el : docList) {
        docWithExpansion.add(this.expandUrl(el, baseUri, innerLoadingOptions));
      }
      doc = docWithExpansion;
    }
    if (doc instanceof String) {
      doc = this.expandUrl(doc, baseUri, innerLoadingOptions);
    }
    return this.innerLoader.load(doc, baseUri, innerLoadingOptions);
  }
}
