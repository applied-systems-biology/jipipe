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

import org.w3id.cwl.cwl1_1.utils.Saveable;

/**
* Auto-generated interface for <I>https://w3id.org/cwl/cwl#Workflow</I><BR>This interface is implemented by {@link WorkflowImpl}<BR> <BLOCKQUOTE>
 A workflow describes a set of **steps** and the **dependencies** between
 those steps.  When a step produces output that will be consumed by a
 second step, the first step is a dependency of the second step.
 
 When there is a dependency, the workflow engine must execute the preceding
 step and wait for it to successfully produce output before executing the
 dependent step.  If two steps are defined in the workflow graph that
 are not directly or indirectly dependent, these steps are **independent**,
 and may execute in any order or execute concurrently.  A workflow is
 complete when all steps have been executed.
 
 Dependencies between parameters are expressed using the `source` field on
 [workflow step input parameters](#WorkflowStepInput) and [workflow output
 parameters](#WorkflowOutputParameter).
 
 The `source` field expresses the dependency of one parameter on another
 such that when a value is associated with the parameter specified by
 `source`, that value is propagated to the destination parameter.  When all
 data links inbound to a given step are fufilled, the step is ready to
 execute.
 
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
public interface Workflow extends Process, Saveable {
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#Identified/id</I><BR>
   * <BLOCKQUOTE>
   * The unique identifier for this object.   * </BLOCKQUOTE>
   */

  java.util.Optional<String> getId();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#Workflow/class</I><BR>

   */

  Workflow_class getClass_();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#Labeled/label</I><BR>
   * <BLOCKQUOTE>
   * A short, human-readable label of this object.   * </BLOCKQUOTE>
   */

  java.util.Optional<String> getLabel();
  /**
   * Getter for property <I>https://w3id.org/cwl/salad#Documented/doc</I><BR>
   * <BLOCKQUOTE>
   * A documentation string for this object, or an array of strings which should be concatenated.   * </BLOCKQUOTE>
   */

  Object getDoc();
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

  java.util.List<Object> getInputs();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#outputs</I><BR>
   * <BLOCKQUOTE>
   * Defines the parameters representing the output of the process.  May be
   * used to generate and/or validate the output object.
   *    * </BLOCKQUOTE>
   */

  java.util.List<Object> getOutputs();
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

  java.util.Optional<java.util.List<Object>> getRequirements();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#hints</I><BR>
   * <BLOCKQUOTE>
   * Declares hints applying to either the runtime environment or the
   * workflow engine that may be helpful in executing this process.  It is
   * not an error if an implementation cannot satisfy all hints, however
   * the implementation may report a warning.
   *    * </BLOCKQUOTE>
   */

  java.util.Optional<java.util.List<Object>> getHints();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#cwlVersion</I><BR>
   * <BLOCKQUOTE>
   * CWL document version. Always required at the document root. Not
   * required for a Process embedded inside another Process.
   *    * </BLOCKQUOTE>
   */

  java.util.Optional<CWLVersion> getCwlVersion();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#Workflow/steps</I><BR>
   * <BLOCKQUOTE>
   * The individual steps that make up the workflow.  Each step is executed when all of its
   * input data links are fufilled.  An implementation may choose to execute
   * the steps in a different order than listed and/or execute steps
   * concurrently, provided that dependencies between steps are met.
   *    * </BLOCKQUOTE>
   */

  java.util.List<Object> getSteps();
}
