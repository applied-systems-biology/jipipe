package org.hkijena.acaq5.ui.components;

import org.hkijena.acaq5.utils.ResourceUtils;

import javax.swing.*;
import java.awt.*;

public class PrefixedIconListCellRenderer extends JLabel implements ListCellRenderer<String> {

    private String prefix;

    public PrefixedIconListCellRenderer(String prefix) {
        this.prefix = prefix;
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {

        if (value == null) {
            setIcon(null);
            setText("<Null>");
        } else {
            setText(value);
            setIcon(new ImageIcon(ResourceUtils.class.getResource(prefix + "/" + value)));
        }

        if (isSelected) {
            setBackground(new Color(184, 207, 229));
        } else {
            setBackground(new Color(255, 255, 255));
        }

        return this;
    }
}
