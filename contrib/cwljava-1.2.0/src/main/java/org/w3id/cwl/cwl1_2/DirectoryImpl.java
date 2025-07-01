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
* Auto-generated class implementation for <I>https://w3id.org/cwl/cwl#Directory</I><BR> <BLOCKQUOTE>
 Represents a directory to present to a command line tool.
 
 Directories are represented as objects with `class` of `Directory`.  Directory objects have
 a number of properties that provide metadata about the directory.
 
 The `location` property of a Directory is a IRI that uniquely identifies
 the directory.  Implementations must support the file:// IRI scheme and may
 support other schemes such as http://.  Alternately to `location`,
 implementations must also accept the `path` property on Directory, which
 must be a filesystem path available on the same host as the CWL runner (for
 inputs) or the runtime environment of a command line tool execution (for
 command line tool outputs).
 
 A Directory object may have a `listing` field.  This is a list of File and
 Directory objects that are contained in the Directory.  For each entry in
 `listing`, the `basename` property defines the name of the File or
 Subdirectory when staged to disk.  If `listing` is not provided, the
 implementation must have some way of fetching the Directory listing at
 runtime based on the `location` field.
 
 If a Directory does not have `location`, it is a Directory literal.  A
 Directory literal must provide `listing`.  Directory literals must be
 created on disk at runtime as needed.
 
 The resources in a Directory literal do not need to have any implied
 relationship in their `location`.  For example, a Directory listing may
 contain two files located on different hosts.  It is the responsibility of
 the runtime to ensure that those files are staged to disk appropriately.
 Secondary files associated with files in `listing` must also be staged to
 the same Directory.
 
 When executing a CommandLineTool, Directories must be recursively staged
 first and have local values of `path` assigned.
 
 Directory objects in CommandLineTool output must provide either a
 `location` IRI or a `path` property in the context of the tool execution
 runtime (local to the compute node, or within the executing container).
 
 An ExpressionTool may forward file references from input to output by using
 the same value for `location`.
 
 Name conflicts (the same `basename` appearing multiple times in `listing`
 or in any entry in `secondaryFiles` in the listing) is a fatal error.
  </BLOCKQUOTE>
 */
public class DirectoryImpl extends SaveableImpl implements Directory {
  private LoadingOptions loadingOptions_ = new LoadingOptionsBuilder().build();
  private java.util.Map<String, Object> extensionFields_ =
      new java.util.HashMap<String, Object>();

  private Directory_class class_;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#Directory/class</I><BR>
   * <BLOCKQUOTE>
   * Must be `Directory` to indicate this object describes a Directory.   * </BLOCKQUOTE>
   */

  public Directory_class getClass_() {
    return this.class_;
  }

  private java.util.Optional<String> location;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#Directory/location</I><BR>
   * <BLOCKQUOTE>
   * An IRI that identifies the directory resource.  This may be a relative
   * reference, in which case it must be resolved using the base IRI of the
   * document.  The location may refer to a local or remote resource.  If
   * the `listing` field is not set, the implementation must use the
   * location IRI to retrieve directory listing.  If an implementation is
   * unable to retrieve the directory listing stored at a remote resource (due to
   * unsupported protocol, access denied, or other issue) it must signal an
   * error.
   * 
   * If the `location` field is not provided, the `listing` field must be
   * provided.  The implementation must assign a unique identifier for
   * the `location` field.
   * 
   * If the `path` field is provided but the `location` field is not, an
   * implementation may assign the value of the `path` field to `location`,
   * then follow the rules above.
   *    * </BLOCKQUOTE>
   */

  public java.util.Optional<String> getLocation() {
    return this.location;
  }

