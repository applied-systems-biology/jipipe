/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui.cache;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

/**
 * UI for a row
 */
public class JIPipeDataSlotRowUI extends JIPipeWorkbenchPanel {
    private final JIPipeDataSlot slot;
    private final int row;

    /**
     * Creates a new instance
     *
     * @param workbench the workbench
     * @param slot      the slot
     * @param row       the row
     */
    public JIPipeDataSlotRowUI(JIPipeWorkbench workbench, JIPipeDataSlot slot, int row) {
        super(workbench);
        this.slot = slot;
        this.row = row;

        this.initialize();
    }

    private void initialize() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(Box.createHorizontalGlue());

        JButton copyButton = new JButton("Copy string", UIUtils.getIconFromResources("actions/edit-copy.png"));
        copyButton.setToolTipText("Copies the string representation");
        copyButton.addActionListener(e -> copyString());
        add(copyButton);

        JButton displayButton = new JButton("Show", UIUtils.getIconFromResources("actions/find.png"));
        displayButton.setToolTipText("Shows the item");
        displayButton.addActionListener(e -> slot.getData(row, JIPipeData.class).display(slot.getNode().getName() + "/" + slot.getName() + "/" + row,
                getWorkbench()));
        add(displayButton);
    }

    private void copyString() {
        String string = "" + slot.getData(row, JIPipeData.class);
        StringSelection selection = new StringSelection(string);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
    }
}
