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
* Auto-generated interface for <I>https://w3id.org/cwl/cwl#MultipleInputFeatureRequirement</I><BR>This interface is implemented by {@link MultipleInputFeatureRequirementImpl}<BR> <BLOCKQUOTE>
 Indicates that the workflow platform must support multiple inbound data links
 listed in the `source` field of [WorkflowStepInput](#WorkflowStepInput).
  </BLOCKQUOTE>
 */
public interface MultipleInputFeatureRequirement extends ProcessRequirement, Saveable {
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#MultipleInputFeatureRequirement/class</I><BR>
   * <BLOCKQUOTE>
   * Always 'MultipleInputFeatureRequirement'   * </BLOCKQUOTE>
   */

  MultipleInputFeatureRequirement_class getClass_();
}
