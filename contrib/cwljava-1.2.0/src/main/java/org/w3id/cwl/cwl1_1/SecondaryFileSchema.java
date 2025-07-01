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
* Auto-generated interface for <I>https://w3id.org/cwl/cwl#SecondaryFileSchema</I><BR>This interface is implemented by {@link SecondaryFileSchemaImpl}<BR>
 */
public interface SecondaryFileSchema extends Saveable {
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

  Object getPattern();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#SecondaryFileSchema/required</I><BR>
   * <BLOCKQUOTE>
   * An implementation must not fail workflow execution if `required` is
   * set to `false` and the expected secondary file does not exist.
   * Default value for `required` field is `true` for secondary files on
   * input and `false` for secondary files on output.
   *    * </BLOCKQUOTE>
   */

  Object getRequired();
}
