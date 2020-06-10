package org.hkijena.acaq5.ui.components;

import com.google.common.html.HtmlEscapers;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.utils.StringUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Renderer for {@link ACAQAlgorithmDeclaration}
 */
public class ACAQAlgorithmDeclarationListCellRenderer extends JLabel implements ListCellRenderer<ACAQAlgorithmDeclaration> {

    private ColorIcon icon = new ColorIcon(16, 32);

    /**
     * Creates a new renderer
     */
    public ACAQAlgorithmDeclarationListCellRenderer() {
        setOpaque(true);
        setIcon(icon);
        setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        setVerticalAlignment(TOP);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends ACAQAlgorithmDeclaration> list, ACAQAlgorithmDeclaration value, int index, boolean isSelected, boolean cellHasFocus) {

        setFont(list.getFont());

        if (value != null) {
            icon.setFillColor(UIUtils.getFillColorFor(value));
            String menuPath = value.getCategory().toString();
            menuPath += "\n" + value.getMenuPath();
            menuPath = StringUtils.getCleanedMenuPath(menuPath).replace("\n", " > ");
            setText("<html><table cellpadding=\"0\"><tr><td>" + HtmlEscapers.htmlEscaper().escape(value.getName()) + "</td>" +
                    "<tr><td><i>" + HtmlEscapers.htmlEscaper().escape(menuPath) + "</td></tr></table></html>");
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
