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
* Auto-generated interface for <I>https://w3id.org/cwl/cwl#WorkflowOutputParameter</I><BR>This interface is implemented by {@link WorkflowOutputParameterImpl}<BR> <BLOCKQUOTE>
 Describe an output parameter of a workflow.  The parameter must be
 connected to one or more parameters defined in the workflow that
 will provide the value of the output parameter. It is legal to
 connect a WorkflowInputParameter to a WorkflowOutputParameter.
 
 See [WorkflowStepInput](#WorkflowStepInput) for discussion of
 `linkMerge` and `pickValue`.
  </BLOCKQUOTE>
 */
public interface WorkflowOutputParameter extends OutputParameter, Saveable {
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

  Object getSecondaryFiles();
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

  java.util.Optional<Boolean> getStreamable();
  /**
   * Getter for property <I>https://w3id.org/cwl/salad#Documented/doc</I><BR>
   * <BLOCKQUOTE>
   * A documentation string for this object, or an array of strings which should be concatenated.   * </BLOCKQUOTE>
   */

  Object getDoc();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#format</I><BR>
   * <BLOCKQUOTE>
   * Only valid when `type: File` or is an array of `items: File`.
   * 
   * This is the file format that will be assigned to the output
   * File object.
   *    * </BLOCKQUOTE>
   */

  Object getFormat();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#outputSource</I><BR>
   * <BLOCKQUOTE>
   * Specifies one or more names of an output from a workflow step (in the form
   * `step_name/output_name` with a `/` separator`), or a workflow input name,
   * that supply their value(s) to the output parameter.
   * the output parameter.  It is valid to reference workflow level inputs
   * here.
   *    * </BLOCKQUOTE>
   */

  Object getOutputSource();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#WorkflowOutputParameter/linkMerge</I><BR>
   * <BLOCKQUOTE>
   * The method to use to merge multiple sources into a single array.
   * If not specified, the default method is "merge_nested".
   *    * </BLOCKQUOTE>
   */

  java.util.Optional<LinkMergeMethod> getLinkMerge();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#WorkflowOutputParameter/pickValue</I><BR>
   * <BLOCKQUOTE>
   * The method to use to choose non-null elements among multiple sources.
   *    * </BLOCKQUOTE>
   */

  java.util.Optional<PickValueMethod> getPickValue();
  /**
   * Getter for property <I>https://w3id.org/cwl/salad#type</I><BR>
   * <BLOCKQUOTE>
   * Specify valid types of data that may be assigned to this parameter.
   *    * </BLOCKQUOTE>
   */

  Object getType();
}
