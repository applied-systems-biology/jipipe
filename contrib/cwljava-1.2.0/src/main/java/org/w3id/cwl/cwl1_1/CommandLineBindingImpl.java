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
* Auto-generated class implementation for <I>https://w3id.org/cwl/cwl#CommandLineBinding</I><BR> <BLOCKQUOTE>
 
 When listed under `inputBinding` in the input schema, the term
 "value" refers to the the corresponding value in the input object.  For
 binding objects listed in `CommandLineTool.arguments`, the term "value"
 refers to the effective value after evaluating `valueFrom`.
 
 The binding behavior when building the command line depends on the data
 type of the value.  If there is a mismatch between the type described by
 the input schema and the effective value, such as resulting from an
 expression evaluation, an implementation must use the data type of the
 effective value.
 
   - **string**: Add `prefix` and the string to the command line.
 
   - **number**: Add `prefix` and decimal representation to command line.
 
   - **boolean**: If true, add `prefix` to the command line.  If false, add
       nothing.
 
   - **File**: Add `prefix` and the value of
     [`File.path`](#File) to the command line.
 
   - **Directory**: Add `prefix` and the value of
     [`Directory.path`](#Directory) to the command line.
 
   - **array**: If `itemSeparator` is specified, add `prefix` and the join
       the array into a single string with `itemSeparator` separating the
       items.  Otherwise first add `prefix`, then recursively process
       individual elements.
       If the array is empty, it does not add anything to command line.
 
   - **object**: Add `prefix` only, and recursively add object fields for
       which `inputBinding` is specified.
 
   - **null**: Add nothing.
  </BLOCKQUOTE>
 */
public class CommandLineBindingImpl extends SaveableImpl implements CommandLineBinding {
  private LoadingOptions loadingOptions_ = new LoadingOptionsBuilder().build();
  private java.util.Map<String, Object> extensionFields_ =
      new java.util.HashMap<String, Object>();

  private java.util.Optional<Boolean> loadContents;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#InputBinding/loadContents</I><BR>
   * <BLOCKQUOTE>
   * Use of `loadContents` in `InputBinding` is deprecated.
   * Preserved for v1.0 backwards compatability.  Will be removed in
   * CWL v2.0.  Use `InputParameter.loadContents` instead.
   * 
   * Only valid when `type: File` or is an array of `items: File`.
   * 
   * Read up to the first 64 KiB of text from the file and place it in the
   * "contents" field of the file object for use by expressions.
   *    * </BLOCKQUOTE>
   */

  public java.util.Optional<Boolean> getLoadContents() {
    return this.loadContents;
  }

  private Object position;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#CommandLineBinding/position</I><BR>
   * <BLOCKQUOTE>
   * The sorting key.  Default position is 0. If the inputBinding is
   * associated with an input parameter, then the value of `self` in the
   * expression will be the value of the input parameter.  Input parameter
   * defaults (as specified by the `InputParameter.default` field) must be
   * applied before evaluating the expression. Expressions must return a
   * single value of type int or a null.
   *    * </BLOCKQUOTE>
   */

  public Object getPosition() {
    return this.position;
  }

  private java.util.Optional<String> prefix;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#CommandLineBinding/prefix</I><BR>
   * <BLOCKQUOTE>
   * Command line prefix to add before the value.   * </BLOCKQUOTE>
   */

  public java.util.Optional<String> getPrefix() {
    return this.prefix;
  }

  private java.util.Optional<Boolean> separate;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#CommandLineBinding/separate</I><BR>
   * <BLOCKQUOTE>
   * If true (default), then the prefix and value must be added as separate
   * command line arguments; if false, prefix and value must be concatenated
   * into a single command line argument.
   *    * </BLOCKQUOTE>
   */

  public java.util.Optional<Boolean> getSeparate() {
    return this.separate;
  }

  private java.util.Optional<String> itemSeparator;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#CommandLineBinding/itemSeparator</I><BR>
   * <BLOCKQUOTE>
   * Join the array elements into a single string with the elements
   * separated by by `itemSeparator`.
   *    * </BLOCKQUOTE>
   */

  public java.util.Optional<String> getItemSeparator() {
    return this.itemSeparator;
  }

  private Object valueFrom;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#CommandLineBinding/valueFrom</I><BR>
   * <BLOCKQUOTE>
   * If `valueFrom` is a constant string value, use this as the value and
   * apply the binding rules above.
   * 
   * If `valueFrom` is an expression, evaluate the expression to yield the
   * actual value to use to build the command line and apply the binding
   * rules above.  If the inputBinding is associated with an input
   * parameter, the value of `self` in the expression will be the value of
   * the input parameter.  Input parameter defaults (as specified by the
   * `InputParameter.default` field) must be applied before evaluating the
   * expression.
   * 
   * If the value of the associated input parameter is `null`, `valueFrom` is
   * not evaluated and nothing is added to the command line.
   * 
   * When a binding is part of the `CommandLineTool.arguments` field,
   * the `valueFrom` field is required.
   *    * </BLOCKQUOTE>
   */

  public Object getValueFrom() {
    return this.valueFrom;
  }

  private java.util.Optional<Boolean> shellQuote;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#CommandLineBinding/shellQuote</I><BR>
   * <BLOCKQUOTE>
   * If `ShellCommandRequirement` is in the requirements for the current command,
   * this controls whether the value is quoted on the command line (default is true).
   * Use `shellQuote: false` to inject metacharacters for operations such as pipes.
   * 
   * If `shellQuote` is true or not provided, the implementation must not
   * permit interpretation of any shell metacharacters or directives.
   *    * </BLOCKQUOTE>
   */

  public java.util.Optional<Boolean> getShellQuote() {
    return this.shellQuote;
  }

  /**
   * Used by {@link org.w3id.cwl.cwl1_1.utils.RootLoader} to construct instances of CommandLineBindingImpl.
   *
   * @param __doc_            Document fragment to load this record object from (presumably a
                              {@link java.util.Map}).
   * @param __baseUri_        Base URI to generate child document IDs against.
   * @param __loadingOptions  Context for loading URIs and populating objects.
   * @param __docRoot_        ID at this position in the document (if available) (maybe?)
   * @throws ValidationException If the document fragment is not a {@link java.util.Map}
   *                             or validation of fields fails.
   */
  public CommandLineBindingImpl(
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
      throw new ValidationException("CommandLineBindingImpl called on non-map");
    }
    final java.util.Map<String, Object> __doc = (java.util.Map<String, Object>) __doc_;
    final java.util.List<ValidationException> __errors =
        new java.util.ArrayList<ValidationException>();
    if (__loadingOptions != null) {
      this.loadingOptions_ = __loadingOptions;
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
    Object position;

    if (__doc.containsKey("position")) {
      try {
        position =
            LoaderInstances
                .union_of_NullInstance_or_IntegerInstance_or_ExpressionLoader
                .loadField(__doc.get("position"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        position = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `position` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      position = null;
    }
    java.util.Optional<String> prefix;

    if (__doc.containsKey("prefix")) {
      try {
        prefix =
            LoaderInstances
                .optional_StringInstance
                .loadField(__doc.get("prefix"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        prefix = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `prefix` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      prefix = null;
    }
    java.util.Optional<Boolean> separate;

    if (__doc.containsKey("separate")) {
      try {
        separate =
            LoaderInstances
                .optional_BooleanInstance
                .loadField(__doc.get("separate"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        separate = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `separate` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      separate = null;
    }
    java.util.Optional<String> itemSeparator;

    if (__doc.containsKey("itemSeparator")) {
      try {
        itemSeparator =
            LoaderInstances
                .optional_StringInstance
                .loadField(__doc.get("itemSeparator"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        itemSeparator = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `itemSeparator` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      itemSeparator = null;
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
    java.util.Optional<Boolean> shellQuote;

    if (__doc.containsKey("shellQuote")) {
      try {
        shellQuote =
            LoaderInstances
                .optional_BooleanInstance
                .loadField(__doc.get("shellQuote"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        shellQuote = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `shellQuote` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      shellQuote = null;
    }
    if (!__errors.isEmpty()) {
      throw new ValidationException("Trying 'RecordField'", __errors);
    }
    this.loadContents = (java.util.Optional<Boolean>) loadContents;
    this.position = (Object) position;
    this.prefix = (java.util.Optional<String>) prefix;
    this.separate = (java.util.Optional<Boolean>) separate;
    this.itemSeparator = (java.util.Optional<String>) itemSeparator;
    this.valueFrom = (Object) valueFrom;
    this.shellQuote = (java.util.Optional<Boolean>) shellQuote;
  }
}
