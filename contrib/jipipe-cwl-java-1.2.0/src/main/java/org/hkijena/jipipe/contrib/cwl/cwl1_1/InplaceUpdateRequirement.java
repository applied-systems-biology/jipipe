/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.contrib.cwl.cwl1_1;

import org.hkijena.jipipe.contrib.cwl.cwl1_1.utils.Saveable;

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
 as output.  Downstream steps which futher process the file must
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
