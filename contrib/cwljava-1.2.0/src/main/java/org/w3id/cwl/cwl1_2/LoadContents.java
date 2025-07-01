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

import org.w3id.cwl.cwl1_2.utils.Saveable;

/**
* Auto-generated interface for <I>https://w3id.org/cwl/cwl#LoadContents</I><BR>
 */
public interface LoadContents extends Saveable {
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#LoadContents/loadContents</I><BR>
   * <BLOCKQUOTE>
   * Only valid when `type: File` or is an array of `items: File`.
   * 
   * If true, the file (or each file in the array) must be a UTF-8
   * text file 64 KiB or smaller, and the implementation must read
   * the entire contents of the file (or file array) and place it
   * in the `contents` field of the File object for use by
   * expressions.  If the size of the file is greater than 64 KiB,
   * the implementation must raise a fatal error.
   *    * </BLOCKQUOTE>
   */

  java.util.Optional<Boolean> getLoadContents();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#LoadContents/loadListing</I><BR>
   * <BLOCKQUOTE>
   * Only valid when `type: Directory` or is an array of `items: Directory`.
   * 
   * Specify the desired behavior for loading the `listing` field of
   * a Directory object for use by expressions.
   * 
   * The order of precedence for loadListing is:
   * 
   *   1. `loadListing` on an individual parameter
   *   2. Inherited from `LoadListingRequirement`
   *   3. By default: `no_listing`
   *    * </BLOCKQUOTE>
   */

  java.util.Optional<LoadListingEnum> getLoadListing();
}
