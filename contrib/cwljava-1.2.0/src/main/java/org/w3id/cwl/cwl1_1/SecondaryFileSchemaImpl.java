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
* Auto-generated class implementation for <I>https://w3id.org/cwl/cwl#SecondaryFileSchema</I><BR>
 */
public class SecondaryFileSchemaImpl extends SaveableImpl implements SecondaryFileSchema {
  private LoadingOptions loadingOptions_ = new LoadingOptionsBuilder().build();
  private java.util.Map<String, Object> extensionFields_ =
      new java.util.HashMap<String, Object>();

  private Object pattern;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#SecondaryFileSchema/pattern</I><BR>
   * <BLOCKQUOTE>
   * Provides a pattern or expression specifying files or directories that
   * should be included alongside the primary file.
   * 
   * If the value is an expression, the value of `self` in the expression
   * must be the primary input or output File object to which this binding
   * applies.  The `basename`, `nameroot` and `nameext` fields must be
   * present in `self`.  For `CommandLineTool` outputs the `path` field must
   * also be present.  The expression must return a filename string relative
   * to the path to the primary File, a File or Directory object with either
   * `path` or `location` and `basename` fields set, or an array consisting
   * of strings or File or Directory objects.  It is legal to reference an
   * unchanged File or Directory object taken from input as a secondaryFile.
   * The expression may return "null" in which case there is no secondaryFile
   * from that expression.
   * 
   * To work on non-filename-preserving storage systems, portable tool
   * descriptions should avoid constructing new values from `location`, but
   * should construct relative references using `basename` or `nameroot`
   * instead.
   * 
   * If a value in `secondaryFiles` is a string that is not an expression,
   * it specifies that the following pattern should be applied to the path
   * of the primary file to yield a filename relative to the primary File:
   * 
   *   1. If string ends with `?` character, remove the last `?` and mark
   *     the resulting secondary file as optional.
   *   2. If string begins with one or more caret `^` characters, for each
   *     caret, remove the last file extension from the path (the last
   *     period `.` and all following characters).  If there are no file
   *     extensions, the path is unchanged.
   *   3. Append the remainder of the string to the end of the file path.
   *    * </BLOCKQUOTE>
   */

  public Object getPattern() {
    return this.pattern;
  }

  private Object required;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#SecondaryFileSchema/required</I><BR>
   * <BLOCKQUOTE>
   * An implementation must not fail workflow execution if `required` is
   * set to `false` and the expected secondary file does not exist.
   * Default value for `required` field is `true` for secondary files on
   * input and `false` for secondary files on output.
   *    * </BLOCKQUOTE>
   */

  public Object getRequired() {
    return this.required;
  }

  /**
   * Used by {@link org.w3id.cwl.cwl1_1.utils.RootLoader} to construct instances of SecondaryFileSchemaImpl.
   *
   * @param __doc_            Document fragment to load this record object from (presumably a
                              {@link java.util.Map}).
   * @param __baseUri_        Base URI to generate child document IDs against.
   * @param __loadingOptions  Context for loading URIs and populating objects.
   * @param __docRoot_        ID at this position in the document (if available) (maybe?)
   * @throws ValidationException If the document fragment is not a {@link java.util.Map}
   *                             or validation of fields fails.
   */
  public SecondaryFileSchemaImpl(
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
      throw new ValidationException("SecondaryFileSchemaImpl called on non-map");
    }
    final java.util.Map<String, Object> __doc = (java.util.Map<String, Object>) __doc_;
    final java.util.List<ValidationException> __errors =
        new java.util.ArrayList<ValidationException>();
    if (__loadingOptions != null) {
      this.loadingOptions_ = __loadingOptions;
    }
    Object pattern;
    try {
      pattern =
          LoaderInstances
              .union_of_StringInstance_or_ExpressionLoader
              .loadField(__doc.get("pattern"), __baseUri, __loadingOptions);
    } catch (ValidationException e) {
      pattern = null; // won't be used but prevents compiler from complaining.
      final String __message = "the `pattern` field is not valid because:";
      __errors.add(new ValidationException(__message, e));
    }
    Object required;

    if (__doc.containsKey("required")) {
      try {
        required =
            LoaderInstances
                .union_of_NullInstance_or_BooleanInstance_or_ExpressionLoader
                .loadField(__doc.get("required"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        required = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `required` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      required = null;
    }
    if (!__errors.isEmpty()) {
      throw new ValidationException("Trying 'RecordField'", __errors);
    }
    this.pattern = (Object) pattern;
    this.required = (Object) required;
  }
}
