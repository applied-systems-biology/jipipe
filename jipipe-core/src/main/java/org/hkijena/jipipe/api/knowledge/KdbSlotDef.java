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
 * One slot of a KDB entry kind: schema as code, never serialized (design
 * spec 5.1). The {@link #name()} is a stable string literal: it is the key
 * authors write under {@code slots} in the YAML wire format and the key
 * consumers read from {@link KdbEntry#getSlotValues()}, so it must never be
 * derived from code.
 *
 * @param name        the slot name as written in the YAML wire format
 * @param type        the value type every value of this slot must narrow to
 * @param cardinality how many values the slot can carry
 */
public record KdbSlotDef(String name, KdbValueType type, KdbCardinality cardinality) {
}
