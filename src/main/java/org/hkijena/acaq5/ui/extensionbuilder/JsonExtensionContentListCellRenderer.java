package org.hkijena.acaq5.ui.extensionbuilder;

import org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.macro.GraphWrapperAlgorithmDeclaration;
import org.hkijena.acaq5.utils.StringUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class JsonExtensionContentListCellRenderer extends JLabel implements ListCellRenderer<Object> {

    public JsonExtensionContentListCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

        if (value instanceof GraphWrapperAlgorithmDeclaration) {
            String name = StringUtils.orElse(((GraphWrapperAlgorithmDeclaration) value).getName(), "<No name>");

            setText("<html><strong>" + name + "</strong><br/>" +
                    "<i>Algorithm</i>" +
                    "</html>");
            setIcon(UIUtils.getIconFromResources("cogs-32.png"));
        } else {
            setText("<Unknown entry>");
            setIcon(UIUtils.getIconFromResources("remove.png"));
        }

        if (isSelected) {
            setBackground(new Color(184, 207, 229));
        } else {
            setBackground(new Color(255, 255, 255));
        }
        return this;
    }
}
