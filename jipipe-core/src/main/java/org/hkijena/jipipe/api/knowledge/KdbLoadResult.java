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
import java.util.Collections;
import java.util.List;

/**
 * The outcome of loading a KDB corpus: the entries that could be built plus
 * the findings that describe everything malformed (design spec 5.2). Loading
 * never throws for malformed content, so this value is always the complete
 * result.
 */
public final class KdbLoadResult {

    private final List<KdbEntry> entries;
    private final List<KdbFinding> findings;

    /**
     * Creates a load result. Called by the loader; both lists are copied into
     * unmodifiable structures.
     *
     * @param entries  the loaded entries
     * @param findings the findings describing malformed or problematic content
     */
    public KdbLoadResult(List<KdbEntry> entries, List<KdbFinding> findings) {
        this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
        this.findings = Collections.unmodifiableList(new ArrayList<>(findings));
    }

    /**
     * The loaded entries.
     *
     * @return an unmodifiable list of entries
     */
    public List<KdbEntry> getEntries() {
        return entries;
    }

    /**
     * All findings.
     *
     * @return an unmodifiable list of findings
     */
    public List<KdbFinding> getFindings() {
        return findings;
    }

    /**
     * The findings of one code.
     *
     * @param code the finding code
     * @return an unmodifiable list of findings with this code
     */
    public List<KdbFinding> getFindings(KdbFindingCode code) {
        List<KdbFinding> result = new ArrayList<>();
        for (KdbFinding finding : findings) {
            if (finding.getCode() == code) {
                result.add(finding);
            }
        }
        return result;
    }

    /**
     * Whether any finding has ERROR severity: the condition the integrity
     * gate fails the build on.
     *
     * @return true if there is at least one ERROR finding
     */
    public boolean hasErrors() {
        for (KdbFinding finding : findings) {
            if (finding.getCode().getSeverity() == KdbFinding.Severity.ERROR) {
                return true;
            }
        }
        return false;
    }
}
