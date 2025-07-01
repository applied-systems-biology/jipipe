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
* Auto-generated class implementation for <I>https://w3id.org/cwl/cwl#File</I><BR> <BLOCKQUOTE>
 Represents a file (or group of files when `secondaryFiles` is provided) that
 will be accessible by tools using standard POSIX file system call API such as
 open(2) and read(2).
 
 Files are represented as objects with `class` of `File`.  File objects have
 a number of properties that provide metadata about the file.
 
 The `location` property of a File is a IRI that uniquely identifies the
 file.  Implementations must support the `file://` IRI scheme and may support
 other schemes such as `http://` and `https://`.  The value of `location` may also be a
 relative reference, in which case it must be resolved relative to the IRI
 of the document it appears in.  Alternately to `location`, implementations
 must also accept the `path` property on File, which must be a filesystem
 path available on the same host as the CWL runner (for inputs) or the
 runtime environment of a command line tool execution (for command line tool
 outputs).
 
 If no `location` or `path` is specified, a file object must specify
 `contents` with the UTF-8 text content of the file.  This is a "file
 literal".  File literals do not correspond to external resources, but are
 created on disk with `contents` with when needed for executing a tool.
 Where appropriate, expressions can return file literals to define new files
 on a runtime.  The maximum size of `contents` is 64 kilobytes.
 
 The `basename` property defines the filename on disk where the file is
 staged.  This may differ from the resource name.  If not provided,
 `basename` must be computed from the last path part of `location` and made
 available to expressions.
 
 The `secondaryFiles` property is a list of File or Directory objects that
 must be staged in the same directory as the primary file.  It is an error
 for file names to be duplicated in `secondaryFiles`.
 
 The `size` property is the size in bytes of the File.  It must be computed
 from the resource and made available to expressions.  The `checksum` field
 contains a cryptographic hash of the file content for use it verifying file
 contents.  Implementations may, at user option, enable or disable
 computation of the `checksum` field for performance or other reasons.
 However, the ability to compute output checksums is required to pass the
 CWL conformance test suite.
 
 When executing a CommandLineTool, the files and secondary files may be
 staged to an arbitrary directory, but must use the value of `basename` for
 the filename.  The `path` property must be file path in the context of the
 tool execution runtime (local to the compute node, or within the executing
 container).  All computed properties should be available to expressions.
 File literals also must be staged and `path` must be set.
 
 When collecting CommandLineTool outputs, `glob` matching returns file paths
 (with the `path` property) and the derived properties. This can all be
 modified by `outputEval`.  Alternately, if the file `cwl.output.json` is
 present in the output, `outputBinding` is ignored.
 
 File objects in the output must provide either a `location` IRI or a `path`
 property in the context of the tool execution runtime (local to the compute
 node, or within the executing container).
 
 When evaluating an ExpressionTool, file objects must be referenced via
 `location` (the expression tool does not have access to files on disk so
 `path` is meaningless) or as file literals.  It is legal to return a file
 object with an existing `location` but a different `basename`.  The
 `loadContents` field of ExpressionTool inputs behaves the same as on
 CommandLineTool inputs, however it is not meaningful on the outputs.
 
 An ExpressionTool may forward file references from input to output by using
 the same value for `location`.
  </BLOCKQUOTE>
 */
public class FileImpl extends SaveableImpl implements File {
  private LoadingOptions loadingOptions_ = new LoadingOptionsBuilder().build();
  private java.util.Map<String, Object> extensionFields_ =
      new java.util.HashMap<String, Object>();

  private File_class class_;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#File/class</I><BR>
   * <BLOCKQUOTE>
   * Must be `File` to indicate this object describes a file.   * </BLOCKQUOTE>
   */

  public File_class getClass_() {
    return this.class_;
  }

  private java.util.Optional<String> location;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#File/location</I><BR>
   * <BLOCKQUOTE>
   * An IRI that identifies the file resource.  This may be a relative
   * reference, in which case it must be resolved using the base IRI of the
   * document.  The location may refer to a local or remote resource; the
   * implementation must use the IRI to retrieve file content.  If an
   * implementation is unable to retrieve the file content stored at a
   * remote resource (due to unsupported protocol, access denied, or other
   * issue) it must signal an error.
   * 
   * If the `location` field is not provided, the `contents` field must be
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
   * The local host path where the File is available when a CommandLineTool is
   * executed.  This field must be set by the implementation.  The final
   * path component must match the value of `basename`.  This field
   * must not be used in any other context.  The command line tool being
   * executed must be able to access the file at `path` using the POSIX
   * `open(2)` syscall.
   * 
   * As a special case, if the `path` field is provided but the `location`
   * field is not, an implementation may assign the value of the `path`
   * field to `location`, and remove the `path` field.
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
   * Getter for property <I>https://w3id.org/cwl/cwl#File/basename</I><BR>
   * <BLOCKQUOTE>
   * The base name of the file, that is, the name of the file without any
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

