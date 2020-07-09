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

package org.hkijena.pipelinej.ui.resultanalysis;

import org.hkijena.pipelinej.api.data.ACAQData;
import org.hkijena.pipelinej.api.data.ACAQDataSlot;
import org.hkijena.pipelinej.api.data.ACAQExportedDataTable;
import org.hkijena.pipelinej.ui.ACAQProjectWorkbench;
import org.hkijena.pipelinej.ui.registries.ACAQUIDatatypeRegistry;

import javax.swing.*;

/**
 * Renders a {@link ACAQDataSlot} row as table cell
 */
public class ACAQDefaultResultDataSlotCellUI extends ACAQResultDataSlotCellUI {

    /**
     * Creates a new renderer
     */
    public ACAQDefaultResultDataSlotCellUI() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public void render(ACAQProjectWorkbench workbenchUI, ACAQDataSlot slot, ACAQExportedDataTable.Row row) {
        setIcon(ACAQUIDatatypeRegistry.getInstance().getIconFor(slot.getAcceptedDataType()));
        setText(ACAQData.getNameOf(slot.getAcceptedDataType()));
    }
}
