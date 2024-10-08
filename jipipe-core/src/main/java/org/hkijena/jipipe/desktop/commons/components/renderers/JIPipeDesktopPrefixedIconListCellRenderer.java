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

import org.hkijena.jipipe.desktop.commons.components.pickers.JIPipeDesktopIconPickerDialog;
import org.hkijena.jipipe.utils.ResourceUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Renders entries in {@link JIPipeDesktopIconPickerDialog}
 */
public class JIPipeDesktopPrefixedIconListCellRenderer extends JLabel implements ListCellRenderer<String> {

    private String prefix;

    /**
     * @param prefix the resource prefix to prepend to the icon names
     */
    public JIPipeDesktopPrefixedIconListCellRenderer(String prefix) {
        this.prefix = prefix;
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {

        if (value == null) {
            setIcon(null);
            setText("<Null>");
        } else {
            setText(value);
            setIcon(new ImageIcon(ResourceUtils.class.getResource(prefix + "/" + value)));
        }

        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }

        return this;
    }
}
