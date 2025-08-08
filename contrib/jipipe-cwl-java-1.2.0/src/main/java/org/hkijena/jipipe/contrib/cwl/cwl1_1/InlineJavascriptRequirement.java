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

package org.hkijena.jipipe.contrib.cwl.cwl1_1;

import org.hkijena.jipipe.contrib.cwl.cwl1_1.utils.Saveable;

/**
* Auto-generated interface for <I>https://w3id.org/cwl/cwl#InlineJavascriptRequirement</I><BR>This interface is implemented by {@link InlineJavascriptRequirementImpl}<BR> <BLOCKQUOTE>
 Indicates that the workflow platform must support inline Javascript expressions.
 If this requirement is not present, the workflow platform must not perform expression
 interpolatation.
  </BLOCKQUOTE>
 */
public interface InlineJavascriptRequirement extends ProcessRequirement, Saveable {
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#InlineJavascriptRequirement/class</I><BR>
   * <BLOCKQUOTE>
   * Always 'InlineJavascriptRequirement'   * </BLOCKQUOTE>
   */

  InlineJavascriptRequirement_class getClass_();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#InlineJavascriptRequirement/expressionLib</I><BR>
   * <BLOCKQUOTE>
   * Additional code fragments that will also be inserted
   * before executing the expression code.  Allows for function definitions that may
   * be called from CWL expressions.
   *    * </BLOCKQUOTE>
   */

  java.util.Optional<java.util.List<String>> getExpressionLib();
}
