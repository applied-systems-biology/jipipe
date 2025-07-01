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
* Auto-generated interface for <I>https://w3id.org/cwl/cwl#WorkflowStep</I><BR>This interface is implemented by {@link WorkflowStepImpl}<BR> <BLOCKQUOTE>
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
 type of each input parameter is implicitly becomes an array of items of the
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
 
 # Subworkflows
 
 To specify a nested workflow as part of a workflow step,
 [SubworkflowFeatureRequirement](#SubworkflowFeatureRequirement) must be
 specified in the workflow or workflow step requirements.
 
 It is a fatal error if a workflow directly or indirectly invokes itself as
 a subworkflow (recursive workflows are not allowed).
  </BLOCKQUOTE>
 */
public interface WorkflowStep extends Identified, Labeled, Documented, Saveable {
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#Identified/id</I><BR>
   * <BLOCKQUOTE>
   * The unique identifier for this object.   * </BLOCKQUOTE>
   */

  java.util.Optional<String> getId();
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
   * Getter for property <I>https://w3id.org/cwl/cwl#in</I><BR>
   * <BLOCKQUOTE>
   * Defines the input parameters of the workflow step.  The process is ready to
   * run when all required input parameters are associated with concrete
   * values.  Input parameters include a schema for each parameter which is
   * used to validate the input object.  It may also be used build a user
   * interface for constructing the input object.
   *    * </BLOCKQUOTE>
   */

  java.util.List<Object> getIn();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#out</I><BR>
   * <BLOCKQUOTE>
   * Defines the parameters representing the output of the process.  May be
   * used to generate and/or validate the output object.
   *    * </BLOCKQUOTE>
   */

  java.util.List<Object> getOut();
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

  java.util.Optional<java.util.List<Object>> getRequirements();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#hints</I><BR>
   * <BLOCKQUOTE>
   * Declares hints applying to either the runtime environment or the
   * workflow engine that may be helpful in executing this workflow step.  It is
   * not an error if an implementation cannot satisfy all hints, however
   * the implementation may report a warning.
   *    * </BLOCKQUOTE>
   */

  java.util.Optional<java.util.List<Object>> getHints();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#run</I><BR>
   * <BLOCKQUOTE>
   * Specifies the process to run.
   *    * </BLOCKQUOTE>
   */

  Object getRun();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#scatter</I><BR>

   */

  Object getScatter();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#scatterMethod</I><BR>
   * <BLOCKQUOTE>
   * Required if `scatter` is an array of more than one element.
   *    * </BLOCKQUOTE>
   */

  java.util.Optional<ScatterMethod> getScatterMethod();
}
