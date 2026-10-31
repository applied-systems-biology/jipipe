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

package org.hkijena.jipipe.desktop.commons.components.slider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Pins the {@link JIPipeDesktopNumberSlider} static math (the source of truth
 * the control uses), so the control's behavior is headless-testable (#1339
 * pass 2, S6). The slider render itself is verified by the user in the
 * running app (no Swing-structure tests).
 */
class NumberSliderMathTest {
    @Test
    void snapRoundsToStepAndClamps() {
        assertEquals(0.37, JIPipeDesktopNumberSlider.snap(0.374, 0, 1, 0.01), 1e-9);
        assertEquals(1.0, JIPipeDesktopNumberSlider.snap(0.999, 0, 1, 0.01), 1e-9);  // clamp high
        assertEquals(0.0, JIPipeDesktopNumberSlider.snap(-0.5, 0, 1, 0.01), 1e-9);   // clamp low
        // Ruling: snap is round-to-nearest (consistent with the 0.374 -> 0.37
        // case above, which ceil/floor cannot both satisfy); the plan's
        // originally-pinned 3.0 was a typo for the nearest tick, 2.0.
        assertEquals(2.0, JIPipeDesktopNumberSlider.snap(2.4, 0, 10, 1.0), 1e-9);    // snap to step 1
    }

    @Test
    void fractionRoundTrip() {
        double v = 0.37;
        assertEquals(v, JIPipeDesktopNumberSlider.valueFromFraction(
                JIPipeDesktopNumberSlider.fractionFromValue(v, 0, 1), 0, 1, 0.01), 1e-9);
        assertEquals(0.0, JIPipeDesktopNumberSlider.valueFromFraction(0.0, 0, 1, 0.01), 1e-9);
        assertEquals(1.0, JIPipeDesktopNumberSlider.valueFromFraction(1.0, 0, 1, 0.01), 1e-9);
    }

    @Test
    void fineStepIsStepOverTen() {
        assertEquals(0.001, JIPipeDesktopNumberSlider.fineStep(0.01), 1e-9);
    }

    @Test
    void parseCommitValidInvalidEmpty() {
        assertEquals(0.5, JIPipeDesktopNumberSlider.parseCommit("0.5", 0, 1, 0.01), 1e-9);
        assertEquals(1.0, JIPipeDesktopNumberSlider.parseCommit("5", 0, 1, 0.01), 1e-9);  // clamp
        assertNull(JIPipeDesktopNumberSlider.parseCommit("", 0, 1, 0.01));
        assertNull(JIPipeDesktopNumberSlider.parseCommit("abc", 0, 1, 0.01));
    }

    @Test
    void formatTrimsTrailingZeros() {
        assertEquals("0.5", JIPipeDesktopNumberSlider.format(0.5, 2));
        assertEquals("1", JIPipeDesktopNumberSlider.format(1.0, 2));
    }
}
