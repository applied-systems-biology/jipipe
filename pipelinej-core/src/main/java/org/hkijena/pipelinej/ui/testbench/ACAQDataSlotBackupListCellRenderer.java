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

package org.hkijena.pipelinej.ui.testbench;

import org.hkijena.pipelinej.api.testbench.ACAQTestbenchSnapshot;
import org.hkijena.pipelinej.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Renders a {@link ACAQTestbenchSnapshot}
 */
public class ACAQDataSlotBackupListCellRenderer extends JLabel implements ListCellRenderer<ACAQTestbenchSnapshot> {

    /**
     * Creates a new renderer
     */
    public ACAQDataSlotBackupListCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        setIcon(UIUtils.getIconFromResources("database.png"));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends ACAQTestbenchSnapshot> list, ACAQTestbenchSnapshot backup, int index, boolean selected, boolean cellHasFocus) {
        if (list.getFont() != null) {
            setFont(list.getFont());
        }

        if (backup != null) {
            LocalDateTime backupTime = backup.getTimestamp();
            String formattedBackupTime = backupTime.format(DateTimeFormatter.ISO_LOCAL_DATE) + " " + backupTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String label = backup.getLabel();
            if (label != null) {
                setText("<html>" + label + " " + "<span style=\"color: #9a9a9a;\">" + formattedBackupTime + "</span></html>");
            } else {
                setText(formattedBackupTime);
            }

        } else {
            setText("<No backup selected>");
            setIcon(null);
        }

        // Update status
        // Update status
        if (selected) {
            setBackground(new Color(184, 207, 229));
        } else {
            setBackground(new Color(255, 255, 255));
        }
        return this;
    }
}
