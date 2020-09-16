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

package org.hkijena.jipipe.ui.resultanalysis;

import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeExportedDataTable;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;

import javax.swing.*;
import java.awt.BorderLayout;

/**
 * Renders a {@link JIPipeDataSlot} row as table cell
 */
public class JIPipeDefaultResultDataSlotPreviewUI extends JIPipeResultDataSlotPreviewUI {

    /**
     * Creates a new renderer
     *
     * @param table the table that renders the preview
     */
    public JIPipeDefaultResultDataSlotPreviewUI(JTable table) {
        super(table);
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        add(new JLabel("N/A"), BorderLayout.CENTER);
    }

    @Override
    public void render(JIPipeProjectWorkbench workbenchUI, JIPipeDataSlot slot, JIPipeExportedDataTable.Row row) {
    }
}
