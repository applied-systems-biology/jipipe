package org.hkijena.acaq5.ui.components;

import org.hkijena.acaq5.extensions.parameters.references.ACAQDataDeclarationRef;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Renders {@link ACAQDataDeclarationRef}
 */
public class ACAQDataDeclarationRefListCellRenderer extends JLabel implements ListCellRenderer<ACAQDataDeclarationRef> {

    /**
     * Creates a new renderer
     */
    public ACAQDataDeclarationRefListCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends ACAQDataDeclarationRef> list, ACAQDataDeclarationRef value, int index, boolean isSelected, boolean cellHasFocus) {
        if (list.getFont() != null) {
            setFont(list.getFont());
        }
        if (value != null && value.getDeclaration() != null) {
            setText(value.getDeclaration().getName());
            setIcon(ACAQUIDatatypeRegistry.getInstance().getIconFor(value.getDeclaration().getDataClass()));
        } else {
            setText("Nothing selected");
            setIcon(UIUtils.getIconFromResources("error.png"));
        }

        if (isSelected) {
            setBackground(new Color(184, 207, 229));
        } else {
            setBackground(new Color(255, 255, 255));
        }
        return this;
    }
}
