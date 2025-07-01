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

public enum CWLVersion {
  DRAFT_2("draft-2"),
  DRAFT_3_DEV1("draft-3.dev1"),
  DRAFT_3_DEV2("draft-3.dev2"),
  DRAFT_3_DEV3("draft-3.dev3"),
  DRAFT_3_DEV4("draft-3.dev4"),
  DRAFT_3_DEV5("draft-3.dev5"),
  DRAFT_3("draft-3"),
  DRAFT_4_DEV1("draft-4.dev1"),
  DRAFT_4_DEV2("draft-4.dev2"),
  DRAFT_4_DEV3("draft-4.dev3"),
  V1_0_DEV4("v1.0.dev4"),
  V1_0("v1.0"),
  V1_1_0_DEV1("v1.1.0-dev1"),
  V1_1("v1.1"),
  V1_2_0_DEV1("v1.2.0-dev1"),
  V1_2_0_DEV2("v1.2.0-dev2"),
  V1_2_0_DEV3("v1.2.0-dev3"),
  V1_2_0_DEV4("v1.2.0-dev4"),
  V1_2_0_DEV5("v1.2.0-dev5"),
  V1_2("v1.2");

  private static String[] symbols = new String[] {"draft-2", "draft-3.dev1", "draft-3.dev2", "draft-3.dev3", "draft-3.dev4", "draft-3.dev5", "draft-3", "draft-4.dev1", "draft-4.dev2", "draft-4.dev3", "v1.0.dev4", "v1.0", "v1.1.0-dev1", "v1.1", "v1.2.0-dev1", "v1.2.0-dev2", "v1.2.0-dev3", "v1.2.0-dev4", "v1.2.0-dev5", "v1.2"};
  private String docVal;

  private CWLVersion(final String docVal) {
    this.docVal = docVal;
  }

  public static CWLVersion fromDocumentVal(final String docVal) {
    for(final CWLVersion val : CWLVersion.values()) {
      if(val.docVal.equals(docVal)) {
        return val;
      }
    }
    throw new ValidationException(String.format("Expected one of %s", CWLVersion.symbols, docVal));
  }
}
