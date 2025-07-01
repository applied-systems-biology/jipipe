// Copyright Common Workflow Language project contributors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.w3id.cwl.cwl1_2;

import org.w3id.cwl.cwl1_2.utils.LoaderInstances;
import org.w3id.cwl.cwl1_2.utils.LoadingOptions;
import org.w3id.cwl.cwl1_2.utils.LoadingOptionsBuilder;
import org.w3id.cwl.cwl1_2.utils.SaveableImpl;
import org.w3id.cwl.cwl1_2.utils.ValidationException;

/**
* Auto-generated class implementation for <I>https://w3id.org/cwl/cwl#Dirent</I><BR> <BLOCKQUOTE>
 Define a file or subdirectory that must be staged to a particular
 place prior to executing the command line tool.  May be the result
 of executing an expression, such as building a configuration file
 from a template.
 
 Usually files are staged within the [designated output directory](#Runtime_environment).
 However, under certain circumstances, files may be staged at
 arbitrary locations, see discussion for `entryname`.
  </BLOCKQUOTE>
 */
public class DirentImpl extends SaveableImpl implements Dirent {
  private LoadingOptions loadingOptions_ = new LoadingOptionsBuilder().build();
  private java.util.Map<String, Object> extensionFields_ =
      new java.util.HashMap<String, Object>();

  private Object entryname;

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

  public Object getEntryname() {
    return this.entryname;
  }

  private Object entry;

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

  public Object getEntry() {
    return this.entry;
  }

  private java.util.Optional<Boolean> writable;

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

  public java.util.Optional<Boolean> getWritable() {
    return this.writable;
  }

  /**
   * Used by {@link org.w3id.cwl.cwl1_2.utils.RootLoader} to construct instances of DirentImpl.
   *
   * @param __doc_            Document fragment to load this record object from (presumably a
                              {@link java.util.Map}).
   * @param __baseUri_        Base URI to generate child document IDs against.
   * @param __loadingOptions  Context for loading URIs and populating objects.
   * @param __docRoot_        ID at this position in the document (if available) (maybe?)
   * @throws ValidationException If the document fragment is not a {@link java.util.Map}
   *                             or validation of fields fails.
   */
  public DirentImpl(
      final Object __doc_,
      final String __baseUri_,
      LoadingOptions __loadingOptions,
      final String __docRoot_) {
    super(__doc_, __baseUri_, __loadingOptions, __docRoot_);
    // Prefix plumbing variables with '__' to reduce likelihood of collision with
    // generated names.
    String __baseUri = __baseUri_;
    String __docRoot = __docRoot_;
    if (!(__doc_ instanceof java.util.Map)) {
      throw new ValidationException("DirentImpl called on non-map");
    }
    final java.util.Map<String, Object> __doc = (java.util.Map<String, Object>) __doc_;
    final java.util.List<ValidationException> __errors =
        new java.util.ArrayList<ValidationException>();
    if (__loadingOptions != null) {
      this.loadingOptions_ = __loadingOptions;
    }
    Object entryname;

    if (__doc.containsKey("entryname")) {
      try {
        entryname =
            LoaderInstances
                .union_of_NullInstance_or_StringInstance_or_ExpressionLoader
                .loadField(__doc.get("entryname"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        entryname = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `entryname` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      entryname = null;
    }
    Object entry;
    try {
      entry =
          LoaderInstances
              .union_of_StringInstance_or_ExpressionLoader
              .loadField(__doc.get("entry"), __baseUri, __loadingOptions);
    } catch (ValidationException e) {
      entry = null; // won't be used but prevents compiler from complaining.
      final String __message = "the `entry` field is not valid because:";
      __errors.add(new ValidationException(__message, e));
    }
    java.util.Optional<Boolean> writable;

    if (__doc.containsKey("writable")) {
      try {
        writable =
            LoaderInstances
                .optional_BooleanInstance
                .loadField(__doc.get("writable"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        writable = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `writable` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      writable = null;
    }
    if (!__errors.isEmpty()) {
      throw new ValidationException("Trying 'RecordField'", __errors);
    }
    this.entryname = (Object) entryname;
    this.entry = (Object) entry;
    this.writable = (java.util.Optional<Boolean>) writable;
  }
}
