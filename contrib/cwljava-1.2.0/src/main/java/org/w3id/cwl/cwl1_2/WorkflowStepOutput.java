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
* Auto-generated interface for <I>https://w3id.org/cwl/cwl#WorkflowStepOutput</I><BR>This interface is implemented by {@link WorkflowStepOutputImpl}<BR> <BLOCKQUOTE>
 Associate an output parameter of the underlying process with a workflow
 parameter.  The workflow parameter (given in the `id` field) be may be used
 as a `source` to connect with input parameters of other workflow steps, or
 with an output parameter of the process.
 
 A unique identifier for this workflow output parameter.  This is
 the identifier to use in the `source` field of `WorkflowStepInput`
 to connect the output value to downstream parameters.
  </BLOCKQUOTE>
 */
public interface WorkflowStepOutput extends Identified, Saveable {
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#Identified/id</I><BR>
   * <BLOCKQUOTE>
   * The unique identifier for this object.   * </BLOCKQUOTE>
   */

  java.util.Optional<String> getId();
}