  private java.util.Optional<String> dirname;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#File/dirname</I><BR>
   * <BLOCKQUOTE>
   * The name of the directory containing file, that is, the path leading up
   * to the final slash in the path such that `dirname + '/' + basename ==
   * path`.
   * 
   * The implementation must set this field based on the value of `path`
   * prior to evaluating parameter references or expressions in a
   * CommandLineTool document.  This field must not be used in any other
   * context.
   *    * </BLOCKQUOTE>
   */

  public java.util.Optional<String> getDirname() {
    return this.dirname;
  }

  private java.util.Optional<String> nameroot;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#File/nameroot</I><BR>
   * <BLOCKQUOTE>
   * The basename root such that `nameroot + nameext == basename`, and
   * `nameext` is empty or begins with a period and contains at most one
   * period.  For the purposes of path splitting leading periods on the
   * basename are ignored; a basename of `.cshrc` will have a nameroot of
   * `.cshrc`.
   * 
   * The implementation must set this field automatically based on the value
   * of `basename` prior to evaluating parameter references or expressions.
   *    * </BLOCKQUOTE>
   */

  public java.util.Optional<String> getNameroot() {
    return this.nameroot;
  }

  private java.util.Optional<String> nameext;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#File/nameext</I><BR>
   * <BLOCKQUOTE>
   * The basename extension such that `nameroot + nameext == basename`, and
   * `nameext` is empty or begins with a period and contains at most one
   * period.  Leading periods on the basename are ignored; a basename of
   * `.cshrc` will have an empty `nameext`.
   * 
   * The implementation must set this field automatically based on the value
   * of `basename` prior to evaluating parameter references or expressions.
   *    * </BLOCKQUOTE>
   */

  public java.util.Optional<String> getNameext() {
    return this.nameext;
  }

  private java.util.Optional<String> checksum;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#File/checksum</I><BR>
   * <BLOCKQUOTE>
   * Optional hash code for validating file integrity.  Currently, must be in the form
   * "sha1$ + hexadecimal string" using the SHA-1 algorithm.
   *    * </BLOCKQUOTE>
   */

  public java.util.Optional<String> getChecksum() {
    return this.checksum;
  }

  private Object size;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#File/size</I><BR>
   * <BLOCKQUOTE>
   * Optional file size (in bytes)   * </BLOCKQUOTE>
   */

  public Object getSize() {
    return this.size;
  }

  private java.util.Optional<java.util.List<Object>> secondaryFiles;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#secondaryFiles</I><BR>
   * <BLOCKQUOTE>
   * A list of additional files or directories that are associated with the
   * primary file and must be transferred alongside the primary file.
   * Examples include indexes of the primary file, or external references
   * which must be included when loading primary document.  A file object
   * listed in `secondaryFiles` may itself include `secondaryFiles` for
   * which the same rules apply.
   *    * </BLOCKQUOTE>
   */

  public java.util.Optional<java.util.List<Object>> getSecondaryFiles() {
    return this.secondaryFiles;
  }

  private java.util.Optional<String> format;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#format</I><BR>
   * <BLOCKQUOTE>
   * The format of the file: this must be an IRI of a concept node that
   * represents the file format, preferably defined within an ontology.
   * If no ontology is available, file formats may be tested by exact match.
   * 
   * Reasoning about format compatibility must be done by checking that an
   * input file format is the same, `owl:equivalentClass` or
   * `rdfs:subClassOf` the format required by the input parameter.
   * `owl:equivalentClass` is transitive with `rdfs:subClassOf`, e.g. if
   * `<B> owl:equivalentClass <C>` and `<B> owl:subclassOf <A>` then infer
   * `<C> owl:subclassOf <A>`.
   * 
   * File format ontologies may be provided in the "$schemas" metadata at the
   * root of the document.  If no ontologies are specified in `$schemas`, the
   * runtime may perform exact file format matches.
   *    * </BLOCKQUOTE>
   */

  public java.util.Optional<String> getFormat() {
    return this.format;
  }

  private java.util.Optional<String> contents;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#File/contents</I><BR>
   * <BLOCKQUOTE>
   * File contents literal.
   * 
   * If neither `location` nor `path` is provided, `contents` must be
   * non-null.  The implementation must assign a unique identifier for the
   * `location` field.  When the file is staged as input to CommandLineTool,
   * the value of `contents` must be written to a file.
   * 
   * If `contents` is set as a result of a Javascript expression,
   * an `entry` in `InitialWorkDirRequirement`, or read in from
   * `cwl.output.json`, there is no specified upper limit on the
   * size of `contents`.  Implementations may have practical limits
   * on the size of `contents` based on memory and storage
   * available to the workflow runner or other factors.
   * 
   * If the `loadContents` field of an `InputParameter` or
   * `OutputParameter` is true, and the input or output File object
   * `location` is valid, the file must be a UTF-8 text file 64 KiB
   * or smaller, and the implementation must read the entire
   * contents of the file and place it in the `contents` field.  If
   * the size of the file is greater than 64 KiB, the
   * implementation must raise a fatal error.
   *    * </BLOCKQUOTE>
   */

