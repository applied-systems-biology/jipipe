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
* Auto-generated interface for <I>https://w3id.org/cwl/cwl#InitialWorkDirRequirement</I><BR>This interface is implemented by {@link InitialWorkDirRequirementImpl}<BR> <BLOCKQUOTE>
 Define a list of files and subdirectories that must be staged by the workflow platform prior to executing the command line tool.
 Normally files are staged within the designated output directory. However, when running inside containers, files may be staged at arbitrary locations, see discussion for [`Dirent.entryname`](#Dirent). Together with `DockerRequirement.dockerOutputDirectory` it is possible to control the locations of both input and output files when running in containers. </BLOCKQUOTE>
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
   * The list of files or subdirectories that must be staged prior
   * to executing the command line tool.
   * 
   * Return type of each expression must validate as `["null",
   * File, Directory, Dirent, {type: array, items: [File,
   * Directory]}]`.
   * 
   * Each `File` or `Directory` that is returned by an Expression
   * must be added to the designated output directory prior to
   * executing the tool.
   * 
   * Each `Dirent` record that is listed or returned by an
   * expression specifies a file to be created or staged in the
   * designated output directory prior to executing the tool.
   * 
   * Expressions may return null, in which case they have no effect.
   * 
   * Files or Directories which are listed in the input parameters
   * and appear in the `InitialWorkDirRequirement` listing must
   * have their `path` set to their staged location.  If the same
   * File or Directory appears more than once in the
   * `InitialWorkDirRequirement` listing, the implementation must
   * choose exactly one value for `path`; how this value is chosen
   * is undefined.
   *    * </BLOCKQUOTE>
   */

  Object getListing();
}
