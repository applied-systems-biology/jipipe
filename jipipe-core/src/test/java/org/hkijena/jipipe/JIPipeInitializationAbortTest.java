/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe;

import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.JIPipeServiceState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Pins the headless-runner guard {@link JIPipe#abortIfInitializationFailed}.
 * <p>
 * Only the <em>no-abort</em> direction is unit-pinned here: the abort itself
 * calls {@link System#exit(int)} through the static seam
 * {@code terminateForFailedInitialization}, which would kill the test JVM. The
 * exit path is exercised by the launcher's CLI tests.
 */
class JIPipeInitializationAbortTest {

    /**
     * A real (but uninitialized) service whose state answer is forced. Construction
     * does not initialize anything and touches no Swing state.
     */
    private static JIPipeService serviceWithState(JIPipeServiceState state) {
        return new JIPipeService() {
            @Override
            public JIPipeServiceState getState() {
                return state;
            }
        };
    }

    @Test
    void initializedServiceDoesNotAbort() {
        assertDoesNotThrow(() -> JIPipe.abortIfInitializationFailed(serviceWithState(JIPipeServiceState.Initialized)),
                "a successfully initialized service must let the runner continue");
    }

    @Test
    void nullServiceDoesNotAbort() {
        assertDoesNotThrow(() -> JIPipe.abortIfInitializationFailed(null),
                "a runner without a service (e.g. already instantiated) must not be aborted");
    }
}