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
* Auto-generated interface for <I>https://w3id.org/cwl/cwl#ScatterFeatureRequirement</I><BR>This interface is implemented by {@link ScatterFeatureRequirementImpl}<BR> <BLOCKQUOTE>
 Indicates that the workflow platform must support the `scatter` and
 `scatterMethod` fields of [WorkflowStep](#WorkflowStep).
  </BLOCKQUOTE>
 */
public interface ScatterFeatureRequirement extends ProcessRequirement, Saveable {
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#ScatterFeatureRequirement/class</I><BR>
   * <BLOCKQUOTE>
   * Always 'ScatterFeatureRequirement'   * </BLOCKQUOTE>
   */

  ScatterFeatureRequirement_class getClass_();
}
