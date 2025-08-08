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
* Auto-generated interface for <I>https://w3id.org/cwl/cwl#InitialWorkDirRequirement</I><BR>This interface is implemented by {@link InitialWorkDirRequirementImpl}<BR> <BLOCKQUOTE>
 Define a list of files and subdirectories that must be created by the workflow platform in the designated output directory prior to executing the command line tool. </BLOCKQUOTE>
 */
public interface InitialWorkDirRequirement extends ProcessRequirement, Saveable {
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#InitialWorkDirRequirement/class</I><BR>
   * <BLOCKQUOTE>
   * InitialWorkDirRequirement   * </BLOCKQUOTE>
   */

  InitialWorkDirRequirement_class getClass_();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#listing</I><BR>
   * <BLOCKQUOTE>
   * The list of files or subdirectories that must be placed in the
   * designated output directory prior to executing the command line tool.
   * 
   * May be an expression. If so, the expression return value must validate as
   * `{type: array, items: ["null", File, File[], Directory, Directory[], Dirent]}`.
   * 
   * Files or Directories which are listed in the input parameters and
   * appear in the `InitialWorkDirRequirement` listing must have their
   * `path` set to their staged location in the designated output directory.
   * If the same File or Directory appears more than once in the
   * `InitialWorkDirRequirement` listing, the implementation must choose
   * exactly one value for `path`; how this value is chosen is undefined.
   *    * </BLOCKQUOTE>
   */

  Object getListing();
}
