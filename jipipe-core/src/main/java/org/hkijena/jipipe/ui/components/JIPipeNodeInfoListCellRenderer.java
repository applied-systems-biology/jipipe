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

package org.hkijena.jipipe.ui.components;

import com.google.common.html.HtmlEscapers;
import org.hkijena.jipipe.api.algorithm.JIPipeNodeInfo;
import org.hkijena.jipipe.ui.registries.JIPipeUIAlgorithmRegistry;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Renderer for {@link JIPipeNodeInfo}
 */
public class JIPipeNodeInfoListCellRenderer extends JLabel implements ListCellRenderer<JIPipeNodeInfo> {

    private ColorIcon icon = new ColorIcon(16, 40);

    /**
     * Creates a new renderer
     */
    public JIPipeNodeInfoListCellRenderer() {
        setOpaque(true);
        setIcon(icon);
        setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        setVerticalAlignment(TOP);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends JIPipeNodeInfo> list, JIPipeNodeInfo info, int index, boolean isSelected, boolean cellHasFocus) {

        setFont(list.getFont());

        if (info != null) {
            icon.setFillColor(UIUtils.getFillColorFor(info));
            String menuPath = info.getCategory().toString();
            menuPath += "\n" + info.getMenuPath();
            menuPath = StringUtils.getCleanedMenuPath(menuPath).replace("\n", " > ");
            setText(String.format("<html><table cellpadding=\"1\"><tr>" +
                            "<td><img src=\"%s\"/></td>" +
                            "<td>%s</td></tr>" +
                            "<tr>" +
                            "<td></td>" +
                            "<td><span style=\"color: gray;\">%s</span></td></tr></table></html>",
                    JIPipeUIAlgorithmRegistry.getInstance().getIconURLFor(info),
                    HtmlEscapers.htmlEscaper().escape(info.getName()),
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
