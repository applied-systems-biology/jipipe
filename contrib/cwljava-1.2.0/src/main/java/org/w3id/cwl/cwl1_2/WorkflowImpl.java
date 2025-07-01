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
* Auto-generated class implementation for <I>https://w3id.org/cwl/cwl#Workflow</I><BR> <BLOCKQUOTE>
 A workflow describes a set of **steps** and the **dependencies** between
 those steps.  When a step produces output that will be consumed by a
 second step, the first step is a dependency of the second step.
 
 When there is a dependency, the workflow engine must execute the preceding
 step and wait for it to successfully produce output before executing the
 dependent step.  If two steps are defined in the workflow graph that
 are not directly or indirectly dependent, these steps are **independent**,
 and may execute in any order or execute concurrently.  A workflow is
 complete when all steps have been executed.
 
 Dependencies between parameters are expressed using the `source`
 field on [workflow step input parameters](#WorkflowStepInput) and
 `outputSource` field on [workflow output
 parameters](#WorkflowOutputParameter).
 
 The `source` field on each workflow step input parameter expresses
 the data links that contribute to the value of the step input
 parameter (the "sink").  A workflow step can only begin execution
 when every data link connected to a step has been fulfilled.
 
 The `outputSource` field on each workflow step input parameter
 expresses the data links that contribute to the value of the
 workflow output parameter (the "sink").  Workflow execution cannot
 complete successfully until every data link connected to an output
 parameter has been fulfilled.
 
 ## Workflow success and failure
 
 A completed step must result in one of `success`, `temporaryFailure` or
 `permanentFailure` states.  An implementation may choose to retry a step
 execution which resulted in `temporaryFailure`.  An implementation may
 choose to either continue running other steps of a workflow, or terminate
 immediately upon `permanentFailure`.
 
 * If any step of a workflow execution results in `permanentFailure`, then
 the workflow status is `permanentFailure`.
 
 * If one or more steps result in `temporaryFailure` and all other steps
 complete `success` or are not executed, then the workflow status is
 `temporaryFailure`.
 
 * If all workflow steps are executed and complete with `success`, then the
 workflow status is `success`.
 
 # Extensions
 
 [ScatterFeatureRequirement](#ScatterFeatureRequirement) and
 [SubworkflowFeatureRequirement](#SubworkflowFeatureRequirement) are
 available as standard [extensions](#Extensions_and_Metadata) to core
 workflow semantics.
  </BLOCKQUOTE>
 */
public class WorkflowImpl extends SaveableImpl implements Workflow {
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

  private Workflow_class class_;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#Workflow/class</I><BR>

   */

  public Workflow_class getClass_() {
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

  private java.util.List<Object> steps;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#Workflow/steps</I><BR>
   * <BLOCKQUOTE>
   * The individual steps that make up the workflow.  Each step is executed when all of its
   * input data links are fulfilled.  An implementation may choose to execute
   * the steps in a different order than listed and/or execute steps
   * concurrently, provided that dependencies between steps are met.
   *    * </BLOCKQUOTE>
   */

  public java.util.List<Object> getSteps() {
    return this.steps;
  }

  /**
   * Used by {@link org.w3id.cwl.cwl1_2.utils.RootLoader} to construct instances of WorkflowImpl.
   *
   * @param __doc_            Document fragment to load this record object from (presumably a
                              {@link java.util.Map}).
   * @param __baseUri_        Base URI to generate child document IDs against.
   * @param __loadingOptions  Context for loading URIs and populating objects.
   * @param __docRoot_        ID at this position in the document (if available) (maybe?)
   * @throws ValidationException If the document fragment is not a {@link java.util.Map}
   *                             or validation of fields fails.
   */
  public WorkflowImpl(
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
      throw new ValidationException("WorkflowImpl called on non-map");
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
    Workflow_class class_;
    try {
      class_ =
          LoaderInstances
              .uri_Workflow_class_False_True_None_None
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
              .idmap_inputs_array_of_WorkflowInputParameter
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
              .idmap_outputs_array_of_WorkflowOutputParameter
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
    java.util.List<Object> steps;
    try {
      steps =
          LoaderInstances
              .idmap_steps_array_of_WorkflowStep
              .loadField(__doc.get("steps"), __baseUri, __loadingOptions);
    } catch (ValidationException e) {
      steps = null; // won't be used but prevents compiler from complaining.
      final String __message = "the `steps` field is not valid because:";
      __errors.add(new ValidationException(__message, e));
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
    this.class_ = (Workflow_class) class_;
    this.steps = (java.util.List<Object>) steps;
  }
}
