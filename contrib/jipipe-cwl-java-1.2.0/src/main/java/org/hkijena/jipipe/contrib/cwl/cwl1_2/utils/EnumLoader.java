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

import java.lang.reflect.Method;
import java.lang.ReflectiveOperationException;

public class EnumLoader<T extends Enum> implements Loader<T>{
  private final Class<T> symbolEnumClass;

  public EnumLoader(final Class<T> symbolEnumClass) {
    this.symbolEnumClass = symbolEnumClass;
  }

  public T load(
      final Object doc,
      final String baseUri,
      final LoadingOptions loadingOptions,
      final String docRoot) {
    final String docString = Loader.validateOfJavaType(String.class, doc);
    try {
      final Method m = symbolEnumClass.getMethod("fromDocumentVal", String.class);
      final T val = (T) m.invoke(null, docString);
      return val;
    } catch (final ReflectiveOperationException e) {
      final Throwable cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      }
      throw new RuntimeException(e);
    }
  }
}
