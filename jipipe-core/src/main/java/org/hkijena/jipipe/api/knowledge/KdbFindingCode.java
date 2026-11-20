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
 * The closed set of KDB finding codes (design spec 5.1, 5.2, 5.8). A finding
 * reports malformed or problematic corpus content instead of throwing: the
 * loader and the service produce findings, the integrity gate decides what
 * fails the build. The list is closed and guarded by the launcher gates, so
 * codes must be added or removed only together with the documentation table.
 */
public enum KdbFindingCode {

    /**
     * The file cannot be classified into the corpus taxonomy: the kind YAML
     * field or the kind directory segment is unknown, the domain directory
     * segment is unknown, or the path does not match
     * {@code <domain>/<kind-directory>/<name>.yaml}.
     */
    UNKNOWN_KIND(KdbFinding.Severity.ERROR),

    /**
     * The {@code slots} mapping contains a key that the kind's slot schema
     * does not declare. The unknown value is dropped; the entry still loads.
     */
    UNKNOWN_SLOT(KdbFinding.Severity.ERROR),

    /**
     * A value cannot narrow to its declared type: a non string for a string
     * shaped type, an unparseable YAML file, a mapping where a scalar was
     * expected, or a non version string for a version.
     */
    TYPE_MISMATCH(KdbFinding.Severity.ERROR),

    /**
     * A SINGLE slot received a list of values instead of exactly one.
     */
    CARDINALITY_VIOLATION(KdbFinding.Severity.ERROR),

    /**
     * The {@code kind} YAML field disagrees with the kind directory segment
     * of the path. The file is skipped because its classification is
     * contradictory.
     */
    KIND_PATH_MISMATCH(KdbFinding.Severity.ERROR),

    /**
     * The entry has no usable {@code title} (missing, empty, or not a string).
     * The entry still loads without a title.
     */
    MISSING_TITLE(KdbFinding.Severity.ERROR),

    /**
     * Two loaded entries carry the same entry key. The first occurrence is
     * kept, later ones are dropped.
     */
    DUPLICATE_ENTRY_KEY(KdbFinding.Severity.ERROR),

    /**
     * A NODE_REF value does not resolve against the live node registry.
     * Produced by the registry gates, not by the loader.
     */
    UNRESOLVED_NODE_REF(KdbFinding.Severity.ERROR),

    /**
     * An ENTRY_REF value does not resolve to a loaded entry key.
     */
    UNRESOLVED_ENTRY_REF(KdbFinding.Severity.WARNING),

    /**
     * A {@code properties} key shadows a schema slot of the kind.
     */
    PROPERTY_SHADOWS_SLOT(KdbFinding.Severity.WARNING),

    /**
     * The corpus root contains no entry files (or does not exist).
     */
    EMPTY_CORPUS(KdbFinding.Severity.WARNING);

    private final KdbFinding.Severity severity;

    KdbFindingCode(KdbFinding.Severity severity) {
        this.severity = severity;
    }

    /**
     * The severity of this code. Structural breakage is {@link KdbFinding.Severity#ERROR}
     * and fails the build at the integrity gate; semantic trouble is
     * {@link KdbFinding.Severity#WARNING} and must stay visible without failing.
     *
     * @return the severity
     */
    public KdbFinding.Severity getSeverity() {
        return severity;
    }
}
