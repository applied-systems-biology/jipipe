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

package org.hkijena.jipipe.desktop.commons.components.layouts;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JIPipeDesktopColumnConstraintsTest {

    @Test
    void defaultSpan() {
        JIPipeDesktopColumnConstraints c = new JIPipeDesktopColumnConstraints(6);
        assertEquals(6, c.getEffectiveSpan(JIPipeDesktopBreakpoint.XL));
        assertEquals(6, c.getEffectiveSpan(JIPipeDesktopBreakpoint.XS));
    }

    @Test
    void withSpanOverride() {
        JIPipeDesktopColumnConstraints c = new JIPipeDesktopColumnConstraints(3)
                .withSpan(JIPipeDesktopBreakpoint.MD, 6)
                .withSpan(JIPipeDesktopBreakpoint.SM, 12);
        assertEquals(6, c.getEffectiveSpan(JIPipeDesktopBreakpoint.XL));
        assertEquals(6, c.getEffectiveSpan(JIPipeDesktopBreakpoint.MD));
        assertEquals(12, c.getEffectiveSpan(JIPipeDesktopBreakpoint.SM));
        assertEquals(3, c.getEffectiveSpan(JIPipeDesktopBreakpoint.XS));
    }

    @Test
    void spanResolutionWalksDownToFirstOverride() {
        JIPipeDesktopColumnConstraints c = new JIPipeDesktopColumnConstraints(3)
                .withSpan(JIPipeDesktopBreakpoint.MD, 6);
        assertEquals(6, c.getEffectiveSpan(JIPipeDesktopBreakpoint.MD));
        assertEquals(3, c.getEffectiveSpan(JIPipeDesktopBreakpoint.XS));
    }

    @Test
    void defaultOffset() {
        JIPipeDesktopColumnConstraints c = new JIPipeDesktopColumnConstraints(6);
        assertEquals(0, c.getEffectiveOffset(JIPipeDesktopBreakpoint.XL));
    }

    @Test
    void withOffset() {
        JIPipeDesktopColumnConstraints c = new JIPipeDesktopColumnConstraints(6)
                .withOffset(3);
        assertEquals(3, c.getEffectiveOffset(JIPipeDesktopBreakpoint.XL));
        assertEquals(3, c.getEffectiveOffset(JIPipeDesktopBreakpoint.XS));
    }

    @Test
    void withOffsetOverride() {
        JIPipeDesktopColumnConstraints c = new JIPipeDesktopColumnConstraints(6)
                .withOffset(3)
                .withOffset(JIPipeDesktopBreakpoint.SM, 0);
        assertEquals(0, c.getEffectiveOffset(JIPipeDesktopBreakpoint.XL));
        assertEquals(0, c.getEffectiveOffset(JIPipeDesktopBreakpoint.SM));
        assertEquals(3, c.getEffectiveOffset(JIPipeDesktopBreakpoint.XS));
    }

    @Test
    void immutable() {
        JIPipeDesktopColumnConstraints c = new JIPipeDesktopColumnConstraints(6);
        JIPipeDesktopColumnConstraints c2 = c.withSpan(JIPipeDesktopBreakpoint.SM, 12);
        assertNotSame(c, c2);
        assertEquals(6, c.getEffectiveSpan(JIPipeDesktopBreakpoint.SM));
        assertEquals(12, c2.getEffectiveSpan(JIPipeDesktopBreakpoint.SM));
    }
}
