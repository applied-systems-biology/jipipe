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
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@code Builder} escape hatch of {@link JIPipeDesktopDialogs} (modern
 * dialog API, #1341, spec §4.4): the button rules (an explicit set replaces
 * the default, {@code cancelable(false)} suppresses Cancel entirely,
 * otherwise the chrome appends the implicit Cancel), message normalization,
 * custom content and size — and the show/build split ({@code build()} only
 * constructs, {@code show()} presents and returns the chosen button).
 * <p>
 * Headless-safe: a capturing {@link DialogPresenter} records the presented
 * dialog instead of showing it, and decisions are simulated inside the
 * presenter (mirroring {@code ChooseButtonsTest}).
 */
class BuilderTest {

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
    void customContentAndExplicitButtons() {
        JPanel content = new JPanel();
        JIPipeDesktopDialog dialog = JIPipeDesktopDialogs.builder(null, "t")
                .message("m")
                .content(content)
                .buttons(Button.custom("Apply").asDefault())
                .cancelable(false)
                .size(400, 300)
                .build();
        assertNull(captured, "build() must construct the dialog without presenting it");
        List<JButton> buttons = dialog.getActionButtons();
        assertEquals(1, buttons.size(),
                "cancelable(false) must suppress the implicit Cancel entirely");
        assertEquals("Apply", buttons.get(0).getText(),
                "the explicit button set must replace the default set, in order");
        assertTrue(dialog.getMessageLabel().getText().contains("m"),
                "the message must be set on the dialog");
        assertEquals(new Dimension(400, 300), dialog.getSize(), "size() must size the dialog");
        assertNotNull(findComponent(dialog, content), "the custom content must be added to the dialog");
    }

    @Test
    void showReturnsChosenButton() {
        Button apply = Button.custom("Apply").asDefault();
        JIPipeDesktopDialogs.setPresenter(dialog -> {
            captured = dialog;
            findButton(dialog, "Apply").doClick();
            return dialog;
        });
        Optional<Button> chosen = JIPipeDesktopDialogs.builder(null, "t")
                .message("m")
                .buttons(apply)
                .show();
        assertTrue(chosen.isPresent(), "show() must return the chosen button");
        assertSame(apply, chosen.get(),
                "show() must return the exact Button instance the caller passed");

        // A dismissal (Cancel / window close / Escape) yields empty.
        JIPipeDesktopDialogs.setPresenter(dialog -> {
            captured = dialog;
            dialog.dismiss();
            return dialog;
        });
        Optional<Button> dismissed = JIPipeDesktopDialogs.builder(null, "t")
                .message("m")
                .buttons(apply)
                .show();
        assertTrue(dismissed.isEmpty(), "a dismissed builder dialog must yield an empty result");
    }

    @Test
    void implicitCancelAndDefaultButtons() {
        // No buttons() call → the default set (OK) plus the implicit Cancel
        // of the default cancelable(true).
        JIPipeDesktopDialog defaults = JIPipeDesktopDialogs.builder(null, "t").message("m").build();
        List<JButton> buttons = defaults.getActionButtons();
        assertEquals(2, buttons.size(), "the default set must be OK plus the implicit Cancel");
        assertEquals("OK", buttons.get(0).getText(), "the default set must start with OK");
        assertEquals("Cancel", buttons.get(1).getText(), "the implicit Cancel must come last");

        // An explicit set under the default cancelable(true) gets the
        // implicit Cancel appended once — by the chrome, not the Builder.
        JIPipeDesktopDialog custom = JIPipeDesktopDialogs.builder(null, "t")
                .message("m")
                .buttons(Button.custom("Apply"))
                .build();
        List<JButton> customButtons = custom.getActionButtons();
        assertEquals(2, customButtons.size(), "the implicit Cancel must be appended exactly once");
        assertEquals("Apply", customButtons.get(0).getText(), "the explicit button keeps its position");
        assertEquals("Cancel", customButtons.get(1).getText(), "the implicit Cancel must come last");
    }

    @Test
    void messageNullIsNormalizedToEmpty() {
        JIPipeDesktopDialog nullMessage = JIPipeDesktopDialogs.builder(null, "t").message(null).build();
        JIPipeDesktopDialog emptyMessage = JIPipeDesktopDialogs.builder(null, "t").message("").build();
        assertEquals(emptyMessage.getMessageLabel().getText(), nullMessage.getMessageLabel().getText(),
                "message(null) must render exactly like the empty message");
    }

    /**
     * Finds the given component instance in the component tree, or null if
     * it is not there. (Identity search by traversal, not layout.)
     */
    private static Component findComponent(Component component, Component target) {
        if (component == target) {
            return component;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                Component found = findComponent(child, target);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * Finds the {@link JButton} with the given label among the dialog's
     * action buttons, or fails. (Component class + content, not layout.)
     */
    private static JButton findButton(JIPipeDesktopDialog dialog, String label) {
        for (JButton button : dialog.getActionButtons()) {
            if (label.equals(button.getText())) {
                return button;
            }
        }
        throw new AssertionError("no '" + label + "' button in the dialog");
    }
}
