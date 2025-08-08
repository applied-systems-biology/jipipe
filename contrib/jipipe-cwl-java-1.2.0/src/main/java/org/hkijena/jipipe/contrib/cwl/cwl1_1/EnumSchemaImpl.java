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

package org.hkijena.jipipe.contrib.cwl.cwl1_1;

import org.hkijena.jipipe.contrib.cwl.cwl1_1.utils.RootLoader;
import org.hkijena.jipipe.contrib.cwl.cwl1_1.utils.LoaderInstances;
import org.hkijena.jipipe.contrib.cwl.cwl1_1.utils.LoadingOptions;
import org.hkijena.jipipe.contrib.cwl.cwl1_1.utils.LoadingOptionsBuilder;
import org.hkijena.jipipe.contrib.cwl.cwl1_1.utils.SaveableImpl;
import org.hkijena.jipipe.contrib.cwl.cwl1_1.utils.ValidationException;

/**
* Auto-generated class implementation for <I>https://w3id.org/cwl/salad#EnumSchema</I><BR> <BLOCKQUOTE>
 Define an enumerated type.
  </BLOCKQUOTE>
 */
public class EnumSchemaImpl extends SaveableImpl implements EnumSchema {
  private LoadingOptions loadingOptions_ = new LoadingOptionsBuilder().build();
  private java.util.Map<String, Object> extensionFields_ =
      new java.util.HashMap<String, Object>();

  private java.util.Optional<String> name;

  /**
   * Getter for property <I>https://w3id.org/cwl/salad#EnumSchema/name</I><BR>

   */

  public java.util.Optional<String> getName() {
    return this.name;
  }

  private java.util.List<String> symbols;

  /**
   * Getter for property <I>https://w3id.org/cwl/salad#symbols</I><BR>
   * <BLOCKQUOTE>
   * Defines the set of valid symbols.   * </BLOCKQUOTE>
   */

  public java.util.List<String> getSymbols() {
    return this.symbols;
  }

  private Enum_name type;

  /**
   * Getter for property <I>https://w3id.org/cwl/salad#type</I><BR>
   * <BLOCKQUOTE>
   * Must be `enum`   * </BLOCKQUOTE>
   */

  public Enum_name getType() {
    return this.type;
  }

  /**
   * Used by {@link RootLoader} to construct instances of EnumSchemaImpl.
   *
   * @param __doc_            Document fragment to load this record object from (presumably a
                              {@link java.util.Map}).
   * @param __baseUri_        Base URI to generate child document IDs against.
   * @param __loadingOptions  Context for loading URIs and populating objects.
   * @param __docRoot_        ID at this position in the document (if available) (maybe?)
   * @throws ValidationException If the document fragment is not a {@link java.util.Map}
   *                             or validation of fields fails.
   */
  public EnumSchemaImpl(
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
      throw new ValidationException("EnumSchemaImpl called on non-map");
    }
    final java.util.Map<String, Object> __doc = (java.util.Map<String, Object>) __doc_;
    final java.util.List<ValidationException> __errors =
        new java.util.ArrayList<ValidationException>();
    if (__loadingOptions != null) {
      this.loadingOptions_ = __loadingOptions;
    }
    java.util.Optional<String> name;

    if (__doc.containsKey("name")) {
      try {
        name =
            LoaderInstances
                .uri_optional_StringInstance_True_False_None_None
                .loadField(__doc.get("name"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        name = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `name` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      name = null;
    }

    Boolean __original_is_null = name == null;
    if (name == null) {
      if (__docRoot != null) {
        name = java.util.Optional.of(__docRoot);
      } else {
        name = java.util.Optional.of("_:" + java.util.UUID.randomUUID().toString());
      }
    }
    if (__original_is_null) {
        __baseUri = __baseUri_;
    } else {
        __baseUri = (String) name.orElse(null);
    }
    java.util.List<String> symbols;
    try {
      symbols =
          LoaderInstances
              .uri_array_of_StringInstance_True_False_None_None
              .loadField(__doc.get("symbols"), __baseUri, __loadingOptions);
    } catch (ValidationException e) {
      symbols = null; // won't be used but prevents compiler from complaining.
      final String __message = "the `symbols` field is not valid because:";
      __errors.add(new ValidationException(__message, e));
    }
    Enum_name type;
    try {
      type =
          LoaderInstances
              .typedsl_Enum_name_2
              .loadField(__doc.get("type"), __baseUri, __loadingOptions);
    } catch (ValidationException e) {
      type = null; // won't be used but prevents compiler from complaining.
      final String __message = "the `type` field is not valid because:";
      __errors.add(new ValidationException(__message, e));
    }
    if (!__errors.isEmpty()) {
      throw new ValidationException("Trying 'RecordField'", __errors);
    }
    this.name = (java.util.Optional<String>) name;
    this.symbols = (java.util.List<String>) symbols;
    this.type = (Enum_name) type;
  }
}
