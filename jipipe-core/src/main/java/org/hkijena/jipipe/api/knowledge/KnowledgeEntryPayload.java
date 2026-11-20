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

package org.hkijena.jipipe.api.knowledge;

/**
 * The result of a knowledge entry lookup (design spec 5.5): the structured
 * corpus entry plus the agent facing rendered text. The rendered part carries
 * the registry sourced node facts resolved per lookup, so two lookups of the
 * same entry can render differently when the registry state changed between
 * them: that is the drift kill working as intended.
 *
 * @param entry    the structured corpus entry
 * @param rendered the rendered agent facing text
 */
public record KnowledgeEntryPayload(KdbEntry entry, String rendered) {
}
