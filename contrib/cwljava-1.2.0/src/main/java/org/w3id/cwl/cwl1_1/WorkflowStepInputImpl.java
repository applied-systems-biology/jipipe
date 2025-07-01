// Copyright Common Workflow Language project contributors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.w3id.cwl.cwl1_1;

import org.w3id.cwl.cwl1_1.utils.LoaderInstances;
import org.w3id.cwl.cwl1_1.utils.LoadingOptions;
import org.w3id.cwl.cwl1_1.utils.LoadingOptionsBuilder;
import org.w3id.cwl.cwl1_1.utils.SaveableImpl;
import org.w3id.cwl.cwl1_1.utils.ValidationException;

/**
* Auto-generated class implementation for <I>https://w3id.org/cwl/cwl#WorkflowStepInput</I><BR> <BLOCKQUOTE>
 The input of a workflow step connects an upstream parameter (from the
 workflow inputs, or the outputs of other workflows steps) with the input
 parameters of the process specified by the `run` field. Only input parameters
 declared by the target process will be passed through at runtime to the process
 though additonal parameters may be specified (for use within `valueFrom`
 expressions for instance) - unconnected or unused parameters do not represent an
 error condition.
 
 ## Input object
 
 A WorkflowStepInput object must contain an `id` field in the form
 `#fieldname` or `#prefix/fieldname`.  When the `id` field contains a slash
 `/` the field name consists of the characters following the final slash
 (the prefix portion may contain one or more slashes to indicate scope).
 This defines a field of the workflow step input object with the value of
 the `source` parameter(s).
 
 ## Merging
 
 To merge multiple inbound data links,
 [MultipleInputFeatureRequirement](#MultipleInputFeatureRequirement) must be specified
 in the workflow or workflow step requirements.
 
 If the sink parameter is an array, or named in a [workflow
 scatter](#WorkflowStep) operation, there may be multiple inbound data links
 listed in the `source` field.  The values from the input links are merged
 depending on the method specified in the `linkMerge` field.  If not
 specified, the default method is "merge_nested".
 
 * **merge_nested**
 
   The input must be an array consisting of exactly one entry for each
   input link.  If "merge_nested" is specified with a single link, the value
   from the link must be wrapped in a single-item list.
 
 * **merge_flattened**
 
   1. The source and sink parameters must be compatible types, or the source
      type must be compatible with single element from the "items" type of
      the destination array parameter.
   2. Source parameters which are arrays are concatenated.
      Source parameters which are single element types are appended as
      single elements.
  </BLOCKQUOTE>
 */
public class WorkflowStepInputImpl extends SaveableImpl implements WorkflowStepInput {
  private LoadingOptions loadingOptions_ = new LoadingOptionsBuilder().build();
  private java.util.Map<String, Object> extensionFields_ =
      new java.util.HashMap<String, Object>();

  private java.util.Optional<String> id;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#Identified/id</I><BR>
   * <BLOCKQUOTE>
   * The unique identifier for this object.   * </BLOCKQUOTE>
   */

  public java.util.Optional<String> getId() {
    return this.id;
  }

  private Object source;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#source</I><BR>
   * <BLOCKQUOTE>
   * Specifies one or more workflow parameters that will provide input to
   * the underlying step parameter.
   *    * </BLOCKQUOTE>
   */

  public Object getSource() {
    return this.source;
  }

  private java.util.Optional<LinkMergeMethod> linkMerge;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#Sink/linkMerge</I><BR>
   * <BLOCKQUOTE>
   * The method to use to merge multiple inbound links into a single array.
   * If not specified, the default method is "merge_nested".
   *    * </BLOCKQUOTE>
   */

  public java.util.Optional<LinkMergeMethod> getLinkMerge() {
    return this.linkMerge;
  }

  private java.util.Optional<Boolean> loadContents;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#LoadContents/loadContents</I><BR>
   * <BLOCKQUOTE>
   * Only valid when `type: File` or is an array of `items: File`.
   * 
   * Read up to the first 64 KiB of text from the file and place it in the
   * "contents" field of the file object for use by expressions.
   *    * </BLOCKQUOTE>
   */

  public java.util.Optional<Boolean> getLoadContents() {
    return this.loadContents;
  }

  private java.util.Optional<LoadListingEnum> loadListing;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#LoadContents/loadListing</I><BR>
   * <BLOCKQUOTE>
   * Only valid when `type: Directory` or is an array of `items: Directory`.
   * 
   * Specify the desired behavior for loading the `listing` field of
   * a Directory object for use by expressions.
   * 
   * The order of precedence for loadListing is:
   * 
   *   1. `loadListing` on an individual parameter
   *   2. Inherited from `LoadListingRequirement`
   *   3. By default: `no_listing`
   *    * </BLOCKQUOTE>
   */

  public java.util.Optional<LoadListingEnum> getLoadListing() {
    return this.loadListing;
  }

  private java.util.Optional<String> label;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#Labeled/label</I><BR>
   * <BLOCKQUOTE>
   * A short, human-readable label of this object.   * </BLOCKQUOTE>
   */

  public java.util.Optional<String> getLabel() {
    return this.label;
  }

  private java.util.Optional<Object> default_;

  /**
   * Getter for property <I>https://w3id.org/cwl/salad#default</I><BR>
   * <BLOCKQUOTE>
   * The default value for this parameter to use if either there is no
   * `source` field, or the value produced by the `source` is `null`.  The
   * default must be applied prior to scattering or evaluating `valueFrom`.
   *    * </BLOCKQUOTE>
   */

  public java.util.Optional<Object> getDefault() {
    return this.default_;
  }

