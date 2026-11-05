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
import org.hkijena.jipipe.utils.UIUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the dialog sizing contract (#1341 testing round 1):
 * the chrome previously never sized itself, so every dialog opened as a
 * 1x56 pixel sliver. {@link JIPipeDesktopDialog#applyDialogSize()} must pack
 * unsized dialogs to at least the minimum width and keep explicitly sized
 * dialogs (the Builder's {@code size()}) untouched. These assertions pin the
 * size contract of the reported defect, not cosmetic layout.
 */
class DialogSizingTest {

    @BeforeAll
    static void boot() {
        JIPipe.ensureInstance();
    }

    @Test
    void unpackedDialogIsPackedToAtLeastTheMinimumWidth() {
        JIPipeDesktopDialog dialog = new JIPipeDesktopDialog(null, "Delete rows");
        dialog.addMessage("Do you really want to delete the selected ROW(S)?");
        dialog.setButtons(Button.YES, Button.NO);
        dialog.applyDialogSize();
        assertTrue(dialog.getWidth() >= 320, "packed width " + dialog.getWidth() + " below minimum");
        assertTrue(dialog.getHeight() > 0);
    }

    @Test
    void emptyMessageStillMeetsTheMinimumWidth() {
        JIPipeDesktopDialog dialog = new JIPipeDesktopDialog(null, "Title only");
        dialog.addMessage("");
        dialog.setButtons(Button.OK);
        dialog.applyDialogSize();
        assertTrue(dialog.getWidth() >= 320, "empty-message width " + dialog.getWidth() + " below minimum");
    }

    @Test
    void explicitSizeIsHonoredAndNotRepacked() {
        JIPipeDesktopDialog dialog = new JIPipeDesktopDialog(null, "Edit");
        dialog.addMessage("Set the value");
        dialog.setButtons(Button.OK, Button.CANCEL);
        dialog.setSize(500, 300);
        dialog.applyDialogSize();
        assertEquals(500, dialog.getWidth());
        assertEquals(300, dialog.getHeight());
    }

    @Test
    void actionButtonsHaveEqualWidths() {
        // Uniform dialog footers (the #1341 UX review): every action button
        // must share the width of the widest one, primary fill included.
        JIPipeDesktopDialog dialog = new JIPipeDesktopDialog(null, "Delete rows");
        dialog.addMessage("Do you really want to delete the selected ROW(S)?");
        dialog.setButtons(Button.YES.asDefault(), Button.NO, Button.CANCEL);
        List<JButton> buttons = dialog.getActionButtons();
        assertEquals(3, buttons.size());
        int width = buttons.get(0).getPreferredSize().width;
        for (JButton button : buttons) {
            assertEquals(width, button.getPreferredSize().width,
                    "action buttons must share one width (primary fill included): " + button.getText());
            assertEquals(UIUtils.DIALOG_BUTTON_HEIGHT, button.getPreferredSize().height);
        }
    }

    @Test
    void inputTextFieldHasRealWidth() {
        AtomicReference<JIPipeDesktopDialog> captured = new AtomicReference<>();
        // Capture the built dialog and dismiss in the same round (the
        // deciding-presenter pattern): the facade loops while no decision
        // is made, so a presenter that decides nothing would hang it.
        JIPipeDesktopDialogs.setPresenter(d -> {
            captured.set(d);
            d.dismiss();
            return d;
        });
        try {
            JIPipeDesktopDialogs.inputText(null, "Rename", "Please enter the new name:", null);
            JIPipeDesktopDialog dialog = captured.get();
            assertNotNull(dialog);
            JTextField field = findTextField(dialog);
            assertNotNull(field);
            assertTrue(field.getColumns() >= 20, "input field too narrow: " + field.getColumns() + " columns");
        } finally {
            JIPipeDesktopDialogs.setPresenter(new ModalPresenter());
        }
    }

    private JTextField findTextField(Container container) {
        for (int i = 0; i < container.getComponentCount(); i++) {
            Component c = container.getComponent(i);
            if (c instanceof JTextField) return (JTextField) c;
            if (c instanceof Container) {
                JTextField result = findTextField((Container) c);
                if (result != null) return result;
            }
        }
        return null;
    }
}
