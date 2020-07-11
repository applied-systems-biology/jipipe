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

package org.hkijena.jipipe.extensions.plots.ui.resultanalysis;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeExportedDataTable;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.registries.JIPipeUIDatatypeRegistry;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeResultDataSlotCellUI;
import org.hkijena.jipipe.utils.PathUtils;

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
public class PlotDataSlotCellUI extends JIPipeResultDataSlotCellUI {

    /**
     * Creates a new renderer
     */
    public PlotDataSlotCellUI() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    private Path findPlotPNGExport(JIPipeDataSlot slot, JIPipeExportedDataTable.Row row) {
        Path rowStorageFolder = getRowStorageFolder(slot, row);
        if (Files.isDirectory(rowStorageFolder)) {
            return PathUtils.findFileByExtensionIn(rowStorageFolder, ".png");
        }
        return null;
    }

    @Override
    public void render(JIPipeProjectWorkbench workbenchUI, JIPipeDataSlot slot, JIPipeExportedDataTable.Row row) {
        setIcon(JIPipeUIDatatypeRegistry.getInstance().getIconFor(slot.getAcceptedDataType()));
        Path pngExport = findPlotPNGExport(slot, row);
        if (pngExport == null) {
            setIcon(JIPipeUIDatatypeRegistry.getInstance().getIconFor(slot.getAcceptedDataType()));
        } else {
            try {
                BufferedImage image = ImageIO.read(pngExport.toFile());
                double factor = image.getWidth() / 100.0;
                Image scaledInstance = image.getScaledInstance(100, (int) (image.getHeight() / factor), Image.SCALE_FAST);
                setIcon(new ImageIcon(scaledInstance));
            } catch (IOException e) {
                setIcon(JIPipeUIDatatypeRegistry.getInstance().getIconFor(slot.getAcceptedDataType()));
            }
        }
        setText(JIPipeData.getNameOf(slot.getAcceptedDataType()));
    }
}
