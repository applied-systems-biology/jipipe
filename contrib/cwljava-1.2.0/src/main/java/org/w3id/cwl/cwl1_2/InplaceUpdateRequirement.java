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
* Auto-generated interface for <I>https://w3id.org/cwl/cwl#InplaceUpdateRequirement</I><BR>This interface is implemented by {@link InplaceUpdateRequirementImpl}<BR> <BLOCKQUOTE>
 
 If `inplaceUpdate` is true, then an implementation supporting this
 feature may permit tools to directly update files with `writable:
 true` in InitialWorkDirRequirement.  That is, as an optimization,
 files may be destructively modified in place as opposed to copied
 and updated.
 
 An implementation must ensure that only one workflow step may
 access a writable file at a time.  It is an error if a file which
 is writable by one workflow step file is accessed (for reading or
 writing) by any other workflow step running independently.
 However, a file which has been updated in a previous completed
 step may be used as input to multiple steps, provided it is
 read-only in every step.
 
 Workflow steps which modify a file must produce the modified file
 as output.  Downstream steps which further process the file must
 use the output of previous steps, and not refer to a common input
 (this is necessary for both ordering and correctness).
 
 Workflow authors should provide this in the `hints` section.  The
 intent of this feature is that workflows produce the same results
 whether or not InplaceUpdateRequirement is supported by the
 implementation, and this feature is primarily available as an
 optimization for particular environments.
 
 Users and implementers should be aware that workflows that
 destructively modify inputs may not be repeatable or reproducible.
 In particular, enabling this feature implies that WorkReuse should
 not be enabled.
  </BLOCKQUOTE>
 */
public interface InplaceUpdateRequirement extends ProcessRequirement, Saveable {
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#InplaceUpdateRequirement/class</I><BR>
   * <BLOCKQUOTE>
   * Always 'InplaceUpdateRequirement'   * </BLOCKQUOTE>
   */

  InplaceUpdateRequirement_class getClass_();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#InplaceUpdateRequirement/inplaceUpdate</I><BR>

   */

  Boolean getInplaceUpdate();
}
