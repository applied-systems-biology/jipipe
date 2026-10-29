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

import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;

import java.util.List;

/**
 * The three generalized workflow relations of a node database entry
 * (spec §10.1) - the replacement for the removed
 * {@code JIPipeNodeDatabase.followUps(entry)} /
 * {@code related(entry, max)} API pair.
 *
 * @param similar  entries that do the same kind of thing as the entry -
 *                 ranked by taxonomy tiers (same function → same framework →
 *                 shared tags), with any entry that is also a
 *                 {@link #previous()} or {@link #next()} neighbor excluded so
 *                 the three lists stay disjoint
 * @param previous entries that run <em>before</em> this one in a workflow
 *                 (family predecessors ∪ template edges pointing into it),
 *                 ranked by edge support then node id
 * @param next     entries that run <em>after</em> this one in a workflow
 *                 (family successors ∪ template edges pointing out of it),
 *                 ranked by edge support then node id
 */
public record NodeRelations(List<JIPipeNodeDatabaseEntry> similar,
                            List<JIPipeNodeDatabaseEntry> previous,
                            List<JIPipeNodeDatabaseEntry> next) {
}
