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
* Auto-generated interface for <I>https://w3id.org/cwl/cwl#Dirent</I><BR>This interface is implemented by {@link DirentImpl}<BR> <BLOCKQUOTE>
 Define a file or subdirectory that must be placed in the designated output
 directory prior to executing the command line tool.  May be the result of
 executing an expression, such as building a configuration file from a
 template.
  </BLOCKQUOTE>
 */
public interface Dirent extends Saveable {
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#entryname</I><BR>
   * <BLOCKQUOTE>
   * The name of the file or subdirectory to create in the output directory.
   * If `entry` is a File or Directory, the `entryname` field overrides the value
   * of `basename` of the File or Directory object.  Optional.
   *    * </BLOCKQUOTE>
   */

  Object getEntryname();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#entry</I><BR>
   * <BLOCKQUOTE>
   * If the value is a string literal or an expression which evaluates to a
   * string, a new file must be created with the string as the file contents.
   * 
   * If the value is an expression that evaluates to a `File` object, this
   * indicates the referenced file should be added to the designated output
   * directory prior to executing the tool.
   * 
   * If the value is an expression that evaluates to a `Dirent` object, this
   * indicates that the File or Directory in `entry` should be added to the
   * designated output directory with the name in `entryname`.
   * 
   * If `writable` is false, the file may be made available using a bind
   * mount or file system link to avoid unnecessary copying of the input
   * file.
   *    * </BLOCKQUOTE>
   */

  Object getEntry();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#Dirent/writable</I><BR>
   * <BLOCKQUOTE>
   * If true, the file or directory must be writable by the tool.  Changes
   * to the file or directory must be isolated and not visible by any other
   * CommandLineTool process.  This may be implemented by making a copy of
   * the original file or directory.  Default false (files and directories
   * read-only by default).
   * 
   * A directory marked as `writable: true` implies that all files and
   * subdirectories are recursively writable as well.
   *    * </BLOCKQUOTE>
   */

  java.util.Optional<Boolean> getWritable();
}
