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
 * One one-hop ENTRY_REF neighbor of a corpus entry, with the relation label
 * and the direction the relation was declared in (design spec 5.4).
 *
 * @param entryKey  the entry key of the neighbor
 * @param relation  the relation label: the ENTRY_REF slot name for an
 *                  outgoing edge, the inverse relation for an incoming edge
 * @param direction whether the relation is declared by the queried entry
 *                  itself or by the neighbor
 */
public record RelatedEntry(String entryKey, String relation, Direction direction) {

    /**
     * The direction of a relation relative to the queried entry.
     */
    public enum Direction {

        /**
         * The relation is declared by the queried entry itself: the neighbor
         * is the value of one of its ENTRY_REF slots ("declared here"). The
         * label is the declared slot name.
         */
        OUTGOING,

        /**
         * The relation is declared by the neighbor, whose ENTRY_REF slot
         * points at the queried entry ("declared on the target"). The label
         * is the inverse of the declared relation.
         */
        INCOMING
    }
}
