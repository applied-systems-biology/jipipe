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

package org.hkijena.pipelinej.extensions.plots.ui.resultanalysis;

import org.hkijena.pipelinej.api.data.ACAQData;
import org.hkijena.pipelinej.api.data.ACAQDataSlot;
import org.hkijena.pipelinej.api.data.ACAQExportedDataTable;
import org.hkijena.pipelinej.ui.ACAQProjectWorkbench;
import org.hkijena.pipelinej.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.pipelinej.ui.resultanalysis.ACAQResultDataSlotCellUI;
import org.hkijena.pipelinej.utils.PathUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Renders filesystem data as table cell
 */
public class PlotDataSlotCellUI extends ACAQResultDataSlotCellUI {

    /**
     * Creates a new renderer
     */
    public PlotDataSlotCellUI() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    private Path findPlotPNGExport(ACAQDataSlot slot, ACAQExportedDataTable.Row row) {
        Path rowStorageFolder = getRowStorageFolder(slot, row);
        if (Files.isDirectory(rowStorageFolder)) {
            return PathUtils.findFileByExtensionIn(rowStorageFolder, ".png");
        }
        return null;
    }

    @Override
    public void render(ACAQProjectWorkbench workbenchUI, ACAQDataSlot slot, ACAQExportedDataTable.Row row) {
        setIcon(ACAQUIDatatypeRegistry.getInstance().getIconFor(slot.getAcceptedDataType()));
        Path pngExport = findPlotPNGExport(slot, row);
        if (pngExport == null) {
            setIcon(ACAQUIDatatypeRegistry.getInstance().getIconFor(slot.getAcceptedDataType()));
        } else {
            try {
                BufferedImage image = ImageIO.read(pngExport.toFile());
                double factor = image.getWidth() / 100.0;
                Image scaledInstance = image.getScaledInstance(100, (int) (image.getHeight() / factor), Image.SCALE_FAST);
                setIcon(new ImageIcon(scaledInstance));
            } catch (IOException e) {
                setIcon(ACAQUIDatatypeRegistry.getInstance().getIconFor(slot.getAcceptedDataType()));
            }
        }
        setText(ACAQData.getNameOf(slot.getAcceptedDataType()));
    }
}
