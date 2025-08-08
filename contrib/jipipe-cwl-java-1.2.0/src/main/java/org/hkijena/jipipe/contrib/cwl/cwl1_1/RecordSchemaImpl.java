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
* Auto-generated class implementation for <I>https://w3id.org/cwl/salad#RecordSchema</I><BR>
 */
public class RecordSchemaImpl extends SaveableImpl implements RecordSchema {
  private LoadingOptions loadingOptions_ = new LoadingOptionsBuilder().build();
  private java.util.Map<String, Object> extensionFields_ =
      new java.util.HashMap<String, Object>();

  private java.util.Optional<java.util.List<Object>> fields;

  /**
   * Getter for property <I>https://w3id.org/cwl/salad#fields</I><BR>
   * <BLOCKQUOTE>
   * Defines the fields of the record.   * </BLOCKQUOTE>
   */

  public java.util.Optional<java.util.List<Object>> getFields() {
    return this.fields;
  }

  private Record_name type;

  /**
   * Getter for property <I>https://w3id.org/cwl/salad#type</I><BR>
   * <BLOCKQUOTE>
   * Must be `record`   * </BLOCKQUOTE>
   */

  public Record_name getType() {
    return this.type;
  }

  /**
   * Used by {@link RootLoader} to construct instances of RecordSchemaImpl.
   *
   * @param __doc_            Document fragment to load this record object from (presumably a
                              {@link java.util.Map}).
   * @param __baseUri_        Base URI to generate child document IDs against.
   * @param __loadingOptions  Context for loading URIs and populating objects.
   * @param __docRoot_        ID at this position in the document (if available) (maybe?)
   * @throws ValidationException If the document fragment is not a {@link java.util.Map}
   *                             or validation of fields fails.
   */
  public RecordSchemaImpl(
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
      throw new ValidationException("RecordSchemaImpl called on non-map");
    }
    final java.util.Map<String, Object> __doc = (java.util.Map<String, Object>) __doc_;
    final java.util.List<ValidationException> __errors =
        new java.util.ArrayList<ValidationException>();
    if (__loadingOptions != null) {
      this.loadingOptions_ = __loadingOptions;
    }
    java.util.Optional<java.util.List<Object>> fields;

    if (__doc.containsKey("fields")) {
      try {
        fields =
            LoaderInstances
                .idmap_fields_optional_array_of_RecordField
                .loadField(__doc.get("fields"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        fields = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `fields` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      fields = null;
    }
    Record_name type;
    try {
      type =
          LoaderInstances
              .typedsl_Record_name_2
              .loadField(__doc.get("type"), __baseUri, __loadingOptions);
    } catch (ValidationException e) {
      type = null; // won't be used but prevents compiler from complaining.
      final String __message = "the `type` field is not valid because:";
      __errors.add(new ValidationException(__message, e));
    }
    if (!__errors.isEmpty()) {
      throw new ValidationException("Trying 'RecordField'", __errors);
    }
    this.fields = (java.util.Optional<java.util.List<Object>>) fields;
    this.type = (Record_name) type;
  }
}
