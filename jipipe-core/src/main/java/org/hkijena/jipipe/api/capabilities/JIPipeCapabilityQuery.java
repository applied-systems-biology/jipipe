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

package org.hkijena.jipipe.api.capabilities;

import java.util.List;

/**
 * The result of querying the registry for one capability id. This is the
 * exact shape answered to an agent over MCP: the resolved status, the install
 * hint to act on, and the ranked list of concrete candidates.
 * <p>
 * {@code candidates} is ordered present-first, then by registration order, so
 * the first absent candidate is the one whose hint is surfaced in
 * {@link #install()}.
 *
 * @param id         the capability id that was queried
 * @param status     the resolved {@link JIPipeCapabilityStatus} of the id
 * @param install    the hint of the highest-ranked absent candidate that is
 *                   installable, or a {@code none} hint when there is none
 * @param candidates the ranked providers that satisfy the id, present-first
 */
public record JIPipeCapabilityQuery(String id, JIPipeCapabilityStatus status, JIPipeInstallHint install,
                                    List<JIPipeCapabilityCandidate> candidates) {
    public JIPipeCapabilityQuery {
        candidates = List.copyOf(candidates);
    }
}
