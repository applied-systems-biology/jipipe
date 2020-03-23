package org.hkijena.acaq5.ui.components;

import org.hkijena.acaq5.api.data.ACAQDataDeclaration;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.utils.ResourceUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class ACAQInheritanceConversionListCellRenderer extends JLabel implements ListCellRenderer<Map.Entry<ACAQDataDeclaration, ACAQDataDeclaration>> {

    public ACAQInheritanceConversionListCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Map.Entry<ACAQDataDeclaration, ACAQDataDeclaration>> list,
                                                  Map.Entry<ACAQDataDeclaration, ACAQDataDeclaration> value, int index, boolean isSelected, boolean cellHasFocus) {
        if (value == null) {
            setText("<Null>");
        } else {
            ACAQDataDeclaration from = value.getKey();
            ACAQDataDeclaration to = value.getValue();
            String stringBuilder = "<html>" +
                    "<table><tr>" +
                    "<td><img src=\"" + ACAQUIDatatypeRegistry.getInstance().getIconURLFor(from.getDataClass()) + "\" /></td>" +
                    "<td>" + from.getName() + "</td>" +
                    "<td><img src=\"" + ResourceUtils.getPluginResource("icons/chevron-right.png") + "\" /></td>" +
                    "<td><img src=\"" + ACAQUIDatatypeRegistry.getInstance().getIconURLFor(to.getDataClass()) + "\" /></td>" +
                    "<td>" + to.getName() + "</td>" +
                    "</tr></table>" +
                    "</html>";
            setText(stringBuilder);
        }

        if (isSelected) {
            setBackground(new Color(184, 207, 229));
        } else {
            setBackground(new Color(255, 255, 255));
        }

        return this;
    }
}