  private Object valueFrom;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#WorkflowStepInput/valueFrom</I><BR>
   * <BLOCKQUOTE>
   * To use valueFrom, [StepInputExpressionRequirement](#StepInputExpressionRequirement) must
   * be specified in the workflow or workflow step requirements.
   * 
   * If `valueFrom` is a constant string value, use this as the value for
   * this input parameter.
   * 
   * If `valueFrom` is a parameter reference or expression, it must be
   * evaluated to yield the actual value to be assiged to the input field.
   * 
   * The `self` value in the parameter reference or expression must be
   * 1. `null` if there is no `source` field
   * 2. the value of the parameter(s) specified in the `source` field when this
   * workflow input parameter **is not** specified in this workflow step's `scatter` field.
   * 3. an element of the parameter specified in the `source` field when this workflow input
   * parameter **is** specified in this workflow step's `scatter` field.
   * 
   * The value of `inputs` in the parameter reference or expression must be
   * the input object to the workflow step after assigning the `source`
   * values, applying `default`, and then scattering.  The order of
   * evaluating `valueFrom` among step input parameters is undefined and the
   * result of evaluating `valueFrom` on a parameter must not be visible to
   * evaluation of `valueFrom` on other parameters.
   *    * </BLOCKQUOTE>
   */

  public Object getValueFrom() {
    return this.valueFrom;
  }

  /**
   * Used by {@link org.w3id.cwl.cwl1_1.utils.RootLoader} to construct instances of WorkflowStepInputImpl.
   *
   * @param __doc_            Document fragment to load this record object from (presumably a
                              {@link java.util.Map}).
   * @param __baseUri_        Base URI to generate child document IDs against.
   * @param __loadingOptions  Context for loading URIs and populating objects.
   * @param __docRoot_        ID at this position in the document (if available) (maybe?)
   * @throws ValidationException If the document fragment is not a {@link java.util.Map}
   *                             or validation of fields fails.
   */
  public WorkflowStepInputImpl(
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
      throw new ValidationException("WorkflowStepInputImpl called on non-map");
    }
    final java.util.Map<String, Object> __doc = (java.util.Map<String, Object>) __doc_;
    final java.util.List<ValidationException> __errors =
        new java.util.ArrayList<ValidationException>();
    if (__loadingOptions != null) {
      this.loadingOptions_ = __loadingOptions;
    }
    java.util.Optional<String> id;

    if (__doc.containsKey("id")) {
      try {
        id =
            LoaderInstances
                .uri_optional_StringInstance_True_False_None_None
                .loadField(__doc.get("id"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        id = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `id` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      id = null;
    }

    Boolean __original_is_null = id == null;
    if (id == null) {
      if (__docRoot != null) {
        id = java.util.Optional.of(__docRoot);
      } else {
        id = java.util.Optional.of("_:" + java.util.UUID.randomUUID().toString());
      }
    }
    if (__original_is_null) {
        __baseUri = __baseUri_;
    } else {
        __baseUri = (String) id.orElse(null);
    }
    Object source;

    if (__doc.containsKey("source")) {
      try {
        source =
            LoaderInstances
                .uri_union_of_NullInstance_or_StringInstance_or_array_of_StringInstance_False_False_2_None
                .loadField(__doc.get("source"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        source = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `source` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      source = null;
    }
    java.util.Optional<LinkMergeMethod> linkMerge;

    if (__doc.containsKey("linkMerge")) {
      try {
        linkMerge =
            LoaderInstances
                .optional_LinkMergeMethod
                .loadField(__doc.get("linkMerge"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        linkMerge = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `linkMerge` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      linkMerge = null;
    }
    java.util.Optional<Boolean> loadContents;

    if (__doc.containsKey("loadContents")) {
      try {
        loadContents =
            LoaderInstances
                .optional_BooleanInstance
                .loadField(__doc.get("loadContents"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        loadContents = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `loadContents` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      loadContents = null;
    }
    java.util.Optional<LoadListingEnum> loadListing;

    if (__doc.containsKey("loadListing")) {
      try {
        loadListing =
            LoaderInstances
                .optional_LoadListingEnum
                .loadField(__doc.get("loadListing"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        loadListing = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `loadListing` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      loadListing = null;
    }
    java.util.Optional<String> label;

    if (__doc.containsKey("label")) {
      try {
        label =
            LoaderInstances
                .optional_StringInstance
                .loadField(__doc.get("label"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        label = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `label` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      label = null;
    }
    java.util.Optional<Object> default_;

    if (__doc.containsKey("default")) {
      try {
        default_ =
            LoaderInstances
                .optional_CWLObjectType
                .loadField(__doc.get("default"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        default_ = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `default` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      default_ = null;
    }
    Object valueFrom;

    if (__doc.containsKey("valueFrom")) {
      try {
        valueFrom =
            LoaderInstances
                .union_of_NullInstance_or_StringInstance_or_ExpressionLoader
                .loadField(__doc.get("valueFrom"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        valueFrom = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `valueFrom` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      valueFrom = null;
    }
    if (!__errors.isEmpty()) {
      throw new ValidationException("Trying 'RecordField'", __errors);
    }
    this.id = (java.util.Optional<String>) id;
    this.source = (Object) source;
    this.linkMerge = (java.util.Optional<LinkMergeMethod>) linkMerge;
    this.loadContents = (java.util.Optional<Boolean>) loadContents;
    this.loadListing = (java.util.Optional<LoadListingEnum>) loadListing;
    this.label = (java.util.Optional<String>) label;
    this.default_ = (java.util.Optional<Object>) default_;
    this.valueFrom = (Object) valueFrom;
  }
}
