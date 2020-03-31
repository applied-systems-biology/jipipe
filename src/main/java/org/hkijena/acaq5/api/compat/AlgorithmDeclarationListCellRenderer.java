package org.hkijena.acaq5.api.compat;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.ui.components.ColorIcon;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Renderer for {@link ACAQAlgorithmDeclaration}
 */
public class AlgorithmDeclarationListCellRenderer extends JLabel implements ListCellRenderer<ACAQAlgorithmDeclaration> {

    /**
     * Creates a new renderer
     */
    public AlgorithmDeclarationListCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        setVerticalAlignment(TOP);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends ACAQAlgorithmDeclaration> list, ACAQAlgorithmDeclaration value, int index, boolean isSelected, boolean cellHasFocus) {

        setFont(list.getFont());

        if (value != null) {
            setIcon(new ColorIcon(16, 16, UIUtils.getFillColorFor(value)));
            setText(value.getName());
//            setText("<html><table cellpadding=\"0\"><tr><td>" + HtmlEscapers.htmlEscaper().escape(value.getName()) + "</td>" +
//                    "<tr><td><i>" + HtmlEscapers.htmlEscaper().escape(value.getDescription()) + "</td></tr></table></html>");
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
