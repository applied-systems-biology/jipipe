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
* Auto-generated interface for <I>https://w3id.org/cwl/cwl#ShellCommandRequirement</I><BR>This interface is implemented by {@link ShellCommandRequirementImpl}<BR> <BLOCKQUOTE>
 Modify the behavior of CommandLineTool to generate a single string
 containing a shell command line.  Each item in the `arguments` list must
 be joined into a string separated by single spaces and quoted to prevent
 interpretation by the shell, unless `CommandLineBinding` for that argument
 contains `shellQuote: false`.  If `shellQuote: false` is specified, the
 argument is joined into the command string without quoting, which allows
 the use of shell metacharacters such as `|` for pipes.
  </BLOCKQUOTE>
 */
public interface ShellCommandRequirement extends ProcessRequirement, Saveable {
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#ShellCommandRequirement/class</I><BR>
   * <BLOCKQUOTE>
   * Always 'ShellCommandRequirement'   * </BLOCKQUOTE>
   */

  ShellCommandRequirement_class getClass_();
}
