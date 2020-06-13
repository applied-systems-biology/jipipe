package org.hkijena.acaq5.ui.cache;

import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.ACAQWorkbenchPanel;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

/**
 * UI for a row
 */
public class ACAQDataSlotRowUI extends ACAQWorkbenchPanel {
    private final ACAQDataSlot slot;
    private final int row;

    /**
     * Creates a new instance
     * @param workbench the workbench
     * @param slot the slot
     * @param row the row
     */
    public ACAQDataSlotRowUI(ACAQWorkbench workbench, ACAQDataSlot slot, int row) {
        super(workbench);
        this.slot = slot;
        this.row = row;

        this.initialize();
    }

    private void initialize() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(Box.createHorizontalGlue());

        JButton copyButton = new JButton("Copy string", UIUtils.getIconFromResources("copy.png"));
        copyButton.setToolTipText("Copies the string representation");
        copyButton.addActionListener(e -> copyString());
        add(copyButton);

        JButton displayButton = new JButton("Show", UIUtils.getIconFromResources("search.png"));
        displayButton.setToolTipText("Shows the item");
        displayButton.addActionListener(e -> slot.getData(row, ACAQData.class).display(slot.getAlgorithm().getName() + "/" + slot.getName() + "/" + row,
                getWorkbench()));
        add(displayButton);
    }

    private void copyString() {
        String string = "" + slot.getData(row, ACAQData.class);
        StringSelection selection = new StringSelection(string);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
    }
}
