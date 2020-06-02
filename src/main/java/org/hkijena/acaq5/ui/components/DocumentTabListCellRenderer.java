/*
 * Copyright by Ruman Gerst
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * This code is licensed under BSD 2-Clause
 * See the LICENSE file provided with this code for the full license.
 */

package org.hkijena.acaq5.ui.components;

import javax.swing.*;
import java.awt.Component;

/**
 * Renders {@link DocumentTabPane} tabs
 */
public class DocumentTabListCellRenderer extends JLabel implements ListCellRenderer<DocumentTabPane.DocumentTab> {

    /**
     * Creates a new renderer
     */
    public DocumentTabListCellRenderer() {
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends DocumentTabPane.DocumentTab> list, DocumentTabPane.DocumentTab value, int index, boolean isSelected, boolean cellHasFocus) {
        if (value != null) {
            setText(value.getTitle());
            setIcon(value.getIcon());
        } else {
            setText(null);
            setIcon(null);
        }
        return this;
    }
}
