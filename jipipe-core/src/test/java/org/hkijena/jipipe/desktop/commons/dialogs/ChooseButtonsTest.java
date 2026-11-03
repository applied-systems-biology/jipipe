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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import java.awt.GraphicsEnvironment;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The button-set rules of {@link JIPipeDesktopDialogs#choose} (modern dialog
 * API, #1341): an implicit Cancel is appended exactly once, an explicit
 * {@link Button#CANCEL} is not duplicated, and the chosen button is returned
 * as the same instance the caller passed.
 * <p>
 * Headless-safe: a capturing {@link DialogPresenter} records the constructed
 * dialog instead of showing it (mirroring {@code ColorChooserDialogRefreshTest}).
 */
class ChooseButtonsTest {

    /**
     * The dialog presented during the current test call, for assertions.
     */
    private JIPipeDesktopDialog captured;

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

    @BeforeEach
    void installCapturingPresenter() {
        // Record the dialog and hand it back unchanged; no decision is made
        // unless a test installs a deciding presenter.
        JIPipeDesktopDialogs.setPresenter(dialog -> {
            captured = dialog;
            return dialog;
        });
    }

    @AfterEach
    void restoreDefaultPresenter() {
        // Don't leak the test presenter into other test classes in the same JVM.
        JIPipeDesktopDialogs.setPresenter(new ModalPresenter());
        captured = null;
    }

    @Test
    void implicitCancelAppendedExactlyOnce() {
        JIPipeDesktopDialogs.choose(null, "t", "m", Button.custom("A"), Button.custom("B"));
        List<JButton> buttons = captured.getActionButtons();
        assertEquals(3, buttons.size(),
                "two options must yield exactly three action buttons (options + one implicit Cancel)");
        assertEquals("Cancel", buttons.get(2).getText(),
                "the implicit Cancel must come last");
        assertEquals(1, buttons.stream().filter(b -> "Cancel".equals(b.getText())).count(),
                "exactly one Cancel button must be present");
    }

    @Test
    void explicitCancelNotDuplicated() {
        JIPipeDesktopDialogs.choose(null, "t", "m", Button.custom("A"), Button.CANCEL);
        List<JButton> buttons = captured.getActionButtons();
        assertEquals(2, buttons.size(),
                "an explicit CANCEL must not be duplicated by the implicit one");
        assertEquals("Cancel", buttons.get(1).getText(),
                "the caller's CANCEL keeps its position");
    }

    @Test
    void zeroOptionsThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> JIPipeDesktopDialogs.choose(null, "t", "m"));
    }

    @Test
    void chosenButtonIsSameInstance() {
        Button custom = Button.custom("Custom");

        JIPipeDesktopDialogs.setPresenter(dialog -> {
            captured = dialog;
            dialog.getActionButtons().get(0).doClick();
            return dialog;
        });
        Optional<Button> chosen = JIPipeDesktopDialogs.choose(null, "t", "m", custom);
        assertTrue(chosen.isPresent(), "clicking the option must set the result");
        assertSame(custom, chosen.get(),
                "the facade must return the exact Button instance the caller passed");
        assertFalse(chosen.get() == Button.CANCEL, "a chosen option is never the Cancel descriptor");
    }
}
