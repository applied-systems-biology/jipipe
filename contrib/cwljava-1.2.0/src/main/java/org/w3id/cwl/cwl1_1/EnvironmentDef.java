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

import org.w3id.cwl.cwl1_1.utils.Saveable;

/**
* Auto-generated interface for <I>https://w3id.org/cwl/cwl#EnvironmentDef</I><BR>This interface is implemented by {@link EnvironmentDefImpl}<BR> <BLOCKQUOTE>
 Define an environment variable that will be set in the runtime environment
 by the workflow platform when executing the command line tool.  May be the
 result of executing an expression, such as getting a parameter from input.
  </BLOCKQUOTE>
 */
public interface EnvironmentDef extends Saveable {
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#EnvironmentDef/envName</I><BR>
   * <BLOCKQUOTE>
   * The environment variable name   * </BLOCKQUOTE>
   */

  String getEnvName();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#EnvironmentDef/envValue</I><BR>
   * <BLOCKQUOTE>
   * The environment variable value   * </BLOCKQUOTE>
   */

  Object getEnvValue();
}
