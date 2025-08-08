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
* Auto-generated interface for <I>https://w3id.org/cwl/cwl#ToolTimeLimit</I><BR>This interface is implemented by {@link ToolTimeLimitImpl}<BR> <BLOCKQUOTE>
 Set an upper limit on the execution time of a CommandLineTool.
 A CommandLineTool whose execution duration exceeds the time
 limit may be preemptively terminated and considered failed.
 May also be used by batch systems to make scheduling decisions.
 The execution duration excludes external operations, such as
 staging of files, pulling a docker image etc, and only counts
 wall-time for the execution of the command line itself.
  </BLOCKQUOTE>
 */
public interface ToolTimeLimit extends ProcessRequirement, Saveable {
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#ToolTimeLimit/class</I><BR>
   * <BLOCKQUOTE>
   * Always 'ToolTimeLimit'   * </BLOCKQUOTE>
   */

  ToolTimeLimit_class getClass_();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#ToolTimeLimit/timelimit</I><BR>
   * <BLOCKQUOTE>
   * The time limit, in seconds.  A time limit of zero means no
   * time limit.  Negative time limits are an error.
   *    * </BLOCKQUOTE>
   */

  Object getTimelimit();
}
