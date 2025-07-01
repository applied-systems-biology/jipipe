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
* Auto-generated class implementation for <I>https://w3id.org/cwl/cwl#ResourceRequirement</I><BR> <BLOCKQUOTE>
 Specify basic hardware resource requirements.
 
 "min" is the minimum amount of a resource that must be reserved to
 schedule a job. If "min" cannot be satisfied, the job should not
 be run.
 
 "max" is the maximum amount of a resource that the job shall be
 allocated. If a node has sufficient resources, multiple jobs may
 be scheduled on a single node provided each job's "max" resource
 requirements are met. If a job attempts to exceed its resource
 allocation, an implementation may deny additional resources, which
 may result in job failure.
 
 If both "min" and "max" are specified, an implementation may
 choose to allocate any amount between "min" and "max", with the
 actual allocation provided in the `runtime` object.
 
 If "min" is specified but "max" is not, then "max" == "min"
 If "max" is specified by "min" is not, then "min" == "max".
 
 It is an error if max < min.
 
 It is an error if the value of any of these fields is negative.
 
 If neither "min" nor "max" is specified for a resource, use the default values below.
  </BLOCKQUOTE>
 */
public class ResourceRequirementImpl extends SaveableImpl implements ResourceRequirement {
  private LoadingOptions loadingOptions_ = new LoadingOptionsBuilder().build();
  private java.util.Map<String, Object> extensionFields_ =
      new java.util.HashMap<String, Object>();

  private ResourceRequirement_class class_;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#ResourceRequirement/class</I><BR>
   * <BLOCKQUOTE>
   * Always 'ResourceRequirement'   * </BLOCKQUOTE>
   */

  public ResourceRequirement_class getClass_() {
    return this.class_;
  }

  private Object coresMin;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#ResourceRequirement/coresMin</I><BR>
   * <BLOCKQUOTE>
   * Minimum reserved number of CPU cores (default is 1).
   * 
   * May be a fractional value to indicate to a scheduling
   * algorithm that one core can be allocated to multiple
   * jobs. For example, a value of 0.25 indicates that up to 4
   * jobs may run in parallel on 1 core.  A value of 1.25 means
   * that up to 3 jobs can run on a 4 core system (4/1.25 ≈ 3).
   * 
   * Processes can only share a core allocation if the sum of each
   * of their `ramMax`, `tmpdirMax`, and `outdirMax` requests also
   * do not exceed the capacity of the node.
   * 
   * Processes sharing a core must have the same level of isolation
   * (typically a container or VM) that they would normally have.
   * 
   * The reported number of CPU cores reserved for the process,
   * which is available to expressions on the CommandLineTool as
   * `runtime.cores`, must be a non-zero integer, and may be
   * calculated by rounding up the cores request to the next whole
   * number.
   * 
   * Scheduling systems may allocate fractional CPU resources by
   * setting quotas or scheduling weights.  Scheduling systems that
   * do not support fractional CPUs may round up the request to the
   * next whole number.
   *    * </BLOCKQUOTE>
   */

  public Object getCoresMin() {
    return this.coresMin;
  }

  private Object coresMax;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#ResourceRequirement/coresMax</I><BR>
   * <BLOCKQUOTE>
   * Maximum reserved number of CPU cores.
   * 
   * See `coresMin` for discussion about fractional CPU requests.
   *    * </BLOCKQUOTE>
   */

  public Object getCoresMax() {
    return this.coresMax;
  }

  private Object ramMin;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#ResourceRequirement/ramMin</I><BR>
   * <BLOCKQUOTE>
   * Minimum reserved RAM in mebibytes (2**20) (default is 256)
   * 
   * May be a fractional value.  If so, the actual RAM request must
   * be rounded up to the next whole number.  The reported amount of
   * RAM reserved for the process, which is available to
   * expressions on the CommandLineTool as `runtime.ram`, must be a
   * non-zero integer.
   *    * </BLOCKQUOTE>
   */

  public Object getRamMin() {
    return this.ramMin;
  }

  private Object ramMax;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#ResourceRequirement/ramMax</I><BR>
   * <BLOCKQUOTE>
   * Maximum reserved RAM in mebibytes (2**20)
   * 
   * See `ramMin` for discussion about fractional RAM requests.
   *    * </BLOCKQUOTE>
   */

  public Object getRamMax() {
    return this.ramMax;
  }

  private Object tmpdirMin;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#ResourceRequirement/tmpdirMin</I><BR>
   * <BLOCKQUOTE>
   * Minimum reserved filesystem based storage for the designated temporary directory, in mebibytes (2**20) (default is 1024)
   * 
   * May be a fractional value.  If so, the actual storage request
   * must be rounded up to the next whole number.  The reported
   * amount of storage reserved for the process, which is available
   * to expressions on the CommandLineTool as `runtime.tmpdirSize`,
   * must be a non-zero integer.
   *    * </BLOCKQUOTE>
   */

  public Object getTmpdirMin() {
    return this.tmpdirMin;
  }

  private Object tmpdirMax;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#ResourceRequirement/tmpdirMax</I><BR>
   * <BLOCKQUOTE>
   * Maximum reserved filesystem based storage for the designated temporary directory, in mebibytes (2**20)
   * 
   * See `tmpdirMin` for discussion about fractional storage requests.
   *    * </BLOCKQUOTE>
   */

  public Object getTmpdirMax() {
    return this.tmpdirMax;
  }

  private Object outdirMin;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#ResourceRequirement/outdirMin</I><BR>
   * <BLOCKQUOTE>
   * Minimum reserved filesystem based storage for the designated output directory, in mebibytes (2**20) (default is 1024)
   * 
   * May be a fractional value.  If so, the actual storage request
   * must be rounded up to the next whole number.  The reported
   * amount of storage reserved for the process, which is available
   * to expressions on the CommandLineTool as `runtime.outdirSize`,
   * must be a non-zero integer.
   *    * </BLOCKQUOTE>
   */

