/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.acaq5.ui.components;

import com.google.common.html.HtmlEscapers;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.ui.registries.ACAQUIAlgorithmRegistry;
import org.hkijena.acaq5.utils.StringUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.Color;
import java.awt.Component;

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
