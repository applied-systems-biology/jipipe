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
* Auto-generated class implementation for <I>https://w3id.org/cwl/cwl#CommandLineTool</I><BR> <BLOCKQUOTE>
 This defines the schema of the CWL Command Line Tool Description document.
  </BLOCKQUOTE>
 */
public class CommandLineToolImpl extends SaveableImpl implements CommandLineTool {
  private LoadingOptions loadingOptions_ = new LoadingOptionsBuilder().build();
  private java.util.Map<String, Object> extensionFields_ =
      new java.util.HashMap<String, Object>();

  private java.util.Optional<String> id;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#Process/id</I><BR>
   * <BLOCKQUOTE>
   * The unique identifier for this object.
   * 
   * Only useful for `$graph` at `Process` level. Should not be exposed
   * to users in graphical or terminal user interfaces.
   *    * </BLOCKQUOTE>
   */

  public java.util.Optional<String> getId() {
    return this.id;
  }

  private CommandLineTool_class class_;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#CommandLineTool/class</I><BR>

   */

  public CommandLineTool_class getClass_() {
    return this.class_;
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

  private Object doc;

  /**
   * Getter for property <I>https://w3id.org/cwl/salad#Documented/doc</I><BR>
   * <BLOCKQUOTE>
   * A documentation string for this object, or an array of strings which should be concatenated.   * </BLOCKQUOTE>
   */

  public Object getDoc() {
    return this.doc;
  }

  private java.util.List<Object> inputs;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#inputs</I><BR>
   * <BLOCKQUOTE>
   * Defines the input parameters of the process.  The process is ready to
   * run when all required input parameters are associated with concrete
   * values.  Input parameters include a schema for each parameter which is
   * used to validate the input object.  It may also be used to build a user
   * interface for constructing the input object.
   * 
   * When accepting an input object, all input parameters must have a value.
   * If an input parameter is missing from the input object, it must be
   * assigned a value of `null` (or the value of `default` for that
   * parameter, if provided) for the purposes of validation and evaluation
   * of expressions.
   *    * </BLOCKQUOTE>
   */

  public java.util.List<Object> getInputs() {
    return this.inputs;
  }

  private java.util.List<Object> outputs;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#outputs</I><BR>
   * <BLOCKQUOTE>
   * Defines the parameters representing the output of the process.  May be
   * used to generate and/or validate the output object.
   *    * </BLOCKQUOTE>
   */

  public java.util.List<Object> getOutputs() {
    return this.outputs;
  }

  private java.util.Optional<java.util.List<Object>> requirements;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#requirements</I><BR>
   * <BLOCKQUOTE>
   * Declares requirements that apply to either the runtime environment or the
   * workflow engine that must be met in order to execute this process.  If
   * an implementation cannot satisfy all requirements, or a requirement is
   * listed which is not recognized by the implementation, it is a fatal
   * error and the implementation must not attempt to run the process,
   * unless overridden at user option.
   *    * </BLOCKQUOTE>
   */

  public java.util.Optional<java.util.List<Object>> getRequirements() {
    return this.requirements;
  }

  private java.util.Optional<java.util.List<Object>> hints;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#hints</I><BR>
   * <BLOCKQUOTE>
   * Declares hints applying to either the runtime environment or the
   * workflow engine that may be helpful in executing this process.  It is
   * not an error if an implementation cannot satisfy all hints, however
   * the implementation may report a warning.
   *    * </BLOCKQUOTE>
   */

  public java.util.Optional<java.util.List<Object>> getHints() {
    return this.hints;
  }

  private java.util.Optional<CWLVersion> cwlVersion;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#cwlVersion</I><BR>
   * <BLOCKQUOTE>
   * CWL document version. Always required at the document root. Not
   * required for a Process embedded inside another Process.
   *    * </BLOCKQUOTE>
   */

  public java.util.Optional<CWLVersion> getCwlVersion() {
    return this.cwlVersion;
  }

  private java.util.Optional<java.util.List<String>> intent;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#Process/intent</I><BR>
   * <BLOCKQUOTE>
   * An identifier for the type of computational operation, of this Process.
   * Especially useful for [`Operation`](Workflow.html#Operation), but can also be used for
   * [`CommandLineTool`](CommandLineTool.html#CommandLineTool),
   * [`Workflow`](Workflow.html#Workflow), or [ExpressionTool](Workflow.html#ExpressionTool).
   * 
   * If provided, then this must be an IRI of a concept node that
   * represents the type of operation, preferably defined within an ontology.
   * 
   * For example, in the domain of bioinformatics, one can use an IRI from
   * the EDAM Ontology's [Operation concept nodes](http://edamontology.org/operation_0004),
   * like [Alignment](http://edamontology.org/operation_2928),
   * or [Clustering](http://edamontology.org/operation_3432); or a more
   * specific Operation concept like
   * [Split read mapping](http://edamontology.org/operation_3199).
   *    * </BLOCKQUOTE>
   */

  public java.util.Optional<java.util.List<String>> getIntent() {
    return this.intent;
  }

  private Object baseCommand;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#baseCommand</I><BR>
   * <BLOCKQUOTE>
   * Specifies the program to execute.  If an array, the first element of
   * the array is the command to execute, and subsequent elements are
   * mandatory command line arguments.  The elements in `baseCommand` must
   * appear before any command line bindings from `inputBinding` or
   * `arguments`.
   * 
   * If `baseCommand` is not provided or is an empty array, the first
   * element of the command line produced after processing `inputBinding` or
   * `arguments` must be used as the program to execute.
   * 
   * If the program includes a path separator character it must
   * be an absolute path, otherwise it is an error.  If the program does not
   * include a path separator, search the `$PATH` variable in the runtime
   * environment of the workflow runner find the absolute path of the
   * executable.
   *    * </BLOCKQUOTE>
   */

  public Object getBaseCommand() {
    return this.baseCommand;
  }

  private java.util.Optional<java.util.List<Object>> arguments;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#arguments</I><BR>
   * <BLOCKQUOTE>
   * Command line bindings which are not directly associated with input
   * parameters. If the value is a string, it is used as a string literal
   * argument. If it is an Expression, the result of the evaluation is used
   * as an argument.
   *    * </BLOCKQUOTE>
   */

  public java.util.Optional<java.util.List<Object>> getArguments() {
    return this.arguments;
  }

  private Object stdin;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#CommandLineTool/stdin</I><BR>
   * <BLOCKQUOTE>
   * A path to a file whose contents must be piped into the command's
   * standard input stream.
   *    * </BLOCKQUOTE>
   */

  public Object getStdin() {
    return this.stdin;
  }

  private Object stderr;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#CommandLineTool/stderr</I><BR>
   * <BLOCKQUOTE>
   * Capture the command's standard error stream to a file written to
   * the designated output directory.
   * 
   * If `stderr` is a string, it specifies the file name to use.
   * 
   * If `stderr` is an expression, the expression is evaluated and must
   * return a string with the file name to use to capture stderr.  If the
   * return value is not a string, or the resulting path contains illegal
   * characters (such as the path separator `/`) it is an error.
   *    * </BLOCKQUOTE>
   */

  public Object getStderr() {
    return this.stderr;
  }

  private Object stdout;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#CommandLineTool/stdout</I><BR>
   * <BLOCKQUOTE>
   * Capture the command's standard output stream to a file written to
   * the designated output directory.
   * 
   * If the `CommandLineTool` contains logically chained commands
   * (e.g. `echo a && echo b`) `stdout` must include the output of
   * every command.
   * 
   * If `stdout` is a string, it specifies the file name to use.
   * 
   * If `stdout` is an expression, the expression is evaluated and must
   * return a string with the file name to use to capture stdout.  If the
   * return value is not a string, or the resulting path contains illegal
   * characters (such as the path separator `/`) it is an error.
   *    * </BLOCKQUOTE>
   */

  public Object getStdout() {
    return this.stdout;
  }

  private java.util.Optional<java.util.List<Object>> successCodes;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#CommandLineTool/successCodes</I><BR>
   * <BLOCKQUOTE>
   * Exit codes that indicate the process completed successfully.
   * 
   * If not specified, only exit code 0 is considered success.
   *    * </BLOCKQUOTE>
   */

  public java.util.Optional<java.util.List<Object>> getSuccessCodes() {
    return this.successCodes;
  }

  private java.util.Optional<java.util.List<Object>> temporaryFailCodes;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#CommandLineTool/temporaryFailCodes</I><BR>
   * <BLOCKQUOTE>
   * Exit codes that indicate the process failed due to a possibly
   * temporary condition, where executing the process with the same
   * runtime environment and inputs may produce different results.
   * 
   * If not specified, no exit codes are considered temporary failure.
   *    * </BLOCKQUOTE>
   */

  public java.util.Optional<java.util.List<Object>> getTemporaryFailCodes() {
    return this.temporaryFailCodes;
  }

  private java.util.Optional<java.util.List<Object>> permanentFailCodes;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#CommandLineTool/permanentFailCodes</I><BR>
   * <BLOCKQUOTE>
   * Exit codes that indicate the process failed due to a permanent logic error, where executing the process with the same runtime environment and same inputs is expected to always fail.
   * If not specified, all exit codes except 0 are considered permanent failure.   * </BLOCKQUOTE>
   */

  public java.util.Optional<java.util.List<Object>> getPermanentFailCodes() {
    return this.permanentFailCodes;
  }

  /**
   * Used by {@link org.w3id.cwl.cwl1_2.utils.RootLoader} to construct instances of CommandLineToolImpl.
   *
   * @param __doc_            Document fragment to load this record object from (presumably a
                              {@link java.util.Map}).
   * @param __baseUri_        Base URI to generate child document IDs against.
   * @param __loadingOptions  Context for loading URIs and populating objects.
   * @param __docRoot_        ID at this position in the document (if available) (maybe?)
   * @throws ValidationException If the document fragment is not a {@link java.util.Map}
   *                             or validation of fields fails.
   */
  public CommandLineToolImpl(
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
      throw new ValidationException("CommandLineToolImpl called on non-map");
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
    CommandLineTool_class class_;
    try {
      class_ =
          LoaderInstances
              .uri_CommandLineTool_class_False_True_None_None
              .loadField(__doc.get("class"), __baseUri, __loadingOptions);
    } catch (ValidationException e) {
      class_ = null; // won't be used but prevents compiler from complaining.
      final String __message = "the `class` field is not valid because:";
      __errors.add(new ValidationException(__message, e));
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
    java.util.List<Object> inputs;
    try {
      inputs =
          LoaderInstances
              .idmap_inputs_array_of_CommandInputParameter
              .loadField(__doc.get("inputs"), __baseUri, __loadingOptions);
    } catch (ValidationException e) {
      inputs = null; // won't be used but prevents compiler from complaining.
      final String __message = "the `inputs` field is not valid because:";
      __errors.add(new ValidationException(__message, e));
    }
    java.util.List<Object> outputs;
    try {
      outputs =
          LoaderInstances
              .idmap_outputs_array_of_CommandOutputParameter
              .loadField(__doc.get("outputs"), __baseUri, __loadingOptions);
    } catch (ValidationException e) {
      outputs = null; // won't be used but prevents compiler from complaining.
      final String __message = "the `outputs` field is not valid because:";
      __errors.add(new ValidationException(__message, e));
    }
    java.util.Optional<java.util.List<Object>> requirements;

    if (__doc.containsKey("requirements")) {
      try {
        requirements =
            LoaderInstances
                .idmap_requirements_optional_array_of_union_of_InlineJavascriptRequirement_or_SchemaDefRequirement_or_LoadListingRequirement_or_DockerRequirement_or_SoftwareRequirement_or_InitialWorkDirRequirement_or_EnvVarRequirement_or_ShellCommandRequirement_or_ResourceRequirement_or_WorkReuse_or_NetworkAccess_or_InplaceUpdateRequirement_or_ToolTimeLimit_or_SubworkflowFeatureRequirement_or_ScatterFeatureRequirement_or_MultipleInputFeatureRequirement_or_StepInputExpressionRequirement
                .loadField(__doc.get("requirements"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        requirements = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `requirements` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      requirements = null;
    }
    java.util.Optional<java.util.List<Object>> hints;

    if (__doc.containsKey("hints")) {
      try {
        hints =
            LoaderInstances
                .idmap_hints_optional_array_of_union_of_InlineJavascriptRequirement_or_SchemaDefRequirement_or_LoadListingRequirement_or_DockerRequirement_or_SoftwareRequirement_or_InitialWorkDirRequirement_or_EnvVarRequirement_or_ShellCommandRequirement_or_ResourceRequirement_or_WorkReuse_or_NetworkAccess_or_InplaceUpdateRequirement_or_ToolTimeLimit_or_SubworkflowFeatureRequirement_or_ScatterFeatureRequirement_or_MultipleInputFeatureRequirement_or_StepInputExpressionRequirement_or_AnyInstance
                .loadField(__doc.get("hints"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        hints = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `hints` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      hints = null;
    }
    java.util.Optional<CWLVersion> cwlVersion;

    if (__doc.containsKey("cwlVersion")) {
      try {
        cwlVersion =
            LoaderInstances
                .uri_optional_CWLVersion_False_True_None_None
                .loadField(__doc.get("cwlVersion"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        cwlVersion = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `cwlVersion` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      cwlVersion = null;
    }
    java.util.Optional<java.util.List<String>> intent;

    if (__doc.containsKey("intent")) {
      try {
        intent =
            LoaderInstances
                .uri_optional_array_of_StringInstance_True_False_None_None
                .loadField(__doc.get("intent"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        intent = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `intent` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      intent = null;
    }
    Object baseCommand;

    if (__doc.containsKey("baseCommand")) {
      try {
        baseCommand =
            LoaderInstances
                .union_of_NullInstance_or_StringInstance_or_array_of_StringInstance
                .loadField(__doc.get("baseCommand"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        baseCommand = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `baseCommand` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      baseCommand = null;
    }
    java.util.Optional<java.util.List<Object>> arguments;

    if (__doc.containsKey("arguments")) {
      try {
        arguments =
            LoaderInstances
                .optional_array_of_union_of_StringInstance_or_ExpressionLoader_or_CommandLineBinding
                .loadField(__doc.get("arguments"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        arguments = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `arguments` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      arguments = null;
    }
    Object stdin;

    if (__doc.containsKey("stdin")) {
      try {
        stdin =
            LoaderInstances
                .union_of_NullInstance_or_StringInstance_or_ExpressionLoader
                .loadField(__doc.get("stdin"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        stdin = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `stdin` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      stdin = null;
    }
    Object stderr;

    if (__doc.containsKey("stderr")) {
      try {
        stderr =
            LoaderInstances
                .union_of_NullInstance_or_StringInstance_or_ExpressionLoader
                .loadField(__doc.get("stderr"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        stderr = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `stderr` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      stderr = null;
    }
    Object stdout;

    if (__doc.containsKey("stdout")) {
      try {
        stdout =
            LoaderInstances
                .union_of_NullInstance_or_StringInstance_or_ExpressionLoader
                .loadField(__doc.get("stdout"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        stdout = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `stdout` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      stdout = null;
    }
    java.util.Optional<java.util.List<Object>> successCodes;

    if (__doc.containsKey("successCodes")) {
      try {
        successCodes =
            LoaderInstances
                .optional_array_of_IntegerInstance
                .loadField(__doc.get("successCodes"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        successCodes = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `successCodes` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      successCodes = null;
    }
    java.util.Optional<java.util.List<Object>> temporaryFailCodes;

    if (__doc.containsKey("temporaryFailCodes")) {
      try {
        temporaryFailCodes =
            LoaderInstances
                .optional_array_of_IntegerInstance
                .loadField(__doc.get("temporaryFailCodes"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        temporaryFailCodes = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `temporaryFailCodes` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      temporaryFailCodes = null;
    }
    java.util.Optional<java.util.List<Object>> permanentFailCodes;

    if (__doc.containsKey("permanentFailCodes")) {
      try {
        permanentFailCodes =
            LoaderInstances
                .optional_array_of_IntegerInstance
                .loadField(__doc.get("permanentFailCodes"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        permanentFailCodes = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `permanentFailCodes` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      permanentFailCodes = null;
    }
    if (!__errors.isEmpty()) {
      throw new ValidationException("Trying 'RecordField'", __errors);
    }
    this.id = (java.util.Optional<String>) id;
    this.label = (java.util.Optional<String>) label;
    this.doc = (Object) doc;
    this.inputs = (java.util.List<Object>) inputs;
    this.outputs = (java.util.List<Object>) outputs;
    this.requirements = (java.util.Optional<java.util.List<Object>>) requirements;
    this.hints = (java.util.Optional<java.util.List<Object>>) hints;
    this.cwlVersion = (java.util.Optional<CWLVersion>) cwlVersion;
    this.intent = (java.util.Optional<java.util.List<String>>) intent;
    this.class_ = (CommandLineTool_class) class_;
    this.baseCommand = (Object) baseCommand;
    this.arguments = (java.util.Optional<java.util.List<Object>>) arguments;
    this.stdin = (Object) stdin;
    this.stderr = (Object) stderr;
    this.stdout = (Object) stdout;
    this.successCodes = (java.util.Optional<java.util.List<Object>>) successCodes;
    this.temporaryFailCodes = (java.util.Optional<java.util.List<Object>>) temporaryFailCodes;
    this.permanentFailCodes = (java.util.Optional<java.util.List<Object>>) permanentFailCodes;
  }
}
