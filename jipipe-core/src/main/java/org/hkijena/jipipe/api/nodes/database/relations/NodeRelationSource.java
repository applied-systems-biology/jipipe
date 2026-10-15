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

package org.hkijena.jipipe.api.nodes.database.relations;

import java.util.function.Consumer;

/**
 * A pluggable provider of workflow edges that feed the
 * {@link NodeRelationIndex} (spec §10.2).
 * <p>
 * The point of this seam is that the index itself is source-agnostic: it only
 * knows node ids and directions. A new source (e.g. the future
 * starter-pattern source, an LLM source, or #1333's repository templates)
 * attaches by implementing this interface and contributing its edges — no
 * change to the index or the database's relation API is required.
 * <p>
 * <b>Resolution is the source's responsibility.</b> A production source
 * resolves every edge endpoint against the live node registry before handing
 * the edge to the sink: an endpoint that no longer resolves (a stale id) must
 * be <em>skipped with a {@code logger.warn}</em>, never propagated and never
 * thrown — the index rebuild and every {@code relations()} call must stay
 * non-fatal even when a template references a node that was removed.
 *
 * @see FamilyRelationSource
 * @see TemplateRelationSource
 */
public interface NodeRelationSource {

    /**
     * Contributes this source's workflow edges to the given sink. Each edge's
     * endpoints must already be resolved node ids (see the class Javadoc).
     *
     * @param sink receives each edge the source mines; may be called with the
     *             same edge more than once — repeated occurrences are what
     *             give an edge its support ranking
     */
    void contribute(Consumer<Edge> sink);
}
