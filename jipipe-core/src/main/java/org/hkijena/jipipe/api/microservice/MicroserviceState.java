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

package org.hkijena.jipipe.api.microservice;

/**
 * Lifecycle states for a {@link Microservice}.
 *
 * <p>State transitions:
 * <ul>
 *   <li>{@code Stopped → Starting} (start called)</li>
 *   <li>{@code Starting → Ready} (onStart succeeded)</li>
 *   <li>{@code Starting → Failed} (onStart threw)</li>
 *   <li>{@code Ready → Stopping} (stop called)</li>
 *   <li>{@code Stopping → Stopped} (onStop completed)</li>
 *   <li>{@code Failed → Starting} (retry via start)</li>
 * </ul>
 *
 * <p>There is no {@code Busy} state — "busy" is a transient condition during
 * work execution tracked by the executor, not a lifecycle state. The optional
 * detail string on {@link AbstractMicroservice} can convey nuance.
 */
public enum MicroserviceState {
    Stopped,
    Starting,
    Ready,
    Stopping,
    Failed
}
