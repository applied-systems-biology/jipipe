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
* Auto-generated interface for <I>https://w3id.org/cwl/salad#RecordField</I><BR>This interface is implemented by {@link RecordFieldImpl}<BR> <BLOCKQUOTE>
 A field of a record. </BLOCKQUOTE>
 */
public interface RecordField extends Documented, Saveable {
  /**
   * Getter for property <I>https://w3id.org/cwl/salad#RecordField/name</I><BR>
   * <BLOCKQUOTE>
   * The name of the field
   *    * </BLOCKQUOTE>
   */

  String getName();
  /**
   * Getter for property <I>https://w3id.org/cwl/salad#Documented/doc</I><BR>
   * <BLOCKQUOTE>
   * A documentation string for this object, or an array of strings which should be concatenated.   * </BLOCKQUOTE>
   */

  Object getDoc();
  /**
   * Getter for property <I>https://w3id.org/cwl/salad#type</I><BR>
   * <BLOCKQUOTE>
   * The field type. If it is an array, it indicates
   * that the field type is a union type of its elements.
   * Its elements may be duplicated.
   *    * </BLOCKQUOTE>
   */

  Object getType();
}
