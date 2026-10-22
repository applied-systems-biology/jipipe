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

package org.hkijena.jipipe.api.nodes.taxonomy;

/**
 * A name-inference pattern of a taxonomy function: a weighted keyword that
 * is matched as a <em>case-insensitive substring</em> of a discovered
 * node's display name.
 * <p>
 * When a name is scored for inference, the aggregate score of a function is
 * the <em>sum</em> of the weights of all of its keywords that occur in the
 * name; the highest-scoring function wins.
 *
 * @param keyword the keyword to look for in the display name (matched case-insensitively as a substring)
 * @param weight  the contribution of one occurrence of the keyword to the aggregate score (must be {@code > 0})
 */
public record JIPipeNodeTaxonomyInferencePattern(String keyword, float weight) {
}
