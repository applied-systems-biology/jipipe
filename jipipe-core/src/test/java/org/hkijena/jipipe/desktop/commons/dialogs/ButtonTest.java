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

package org.hkijena.jipipe.desktop.commons.dialogs;

import org.hkijena.jipipe.JIPipe;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.Icon;
import java.awt.GraphicsEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the immutable {@link Button} descriptor used by the modern
 * dialog API (#1341). Boots a JIPipe instance so {@code RESOURCES} icons
 * resolve (mirroring the bootstrap pattern of
 * {@code ColorChooserDialogRefreshTest}, including its headless guard).
 */
class ButtonTest {

    @BeforeAll
    static void boot() {
        try {
            if (GraphicsEnvironment.isHeadless()) {
                Assumptions.assumeTrue(false, "headless environment");
            }
        } catch (Throwable t) {
            Assumptions.assumeTrue(false, "AWT not available: " + t);
        }
        JIPipe.ensureInstance();
    }

    @Test
    void constantsHaveExpectedLabelsAndThemeIcons() {
        assertEquals("OK", Button.OK.getLabel());
        assertEquals("Cancel", Button.CANCEL.getLabel());
        assertEquals("Yes", Button.YES.getLabel());
        assertEquals("No", Button.NO.getLabel());
        for (Button b : new Button[]{Button.OK, Button.CANCEL, Button.YES, Button.NO}) {
            assertTrue(b.getIcon() instanceof Icon,
                    "expected a non-null 16px theme icon for " + b.getLabel());
        }
    }

    @Test
    void constantsAreNotDefault() {
        assertFalse(Button.OK.isDefault());
        assertFalse(Button.CANCEL.isDefault());
        assertFalse(Button.YES.isDefault());
        assertFalse(Button.NO.isDefault());
    }

    @Test
    void customCreatesNewInstanceWithLabelNoIconAndNotDefault() {
        Button a = Button.custom("X");
        Button b = Button.custom("X");
        assertNotSame(a, b, "custom() must create a new instance each call");
        assertEquals("X", a.getLabel());
        assertNull(a.getIcon());
        assertFalse(a.isDefault());
    }

    @Test
    void customCarriesTheGivenIcon() {
        Icon icon = JIPipe.RESOURCES.getIcon16("actions/ok.png");
        Button b = Button.custom("X", icon);
        assertEquals("X", b.getLabel());
        assertSame(icon, b.getIcon());
        assertFalse(b.isDefault());
    }

    @Test
    void asDefaultReturnsADistinctDefaultedCopy() {
        Button original = Button.custom("X");
        Button defaulted = original.asDefault();
        assertNotSame(original, defaulted, "asDefault() must return a different instance");
        assertTrue(defaulted.isDefault());
        assertFalse(original.isDefault(), "asDefault() must not mutate the original");
        assertEquals("X", defaulted.getLabel());
    }
}
