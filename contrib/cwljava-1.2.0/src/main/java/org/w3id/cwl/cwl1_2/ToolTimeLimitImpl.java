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
* Auto-generated class implementation for <I>https://w3id.org/cwl/cwl#ToolTimeLimit</I><BR> <BLOCKQUOTE>
 Set an upper limit on the execution time of a CommandLineTool.
 A CommandLineTool whose execution duration exceeds the time
 limit may be preemptively terminated and considered failed.
 May also be used by batch systems to make scheduling decisions.
 The execution duration excludes external operations, such as
 staging of files, pulling a docker image etc, and only counts
 wall-time for the execution of the command line itself.
  </BLOCKQUOTE>
 */
public class ToolTimeLimitImpl extends SaveableImpl implements ToolTimeLimit {
  private LoadingOptions loadingOptions_ = new LoadingOptionsBuilder().build();
  private java.util.Map<String, Object> extensionFields_ =
      new java.util.HashMap<String, Object>();

  private ToolTimeLimit_class class_;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#ToolTimeLimit/class</I><BR>
   * <BLOCKQUOTE>
   * Always 'ToolTimeLimit'   * </BLOCKQUOTE>
   */

  public ToolTimeLimit_class getClass_() {
    return this.class_;
  }

  private Object timelimit;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#ToolTimeLimit/timelimit</I><BR>
   * <BLOCKQUOTE>
   * The time limit, in seconds.  A time limit of zero means no
   * time limit.  Negative time limits are an error.
   *    * </BLOCKQUOTE>
   */

  public Object getTimelimit() {
    return this.timelimit;
  }

  /**
   * Used by {@link org.w3id.cwl.cwl1_2.utils.RootLoader} to construct instances of ToolTimeLimitImpl.
   *
   * @param __doc_            Document fragment to load this record object from (presumably a
                              {@link java.util.Map}).
   * @param __baseUri_        Base URI to generate child document IDs against.
   * @param __loadingOptions  Context for loading URIs and populating objects.
   * @param __docRoot_        ID at this position in the document (if available) (maybe?)
   * @throws ValidationException If the document fragment is not a {@link java.util.Map}
   *                             or validation of fields fails.
   */
  public ToolTimeLimitImpl(
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
      throw new ValidationException("ToolTimeLimitImpl called on non-map");
    }
    final java.util.Map<String, Object> __doc = (java.util.Map<String, Object>) __doc_;
    final java.util.List<ValidationException> __errors =
        new java.util.ArrayList<ValidationException>();
    if (__loadingOptions != null) {
      this.loadingOptions_ = __loadingOptions;
    }
    ToolTimeLimit_class class_;
    try {
      class_ =
          LoaderInstances
              .uri_ToolTimeLimit_class_False_True_None_None
              .loadField(__doc.get("class"), __baseUri, __loadingOptions);
    } catch (ValidationException e) {
      class_ = null; // won't be used but prevents compiler from complaining.
      final String __message = "the `class` field is not valid because:";
      __errors.add(new ValidationException(__message, e));
    }
    Object timelimit;
    try {
      timelimit =
          LoaderInstances
              .union_of_IntegerInstance_or_LongInstance_or_ExpressionLoader
              .loadField(__doc.get("timelimit"), __baseUri, __loadingOptions);
    } catch (ValidationException e) {
      timelimit = null; // won't be used but prevents compiler from complaining.
      final String __message = "the `timelimit` field is not valid because:";
      __errors.add(new ValidationException(__message, e));
    }
    if (!__errors.isEmpty()) {
      throw new ValidationException("Trying 'RecordField'", __errors);
    }
    this.class_ = (ToolTimeLimit_class) class_;
    this.timelimit = (Object) timelimit;
  }
}
