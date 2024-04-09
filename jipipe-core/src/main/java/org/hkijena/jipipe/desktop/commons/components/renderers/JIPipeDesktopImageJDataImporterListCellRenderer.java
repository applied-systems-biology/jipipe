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
import org.hkijena.jipipe.api.compat.ImageJDataImporter;

import javax.swing.*;
import java.awt.*;

/**
 * Renders {@link ImageJDataImporter}
 */
public class JIPipeDesktopImageJDataImporterListCellRenderer extends JLabel implements ListCellRenderer<ImageJDataImporter> {

    /**
     * Creates a new renderer
     */
    public JIPipeDesktopImageJDataImporterListCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends ImageJDataImporter> list, ImageJDataImporter value, int index, boolean isSelected, boolean cellHasFocus) {
        if (list.getFont() != null) {
            setFont(list.getFont());
        }
        if (value != null) {
            setText(value.getName());
            setIcon(JIPipe.getDataTypes().getIconFor(value.getImportedJIPipeDataType()));
        } else {
            setText("<No importer>");
            setIcon(null);
        }

        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }
        return this;
    }
}
