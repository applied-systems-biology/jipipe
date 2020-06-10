package org.hkijena.acaq5.ui.extensionbuilder;

import org.hkijena.acaq5.api.traits.ACAQJsonTraitDeclaration;
import org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.GraphWrapperAlgorithmDeclaration;
import org.hkijena.acaq5.utils.StringUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Renders the contents of a {@link org.hkijena.acaq5.ACAQJsonExtension}
 */
public class JsonExtensionContentListCellRenderer extends JLabel implements ListCellRenderer<Object> {

    /**
     * Creates a new renderer
     */
    public JsonExtensionContentListCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

        if (value instanceof GraphWrapperAlgorithmDeclaration) {
            String name = StringUtils.orElse(((GraphWrapperAlgorithmDeclaration) value).getName(), "&lt;No name&gt;");

            setText("<html><strong>" + name + "</strong><br/>" +
                    "<i>Algorithm</i>" +
                    "</html>");
            setIcon(UIUtils.getIconFromResources("cogs-32.png"));
        } else if (value instanceof ACAQJsonTraitDeclaration) {
            String name = StringUtils.orElse(((ACAQJsonTraitDeclaration) value).getName(), "&lt;No name&gt;");
            setText("<html><strong>" + name + "</strong><br/>" +
                    "<i>Annotation type</i>" +
                    "</html>");
            setIcon(UIUtils.getIconFromResources("tags-32.png"));
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
