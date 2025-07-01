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
* Auto-generated class implementation for <I>https://w3id.org/cwl/cwl#CommandOutputParameter</I><BR> <BLOCKQUOTE>
 An output parameter for a CommandLineTool. </BLOCKQUOTE>
 */
public class CommandOutputParameterImpl extends SaveableImpl implements CommandOutputParameter {
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

  private java.util.Optional<String> label;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#Labeled/label</I><BR>
   * <BLOCKQUOTE>
   * A short, human-readable label of this object.   * </BLOCKQUOTE>
   */

  public java.util.Optional<String> getLabel() {
    return this.label;
  }

  private Object secondaryFiles;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#secondaryFiles</I><BR>
   * <BLOCKQUOTE>
   * Only valid when `type: File` or is an array of `items: File`.
   * 
   * Provides a pattern or expression specifying files or
   * directories that should be included alongside the primary
   * file.  Secondary files may be required or optional.  When not
   * explicitly specified, secondary files specified for `inputs`
   * are required and `outputs` are optional.  An implementation
   * must include matching Files and Directories in the
   * `secondaryFiles` property of the primary file.  These Files
   * and Directories must be transferred and staged alongside the
   * primary file.  An implementation may fail workflow execution
   * if a required secondary file does not exist.
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

  public Object getSecondaryFiles() {
    return this.secondaryFiles;
  }

  private java.util.Optional<Boolean> streamable;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#FieldBase/streamable</I><BR>
   * <BLOCKQUOTE>
   * Only valid when `type: File` or is an array of `items: File`.
   * 
   * A value of `true` indicates that the file is read or written
   * sequentially without seeking.  An implementation may use this flag to
   * indicate whether it is valid to stream file contents using a named
   * pipe.  Default: `false`.
   *    * </BLOCKQUOTE>
   */

  public java.util.Optional<Boolean> getStreamable() {
    return this.streamable;
  }

  private Object doc;

  /**
   * Getter for property <I>https://w3id.org/cwl/salad#Documented/doc</I><BR>
   * <BLOCKQUOTE>
   * A documentation string for this object, or an array of strings which should be concatenated.   * </BLOCKQUOTE>
   */

  public Object getDoc() {
    return this.doc;
  }

  private Object format;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#format</I><BR>
   * <BLOCKQUOTE>
   * Only valid when `type: File` or is an array of `items: File`.
   * 
   * This is the file format that will be assigned to the output
   * File object.
   *    * </BLOCKQUOTE>
   */

  public Object getFormat() {
    return this.format;
  }

  private Object type;

  /**
   * Getter for property <I>https://w3id.org/cwl/salad#type</I><BR>
   * <BLOCKQUOTE>
   * Specify valid types of data that may be assigned to this parameter.
   *    * </BLOCKQUOTE>
   */

  public Object getType() {
    return this.type;
  }

  private java.util.Optional<CommandOutputBinding> outputBinding;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#CommandOutputParameter/outputBinding</I><BR>
   * <BLOCKQUOTE>
   * Describes how to generate this output object based on the files produced by a CommandLineTool   * </BLOCKQUOTE>
   */

  public java.util.Optional<CommandOutputBinding> getOutputBinding() {
    return this.outputBinding;
  }

  /**
   * Used by {@link org.w3id.cwl.cwl1_1.utils.RootLoader} to construct instances of CommandOutputParameterImpl.
   *
   * @param __doc_            Document fragment to load this record object from (presumably a
                              {@link java.util.Map}).
   * @param __baseUri_        Base URI to generate child document IDs against.
   * @param __loadingOptions  Context for loading URIs and populating objects.
   * @param __docRoot_        ID at this position in the document (if available) (maybe?)
   * @throws ValidationException If the document fragment is not a {@link java.util.Map}
   *                             or validation of fields fails.
   */
  public CommandOutputParameterImpl(
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
      throw new ValidationException("CommandOutputParameterImpl called on non-map");
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
    Object secondaryFiles;

    if (__doc.containsKey("secondaryFiles")) {
      try {
        secondaryFiles =
            LoaderInstances
                .secondaryfilesdsl_union_of_NullInstance_or_SecondaryFileSchema_or_array_of_SecondaryFileSchema
                .loadField(__doc.get("secondaryFiles"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        secondaryFiles = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `secondaryFiles` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      secondaryFiles = null;
    }
    java.util.Optional<Boolean> streamable;

    if (__doc.containsKey("streamable")) {
      try {
        streamable =
            LoaderInstances
                .optional_BooleanInstance
                .loadField(__doc.get("streamable"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        streamable = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `streamable` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      streamable = null;
    }
    Object doc;

    if (__doc.containsKey("doc")) {
      try {
        doc =
            LoaderInstances
                .union_of_NullInstance_or_StringInstance_or_array_of_StringInstance
                .loadField(__doc.get("doc"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        doc = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `doc` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      doc = null;
    }
    Object format;

    if (__doc.containsKey("format")) {
      try {
        format =
            LoaderInstances
                .uri_union_of_NullInstance_or_StringInstance_or_ExpressionLoader_True_False_None_True
                .loadField(__doc.get("format"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        format = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `format` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      format = null;
    }
    Object type;
    try {
      type =
          LoaderInstances
              .typedsl_union_of_CWLType_or_stdout_or_stderr_or_CommandOutputRecordSchema_or_CommandOutputEnumSchema_or_CommandOutputArraySchema_or_StringInstance_or_array_of_union_of_CWLType_or_CommandOutputRecordSchema_or_CommandOutputEnumSchema_or_CommandOutputArraySchema_or_StringInstance_2
              .loadField(__doc.get("type"), __baseUri, __loadingOptions);
    } catch (ValidationException e) {
      type = null; // won't be used but prevents compiler from complaining.
      final String __message = "the `type` field is not valid because:";
      __errors.add(new ValidationException(__message, e));
    }
    java.util.Optional<CommandOutputBinding> outputBinding;

    if (__doc.containsKey("outputBinding")) {
      try {
        outputBinding =
            LoaderInstances
                .optional_CommandOutputBinding
                .loadField(__doc.get("outputBinding"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        outputBinding = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `outputBinding` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      outputBinding = null;
    }
    if (!__errors.isEmpty()) {
      throw new ValidationException("Trying 'RecordField'", __errors);
    }
    this.label = (java.util.Optional<String>) label;
    this.secondaryFiles = (Object) secondaryFiles;
    this.streamable = (java.util.Optional<Boolean>) streamable;
    this.doc = (Object) doc;
    this.id = (java.util.Optional<String>) id;
    this.format = (Object) format;
    this.type = (Object) type;
    this.outputBinding = (java.util.Optional<CommandOutputBinding>) outputBinding;
  }
}
