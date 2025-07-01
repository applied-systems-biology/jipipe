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
* Auto-generated class implementation for <I>https://w3id.org/cwl/cwl#EnvironmentDef</I><BR> <BLOCKQUOTE>
 Define an environment variable that will be set in the runtime environment
 by the workflow platform when executing the command line tool.  May be the
 result of executing an expression, such as getting a parameter from input.
  </BLOCKQUOTE>
 */
public class EnvironmentDefImpl extends SaveableImpl implements EnvironmentDef {
  private LoadingOptions loadingOptions_ = new LoadingOptionsBuilder().build();
  private java.util.Map<String, Object> extensionFields_ =
      new java.util.HashMap<String, Object>();

  private String envName;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#EnvironmentDef/envName</I><BR>
   * <BLOCKQUOTE>
   * The environment variable name   * </BLOCKQUOTE>
   */

  public String getEnvName() {
    return this.envName;
  }

  private Object envValue;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#EnvironmentDef/envValue</I><BR>
   * <BLOCKQUOTE>
   * The environment variable value   * </BLOCKQUOTE>
   */

  public Object getEnvValue() {
    return this.envValue;
  }

  /**
   * Used by {@link org.w3id.cwl.cwl1_1.utils.RootLoader} to construct instances of EnvironmentDefImpl.
   *
   * @param __doc_            Document fragment to load this record object from (presumably a
                              {@link java.util.Map}).
   * @param __baseUri_        Base URI to generate child document IDs against.
   * @param __loadingOptions  Context for loading URIs and populating objects.
   * @param __docRoot_        ID at this position in the document (if available) (maybe?)
   * @throws ValidationException If the document fragment is not a {@link java.util.Map}
   *                             or validation of fields fails.
   */
  public EnvironmentDefImpl(
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
      throw new ValidationException("EnvironmentDefImpl called on non-map");
    }
    final java.util.Map<String, Object> __doc = (java.util.Map<String, Object>) __doc_;
    final java.util.List<ValidationException> __errors =
        new java.util.ArrayList<ValidationException>();
    if (__loadingOptions != null) {
      this.loadingOptions_ = __loadingOptions;
    }
    String envName;
    try {
      envName =
          LoaderInstances
              .StringInstance
              .loadField(__doc.get("envName"), __baseUri, __loadingOptions);
    } catch (ValidationException e) {
      envName = null; // won't be used but prevents compiler from complaining.
      final String __message = "the `envName` field is not valid because:";
      __errors.add(new ValidationException(__message, e));
    }
    Object envValue;
    try {
      envValue =
          LoaderInstances
              .union_of_StringInstance_or_ExpressionLoader
              .loadField(__doc.get("envValue"), __baseUri, __loadingOptions);
    } catch (ValidationException e) {
      envValue = null; // won't be used but prevents compiler from complaining.
      final String __message = "the `envValue` field is not valid because:";
      __errors.add(new ValidationException(__message, e));
    }
    if (!__errors.isEmpty()) {
      throw new ValidationException("Trying 'RecordField'", __errors);
    }
    this.envName = (String) envName;
    this.envValue = (Object) envValue;
  }
}
