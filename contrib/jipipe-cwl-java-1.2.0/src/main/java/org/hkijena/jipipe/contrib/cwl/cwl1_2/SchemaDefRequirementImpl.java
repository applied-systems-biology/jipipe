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
* Auto-generated class implementation for <I>https://w3id.org/cwl/cwl#SchemaDefRequirement</I><BR> <BLOCKQUOTE>
 This field consists of an array of type definitions which must be used when
 interpreting the `inputs` and `outputs` fields.  When a `type` field
 contains a IRI, the implementation must check if the type is defined in
 `schemaDefs` and use that definition.  If the type is not found in
 `schemaDefs`, it is an error.  The entries in `schemaDefs` must be
 processed in the order listed such that later schema definitions may refer
 to earlier schema definitions.
 
 - **Type definitions are allowed for `enum` and `record` types only.**
 - Type definitions may be shared by defining them in a file and then
   `$include`-ing them in the `types` field.
 - A file can contain a list of type definitions
  </BLOCKQUOTE>
 */
public class SchemaDefRequirementImpl extends SaveableImpl implements SchemaDefRequirement {
  private LoadingOptions loadingOptions_ = new LoadingOptionsBuilder().build();
  private java.util.Map<String, Object> extensionFields_ =
      new java.util.HashMap<String, Object>();

  private SchemaDefRequirement_class class_;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#SchemaDefRequirement/class</I><BR>
   * <BLOCKQUOTE>
   * Always 'SchemaDefRequirement'   * </BLOCKQUOTE>
   */

  public SchemaDefRequirement_class getClass_() {
    return this.class_;
  }

  private java.util.List<Object> types;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#SchemaDefRequirement/types</I><BR>
   * <BLOCKQUOTE>
   * The list of type definitions.   * </BLOCKQUOTE>
   */

  public java.util.List<Object> getTypes() {
    return this.types;
  }

  /**
   * Used by {@link RootLoader} to construct instances of SchemaDefRequirementImpl.
   *
   * @param __doc_            Document fragment to load this record object from (presumably a
                              {@link java.util.Map}).
   * @param __baseUri_        Base URI to generate child document IDs against.
   * @param __loadingOptions  Context for loading URIs and populating objects.
   * @param __docRoot_        ID at this position in the document (if available) (maybe?)
   * @throws ValidationException If the document fragment is not a {@link java.util.Map}
   *                             or validation of fields fails.
   */
  public SchemaDefRequirementImpl(
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
      throw new ValidationException("SchemaDefRequirementImpl called on non-map");
    }
    final java.util.Map<String, Object> __doc = (java.util.Map<String, Object>) __doc_;
    final java.util.List<ValidationException> __errors =
        new java.util.ArrayList<ValidationException>();
    if (__loadingOptions != null) {
      this.loadingOptions_ = __loadingOptions;
    }
    SchemaDefRequirement_class class_;
    try {
      class_ =
          LoaderInstances
              .uri_SchemaDefRequirement_class_False_True_None_None
              .loadField(__doc.get("class"), __baseUri, __loadingOptions);
    } catch (ValidationException e) {
      class_ = null; // won't be used but prevents compiler from complaining.
      final String __message = "the `class` field is not valid because:";
      __errors.add(new ValidationException(__message, e));
    }
    java.util.List<Object> types;
    try {
      types =
          LoaderInstances
              .array_of_union_of_CommandInputRecordSchema_or_CommandInputEnumSchema_or_CommandInputArraySchema
              .loadField(__doc.get("types"), __baseUri, __loadingOptions);
    } catch (ValidationException e) {
      types = null; // won't be used but prevents compiler from complaining.
      final String __message = "the `types` field is not valid because:";
      __errors.add(new ValidationException(__message, e));
    }
    if (!__errors.isEmpty()) {
      throw new ValidationException("Trying 'RecordField'", __errors);
    }
    this.class_ = (SchemaDefRequirement_class) class_;
    this.types = (java.util.List<Object>) types;
  }
}
