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
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The unique-value contract of {@link JIPipeDesktopDialogs#inputUniqueText}
 * (modern dialog API, #1341): submitting an already-taken value keeps the
 * dialog open for editing — an inline error label shows the substituted
 * error template and the field content is reselected — while a free value,
 * an empty/whitespace submission, or a dismissal ends the dialog.
 * <p>
 * Headless-safe: a capturing {@link DialogPresenter} records the presented
 * dialog instead of showing it, and the user's decisions are simulated
 * inside the presenter (the deciding-presenter pattern of
 * {@code InputTextEmptySubmitTest}). Because a taken submission hides the
 * dialog (the chrome's only OK path) and the facade answers by
 * re-presenting, the presenter is invoked once per round and
 * {@code captured} ends up as the dialog of the last round.
 */
class InputUniqueTextTest {

    /**
     * The dialog presented during the current test call (the dialog of the
     * last presentation round), for assertions.
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
        // The default presenter: record the dialog and hand it back unchanged,
        // so the facade's return value reflects "no decision yet" unless a
        // test installs a deciding presenter.
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
    void takenValueShowsErrorAndStaysOpen() {
        // Round 1: submit the taken value "a". Every later round leaves the
        // re-presented dialog undecided — the dialog "stays open" for
        // editing, so the facade must keep waiting instead of returning.
        AtomicBoolean submitted = new AtomicBoolean();
        JIPipeDesktopDialogs.setPresenter(dialog -> {
            captured = dialog;
            if (submitted.compareAndSet(false, true)) {
                findButton(dialog, "OK").doClick();
            }
            return dialog;
        });
        Set<String> taken = Set.of("a");
        Optional<String> result = JIPipeDesktopDialogs.inputUniqueText(null, "t", "m", "a",
                taken::contains, "Name {value} is already in use");
        assertTrue(result.isEmpty(), "a still-open (undecided) unique-text dialog must not return a value");
        assertTrue(captured.getResult().isEmpty(),
                "the re-presented dialog must not carry the earlier OK decision");
        JTextField field = findTextField(captured);
        assertNotNull(field, "the re-presented dialog must contain the text field");
        assertEquals("a", field.getText(), "the re-presented dialog must pre-fill the taken submission");
        assertEquals("a", field.getSelectedText(), "the taken text must be reselected for editing");
        JLabel error = findLabelWithText(captured, "Name a is already in use");
        assertNotNull(error, "a taken submission must show an inline error label with the substituted text");
    }

    @Test
    void freeValueAfterErrorSucceeds() {
        // Round 1: submit "a" (taken). Round 2: edit the re-presented dialog
        // to "b" (free) and submit.
        AtomicInteger round = new AtomicInteger();
        JIPipeDesktopDialogs.setPresenter(dialog -> {
            captured = dialog;
            if (round.incrementAndGet() == 1) {
                findButton(dialog, "OK").doClick();
            } else {
                findTextField(dialog).setText("b");
                findButton(dialog, "OK").doClick();
            }
            return dialog;
        });
        Optional<String> result = JIPipeDesktopDialogs.inputUniqueText(null, "t", "m", "a",
                v -> v.equals("a"), "Name {value} is already in use");
        assertEquals(Optional.of("b"), result, "a free value submitted after a taken one must be returned");
    }

    @Test
    void errorTemplateSubstitutesValue() {
        AtomicBoolean submitted = new AtomicBoolean();
        JIPipeDesktopDialogs.setPresenter(dialog -> {
            captured = dialog;
            if (submitted.compareAndSet(false, true)) {
                findButton(dialog, "OK").doClick();
            }
            return dialog;
        });
        JIPipeDesktopDialogs.inputUniqueText(null, "t", "m", "a",
                v -> v.equals("a"), "Name {value} is already in use");
        JLabel error = findLabelWithText(captured, "Name a is already in use");
        assertNotNull(error, "the error template must be shown after a taken submission");
        assertEquals("Name a is already in use", error.getText(),
                "the error label must show the template with {value} replaced by the submission");
    }

    @Test
    void emptySubmitIsDismissed() {
        // The empty/whitespace rule of inputText (spec §4.2) applies here —
        // and it must end the loop: with every value "taken" (the predicate
        // below), an exists-check before the blank check would never finish.
        AtomicInteger rounds = new AtomicInteger();
        JIPipeDesktopDialogs.setPresenter(dialog -> {
            captured = dialog;
            if (rounds.incrementAndGet() == 1) {
                findButton(dialog, "OK").doClick();
            }
            return dialog;
        });
        Optional<String> result = JIPipeDesktopDialogs.inputUniqueText(null, "t", "m", "   ",
                v -> true, "Name {value} is already in use");
        assertTrue(result.isEmpty(), "an empty/whitespace submission must count as dismiss");
        assertEquals(1, rounds.get(), "an empty submission must end the dialog, not re-present it");
    }

    /**
     * Finds the first {@link JTextField} in the component tree, or null if
     * there is none. (Component class, not layout.)
     */
    private static JTextField findTextField(Component component) {
        if (component instanceof JTextField field) {
            return field;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JTextField found = findTextField(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * Finds the first {@link JLabel} with exactly the given text in the
     * component tree, or null if there is none. (Component class + content,
     * not layout.)
     */
    private static JLabel findLabelWithText(Component component, String text) {
        if (component instanceof JLabel label && text.equals(label.getText())) {
            return label;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JLabel found = findLabelWithText(child, text);
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
