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
* Auto-generated interface for <I>https://w3id.org/cwl/cwl#ToolTimeLimit</I><BR>This interface is implemented by {@link ToolTimeLimitImpl}<BR> <BLOCKQUOTE>
 Set an upper limit on the execution time of a CommandLineTool.
 A CommandLineTool whose execution duration exceeds the time
 limit may be preemptively terminated and considered failed.
 May also be used by batch systems to make scheduling decisions.
 The execution duration excludes external operations, such as
 staging of files, pulling a docker image etc, and only counts
 wall-time for the execution of the command line itself.
  </BLOCKQUOTE>
 */
public interface ToolTimeLimit extends ProcessRequirement, Saveable {
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#ToolTimeLimit/class</I><BR>
   * <BLOCKQUOTE>
   * Always 'ToolTimeLimit'   * </BLOCKQUOTE>
   */

  ToolTimeLimit_class getClass_();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#ToolTimeLimit/timelimit</I><BR>
   * <BLOCKQUOTE>
   * The time limit, in seconds.  A time limit of zero means no
   * time limit.  Negative time limits are an error.
   *    * </BLOCKQUOTE>
   */

  Object getTimelimit();
}
