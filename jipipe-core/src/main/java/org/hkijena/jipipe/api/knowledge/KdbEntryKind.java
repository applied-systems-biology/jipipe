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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The closed set of KDB entry kinds, each with its ordered slot list: schema
 * as code, extendable only through a code change (design spec 5.1).
 * <p>
 * {@code domain} is deliberately not a slot of any kind: it is derived from
 * the file's folder, and a slot that restates the folder would be the shadow
 * the {@code properties} rule warns about.
 * <p>
 * Both the kind id (the value authors write into the {@code kind} YAML field)
 * and the directory name (the second path segment of an entry, which
 * pluralizes the kind id) are stable string literals: they are persisted in
 * the corpus and in entry keys, so they must never be derived from code.
 */
public enum KdbEntryKind {

    /**
     * Explains a JIPipe or image analysis concept (the what/when layer).
     */
    CONCEPT("concept", "concepts", List.of(
            new KdbSlotDef("prerequisites", KdbValueType.ENTRY_REF, KdbCardinality.MULTI),
            new KdbSlotDef("relatedConcepts", KdbValueType.ENTRY_REF, KdbCardinality.MULTI),
            new KdbSlotDef("nodes", KdbValueType.NODE_REF, KdbCardinality.MULTI))),

    /**
     * A workflow recipe: which nodes in what order with which observed
     * parameter values (the how layer).
     */
    PATTERN("pattern", "patterns", List.of(
            new KdbSlotDef("problem", KdbValueType.TEXT, KdbCardinality.SINGLE),
            new KdbSlotDef("nodeChain", KdbValueType.NODE_REF, KdbCardinality.MULTI),
            new KdbSlotDef("parameterEvidence", KdbValueType.TEXT, KdbCardinality.MULTI),
            new KdbSlotDef("elaborates", KdbValueType.ENTRY_REF, KdbCardinality.MULTI),
            new KdbSlotDef("crossRefs", KdbValueType.ENTRY_REF, KdbCardinality.MULTI))),

    /**
     * A common error with symptom, cause, and fix.
     */
    MISTAKE("mistake", "mistakes", List.of(
            new KdbSlotDef("symptom", KdbValueType.TEXT, KdbCardinality.SINGLE),
            new KdbSlotDef("cause", KdbValueType.TEXT, KdbCardinality.SINGLE),
            new KdbSlotDef("fix", KdbValueType.TEXT, KdbCardinality.SINGLE),
            new KdbSlotDef("nodes", KdbValueType.NODE_REF, KdbCardinality.MULTI),
            new KdbSlotDef("crossRefs", KdbValueType.ENTRY_REF, KdbCardinality.MULTI))),

    /**
     * How to choose between options.
     */
    DECISION_TREE("decision-tree", "decision-trees", List.of(
            new KdbSlotDef("question", KdbValueType.TEXT, KdbCardinality.SINGLE),
            new KdbSlotDef("options", KdbValueType.ENTRY_REF, KdbCardinality.MULTI),
            new KdbSlotDef("nodes", KdbValueType.NODE_REF, KdbCardinality.MULTI))),

    /**
     * Knowledge about specific nodes that the node registry cannot carry
     * (ordering constraints, gotchas).
     */
    NODE_NOTE("node-note", "node-notes", List.of(
            new KdbSlotDef("nodes", KdbValueType.NODE_REF, KdbCardinality.MULTI),
            new KdbSlotDef("caveat", KdbValueType.TEXT, KdbCardinality.SINGLE)));

    private final String id;
    private final String directoryName;
    private final List<KdbSlotDef> slots;

    KdbEntryKind(String id, String directoryName, List<KdbSlotDef> slots) {
        this.id = id;
        this.directoryName = directoryName;
        this.slots = List.copyOf(slots);
    }

    /**
     * The stable kind id, identical to the value authors write into the
     * {@code kind} YAML field.
     *
     * @return the kind id
     */
    public String getId() {
        return id;
    }

    /**
     * The directory name that pluralizes this kind, identical to the second
     * path segment of entries of this kind.
     *
     * @return the directory name
     */
    public String getDirectoryName() {
        return directoryName;
    }

    /**
     * The ordered slot list of this kind.
     *
     * @return an unmodifiable slot list
     */
    public List<KdbSlotDef> getSlots() {
        return slots;
    }

    /**
     * Looks a slot up by name.
     *
     * @param name the slot name
     * @return the slot definition, or empty if this kind does not declare the slot
     */
    public Optional<KdbSlotDef> getSlot(String name) {
        for (KdbSlotDef slotDef : slots) {
            if (slotDef.name().equals(name)) {
                return Optional.of(slotDef);
            }
        }
        return Optional.empty();
    }

    /**
     * Looks a kind up by its id (the {@code kind} YAML field value).
     *
     * @param id the kind id
     * @return the kind, or empty if the id is not part of the closed set
     */
    public static Optional<KdbEntryKind> byId(String id) {
        for (KdbEntryKind kind : values()) {
            if (kind.id.equals(id)) {
                return Optional.of(kind);
            }
        }
        return Optional.empty();
    }

    /**
     * Looks a kind up by its directory name (the second path segment).
     *
     * @param directoryName the directory name
     * @return the kind, or empty if the directory name is not part of the closed set
     */
    public static Optional<KdbEntryKind> byDirectoryName(String directoryName) {
        for (KdbEntryKind kind : values()) {
            if (kind.directoryName.equals(directoryName)) {
                return Optional.of(kind);
            }
        }
        return Optional.empty();
    }
}
