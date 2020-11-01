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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.extensions.parameters.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.Color;
import java.awt.Component;

/**
 * Renders {@link JIPipeDataInfoRef}
 */
public class JIPipeDataInfoRefListCellRenderer extends JLabel implements ListCellRenderer<JIPipeDataInfoRef> {

    /**
     * Creates a new renderer
     */
    public JIPipeDataInfoRefListCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends JIPipeDataInfoRef> list, JIPipeDataInfoRef value, int index, boolean isSelected, boolean cellHasFocus) {
        if (list.getFont() != null) {
            setFont(list.getFont());
        }
        if (value != null && value.getInfo() != null) {
            setText(value.getInfo().getName());
            setIcon(JIPipe.getDataTypes().getIconFor(value.getInfo().getDataClass()));
        } else {
            setText("Nothing selected");
            setIcon(UIUtils.getIconFromResources("emblems/vcs-conflicting.png"));
        }

        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }
        return this;
    }
}
