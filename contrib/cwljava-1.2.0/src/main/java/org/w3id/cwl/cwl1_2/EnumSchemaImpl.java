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
   * Used by {@link org.w3id.cwl.cwl1_2.utils.RootLoader} to construct instances of EnumSchemaImpl.
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
