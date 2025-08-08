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
