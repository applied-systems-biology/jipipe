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
* Auto-generated interface for <I>https://w3id.org/cwl/cwl#InitialWorkDirRequirement</I><BR>This interface is implemented by {@link InitialWorkDirRequirementImpl}<BR> <BLOCKQUOTE>
 Define a list of files and subdirectories that must be created by the workflow platform in the designated output directory prior to executing the command line tool. </BLOCKQUOTE>
 */
public interface InitialWorkDirRequirement extends ProcessRequirement, Saveable {
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#InitialWorkDirRequirement/class</I><BR>
   * <BLOCKQUOTE>
   * InitialWorkDirRequirement   * </BLOCKQUOTE>
   */

  InitialWorkDirRequirement_class getClass_();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#listing</I><BR>
   * <BLOCKQUOTE>
   * The list of files or subdirectories that must be placed in the
   * designated output directory prior to executing the command line tool.
   * 
   * May be an expression. If so, the expression return value must validate as
   * `{type: array, items: ["null", File, File[], Directory, Directory[], Dirent]}`.
   * 
   * Files or Directories which are listed in the input parameters and
   * appear in the `InitialWorkDirRequirement` listing must have their
   * `path` set to their staged location in the designated output directory.
   * If the same File or Directory appears more than once in the
   * `InitialWorkDirRequirement` listing, the implementation must choose
   * exactly one value for `path`; how this value is chosen is undefined.
   *    * </BLOCKQUOTE>
   */

  Object getListing();
}