  public Object getOutdirMin() {
    return this.outdirMin;
  }

  private Object outdirMax;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#ResourceRequirement/outdirMax</I><BR>
   * <BLOCKQUOTE>
   * Maximum reserved filesystem based storage for the designated output directory, in mebibytes (2**20)
   * 
   * See `outdirMin` for discussion about fractional storage requests.
   *    * </BLOCKQUOTE>
   */

  public Object getOutdirMax() {
    return this.outdirMax;
  }

  /**
   * Used by {@link org.w3id.cwl.cwl1_2.utils.RootLoader} to construct instances of ResourceRequirementImpl.
   *
   * @param __doc_            Document fragment to load this record object from (presumably a
                              {@link java.util.Map}).
   * @param __baseUri_        Base URI to generate child document IDs against.
   * @param __loadingOptions  Context for loading URIs and populating objects.
   * @param __docRoot_        ID at this position in the document (if available) (maybe?)
   * @throws ValidationException If the document fragment is not a {@link java.util.Map}
   *                             or validation of fields fails.
   */
  public ResourceRequirementImpl(
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
      throw new ValidationException("ResourceRequirementImpl called on non-map");
    }
    final java.util.Map<String, Object> __doc = (java.util.Map<String, Object>) __doc_;
    final java.util.List<ValidationException> __errors =
        new java.util.ArrayList<ValidationException>();
    if (__loadingOptions != null) {
      this.loadingOptions_ = __loadingOptions;
    }
    ResourceRequirement_class class_;
    try {
      class_ =
          LoaderInstances
              .uri_ResourceRequirement_class_False_True_None_None
              .loadField(__doc.get("class"), __baseUri, __loadingOptions);
    } catch (ValidationException e) {
      class_ = null; // won't be used but prevents compiler from complaining.
      final String __message = "the `class` field is not valid because:";
      __errors.add(new ValidationException(__message, e));
    }
    Object coresMin;

    if (__doc.containsKey("coresMin")) {
      try {
        coresMin =
            LoaderInstances
                .union_of_NullInstance_or_IntegerInstance_or_LongInstance_or_DoubleInstance_or_ExpressionLoader
                .loadField(__doc.get("coresMin"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        coresMin = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `coresMin` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      coresMin = null;
    }
    Object coresMax;

    if (__doc.containsKey("coresMax")) {
      try {
        coresMax =
            LoaderInstances
                .union_of_NullInstance_or_IntegerInstance_or_LongInstance_or_DoubleInstance_or_ExpressionLoader
                .loadField(__doc.get("coresMax"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        coresMax = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `coresMax` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      coresMax = null;
    }
    Object ramMin;

    if (__doc.containsKey("ramMin")) {
      try {
        ramMin =
            LoaderInstances
                .union_of_NullInstance_or_IntegerInstance_or_LongInstance_or_DoubleInstance_or_ExpressionLoader
                .loadField(__doc.get("ramMin"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        ramMin = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `ramMin` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      ramMin = null;
    }
    Object ramMax;

    if (__doc.containsKey("ramMax")) {
      try {
        ramMax =
            LoaderInstances
                .union_of_NullInstance_or_IntegerInstance_or_LongInstance_or_DoubleInstance_or_ExpressionLoader
                .loadField(__doc.get("ramMax"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        ramMax = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `ramMax` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      ramMax = null;
    }
    Object tmpdirMin;

    if (__doc.containsKey("tmpdirMin")) {
      try {
        tmpdirMin =
            LoaderInstances
                .union_of_NullInstance_or_IntegerInstance_or_LongInstance_or_DoubleInstance_or_ExpressionLoader
                .loadField(__doc.get("tmpdirMin"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        tmpdirMin = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `tmpdirMin` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      tmpdirMin = null;
    }
    Object tmpdirMax;

    if (__doc.containsKey("tmpdirMax")) {
      try {
        tmpdirMax =
            LoaderInstances
                .union_of_NullInstance_or_IntegerInstance_or_LongInstance_or_DoubleInstance_or_ExpressionLoader
                .loadField(__doc.get("tmpdirMax"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        tmpdirMax = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `tmpdirMax` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      tmpdirMax = null;
    }
    Object outdirMin;

    if (__doc.containsKey("outdirMin")) {
      try {
        outdirMin =
            LoaderInstances
                .union_of_NullInstance_or_IntegerInstance_or_LongInstance_or_DoubleInstance_or_ExpressionLoader
                .loadField(__doc.get("outdirMin"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        outdirMin = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `outdirMin` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      outdirMin = null;
    }
    Object outdirMax;

    if (__doc.containsKey("outdirMax")) {
      try {
        outdirMax =
            LoaderInstances
                .union_of_NullInstance_or_IntegerInstance_or_LongInstance_or_DoubleInstance_or_ExpressionLoader
                .loadField(__doc.get("outdirMax"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        outdirMax = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `outdirMax` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      outdirMax = null;
    }
    if (!__errors.isEmpty()) {
      throw new ValidationException("Trying 'RecordField'", __errors);
    }
    this.class_ = (ResourceRequirement_class) class_;
    this.coresMin = (Object) coresMin;
    this.coresMax = (Object) coresMax;
    this.ramMin = (Object) ramMin;
    this.ramMax = (Object) ramMax;
    this.tmpdirMin = (Object) tmpdirMin;
    this.tmpdirMax = (Object) tmpdirMax;
    this.outdirMin = (Object) outdirMin;
    this.outdirMax = (Object) outdirMax;
  }
}
