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
* Auto-generated interface for <I>https://w3id.org/cwl/cwl#ShellCommandRequirement</I><BR>This interface is implemented by {@link ShellCommandRequirementImpl}<BR> <BLOCKQUOTE>
 Modify the behavior of CommandLineTool to generate a single string
 containing a shell command line.  Each item in the `arguments` list must
 be joined into a string separated by single spaces and quoted to prevent
 interpretation by the shell, unless `CommandLineBinding` for that argument
 contains `shellQuote: false`.  If `shellQuote: false` is specified, the
 argument is joined into the command string without quoting, which allows
 the use of shell metacharacters such as `|` for pipes.
  </BLOCKQUOTE>
 */
public interface ShellCommandRequirement extends ProcessRequirement, Saveable {
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#ShellCommandRequirement/class</I><BR>
   * <BLOCKQUOTE>
   * Always 'ShellCommandRequirement'   * </BLOCKQUOTE>
   */

  ShellCommandRequirement_class getClass_();
}