  private java.util.Optional<String> path;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#path</I><BR>
   * <BLOCKQUOTE>
   * The local path where the Directory is made available prior to executing a
   * CommandLineTool.  This must be set by the implementation.  This field
   * must not be used in any other context.  The command line tool being
   * executed must be able to access the directory at `path` using the POSIX
   * `opendir(2)` syscall.
   * 
   * If the `path` contains [POSIX shell metacharacters](http://pubs.opengroup.org/onlinepubs/9699919799/utilities/V3_chap02.html#tag_18_02)
   * (`|`,`&`, `;`, `<`, `>`, `(`,`)`, `$`,`` ` ``, `\`, `"`, `'`,
   * `<space>`, `<tab>`, and `<newline>`) or characters
   * [not allowed](http://www.iana.org/assignments/idna-tables-6.3.0/idna-tables-6.3.0.xhtml)
   * for [Internationalized Domain Names for Applications](https://tools.ietf.org/html/rfc6452)
   * then implementations may terminate the process with a
   * `permanentFailure`.
   *    * </BLOCKQUOTE>
   */

  public java.util.Optional<String> getPath() {
    return this.path;
  }

  private java.util.Optional<String> basename;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#Directory/basename</I><BR>
   * <BLOCKQUOTE>
   * The base name of the directory, that is, the name of the file without any
   * leading directory path.  The base name must not contain a slash `/`.
   * 
   * If not provided, the implementation must set this field based on the
   * `location` field by taking the final path component after parsing
   * `location` as an IRI.  If `basename` is provided, it is not required to
   * match the value from `location`.
   * 
   * When this file is made available to a CommandLineTool, it must be named
   * with `basename`, i.e. the final component of the `path` field must match
   * `basename`.
   *    * </BLOCKQUOTE>
   */

  public java.util.Optional<String> getBasename() {
    return this.basename;
  }

  private java.util.Optional<java.util.List<Object>> listing;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#listing</I><BR>
   * <BLOCKQUOTE>
   * List of files or subdirectories contained in this directory.  The name
   * of each file or subdirectory is determined by the `basename` field of
   * each `File` or `Directory` object.  It is an error if a `File` shares a
   * `basename` with any other entry in `listing`.  If two or more
   * `Directory` object share the same `basename`, this must be treated as
   * equivalent to a single subdirectory with the listings recursively
   * merged.
   *    * </BLOCKQUOTE>
   */

  public java.util.Optional<java.util.List<Object>> getListing() {
    return this.listing;
  }

  /**
   * Used by {@link org.w3id.cwl.cwl1_2.utils.RootLoader} to construct instances of DirectoryImpl.
   *
   * @param __doc_            Document fragment to load this record object from (presumably a
                              {@link java.util.Map}).
   * @param __baseUri_        Base URI to generate child document IDs against.
   * @param __loadingOptions  Context for loading URIs and populating objects.
   * @param __docRoot_        ID at this position in the document (if available) (maybe?)
   * @throws ValidationException If the document fragment is not a {@link java.util.Map}
   *                             or validation of fields fails.
   */
  public DirectoryImpl(
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
      throw new ValidationException("DirectoryImpl called on non-map");
    }
    final java.util.Map<String, Object> __doc = (java.util.Map<String, Object>) __doc_;
    final java.util.List<ValidationException> __errors =
        new java.util.ArrayList<ValidationException>();
    if (__loadingOptions != null) {
      this.loadingOptions_ = __loadingOptions;
    }
    Directory_class class_;
    try {
      class_ =
          LoaderInstances
              .uri_Directory_class_False_True_None_None
              .loadField(__doc.get("class"), __baseUri, __loadingOptions);
    } catch (ValidationException e) {
      class_ = null; // won't be used but prevents compiler from complaining.
      final String __message = "the `class` field is not valid because:";
      __errors.add(new ValidationException(__message, e));
    }
    java.util.Optional<String> location;

    if (__doc.containsKey("location")) {
      try {
        location =
            LoaderInstances
                .uri_optional_StringInstance_False_False_None_None
                .loadField(__doc.get("location"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        location = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `location` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      location = null;
    }
    java.util.Optional<String> path;

    if (__doc.containsKey("path")) {
      try {
        path =
            LoaderInstances
                .uri_optional_StringInstance_False_False_None_None
                .loadField(__doc.get("path"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        path = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `path` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      path = null;
    }
    java.util.Optional<String> basename;

    if (__doc.containsKey("basename")) {
      try {
        basename =
            LoaderInstances
                .optional_StringInstance
                .loadField(__doc.get("basename"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        basename = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `basename` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      basename = null;
    }
    java.util.Optional<java.util.List<Object>> listing;

    if (__doc.containsKey("listing")) {
      try {
        listing =
            LoaderInstances
                .optional_array_of_union_of_File_or_Directory
                .loadField(__doc.get("listing"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        listing = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `listing` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      listing = null;
    }
    if (!__errors.isEmpty()) {
      throw new ValidationException("Trying 'RecordField'", __errors);
    }
    this.class_ = (Directory_class) class_;
    this.location = (java.util.Optional<String>) location;
    this.path = (java.util.Optional<String>) path;
    this.basename = (java.util.Optional<String>) basename;
    this.listing = (java.util.Optional<java.util.List<Object>>) listing;
  }
}