  public java.util.Optional<String> getContents() {
    return this.contents;
  }

  /**
   * Used by {@link org.w3id.cwl.cwl1_2.utils.RootLoader} to construct instances of FileImpl.
   *
   * @param __doc_            Document fragment to load this record object from (presumably a
                              {@link java.util.Map}).
   * @param __baseUri_        Base URI to generate child document IDs against.
   * @param __loadingOptions  Context for loading URIs and populating objects.
   * @param __docRoot_        ID at this position in the document (if available) (maybe?)
   * @throws ValidationException If the document fragment is not a {@link java.util.Map}
   *                             or validation of fields fails.
   */
  public FileImpl(
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
      throw new ValidationException("FileImpl called on non-map");
    }
    final java.util.Map<String, Object> __doc = (java.util.Map<String, Object>) __doc_;
    final java.util.List<ValidationException> __errors =
        new java.util.ArrayList<ValidationException>();
    if (__loadingOptions != null) {
      this.loadingOptions_ = __loadingOptions;
    }
    File_class class_;
    try {
      class_ =
          LoaderInstances
              .uri_File_class_False_True_None_None
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
    java.util.Optional<String> dirname;

    if (__doc.containsKey("dirname")) {
      try {
        dirname =
            LoaderInstances
                .optional_StringInstance
                .loadField(__doc.get("dirname"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        dirname = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `dirname` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      dirname = null;
    }
    java.util.Optional<String> nameroot;

    if (__doc.containsKey("nameroot")) {
      try {
        nameroot =
            LoaderInstances
                .optional_StringInstance
                .loadField(__doc.get("nameroot"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        nameroot = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `nameroot` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      nameroot = null;
    }
    java.util.Optional<String> nameext;

    if (__doc.containsKey("nameext")) {
      try {
        nameext =
            LoaderInstances
                .optional_StringInstance
                .loadField(__doc.get("nameext"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        nameext = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `nameext` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      nameext = null;
    }
    java.util.Optional<String> checksum;

    if (__doc.containsKey("checksum")) {
      try {
        checksum =
            LoaderInstances
                .optional_StringInstance
                .loadField(__doc.get("checksum"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        checksum = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `checksum` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      checksum = null;
    }
    Object size;

    if (__doc.containsKey("size")) {
      try {
        size =
            LoaderInstances
                .union_of_NullInstance_or_IntegerInstance_or_LongInstance
                .loadField(__doc.get("size"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        size = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `size` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      size = null;
    }
    java.util.Optional<java.util.List<Object>> secondaryFiles;

    if (__doc.containsKey("secondaryFiles")) {
      try {
        secondaryFiles =
            LoaderInstances
                .secondaryfilesdsl_optional_array_of_union_of_File_or_Directory
                .loadField(__doc.get("secondaryFiles"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        secondaryFiles = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `secondaryFiles` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      secondaryFiles = null;
    }
    java.util.Optional<String> format;

    if (__doc.containsKey("format")) {
      try {
        format =
            LoaderInstances
                .uri_optional_StringInstance_True_False_None_True
                .loadField(__doc.get("format"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        format = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `format` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      format = null;
    }
    java.util.Optional<String> contents;

    if (__doc.containsKey("contents")) {
      try {
        contents =
            LoaderInstances
                .optional_StringInstance
                .loadField(__doc.get("contents"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        contents = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `contents` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      contents = null;
    }
    if (!__errors.isEmpty()) {
      throw new ValidationException("Trying 'RecordField'", __errors);
    }
    this.class_ = (File_class) class_;
    this.location = (java.util.Optional<String>) location;
    this.path = (java.util.Optional<String>) path;
    this.basename = (java.util.Optional<String>) basename;
    this.dirname = (java.util.Optional<String>) dirname;
    this.nameroot = (java.util.Optional<String>) nameroot;
    this.nameext = (java.util.Optional<String>) nameext;
    this.checksum = (java.util.Optional<String>) checksum;
    this.size = (Object) size;
    this.secondaryFiles = (java.util.Optional<java.util.List<Object>>) secondaryFiles;
    this.format = (java.util.Optional<String>) format;
    this.contents = (java.util.Optional<String>) contents;
  }
}
