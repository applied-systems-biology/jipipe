package org.hkijena.acaq5.ui.testbench;

import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ACAQDataSlotBackupListCellRenderer extends JLabel implements ListCellRenderer<ACAQAlgorithmBackup> {

    public ACAQDataSlotBackupListCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        setIcon(UIUtils.getIconFromResources("database.png"));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends ACAQAlgorithmBackup> list, ACAQAlgorithmBackup backup, int index, boolean selected, boolean cellHasFocus) {
        if(list.getFont() != null) {
            setFont(list.getFont());
        }

        if(backup != null) {
            LocalDateTime backupTime = backup.getTimestamp();
            setText(backupTime.format(DateTimeFormatter.ISO_LOCAL_DATE) + " " + backupTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        }
        else {
            setText("<No backup selected>");
            setIcon(null);
        }

        // Update status
        // Update status
        if(selected) {
            setBackground(new Color(184, 207, 229));
        }
        else {
            setBackground(new Color(255,255,255));
        }
        return this;
    }
}
