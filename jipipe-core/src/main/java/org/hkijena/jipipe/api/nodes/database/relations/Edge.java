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

/**
 * A single directed workflow edge between two node ids: {@code from} runs
 * before {@code to} (i.e. {@code to} is a next step of {@code from}, and
 * {@code from} is a previous step of {@code to}).
 * <p>
 * The edge is identified by the node ids only — never by node instances or
 * graph UUIDs — so that edges mined from different sources (family step
 * graphs, template graphs, a future starter-pattern source) can be merged in
 * the same {@link NodeRelationIndex} and their support (the number of
 * sources/occurrences carrying the edge) can be accumulated.
 * <p>
 * Immutable; usable as a map key.
 *
 * @param from the node id of the predecessor ("runs before")
 * @param to   the node id of the successor ("runs after")
 */
public record Edge(String from, String to) {
}
