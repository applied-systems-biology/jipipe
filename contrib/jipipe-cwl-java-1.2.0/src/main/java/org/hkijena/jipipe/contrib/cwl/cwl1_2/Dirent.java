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
* Auto-generated interface for <I>https://w3id.org/cwl/cwl#Dirent</I><BR>This interface is implemented by {@link DirentImpl}<BR> <BLOCKQUOTE>
 Define a file or subdirectory that must be staged to a particular
 place prior to executing the command line tool.  May be the result
 of executing an expression, such as building a configuration file
 from a template.
 
 Usually files are staged within the [designated output directory](#Runtime_environment).
 However, under certain circumstances, files may be staged at
 arbitrary locations, see discussion for `entryname`.
  </BLOCKQUOTE>
 */
public interface Dirent extends Saveable {
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#entryname</I><BR>
   * <BLOCKQUOTE>
   * The "target" name of the file or subdirectory.  If `entry` is
   * a File or Directory, the `entryname` field overrides the value
   * of `basename` of the File or Directory object.
   * 
   * * Required when `entry` evaluates to file contents only
   * * Optional when `entry` evaluates to a File or Directory object with a `basename`
   * * Invalid when `entry` evaluates to an array of File or Directory objects.
   * 
   * If `entryname` is a relative path, it specifies a name within
   * the designated output directory.  A relative path starting
   * with `../` or that resolves to location above the designated output directory is an error.
   * 
   * If `entryname` is an absolute path (starts with a slash `/`)
   * it is an error unless the following conditions are met:
   * 
   *   * `DockerRequirement` is present in `requirements`
   *   * The program is will run inside a software container
   *   where, from the perspective of the program, the root
   *   filesystem is not shared with any other user or
   *   running program.
   * 
   * In this case, and the above conditions are met, then
   * `entryname` may specify the absolute path within the container
   * where the file or directory must be placed.
   *    * </BLOCKQUOTE>
   */

  Object getEntryname();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#entry</I><BR>
   * <BLOCKQUOTE>
   * If the value is a string literal or an expression which evaluates to a
   * string, a new text file must be created with the string as the file contents.
   * 
   * If the value is an expression that evaluates to a `File` or
   * `Directory` object, or an array of `File` or `Directory`
   * objects, this indicates the referenced file or directory
   * should be added to the designated output directory prior to
   * executing the tool.
   * 
   * If the value is an expression that evaluates to `null`,
   * nothing is added to the designated output directory, the entry
   * has no effect.
   * 
   * If the value is an expression that evaluates to some other
   * array, number, or object not consisting of `File` or
   * `Directory` objects, a new file must be created with the value
   * serialized to JSON text as the file contents.  The JSON
   * serialization behavior should match the behavior of string
   * interpolation of [Parameter
   * references](#Parameter_references).
   *    * </BLOCKQUOTE>
   */

  Object getEntry();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#Dirent/writable</I><BR>
   * <BLOCKQUOTE>
   * If true, the File or Directory (or array of Files or
   * Directories) declared in `entry` must be writable by the tool.
   * 
   * Changes to the file or directory must be isolated and not
   * visible by any other CommandLineTool process.  This may be
   * implemented by making a copy of the original file or
   * directory.
   * 
   * Disruptive changes to the referenced file or directory must not
   * be allowed unless `InplaceUpdateRequirement.inplaceUpdate` is true.
   * 
   * Default false (files and directories read-only by default).
   * 
   * A directory marked as `writable: true` implies that all files and
   * subdirectories are recursively writable as well.
   * 
   * If `writable` is false, the file may be made available using a
   * bind mount or file system link to avoid unnecessary copying of
   * the input file.  Command line tools may receive an error on
   * attempting to rename or delete files or directories that are
   * not explicitly marked as writable.
   *    * </BLOCKQUOTE>
   */

  java.util.Optional<Boolean> getWritable();
}
