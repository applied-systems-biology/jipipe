/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop.commons.components.renderers;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.utils.ResourceUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * Renders inheritance conversion entries
 */
public class JIPipeDesktopInheritanceConversionListCellRenderer extends JLabel implements ListCellRenderer<Map.Entry<JIPipeDataInfo, JIPipeDataInfo>> {

    /**
     * Creates a new renderer
     */
    public JIPipeDesktopInheritanceConversionListCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Map.Entry<JIPipeDataInfo, JIPipeDataInfo>> list,
                                                  Map.Entry<JIPipeDataInfo, JIPipeDataInfo> value, int index, boolean isSelected, boolean cellHasFocus) {
        if (value == null) {
            setText("<Null>");
        } else {
            JIPipeDataInfo from = value.getKey();
            JIPipeDataInfo to = value.getValue();
            String stringBuilder = "<html>" +
                    "<table><tr>" +
                    "<td><img src=\"" + JIPipe.getDataTypes().getIconURLFor(from.getDataClass()) + "\" /></td>" +
                    "<td>" + from.getName() + "</td>" +
                    "<td><img src=\"" + ResourceUtils.getPluginResource("icons/actions/caret-right.png") + "\" /></td>" +
                    "<td><img src=\"" + JIPipe.getDataTypes().getIconURLFor(to.getDataClass()) + "\" /></td>" +
                    "<td>" + to.getName() + "</td>" +
                    "</tr></table>" +
                    "</html>";
            setText(stringBuilder);
        }

        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }

        return this;
    }
}
