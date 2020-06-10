package org.hkijena.acaq5.ui.components;

import com.google.common.html.HtmlEscapers;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.ui.registries.ACAQUIAlgorithmRegistry;
import org.hkijena.acaq5.utils.StringUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Renderer for {@link ACAQAlgorithmDeclaration}
 */
public class ACAQAlgorithmDeclarationListCellRenderer extends JLabel implements ListCellRenderer<ACAQAlgorithmDeclaration> {

    private ColorIcon icon = new ColorIcon(16, 40);

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
    public Component getListCellRendererComponent(JList<? extends ACAQAlgorithmDeclaration> list, ACAQAlgorithmDeclaration declaration, int index, boolean isSelected, boolean cellHasFocus) {

        setFont(list.getFont());

        if (declaration != null) {
            icon.setFillColor(UIUtils.getFillColorFor(declaration));
            String menuPath = declaration.getCategory().toString();
            menuPath += "\n" + declaration.getMenuPath();
            menuPath = StringUtils.getCleanedMenuPath(menuPath).replace("\n", " > ");
            setText(String.format("<html><table cellpadding=\"1\"><tr>" +
                            "<td><img src=\"%s\"/></td>" +
                            "<td>%s</td></tr>" +
                            "<tr>" +
                            "<td></td>" +
                            "<td><span style=\"color: gray;\">%s</span></td></tr></table></html>",
                    ACAQUIAlgorithmRegistry.getInstance().getIconURLFor(declaration),
                    HtmlEscapers.htmlEscaper().escape(declaration.getName()),
                    menuPath
            ));
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
