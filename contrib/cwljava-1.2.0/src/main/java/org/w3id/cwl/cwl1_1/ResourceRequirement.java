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
* Auto-generated interface for <I>https://w3id.org/cwl/cwl#ResourceRequirement</I><BR>This interface is implemented by {@link ResourceRequirementImpl}<BR> <BLOCKQUOTE>
 Specify basic hardware resource requirements.
 
 "min" is the minimum amount of a resource that must be reserved to schedule
 a job. If "min" cannot be satisfied, the job should not be run.
 
 "max" is the maximum amount of a resource that the job shall be permitted
 to use. If a node has sufficient resources, multiple jobs may be scheduled
 on a single node provided each job's "max" resource requirements are
 met. If a job attempts to exceed its "max" resource allocation, an
 implementation may deny additional resources, which may result in job
 failure.
 
 If "min" is specified but "max" is not, then "max" == "min"
 If "max" is specified by "min" is not, then "min" == "max".
 
 It is an error if max < min.
 
 It is an error if the value of any of these fields is negative.
 
 If neither "min" nor "max" is specified for a resource, use the default values below.
  </BLOCKQUOTE>
 */
public interface ResourceRequirement extends ProcessRequirement, Saveable {
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#ResourceRequirement/class</I><BR>
   * <BLOCKQUOTE>
   * Always 'ResourceRequirement'   * </BLOCKQUOTE>
   */

  ResourceRequirement_class getClass_();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#ResourceRequirement/coresMin</I><BR>
   * <BLOCKQUOTE>
   * Minimum reserved number of CPU cores (default is 1)   * </BLOCKQUOTE>
   */

  Object getCoresMin();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#ResourceRequirement/coresMax</I><BR>
   * <BLOCKQUOTE>
   * Maximum reserved number of CPU cores   * </BLOCKQUOTE>
   */

  Object getCoresMax();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#ResourceRequirement/ramMin</I><BR>
   * <BLOCKQUOTE>
   * Minimum reserved RAM in mebibytes (2**20) (default is 256)   * </BLOCKQUOTE>
   */

  Object getRamMin();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#ResourceRequirement/ramMax</I><BR>
   * <BLOCKQUOTE>
   * Maximum reserved RAM in mebibytes (2**20)   * </BLOCKQUOTE>
   */

  Object getRamMax();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#ResourceRequirement/tmpdirMin</I><BR>
   * <BLOCKQUOTE>
   * Minimum reserved filesystem based storage for the designated temporary directory, in mebibytes (2**20) (default is 1024)   * </BLOCKQUOTE>
   */

  Object getTmpdirMin();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#ResourceRequirement/tmpdirMax</I><BR>
   * <BLOCKQUOTE>
   * Maximum reserved filesystem based storage for the designated temporary directory, in mebibytes (2**20)   * </BLOCKQUOTE>
   */

  Object getTmpdirMax();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#ResourceRequirement/outdirMin</I><BR>
   * <BLOCKQUOTE>
   * Minimum reserved filesystem based storage for the designated output directory, in mebibytes (2**20) (default is 1024)   * </BLOCKQUOTE>
   */

  Object getOutdirMin();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#ResourceRequirement/outdirMax</I><BR>
   * <BLOCKQUOTE>
   * Maximum reserved filesystem based storage for the designated output directory, in mebibytes (2**20)   * </BLOCKQUOTE>
   */

  Object getOutdirMax();
}
