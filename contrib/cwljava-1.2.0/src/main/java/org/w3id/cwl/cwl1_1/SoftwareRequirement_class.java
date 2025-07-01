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

import org.w3id.cwl.cwl1_1.utils.ValidationException;

public enum SoftwareRequirement_class {
  SOFTWAREREQUIREMENT("SoftwareRequirement");

  private static String[] symbols = new String[] {"SoftwareRequirement"};
  private String docVal;

  private SoftwareRequirement_class(final String docVal) {
    this.docVal = docVal;
  }

  public static SoftwareRequirement_class fromDocumentVal(final String docVal) {
    for(final SoftwareRequirement_class val : SoftwareRequirement_class.values()) {
      if(val.docVal.equals(docVal)) {
        return val;
      }
    }
    throw new ValidationException(String.format("Expected one of %s", SoftwareRequirement_class.symbols, docVal));
  }
}
