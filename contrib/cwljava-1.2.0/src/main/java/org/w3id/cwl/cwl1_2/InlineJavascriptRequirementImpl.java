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

package org.w3id.cwl.cwl1_2;

import org.w3id.cwl.cwl1_2.utils.LoaderInstances;
import org.w3id.cwl.cwl1_2.utils.LoadingOptions;
import org.w3id.cwl.cwl1_2.utils.LoadingOptionsBuilder;
import org.w3id.cwl.cwl1_2.utils.SaveableImpl;
import org.w3id.cwl.cwl1_2.utils.ValidationException;

/**
* Auto-generated class implementation for <I>https://w3id.org/cwl/cwl#InlineJavascriptRequirement</I><BR> <BLOCKQUOTE>
 Indicates that the workflow platform must support inline Javascript expressions.
 If this requirement is not present, the workflow platform must not perform expression
 interpolation.
  </BLOCKQUOTE>
 */
public class InlineJavascriptRequirementImpl extends SaveableImpl implements InlineJavascriptRequirement {
  private LoadingOptions loadingOptions_ = new LoadingOptionsBuilder().build();
  private java.util.Map<String, Object> extensionFields_ =
      new java.util.HashMap<String, Object>();

  private InlineJavascriptRequirement_class class_;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#InlineJavascriptRequirement/class</I><BR>
   * <BLOCKQUOTE>
   * Always 'InlineJavascriptRequirement'   * </BLOCKQUOTE>
   */

  public InlineJavascriptRequirement_class getClass_() {
    return this.class_;
  }

  private java.util.Optional<java.util.List<String>> expressionLib;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#InlineJavascriptRequirement/expressionLib</I><BR>
   * <BLOCKQUOTE>
   * Additional code fragments that will also be inserted
   * before executing the expression code.  Allows for function definitions that may
   * be called from CWL expressions.
   *    * </BLOCKQUOTE>
   */

  public java.util.Optional<java.util.List<String>> getExpressionLib() {
    return this.expressionLib;
  }

  /**
   * Used by {@link org.w3id.cwl.cwl1_2.utils.RootLoader} to construct instances of InlineJavascriptRequirementImpl.
   *
   * @param __doc_            Document fragment to load this record object from (presumably a
                              {@link java.util.Map}).
   * @param __baseUri_        Base URI to generate child document IDs against.
   * @param __loadingOptions  Context for loading URIs and populating objects.
   * @param __docRoot_        ID at this position in the document (if available) (maybe?)
   * @throws ValidationException If the document fragment is not a {@link java.util.Map}
   *                             or validation of fields fails.
   */
  public InlineJavascriptRequirementImpl(
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
      throw new ValidationException("InlineJavascriptRequirementImpl called on non-map");
    }
    final java.util.Map<String, Object> __doc = (java.util.Map<String, Object>) __doc_;
    final java.util.List<ValidationException> __errors =
        new java.util.ArrayList<ValidationException>();
    if (__loadingOptions != null) {
      this.loadingOptions_ = __loadingOptions;
    }
    InlineJavascriptRequirement_class class_;
    try {
      class_ =
          LoaderInstances
              .uri_InlineJavascriptRequirement_class_False_True_None_None
              .loadField(__doc.get("class"), __baseUri, __loadingOptions);
    } catch (ValidationException e) {
      class_ = null; // won't be used but prevents compiler from complaining.
      final String __message = "the `class` field is not valid because:";
      __errors.add(new ValidationException(__message, e));
    }
    java.util.Optional<java.util.List<String>> expressionLib;

    if (__doc.containsKey("expressionLib")) {
      try {
        expressionLib =
            LoaderInstances
                .optional_array_of_StringInstance
                .loadField(__doc.get("expressionLib"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        expressionLib = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `expressionLib` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      expressionLib = null;
    }
    if (!__errors.isEmpty()) {
      throw new ValidationException("Trying 'RecordField'", __errors);
    }
    this.class_ = (InlineJavascriptRequirement_class) class_;
    this.expressionLib = (java.util.Optional<java.util.List<String>>) expressionLib;
  }
}
