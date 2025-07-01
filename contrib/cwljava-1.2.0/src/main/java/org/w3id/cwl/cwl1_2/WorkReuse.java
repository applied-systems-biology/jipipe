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
* Auto-generated interface for <I>https://w3id.org/cwl/cwl#WorkReuse</I><BR>This interface is implemented by {@link WorkReuseImpl}<BR> <BLOCKQUOTE>
 For implementations that support reusing output from past work (on
 the assumption that same code and same input produce same
 results), control whether to enable or disable the reuse behavior
 for a particular tool or step (to accommodate situations where that
 assumption is incorrect).  A reused step is not executed but
 instead returns the same output as the original execution.
 
 If `WorkReuse` is not specified, correct tools should assume it
 is enabled by default.
  </BLOCKQUOTE>
 */
public interface WorkReuse extends ProcessRequirement, Saveable {
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#WorkReuse/class</I><BR>
   * <BLOCKQUOTE>
   * Always 'WorkReuse'   * </BLOCKQUOTE>
   */

  WorkReuse_class getClass_();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#WorkReuse/enableReuse</I><BR>

   */

  Object getEnableReuse();
}
