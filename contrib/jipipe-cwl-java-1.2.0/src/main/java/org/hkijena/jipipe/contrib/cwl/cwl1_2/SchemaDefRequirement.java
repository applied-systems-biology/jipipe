/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.contrib.cwl.cwl1_2;

import org.hkijena.jipipe.contrib.cwl.cwl1_2.utils.Saveable;

/**
* Auto-generated interface for <I>https://w3id.org/cwl/cwl#SchemaDefRequirement</I><BR>This interface is implemented by {@link SchemaDefRequirementImpl}<BR> <BLOCKQUOTE>
 This field consists of an array of type definitions which must be used when
 interpreting the `inputs` and `outputs` fields.  When a `type` field
 contains a IRI, the implementation must check if the type is defined in
 `schemaDefs` and use that definition.  If the type is not found in
 `schemaDefs`, it is an error.  The entries in `schemaDefs` must be
 processed in the order listed such that later schema definitions may refer
 to earlier schema definitions.
 
 - **Type definitions are allowed for `enum` and `record` types only.**
 - Type definitions may be shared by defining them in a file and then
   `$include`-ing them in the `types` field.
 - A file can contain a list of type definitions
  </BLOCKQUOTE>
 */
public interface SchemaDefRequirement extends ProcessRequirement, Saveable {
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#SchemaDefRequirement/class</I><BR>
   * <BLOCKQUOTE>
   * Always 'SchemaDefRequirement'   * </BLOCKQUOTE>
   */

  SchemaDefRequirement_class getClass_();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#SchemaDefRequirement/types</I><BR>
   * <BLOCKQUOTE>
   * The list of type definitions.   * </BLOCKQUOTE>
   */

  java.util.List<Object> getTypes();
}
