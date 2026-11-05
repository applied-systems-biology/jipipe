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
import javax.swing.JComboBox;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        assertEquals("Cancel", buttons.get(0).getText(),
                "the implicit Cancel must be left-most (the button order contract)");
        assertEquals(1, buttons.stream().filter(b -> "Cancel".equals(b.getText())).count(),
                "exactly one Cancel button must be present");
    }

    @Test
    void explicitCancelNotDuplicated() {
        JIPipeDesktopDialogs.choose(null, "t", "m", Button.custom("A"), Button.CANCEL);
        List<JButton> buttons = captured.getActionButtons();
        assertEquals(2, buttons.size(),
                "an explicit CANCEL must not be duplicated by the implicit one");
        assertEquals("Cancel", buttons.get(0).getText(),
                "the caller's CANCEL is left-most (the button order contract)");
    }

    @Test
    void wideOptionSetsSwitchToComboBox() {
        // Five long options exceed the button-row width threshold: the
        // choose dialog must render a combo box instead (the theme-template
        // case from the #1341 UX review).
        AtomicReference<JIPipeDesktopDialog> captured = new AtomicReference<>();
        AtomicReference<Optional<Button>> decided = new AtomicReference<>();
        JIPipeDesktopDialogs.setPresenter(d -> {
            captured.set(d);
            // decide inside the presenter: select the third option, click OK
            JComboBox<?> combo = findComboBox(d);
            assertNotNull(combo, "a wide option set must render as a combo box");
            assertEquals(6, combo.getItemCount(), "the combo lists every option");
            combo.setSelectedIndex(2);
            for (JButton button : d.getActionButtons()) {
                if (button.getText().equals("OK")) {
                    button.doClick();
                    break;
                }
            }
            return d;
        });
        try {
            Button a = Button.custom("JIPipe Light");
            Button b = Button.custom("JIPipe Classic Light");
            Button c = Button.custom("JIPipe Dark");
            Button e = Button.custom("JIPipe Dark Neon");
            Button f = Button.custom("JIPipe Dark High Contrast");
            Button g = Button.custom("JIPipe Dark Mid Contrast");
            decided.set(JIPipeDesktopDialogs.choose(null, "New theme from template",
                    "Please select the theme style that you want to use as base:", a, b, c, e, f, g));
            assertTrue(decided.get().isPresent());
            assertEquals(c, decided.get().get(), "OK must submit the combo's selected option");
        } finally {
            JIPipeDesktopDialogs.setPresenter(new ModalPresenter());
        }
    }

    @Test
    void narrowOptionSetsKeepButtons() {
        AtomicReference<JIPipeDesktopDialog> captured = new AtomicReference<>();
        JIPipeDesktopDialogs.setPresenter(d -> {
            captured.set(d);
            assertNull(findComboBox(d), "a narrow option set must render buttons, not a combo box");
            d.dismiss();
            return d;
        });
        try {
            JIPipeDesktopDialogs.choose(null, "Load template", "Choose:",
                    Button.custom("Load with data").asDefault(), Button.custom("Load only pipeline"));
            assertTrue(captured.get().getResult().isEmpty());
        } finally {
            JIPipeDesktopDialogs.setPresenter(new ModalPresenter());
        }
    }

    @Test
    void comboVariantStillTreatsDismissAsEmpty() {
        AtomicReference<Optional<Button>> decided = new AtomicReference<>();
        JIPipeDesktopDialogs.setPresenter(d -> {
            JComboBox<?> combo = findComboBox(d);
            assertNotNull(combo);
            combo.setSelectedIndex(1);
            d.dismiss(); // X / Escape / Cancel
            return d;
        });
        try {
            decided.set(JIPipeDesktopDialogs.choose(null, "New theme from template", "Pick:",
                    Button.custom("JIPipe Light"), Button.custom("JIPipe Classic Light"),
                    Button.custom("JIPipe Dark"), Button.custom("JIPipe Dark Neon"),
                    Button.custom("JIPipe Dark High Contrast"), Button.custom("JIPipe Dark Mid Contrast")));
            assertTrue(decided.get().isEmpty(), "dismissal must stay empty in the combo variant");
        } finally {
            JIPipeDesktopDialogs.setPresenter(new ModalPresenter());
        }
    }

    private JComboBox<?> findComboBox(Container container) {
        for (int i = 0; i < container.getComponentCount(); i++) {
            Component c = container.getComponent(i);
            if (c instanceof JComboBox) return (JComboBox<?>) c;
            if (c instanceof Container) {
                JComboBox<?> result = findComboBox((Container) c);
                if (result != null) return result;
            }
        }
        return null;
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
            // Resolved by identity: the order contract places the Cancel
            // button left-most, so no positional lookup is safe.
            dialog.getActionButton(custom).doClick();
            return dialog;
        });
        Optional<Button> chosen = JIPipeDesktopDialogs.choose(null, "t", "m", custom);
        assertTrue(chosen.isPresent(), "clicking the option must set the result");
        assertSame(custom, chosen.get(),
                "the facade must return the exact Button instance the caller passed");
        assertFalse(chosen.get() == Button.CANCEL, "a chosen option is never the Cancel descriptor");
    }
}
