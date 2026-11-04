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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the {@link JIPipeDesktopDialog} chrome and the
 * {@link DialogPresenter} seam (modern dialog API, #1341).
 * <p>
 * Headless-safe: the dialog is constructed but never shown - the tests
 * install a no-op presenter and drive the constructed dialog via
 * {@link JButton#doClick()}. AWT is required to construct the dialog, so the
 * suite skips in headless environments (mirroring
 * {@code ColorChooserDialogRefreshTest}).
 */
class DialogChromeTest {

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

    @AfterAll
    static void restorePresenter() {
        // Don't leak the no-op test presenter into other test classes in the same JVM.
        JIPipeDesktopDialog.setPresenter(new ModalPresenter());
    }

    @BeforeEach
    void installNoOpPresenter() {
        // The tests never show a dialog; keep showAndWait() inert and side-effect-free.
        JIPipeDesktopDialog.setPresenter(dialog -> dialog);
    }

    private static JIPipeDesktopDialog newDialog() {
        return new JIPipeDesktopDialog(null, "Test");
    }

    @Test
    void dismissSetsEmptyResult() {
        JIPipeDesktopDialog d = newDialog();
        d.setButtons(Button.OK);
        d.dismiss();
        Optional<Button> result = d.getResult();
        assertTrue(result.isEmpty(), "dismiss() must leave the result empty");
    }

    @Test
    void okButtonClickSetsResultToSameInstance() {
        Button b = Button.custom("Do it").asDefault();
        JIPipeDesktopDialog d = newDialog();
        d.setButtons(b, Button.CANCEL);
        d.getActionButtons().get(0).doClick();
        Optional<Button> result = d.getResult();
        assertTrue(result.isPresent(), "clicking a non-cancel button must set the result");
        assertSame(b, result.get(), "the result must be the exact Button instance the caller passed");
    }

    @Test
    void cancelClickDismisses() {
        JIPipeDesktopDialog d = newDialog();
        d.setButtons(Button.OK, Button.CANCEL);
        JButton cancel = d.getActionButtons().get(1);
        cancel.doClick();
        Optional<Button> result = d.getResult();
        assertTrue(result.isEmpty(), "clicking CANCEL must funnel into dismiss() (empty result)");
    }

    @Test
    void cancelableAppendsImplicitCancel() {
        JIPipeDesktopDialog d = newDialog();
        d.setCancelable(true);
        d.setButtons(Button.YES, Button.NO);
        List<JButton> buttons = d.getActionButtons();
        assertEquals(3, buttons.size(), "cancelable dialog must append the implicit Cancel button");
        assertEquals("Cancel", buttons.get(2).getText(), "implicit Cancel must come last");

        JIPipeDesktopDialog notCancelable = newDialog();
        notCancelable.setCancelable(false);
        notCancelable.setButtons(Button.YES, Button.NO);
        assertEquals(2, notCancelable.getActionButtons().size(), "non-cancelable dialog must not append a Cancel button");
    }

    @Test
    void defaultButtonIsRegisteredOnRootPane() {
        JIPipeDesktopDialog d = newDialog();
        Button def = Button.custom("Go").asDefault();
        d.setButtons(def, Button.CANCEL);
        JButton go = null;
        for (JButton jButton : d.getActionButtons()) {
            if ("Go".equals(jButton.getText())) {
                go = jButton;
            }
        }
        assertNotNull(go, "the default-flagged button must be among the action buttons");
        assertSame(go, d.getRootPane().getDefaultButton(),
                "the default-flagged button must be registered as the root pane's default button (Enter)");

        d.setButtons(Button.YES, Button.NO);
        assertNull(d.getRootPane().getDefaultButton(),
                "re-calling setButtons without a default-flagged button must clear a stale default");
    }

    @Test
    void escapeAndCloseFunnelToDismiss() {
        JIPipeDesktopDialog d = newDialog();
        assertEquals(JDialog.DISPOSE_ON_CLOSE, d.getDefaultCloseOperation(), "window close must dispose the dialog");

        // Wiring-level check: the root pane must carry a bound ESCAPE action,
        // and performing it must funnel into dismiss() (empty result).
        // Dispatching a synthetic KeyEvent does not reach root-pane keyboard
        // actions of an unrealized dialog, so the bound action is performed
        // directly (spec §6.1 fallback).
        Action esc = (Action) d.getRootPane().getActionForKeyStroke(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
        assertNotNull(esc, "root pane must have a bound ESCAPE keyboard action");
        esc.actionPerformed(new ActionEvent(d.getRootPane(), ActionEvent.ACTION_PERFORMED, "ESCAPE"));
        assertTrue(d.getResult().isEmpty(), "ESCAPE must funnel into dismiss() (empty result)");
    }

    @Test
    void messageLabelReceivesText() {
        JIPipeDesktopDialog d = newDialog();
        String message = "Hello <b>world</b>";
        d.addMessage(message);
        assertNotNull(d.getMessageLabel(), "addMessage() must install a message label");
        assertTrue(d.getMessageLabel().getText().contains(message),
                "message label text must contain the given message");
    }

    @Test
    void addMessagePassesThroughHtmlDocument() {
        // A message that already is a full <html> document must not be nested
        // in the wrapper div (Swing mangles double-wrapped documents).
        JIPipeDesktopDialog d = newDialog();
        String document = "<html><b>Hi</b></html>";
        d.addMessage(document);
        assertEquals(document, d.getMessageLabel().getText(),
                "a full <html> document must be set verbatim, without the added wrapper div");

        // Plain messages keep the word-wrapping wrapper.
        JIPipeDesktopDialog plain = newDialog();
        plain.addMessage("plain message");
        assertTrue(plain.getMessageLabel().getText().contains("<div style=\"width: 400px\">"),
                "a plain message must still be wrapped for word-wrapping");
    }
}
