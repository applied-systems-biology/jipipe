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

package org.hkijena.jipipe.ui.extensionbuilder;

import org.hkijena.jipipe.api.grouping.JsonNodeInfo;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.Color;
import java.awt.Component;

/**
 * Renders the contents of a {@link org.hkijena.jipipe.JIPipeJsonExtension}
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
        if (value instanceof JsonNodeInfo) {
            String name = StringUtils.orElse(((JsonNodeInfo) value).getName(), "&lt;No name&gt;");
            setText("<html><strong>" + name + "</strong><br/>" + "<i>Algorithm</i>" + "</html>");
            setIcon(UIUtils.getIcon32FromResources("cogs.png"));
        } else {
            setText("<Unknown entry>");
            setIcon(UIUtils.getIconFromResources("actions/cancel.png"));
        }
        if (isSelected) {
            setBackground(new Color(184, 207, 229));
        } else {
            setBackground(new Color(255, 255, 255));
        }
        return this;
    }
}

