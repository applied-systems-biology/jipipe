package org.hkijena.acaq5.ui.extensions;

import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.ACAQJsonExtension;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.Color;
import java.awt.Component;

/**
 * Renders an {@link ACAQDependency}
 */
public class ACAQDependencyListCellRenderer extends JLabel implements ListCellRenderer<ACAQDependency> {

    /**
     * Creates a new renderer
     */
    public ACAQDependencyListCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends ACAQDependency> list, ACAQDependency value, int index, boolean isSelected, boolean cellHasFocus) {

        if (value == null) {
            setText("<Null>");
            setIcon(null);
        } else {
            setText("<html><strong>" + value.getMetadata().getName() + "</strong><br/>Version " + value.getDependencyVersion() + "</html>");
            if (value instanceof ACAQJsonExtension)
                setIcon(UIUtils.getIconFromResources("module-json-32.png"));
            else
                setIcon(UIUtils.getIconFromResources("module-java-32.png"));
        }

        if (isSelected) {
            setBackground(new Color(184, 207, 229));
        } else {
            setBackground(new Color(255, 255, 255));
        }

        return this;
    }
}
