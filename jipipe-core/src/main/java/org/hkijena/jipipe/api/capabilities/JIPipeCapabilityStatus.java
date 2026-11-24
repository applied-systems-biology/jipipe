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
 * The resolved state of one capability id at a point in time.
 * <p>
 * Status is never persisted: the registry derives it on demand from the
 * registered providers' presence probes and the requirement graph, so a
 * provider that appears or disappears between two queries changes the status
 * without restarting anything.
 *
 * @param id         the capability id the status was resolved for
 * @param state      the resolved state
 * @param blockedBy  the deepest missing requirements, empty unless the state is {@link State#BLOCKED}
 * @param providedBy the provider ids whose presence probe currently reports {@link State#AVAILABLE},
 *                   empty when no provider is present
 */
public record JIPipeCapabilityStatus(String id, State state, List<String> blockedBy, List<String> providedBy) {
    /**
     * The states a capability can be in.
     */
    public enum State {
        /**
         * At least one present provider satisfies the capability and all of its
         * requirements resolve to available.
         */
        AVAILABLE,
        /**
         * A provider for the capability can be installed or configured (it
         * carries an install or setup descriptor), but is not present right
         * now. Absent providers without a descriptor report MISSING.
         */
        NEEDS_SETUP,
        /**
         * The capability's requirements include a chain that cannot be
         * satisfied; {@code blockedBy} names the deepest missing capabilities.
         */
        BLOCKED,
        /**
         * No provider can satisfy the capability.
         */
        MISSING,
        /**
         * A probe-side value only: a provider's presence probe returns this
         * when the provider is not present. It never appears as a resolved
         * status; a capability without a present provider resolves to
         * {@link #NEEDS_SETUP} or {@link #MISSING}.
         */
        UNAVAILABLE
    }
}
