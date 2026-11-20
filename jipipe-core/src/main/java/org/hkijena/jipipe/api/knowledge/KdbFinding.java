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
 * One report about malformed or problematic corpus content. Findings replace
 * exceptions: loading never throws for malformed content, it collects
 * findings, and the integrity gate decides what fails the build (design spec
 * 5.2, 5.8).
 */
public final class KdbFinding {

    /**
     * How severe a finding is. Structural breakage is {@link #ERROR} and
     * fails the build at the integrity gate; semantic trouble is
     * {@link #WARNING} and stays visible without failing.
     */
    public enum Severity {
        ERROR, WARNING
    }

    private final KdbFindingCode code;
    private final String entryKey;
    private final String message;

    /**
     * Creates a finding.
     *
     * @param code     the finding code
     * @param entryKey the affected entry key, or null if no single entry is affected
     * @param message  the human readable message; for value level findings it carries the entry key
     */
    public KdbFinding(KdbFindingCode code, String entryKey, String message) {
        this.code = code;
        this.entryKey = entryKey;
        this.message = message;
    }

    public KdbFindingCode getCode() {
        return code;
    }

    /**
     * The affected entry key, if the finding is about one entry.
     *
     * @return the entry key, or null
     */
    public String getEntryKey() {
        return entryKey;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(code).append(" [").append(code.getSeverity()).append("]");
        if (entryKey != null) {
            builder.append(' ').append(entryKey);
        }
        builder.append(": ").append(message);
        return builder.toString();
    }
}
