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

package org.hkijena.jipipe.api.nodes.database;

/**
 * The shared scored-result shape of both node database searches
 * ({@link JIPipeEnhancedNodeDatabaseSearch} and {@link JIPipeAINodeDatabaseSearch}):
 * one entry of a result list together with the relevance score the search
 * computed for it and whether the entry was pinned for the query.
 * <p>
 * The score semantics are search-specific: the enhanced search reports its
 * fused distance-like final score (lower is better; the exact-name and
 * name-prefix ladder tiers keep their sentinel scores), while the AI search
 * reports the cosine similarity after the classification penalties (higher
 * is better; exactly the value its ranking sorts by). Consumers (the merged
 * ranking of #1409) must not mix scores across searches.
 */
public record ScoredNodeDatabaseEntry(JIPipeNodeDatabaseEntry entry, double score, boolean pinned) {
}
