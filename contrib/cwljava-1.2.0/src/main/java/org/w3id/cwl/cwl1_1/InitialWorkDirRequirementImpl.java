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
* Auto-generated class implementation for <I>https://w3id.org/cwl/cwl#InitialWorkDirRequirement</I><BR> <BLOCKQUOTE>
 Define a list of files and subdirectories that must be created by the workflow platform in the designated output directory prior to executing the command line tool. </BLOCKQUOTE>
 */
public class InitialWorkDirRequirementImpl extends SaveableImpl implements InitialWorkDirRequirement {
  private LoadingOptions loadingOptions_ = new LoadingOptionsBuilder().build();
  private java.util.Map<String, Object> extensionFields_ =
      new java.util.HashMap<String, Object>();

  private InitialWorkDirRequirement_class class_;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#InitialWorkDirRequirement/class</I><BR>
   * <BLOCKQUOTE>
   * InitialWorkDirRequirement   * </BLOCKQUOTE>
   */

  public InitialWorkDirRequirement_class getClass_() {
    return this.class_;
  }

  private Object listing;

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

  public Object getListing() {
    return this.listing;
  }

  /**
   * Used by {@link org.w3id.cwl.cwl1_1.utils.RootLoader} to construct instances of InitialWorkDirRequirementImpl.
   *
   * @param __doc_            Document fragment to load this record object from (presumably a
                              {@link java.util.Map}).
   * @param __baseUri_        Base URI to generate child document IDs against.
   * @param __loadingOptions  Context for loading URIs and populating objects.
   * @param __docRoot_        ID at this position in the document (if available) (maybe?)
   * @throws ValidationException If the document fragment is not a {@link java.util.Map}
   *                             or validation of fields fails.
   */
  public InitialWorkDirRequirementImpl(
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
      throw new ValidationException("InitialWorkDirRequirementImpl called on non-map");
    }
    final java.util.Map<String, Object> __doc = (java.util.Map<String, Object>) __doc_;
    final java.util.List<ValidationException> __errors =
        new java.util.ArrayList<ValidationException>();
    if (__loadingOptions != null) {
      this.loadingOptions_ = __loadingOptions;
    }
    InitialWorkDirRequirement_class class_;
    try {
      class_ =
          LoaderInstances
              .uri_InitialWorkDirRequirement_class_False_True_None_None
              .loadField(__doc.get("class"), __baseUri, __loadingOptions);
    } catch (ValidationException e) {
      class_ = null; // won't be used but prevents compiler from complaining.
      final String __message = "the `class` field is not valid because:";
      __errors.add(new ValidationException(__message, e));
    }
    Object listing;
    try {
      listing =
          LoaderInstances
              .union_of_array_of_union_of_NullInstance_or_File_or_array_of_union_of_File_or_Directory_or_Directory_or_Dirent_or_ExpressionLoader_or_ExpressionLoader
              .loadField(__doc.get("listing"), __baseUri, __loadingOptions);
    } catch (ValidationException e) {
      listing = null; // won't be used but prevents compiler from complaining.
      final String __message = "the `listing` field is not valid because:";
      __errors.add(new ValidationException(__message, e));
    }
    if (!__errors.isEmpty()) {
      throw new ValidationException("Trying 'RecordField'", __errors);
    }
    this.class_ = (InitialWorkDirRequirement_class) class_;
    this.listing = (Object) listing;
  }
}
