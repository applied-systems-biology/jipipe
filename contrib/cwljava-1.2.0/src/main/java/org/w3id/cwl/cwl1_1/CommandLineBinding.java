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
* Auto-generated interface for <I>https://w3id.org/cwl/cwl#CommandLineBinding</I><BR>This interface is implemented by {@link CommandLineBindingImpl}<BR> <BLOCKQUOTE>
 
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
public interface CommandLineBinding extends InputBinding, Saveable {
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

  java.util.Optional<Boolean> getLoadContents();
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

  Object getPosition();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#CommandLineBinding/prefix</I><BR>
   * <BLOCKQUOTE>
   * Command line prefix to add before the value.   * </BLOCKQUOTE>
   */

  java.util.Optional<String> getPrefix();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#CommandLineBinding/separate</I><BR>
   * <BLOCKQUOTE>
   * If true (default), then the prefix and value must be added as separate
   * command line arguments; if false, prefix and value must be concatenated
   * into a single command line argument.
   *    * </BLOCKQUOTE>
   */

  java.util.Optional<Boolean> getSeparate();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#CommandLineBinding/itemSeparator</I><BR>
   * <BLOCKQUOTE>
   * Join the array elements into a single string with the elements
   * separated by by `itemSeparator`.
   *    * </BLOCKQUOTE>
   */

  java.util.Optional<String> getItemSeparator();
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

  Object getValueFrom();
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

  java.util.Optional<Boolean> getShellQuote();
}
