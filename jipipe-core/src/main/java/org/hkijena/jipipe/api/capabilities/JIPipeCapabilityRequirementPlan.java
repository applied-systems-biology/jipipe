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
 * The plan for making one requested capability available: one row per
 * capability that is not available yet (the requested capability itself when
 * it is, and each missing requirement it transitively pulls in), plus the
 * order in which the missing capabilities must be installed.
 * <p>
 * The rows are in discovery order: a requirement is first reached through the
 * depth-first walk of the requesting capability's own requirements, so a
 * capability that is required both directly and indirectly is attributed to
 * the parent it was discovered through. {@code installOrder} is a
 * topological order of the missing capabilities (requirements first),
 * excluding the requested capability itself: it lists what has to be
 * installed to satisfy the request, not the request itself.
 *
 * @param requestedId the capability id the plan was computed for
 * @param rows        one {@link JIPipeRequirementRow} per missing capability,
 *                    discovery order
 * @param installOrder topological order of the missing requirements,
 *                     requirements first
 */
public record JIPipeCapabilityRequirementPlan(String requestedId, List<JIPipeRequirementRow> rows,
                                              List<String> installOrder) {
    public JIPipeCapabilityRequirementPlan {
        rows = List.copyOf(rows);
        installOrder = List.copyOf(installOrder);
    }

    /**
     * One entry of an install plan: a capability that is not available yet,
     * why it is needed, and how it could be obtained.
     *
     * @param capabilityId         the missing capability
     * @param reason               the id of the capability whose requirement
     *                             pull made this row; for the requested
     *                             capability's own row that is the requested
     *                             capability itself
     * @param options              the ranked providers that could satisfy the
     *                             capability, empty when none is registered
     * @param preselectedProviderId the provider id selected by default in the
     *                              UI's equivalent combobox, or null when no
     *                              candidate carries an installable hint
     */
    public record JIPipeRequirementRow(String capabilityId, String reason,
                                       List<JIPipeCapabilityCandidate> options, String preselectedProviderId) {
        public JIPipeRequirementRow {
            options = List.copyOf(options);
        }
    }
}
