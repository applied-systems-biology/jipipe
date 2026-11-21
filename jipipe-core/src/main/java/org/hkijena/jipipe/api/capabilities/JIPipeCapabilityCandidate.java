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
 * One entry in a capability query's ranked candidate list: a concrete
 * provider that can satisfy the queried capability.
 * <p>
 * Candidates are ordered present-first, then by registration order, so the
 * first absent candidate is the one whose hint is surfaced as the query's
 * {@link JIPipeCapabilityQuery#install()}.
 *
 * @param providerId    the provider's identity (kind prefix applied to its
 *                      first provided id)
 * @param displayName   the human-readable name shown in the UI, or null to
 *                      fall back to {@code providerId}
 * @param hint          how to install this provider when absent, or null
 * @param present       true when the provider's presence probe reports
 *                      {@link JIPipeCapabilityStatus.State#AVAILABLE}
 */
public record JIPipeCapabilityCandidate(String providerId, String displayName, JIPipeInstallHint hint, boolean present) {
}
