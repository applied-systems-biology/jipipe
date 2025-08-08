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

package org.hkijena.jipipe.contrib.cwl.cwl1_2;

import org.hkijena.jipipe.contrib.cwl.cwl1_2.utils.Saveable;

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
