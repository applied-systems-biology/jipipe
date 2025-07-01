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
* Auto-generated class implementation for <I>https://w3id.org/cwl/cwl#WorkflowStep</I><BR> <BLOCKQUOTE>
 A workflow step is an executable element of a workflow.  It specifies the
 underlying process implementation (such as `CommandLineTool` or another
 `Workflow`) in the `run` field and connects the input and output parameters
 of the underlying process to workflow parameters.
 
 # Scatter/gather
 
 To use scatter/gather,
 [ScatterFeatureRequirement](#ScatterFeatureRequirement) must be specified
 in the workflow or workflow step requirements.
 
 A "scatter" operation specifies that the associated workflow step or
 subworkflow should execute separately over a list of input elements.  Each
 job making up a scatter operation is independent and may be executed
 concurrently.
 
 The `scatter` field specifies one or more input parameters which will be
 scattered.  An input parameter may be listed more than once.  The declared
 type of each input parameter implicitly becomes an array of items of the
 input parameter type.  If a parameter is listed more than once, it becomes
 a nested array.  As a result, upstream parameters which are connected to
 scattered parameters must be arrays.
 
 All output parameter types are also implicitly wrapped in arrays.  Each job
 in the scatter results in an entry in the output array.
 
 If any scattered parameter runtime value is an empty array, all outputs are
 set to empty arrays and no work is done for the step, according to
 applicable scattering rules.
 
 If `scatter` declares more than one input parameter, `scatterMethod`
 describes how to decompose the input into a discrete set of jobs.
 
   * **dotproduct** specifies that each of the input arrays are aligned and one
       element taken from each array to construct each job.  It is an error
       if all input arrays are not the same length.
 
   * **nested_crossproduct** specifies the Cartesian product of the inputs,
       producing a job for every combination of the scattered inputs.  The
       output must be nested arrays for each level of scattering, in the
       order that the input arrays are listed in the `scatter` field.
 
   * **flat_crossproduct** specifies the Cartesian product of the inputs,
       producing a job for every combination of the scattered inputs.  The
       output arrays must be flattened to a single level, but otherwise listed in the
       order that the input arrays are listed in the `scatter` field.
 
 # Conditional execution (Optional)
 
 Conditional execution makes execution of a step conditional on an
 expression.  A step that is not executed is "skipped".  A skipped
 step produces `null` for all output parameters.
 
 The condition is evaluated after `scatter`, using the input object
 of each individual scatter job.  This means over a set of scatter
 jobs, some may be executed and some may be skipped.  When the
 results are gathered, skipped steps must be `null` in the output
 arrays.
 
 The `when` field controls conditional execution.  This is an
 expression that must be evaluated with `inputs` bound to the step
 input object (or individual scatter job), and returns a boolean
 value.  It is an error if this expression returns a value other
 than `true` or `false`.
 
 Conditionals in CWL are an optional feature and are not required
 to be implemented by all consumers of CWL documents.  An
 implementation that does not support conditionals must return a
 fatal error when attempting to execute a workflow that uses
 conditional constructs the implementation does not support.
 
 # Subworkflows
 
 To specify a nested workflow as part of a workflow step,
 [SubworkflowFeatureRequirement](#SubworkflowFeatureRequirement) must be
 specified in the workflow or workflow step requirements.
 
 It is a fatal error if a workflow directly or indirectly invokes itself as
 a subworkflow (recursive workflows are not allowed).
  </BLOCKQUOTE>
 */
public class WorkflowStepImpl extends SaveableImpl implements WorkflowStep {
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

  private Object doc;

  /**
   * Getter for property <I>https://w3id.org/cwl/salad#Documented/doc</I><BR>
   * <BLOCKQUOTE>
   * A documentation string for this object, or an array of strings which should be concatenated.   * </BLOCKQUOTE>
   */

  public Object getDoc() {
    return this.doc;
  }

  private java.util.List<Object> in;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#in</I><BR>
   * <BLOCKQUOTE>
   * Defines the input parameters of the workflow step.  The process is ready to
   * run when all required input parameters are associated with concrete
   * values.  Input parameters include a schema for each parameter which is
   * used to validate the input object.  It may also be used build a user
   * interface for constructing the input object.
   *    * </BLOCKQUOTE>
   */

  public java.util.List<Object> getIn() {
    return this.in;
  }

  private java.util.List<Object> out;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#out</I><BR>
   * <BLOCKQUOTE>
   * Defines the parameters representing the output of the process.  May be
   * used to generate and/or validate the output object.
   *    * </BLOCKQUOTE>
   */

  public java.util.List<Object> getOut() {
    return this.out;
  }

  private java.util.Optional<java.util.List<Object>> requirements;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#requirements</I><BR>
   * <BLOCKQUOTE>
   * Declares requirements that apply to either the runtime environment or the
   * workflow engine that must be met in order to execute this workflow step.  If
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
   * workflow engine that may be helpful in executing this workflow step.  It is
   * not an error if an implementation cannot satisfy all hints, however
   * the implementation may report a warning.
   *    * </BLOCKQUOTE>
   */

  public java.util.Optional<java.util.List<Object>> getHints() {
    return this.hints;
  }

  private Object run;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#run</I><BR>
   * <BLOCKQUOTE>
   * Specifies the process to run.  If `run` is a string, it must be an absolute IRI
   * or a relative path from the primary document.
   *    * </BLOCKQUOTE>
   */

  public Object getRun() {
    return this.run;
  }

  private java.util.Optional<String> when;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#WorkflowStep/when</I><BR>
   * <BLOCKQUOTE>
   * If defined, only run the step when the expression evaluates to
   * `true`.  If `false` the step is skipped.  A skipped step
   * produces a `null` on each output.
   *    * </BLOCKQUOTE>
   */

  public java.util.Optional<String> getWhen() {
    return this.when;
  }

  private Object scatter;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#scatter</I><BR>

   */

  public Object getScatter() {
    return this.scatter;
  }

  private java.util.Optional<ScatterMethod> scatterMethod;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#scatterMethod</I><BR>
   * <BLOCKQUOTE>
   * Required if `scatter` is an array of more than one element.
   *    * </BLOCKQUOTE>
   */

  public java.util.Optional<ScatterMethod> getScatterMethod() {
    return this.scatterMethod;
  }

  /**
   * Used by {@link org.w3id.cwl.cwl1_2.utils.RootLoader} to construct instances of WorkflowStepImpl.
   *
   * @param __doc_            Document fragment to load this record object from (presumably a
                              {@link java.util.Map}).
   * @param __baseUri_        Base URI to generate child document IDs against.
   * @param __loadingOptions  Context for loading URIs and populating objects.
   * @param __docRoot_        ID at this position in the document (if available) (maybe?)
   * @throws ValidationException If the document fragment is not a {@link java.util.Map}
   *                             or validation of fields fails.
   */
  public WorkflowStepImpl(
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
      throw new ValidationException("WorkflowStepImpl called on non-map");
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
    java.util.List<Object> in;
    try {
      in =
          LoaderInstances
              .idmap_in_array_of_WorkflowStepInput
              .loadField(__doc.get("in"), __baseUri, __loadingOptions);
    } catch (ValidationException e) {
      in = null; // won't be used but prevents compiler from complaining.
      final String __message = "the `in` field is not valid because:";
      __errors.add(new ValidationException(__message, e));
    }
    java.util.List<Object> out;
    try {
      out =
          LoaderInstances
              .uri_array_of_union_of_StringInstance_or_WorkflowStepOutput_True_False_None_None
              .loadField(__doc.get("out"), __baseUri, __loadingOptions);
    } catch (ValidationException e) {
      out = null; // won't be used but prevents compiler from complaining.
      final String __message = "the `out` field is not valid because:";
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
                .idmap_hints_optional_array_of_AnyInstance
                .loadField(__doc.get("hints"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        hints = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `hints` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      hints = null;
    }
    Object run;
    try {
      run =
          LoaderInstances
              .uri_union_of_StringInstance_or_CommandLineTool_or_ExpressionTool_or_Workflow_or_Operation_False_False_None_None
              .loadField(__doc.get("run"), __baseUri, __loadingOptions);
    } catch (ValidationException e) {
      run = null; // won't be used but prevents compiler from complaining.
      final String __message = "the `run` field is not valid because:";
      __errors.add(new ValidationException(__message, e));
    }
    java.util.Optional<String> when;

    if (__doc.containsKey("when")) {
      try {
        when =
            LoaderInstances
                .optional_ExpressionLoader
                .loadField(__doc.get("when"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        when = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `when` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      when = null;
    }
    Object scatter;

    if (__doc.containsKey("scatter")) {
      try {
        scatter =
            LoaderInstances
                .uri_union_of_NullInstance_or_StringInstance_or_array_of_StringInstance_False_False_0_None
                .loadField(__doc.get("scatter"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        scatter = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `scatter` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      scatter = null;
    }
    java.util.Optional<ScatterMethod> scatterMethod;

    if (__doc.containsKey("scatterMethod")) {
      try {
        scatterMethod =
            LoaderInstances
                .uri_optional_ScatterMethod_False_True_None_None
                .loadField(__doc.get("scatterMethod"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        scatterMethod = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `scatterMethod` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      scatterMethod = null;
    }
    if (!__errors.isEmpty()) {
      throw new ValidationException("Trying 'RecordField'", __errors);
    }
    this.id = (java.util.Optional<String>) id;
    this.label = (java.util.Optional<String>) label;
    this.doc = (Object) doc;
    this.in = (java.util.List<Object>) in;
    this.out = (java.util.List<Object>) out;
    this.requirements = (java.util.Optional<java.util.List<Object>>) requirements;
    this.hints = (java.util.Optional<java.util.List<Object>>) hints;
    this.run = (Object) run;
    this.when = (java.util.Optional<String>) when;
    this.scatter = (Object) scatter;
    this.scatterMethod = (java.util.Optional<ScatterMethod>) scatterMethod;
  }
}
