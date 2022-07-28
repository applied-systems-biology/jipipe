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

package org.hkijena.jipipe.ui.cache.renderers;

import org.hkijena.jipipe.api.JIPipeProjectCacheState;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Renders {@link JIPipeProjectCacheState}
 */
public class CacheStateListCellRenderer extends JLabel implements ListCellRenderer<JIPipeProjectCacheState> {

    /**
     * Creates a new renderer
     */
    public CacheStateListCellRenderer() {
        setOpaque(true);
        setIcon(UIUtils.getIconFromResources("actions/camera.png"));
        setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends JIPipeProjectCacheState> list, JIPipeProjectCacheState value, int index, boolean isSelected, boolean cellHasFocus) {
        if (list.getFont() != null) {
            setFont(list.getFont());
        }
        if (value != null) {
            setText(value.renderGenerationTime());
        } else {
            setText("Current snapshot");
        }

        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }
        return this;
    }
}
