package org.hkijena.acaq5.ui.components;

import com.google.common.html.HtmlEscapers;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.ui.registries.ACAQUITraitRegistry;

import javax.swing.*;
import java.awt.*;

/**
 * Renderer for {@link ACAQAlgorithmDeclaration}
 */
public class ACAQTraitDeclarationListCellRenderer extends JLabel implements ListCellRenderer<ACAQTraitDeclaration> {

    /**
     * Creates a new renderer
     */
    public ACAQTraitDeclarationListCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        setVerticalAlignment(TOP);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends ACAQTraitDeclaration> list, ACAQTraitDeclaration value, int index, boolean isSelected, boolean cellHasFocus) {

        setFont(list.getFont());

        if (value != null) {
            setIcon(ACAQUITraitRegistry.getInstance().getIconFor(value));
            setText(String.format("<html>%s <span style=\"color: gray\"><i>%s</i></span>%s</html>",
                    HtmlEscapers.htmlEscaper().escape(value.getName()),
                    value.getId(),
                    value.isHidden() ? "*" : ""));
        } else {
            setText("<Null>");
        }

        if (isSelected) {
            setBackground(new Color(184, 207, 229));
        } else {
            setBackground(new Color(255, 255, 255));
        }
        return this;
    }
}
