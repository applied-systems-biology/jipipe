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

package org.hkijena.jipipe.desktop.commons.components.tabs;

import javax.swing.*;
import java.awt.*;

/**
 * Renders {@link JIPipeDesktopTabPane} tabs
 */
public class JIPipeDesktopTabListCellRenderer extends JLabel implements ListCellRenderer<JIPipeDesktopTabPane.DocumentTab> {

    /**
     * Creates a new renderer
     */
    public JIPipeDesktopTabListCellRenderer() {
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends JIPipeDesktopTabPane.DocumentTab> list, JIPipeDesktopTabPane.DocumentTab value, int index, boolean isSelected, boolean cellHasFocus) {
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
