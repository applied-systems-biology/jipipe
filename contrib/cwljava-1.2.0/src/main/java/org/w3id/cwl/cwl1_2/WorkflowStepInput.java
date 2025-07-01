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

import org.w3id.cwl.cwl1_2.utils.Saveable;

/**
* Auto-generated interface for <I>https://w3id.org/cwl/cwl#WorkflowStepInput</I><BR>This interface is implemented by {@link WorkflowStepInputImpl}<BR> <BLOCKQUOTE>
 The input of a workflow step connects an upstream parameter (from the
 workflow inputs, or the outputs of other workflows steps) with the input
 parameters of the process specified by the `run` field. Only input parameters
 declared by the target process will be passed through at runtime to the process
 though additional parameters may be specified (for use within `valueFrom`
 expressions for instance) - unconnected or unused parameters do not represent an
 error condition.
 
 # Input object
 
 A WorkflowStepInput object must contain an `id` field in the form
 `#fieldname` or `#prefix/fieldname`.  When the `id` field contains a slash
 `/` the field name consists of the characters following the final slash
 (the prefix portion may contain one or more slashes to indicate scope).
 This defines a field of the workflow step input object with the value of
 the `source` parameter(s).
 
 # Merging multiple inbound data links
 
 To merge multiple inbound data links,
 [MultipleInputFeatureRequirement](#MultipleInputFeatureRequirement) must be specified
 in the workflow or workflow step requirements.
 
 If the sink parameter is an array, or named in a [workflow
 scatter](#WorkflowStep) operation, there may be multiple inbound
 data links listed in the `source` field.  The values from the
 input links are merged depending on the method specified in the
 `linkMerge` field.  If both `linkMerge` and `pickValue` are null
 or not specified, and there is more than one element in the
 `source` array, the default method is "merge_nested".
 
 If both `linkMerge` and `pickValue` are null or not specified, and
 there is only a single element in the `source`, then the input
 parameter takes the scalar value from the single input link (it is
 *not* wrapped in a single-list).
 
 * **merge_nested**
 
   The input must be an array consisting of exactly one entry for each
   input link.  If "merge_nested" is specified with a single link, the value
   from the link must be wrapped in a single-item list.
 
 * **merge_flattened**
 
   1. The source and sink parameters must be compatible types, or the source
      type must be compatible with single element from the "items" type of
      the destination array parameter.
   2. Source parameters which are arrays are concatenated.
      Source parameters which are single element types are appended as
      single elements.
 
 # Picking non-null values among inbound data links
 
 If present, `pickValue` specifies how to pick non-null values among inbound data links.
 
 `pickValue` is evaluated
   1. Once all source values from upstream step or parameters are available.
   2. After `linkMerge`.
   3. Before `scatter` or `valueFrom`.
 
 This is specifically intended to be useful in combination with
 [conditional execution](#WorkflowStep), where several upstream
 steps may be connected to a single input (`source` is a list), and
 skipped steps produce null values.
 
 Static type checkers should check for type consistency after inferring what the type
 will be after `pickValue` is applied, just as they do currently for `linkMerge`.
 
 * **first_non_null**
 
   For the first level of a list input, pick the first non-null element.  The result is a scalar.
   It is an error if there is no non-null element.  Examples:
   * `[null, x, null, y] -> x`
   * `[null, [null], null, y] -> [null]`
   * `[null, null, null] -> Runtime Error`
 
   *Intended use case*: If-else pattern where the
   value comes either from a conditional step or from a default or
   fallback value. The conditional step(s) should be placed first in
   the list.
 
 * **the_only_non_null**
 
   For the first level of a list input, pick the single non-null element.  The result is a scalar.
   It is an error if there is more than one non-null element.  Examples:
 
   * `[null, x, null] -> x`
   * `[null, x, null, y] -> Runtime Error`
   * `[null, [null], null] -> [null]`
   * `[null, null, null] -> Runtime Error`
 
   *Intended use case*: Switch type patterns where developer considers
   more than one active code path as a workflow error
   (possibly indicating an error in writing `when` condition expressions).
 
 * **all_non_null**
 
   For the first level of a list input, pick all non-null values.
   The result is a list, which may be empty.  Examples:
 
   * `[null, x, null] -> [x]`
   * `[x, null, y] -> [x, y]`
   * `[null, [x], [null]] -> [[x], [null]]`
   * `[null, null, null] -> []`
 
   *Intended use case*: It is valid to have more than one source, but
    sources are conditional, so null sources (from skipped steps)
    should be filtered out.
  </BLOCKQUOTE>
 */
