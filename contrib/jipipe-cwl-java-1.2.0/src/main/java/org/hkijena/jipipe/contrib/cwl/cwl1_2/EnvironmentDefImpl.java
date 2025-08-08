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

import org.hkijena.jipipe.contrib.cwl.cwl1_2.utils.LoaderInstances;
import org.hkijena.jipipe.contrib.cwl.cwl1_2.utils.RootLoader;
import org.hkijena.jipipe.contrib.cwl.cwl1_2.utils.LoadingOptions;
import org.hkijena.jipipe.contrib.cwl.cwl1_2.utils.LoadingOptionsBuilder;
import org.hkijena.jipipe.contrib.cwl.cwl1_2.utils.SaveableImpl;
import org.hkijena.jipipe.contrib.cwl.cwl1_2.utils.ValidationException;

/**
* Auto-generated class implementation for <I>https://w3id.org/cwl/cwl#EnvironmentDef</I><BR> <BLOCKQUOTE>
 Define an environment variable that will be set in the runtime environment
 by the workflow platform when executing the command line tool.  May be the
 result of executing an expression, such as getting a parameter from input.
  </BLOCKQUOTE>
 */
public class EnvironmentDefImpl extends SaveableImpl implements EnvironmentDef {
  private LoadingOptions loadingOptions_ = new LoadingOptionsBuilder().build();
  private java.util.Map<String, Object> extensionFields_ =
      new java.util.HashMap<String, Object>();

  private String envName;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#EnvironmentDef/envName</I><BR>
   * <BLOCKQUOTE>
   * The environment variable name   * </BLOCKQUOTE>
   */

  public String getEnvName() {
    return this.envName;
  }

  private Object envValue;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#EnvironmentDef/envValue</I><BR>
   * <BLOCKQUOTE>
   * The environment variable value   * </BLOCKQUOTE>
   */

  public Object getEnvValue() {
    return this.envValue;
  }

  /**
   * Used by {@link RootLoader} to construct instances of EnvironmentDefImpl.
   *
   * @param __doc_            Document fragment to load this record object from (presumably a
                              {@link java.util.Map}).
   * @param __baseUri_        Base URI to generate child document IDs against.
   * @param __loadingOptions  Context for loading URIs and populating objects.
   * @param __docRoot_        ID at this position in the document (if available) (maybe?)
   * @throws ValidationException If the document fragment is not a {@link java.util.Map}
   *                             or validation of fields fails.
   */
  public EnvironmentDefImpl(
      final Object __doc_,
      final String __baseUri_,
      LoadingOptions __loadingOptions,
      final String __docRoot_) {
    super(__doc_, __baseUri_, __loadingOptions, __docRoot_);
    // Prefix plumbing variables with '__' to reduce likelihood of collision with
    // generated names.
    String __baseUri = __baseUri_;
    String __docRoot = __docRoot_;
    if (!(__doc_ instanceof java.util.Map)) {
      throw new ValidationException("EnvironmentDefImpl called on non-map");
    }
    final java.util.Map<String, Object> __doc = (java.util.Map<String, Object>) __doc_;
    final java.util.List<ValidationException> __errors =
        new java.util.ArrayList<ValidationException>();
    if (__loadingOptions != null) {
      this.loadingOptions_ = __loadingOptions;
    }
    String envName;
    try {
      envName =
          LoaderInstances
              .StringInstance
              .loadField(__doc.get("envName"), __baseUri, __loadingOptions);
    } catch (ValidationException e) {
      envName = null; // won't be used but prevents compiler from complaining.
      final String __message = "the `envName` field is not valid because:";
      __errors.add(new ValidationException(__message, e));
    }
    Object envValue;
    try {
      envValue =
          LoaderInstances
              .union_of_StringInstance_or_ExpressionLoader
              .loadField(__doc.get("envValue"), __baseUri, __loadingOptions);
    } catch (ValidationException e) {
      envValue = null; // won't be used but prevents compiler from complaining.
      final String __message = "the `envValue` field is not valid because:";
      __errors.add(new ValidationException(__message, e));
    }
    if (!__errors.isEmpty()) {
      throw new ValidationException("Trying 'RecordField'", __errors);
    }
    this.envName = (String) envName;
    this.envValue = (Object) envValue;
  }
}
