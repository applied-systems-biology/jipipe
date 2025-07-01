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

import org.w3id.cwl.cwl1_2.utils.ValidationException;

public enum Union_name {
  UNION("union");

  private static String[] symbols = new String[] {"union"};
  private String docVal;

  private Union_name(final String docVal) {
    this.docVal = docVal;
  }

  public static Union_name fromDocumentVal(final String docVal) {
    for(final Union_name val : Union_name.values()) {
      if(val.docVal.equals(docVal)) {
        return val;
      }
    }
    throw new ValidationException(String.format("Expected one of %s", Union_name.symbols, docVal));
  }
}
