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
* Auto-generated class implementation for <I>https://w3id.org/cwl/cwl#InplaceUpdateRequirement</I><BR> <BLOCKQUOTE>
 
 If `inplaceUpdate` is true, then an implementation supporting this
 feature may permit tools to directly update files with `writable:
 true` in InitialWorkDirRequirement.  That is, as an optimization,
 files may be destructively modified in place as opposed to copied
 and updated.
 
 An implementation must ensure that only one workflow step may
 access a writable file at a time.  It is an error if a file which
 is writable by one workflow step file is accessed (for reading or
 writing) by any other workflow step running independently.
 However, a file which has been updated in a previous completed
 step may be used as input to multiple steps, provided it is
 read-only in every step.
 
 Workflow steps which modify a file must produce the modified file
 as output.  Downstream steps which further process the file must
 use the output of previous steps, and not refer to a common input
 (this is necessary for both ordering and correctness).
 
 Workflow authors should provide this in the `hints` section.  The
 intent of this feature is that workflows produce the same results
 whether or not InplaceUpdateRequirement is supported by the
 implementation, and this feature is primarily available as an
 optimization for particular environments.
 
 Users and implementers should be aware that workflows that
 destructively modify inputs may not be repeatable or reproducible.
 In particular, enabling this feature implies that WorkReuse should
 not be enabled.
  </BLOCKQUOTE>
 */
public class InplaceUpdateRequirementImpl extends SaveableImpl implements InplaceUpdateRequirement {
  private LoadingOptions loadingOptions_ = new LoadingOptionsBuilder().build();
  private java.util.Map<String, Object> extensionFields_ =
      new java.util.HashMap<String, Object>();

  private InplaceUpdateRequirement_class class_;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#InplaceUpdateRequirement/class</I><BR>
   * <BLOCKQUOTE>
   * Always 'InplaceUpdateRequirement'   * </BLOCKQUOTE>
   */

  public InplaceUpdateRequirement_class getClass_() {
    return this.class_;
  }

  private Boolean inplaceUpdate;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#InplaceUpdateRequirement/inplaceUpdate</I><BR>

   */

  public Boolean getInplaceUpdate() {
    return this.inplaceUpdate;
  }

  /**
   * Used by {@link org.w3id.cwl.cwl1_2.utils.RootLoader} to construct instances of InplaceUpdateRequirementImpl.
   *
   * @param __doc_            Document fragment to load this record object from (presumably a
                              {@link java.util.Map}).
   * @param __baseUri_        Base URI to generate child document IDs against.
   * @param __loadingOptions  Context for loading URIs and populating objects.
   * @param __docRoot_        ID at this position in the document (if available) (maybe?)
   * @throws ValidationException If the document fragment is not a {@link java.util.Map}
   *                             or validation of fields fails.
   */
  public InplaceUpdateRequirementImpl(
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
      throw new ValidationException("InplaceUpdateRequirementImpl called on non-map");
    }
    final java.util.Map<String, Object> __doc = (java.util.Map<String, Object>) __doc_;
    final java.util.List<ValidationException> __errors =
        new java.util.ArrayList<ValidationException>();
    if (__loadingOptions != null) {
      this.loadingOptions_ = __loadingOptions;
    }
    InplaceUpdateRequirement_class class_;
    try {
      class_ =
          LoaderInstances
              .uri_InplaceUpdateRequirement_class_False_True_None_None
              .loadField(__doc.get("class"), __baseUri, __loadingOptions);
    } catch (ValidationException e) {
      class_ = null; // won't be used but prevents compiler from complaining.
      final String __message = "the `class` field is not valid because:";
      __errors.add(new ValidationException(__message, e));
    }
    Boolean inplaceUpdate;
    try {
      inplaceUpdate =
          LoaderInstances
              .BooleanInstance
              .loadField(__doc.get("inplaceUpdate"), __baseUri, __loadingOptions);
    } catch (ValidationException e) {
      inplaceUpdate = null; // won't be used but prevents compiler from complaining.
      final String __message = "the `inplaceUpdate` field is not valid because:";
      __errors.add(new ValidationException(__message, e));
    }
    if (!__errors.isEmpty()) {
      throw new ValidationException("Trying 'RecordField'", __errors);
    }
    this.class_ = (InplaceUpdateRequirement_class) class_;
    this.inplaceUpdate = (Boolean) inplaceUpdate;
  }
}
