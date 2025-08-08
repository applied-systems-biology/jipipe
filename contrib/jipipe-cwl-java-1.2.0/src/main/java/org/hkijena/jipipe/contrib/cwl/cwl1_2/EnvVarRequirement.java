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
* Auto-generated interface for <I>https://w3id.org/cwl/cwl#EnvVarRequirement</I><BR>This interface is implemented by {@link EnvVarRequirementImpl}<BR> <BLOCKQUOTE>
 Define a list of environment variables which will be set in the
 execution environment of the tool.  See `EnvironmentDef` for details.
  </BLOCKQUOTE>
 */
public interface EnvVarRequirement extends ProcessRequirement, Saveable {
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#EnvVarRequirement/class</I><BR>
   * <BLOCKQUOTE>
   * Always 'EnvVarRequirement'   * </BLOCKQUOTE>
   */

  EnvVarRequirement_class getClass_();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#EnvVarRequirement/envDef</I><BR>
   * <BLOCKQUOTE>
   * The list of environment variables.   * </BLOCKQUOTE>
   */

  java.util.List<Object> getEnvDef();
}
