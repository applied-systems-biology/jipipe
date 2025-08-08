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
* Auto-generated interface for <I>https://w3id.org/cwl/cwl#EnvironmentDef</I><BR>This interface is implemented by {@link EnvironmentDefImpl}<BR> <BLOCKQUOTE>
 Define an environment variable that will be set in the runtime environment
 by the workflow platform when executing the command line tool.  May be the
 result of executing an expression, such as getting a parameter from input.
  </BLOCKQUOTE>
 */
public interface EnvironmentDef extends Saveable {
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#EnvironmentDef/envName</I><BR>
   * <BLOCKQUOTE>
   * The environment variable name   * </BLOCKQUOTE>
   */

  String getEnvName();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#EnvironmentDef/envValue</I><BR>
   * <BLOCKQUOTE>
   * The environment variable value   * </BLOCKQUOTE>
   */

  Object getEnvValue();
}
