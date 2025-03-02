package org.hkijena.jipipe.desktop.commons.components.filechoosernext;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;

public class FileNameExtensionsFilterListCellRenderer extends JLabel implements ListCellRenderer<FileNameExtensionFilter> {

    public FileNameExtensionsFilterListCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends FileNameExtensionFilter> list, FileNameExtensionFilter value, int index, boolean isSelected, boolean cellHasFocus) {

        setText(value.getDescription());

        if(isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        }
        else {
            setBackground(UIManager.getColor("List.background"));
        }
        return this;
    }
}
