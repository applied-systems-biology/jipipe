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

package org.w3id.cwl.cwl1_1;

import org.w3id.cwl.cwl1_1.utils.LoaderInstances;
import org.w3id.cwl.cwl1_1.utils.LoadingOptions;
import org.w3id.cwl.cwl1_1.utils.LoadingOptionsBuilder;
import org.w3id.cwl.cwl1_1.utils.SaveableImpl;
import org.w3id.cwl.cwl1_1.utils.ValidationException;

/**
* Auto-generated class implementation for <I>https://w3id.org/cwl/cwl#Dirent</I><BR> <BLOCKQUOTE>
 Define a file or subdirectory that must be placed in the designated output
 directory prior to executing the command line tool.  May be the result of
 executing an expression, such as building a configuration file from a
 template.
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
   * The name of the file or subdirectory to create in the output directory.
   * If `entry` is a File or Directory, the `entryname` field overrides the value
   * of `basename` of the File or Directory object.  Optional.
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

  public Object getEntry() {
    return this.entry;
  }

  private java.util.Optional<Boolean> writable;

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

  public java.util.Optional<Boolean> getWritable() {
    return this.writable;
  }

  /**
   * Used by {@link org.w3id.cwl.cwl1_1.utils.RootLoader} to construct instances of DirentImpl.
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
