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

import java.util.Set;

/**
 * One fused search result of {@link KnowledgeService#searchKnowledge}.
 *
 * @param entryKey the entry key of the matching corpus entry
 * @param score    the fused score (reciprocal rank fusion over the lanes
 *                 that contributed)
 * @param lanes    the lanes that contributed to the hit: a subset of
 *                 {@link KnowledgeService#LANE_LEXICAL},
 *                 {@link KnowledgeService#LANE_RELATION}, and (from the
 *                 vector task onward) further lane ids
 */
public record KnowledgeResult(String entryKey, double score, Set<String> lanes) {
}
