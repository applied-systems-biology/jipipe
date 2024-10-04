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

package org.hkijena.jipipe.desktop.app.resultanalysis;

import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataAnnotationInfo;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableRowInfo;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;

import javax.swing.*;
import java.awt.*;

/**
 * Renders a {@link JIPipeDataSlot} row as table cell
 */
public class JIPipeDesktopDefaultResultDataSlotPreview extends JIPipeDesktopResultDataSlotPreview {
    /**
     * Creates a new renderer
     *
     * @param workbench      the workbench
     * @param table          the table where the data is rendered in
     * @param slot           the data slot
     * @param row            the row
     * @param dataAnnotation the data annotation (optional)
     */
    public JIPipeDesktopDefaultResultDataSlotPreview(JIPipeDesktopProjectWorkbench workbench, JTable table, JIPipeDataSlot slot, JIPipeDataTableRowInfo row, JIPipeDataAnnotationInfo dataAnnotation) {
        super(workbench, table, slot, row, dataAnnotation);
        initialize();
    }

    private void initialize() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        add(new JLabel("N/A"), BorderLayout.CENTER);
    }

    @Override
    public void renderPreview() {

    }
}