public interface WorkflowStepInput extends Identified, Sink, LoadContents, Labeled, Saveable {
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#Identified/id</I><BR>
   * <BLOCKQUOTE>
   * The unique identifier for this object.   * </BLOCKQUOTE>
   */

  java.util.Optional<String> getId();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#source</I><BR>
   * <BLOCKQUOTE>
   * Specifies one or more workflow parameters that will provide input to
   * the underlying step parameter.
   *    * </BLOCKQUOTE>
   */

  Object getSource();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#Sink/linkMerge</I><BR>
   * <BLOCKQUOTE>
   * The method to use to merge multiple inbound links into a single array.
   * If not specified, the default method is "merge_nested".
   *    * </BLOCKQUOTE>
   */

  java.util.Optional<LinkMergeMethod> getLinkMerge();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#Sink/pickValue</I><BR>
   * <BLOCKQUOTE>
   * The method to use to choose non-null elements among multiple sources.
   *    * </BLOCKQUOTE>
   */

  java.util.Optional<PickValueMethod> getPickValue();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#LoadContents/loadContents</I><BR>
   * <BLOCKQUOTE>
   * Only valid when `type: File` or is an array of `items: File`.
   * 
   * If true, the file (or each file in the array) must be a UTF-8
   * text file 64 KiB or smaller, and the implementation must read
   * the entire contents of the file (or file array) and place it
   * in the `contents` field of the File object for use by
   * expressions.  If the size of the file is greater than 64 KiB,
   * the implementation must raise a fatal error.
   *    * </BLOCKQUOTE>
   */

  java.util.Optional<Boolean> getLoadContents();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#LoadContents/loadListing</I><BR>
   * <BLOCKQUOTE>
   * Only valid when `type: Directory` or is an array of `items: Directory`.
   * 
   * Specify the desired behavior for loading the `listing` field of
   * a Directory object for use by expressions.
   * 
   * The order of precedence for loadListing is:
   * 
   *   1. `loadListing` on an individual parameter
   *   2. Inherited from `LoadListingRequirement`
   *   3. By default: `no_listing`
   *    * </BLOCKQUOTE>
   */

  java.util.Optional<LoadListingEnum> getLoadListing();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#Labeled/label</I><BR>
   * <BLOCKQUOTE>
   * A short, human-readable label of this object.   * </BLOCKQUOTE>
   */

  java.util.Optional<String> getLabel();
  /**
   * Getter for property <I>https://w3id.org/cwl/salad#default</I><BR>
   * <BLOCKQUOTE>
   * The default value for this parameter to use if either there is no
   * `source` field, or the value produced by the `source` is `null`.  The
   * default must be applied prior to scattering or evaluating `valueFrom`.
   *    * </BLOCKQUOTE>
   */

  java.util.Optional<Object> getDefault();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#WorkflowStepInput/valueFrom</I><BR>
   * <BLOCKQUOTE>
   * To use valueFrom, [StepInputExpressionRequirement](#StepInputExpressionRequirement) must
   * be specified in the workflow or workflow step requirements.
   * 
   * If `valueFrom` is a constant string value, use this as the value for
   * this input parameter.
   * 
   * If `valueFrom` is a parameter reference or expression, it must be
   * evaluated to yield the actual value to be assigned to the input field.
   * 
   * The `self` value in the parameter reference or expression must be
   * 1. `null` if there is no `source` field
   * 2. the value of the parameter(s) specified in the `source` field when this
   * workflow input parameter **is not** specified in this workflow step's `scatter` field.
   * 3. an element of the parameter specified in the `source` field when this workflow input
   * parameter **is** specified in this workflow step's `scatter` field.
   * 
   * The value of `inputs` in the parameter reference or expression must be
   * the input object to the workflow step after assigning the `source`
   * values, applying `default`, and then scattering.  The order of
   * evaluating `valueFrom` among step input parameters is undefined and the
   * result of evaluating `valueFrom` on a parameter must not be visible to
   * evaluation of `valueFrom` on other parameters.
   *    * </BLOCKQUOTE>
   */

  Object getValueFrom();
}
