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

package org.hkijena.acaq5.extensions.imagejdatatypes.resultanalysis;

import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.WindowManager;
import ij.gui.Roi;
import ij.macro.Interpreter;
import ij.plugin.frame.RoiManager;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQExportedDataTable;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.resultanalysis.ACAQDefaultResultDataSlotRowUI;
import org.hkijena.acaq5.utils.PathUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Result UI for {@link ROIListData}
 */
public class ROIDataSlotRowUI extends ACAQDefaultResultDataSlotRowUI {

    /**
     * @param workbenchUI the workbench
     * @param slot        the data slot
     * @param row         the slot row
     */
    public ROIDataSlotRowUI(ACAQProjectWorkbench workbenchUI, ACAQDataSlot slot, ACAQExportedDataTable.Row row) {
        super(workbenchUI, slot, row);
    }

    private Path findROIFile() {
        if (getRowStorageFolder() != null && Files.isDirectory(getRowStorageFolder())) {
            Path zipFile = PathUtils.findFileByExtensionIn(getRowStorageFolder(), ".zip");
            if (zipFile == null) {
                return PathUtils.findFileByExtensionIn(getRowStorageFolder(), ".roi");
            } else {
                return zipFile;
            }
        }
        return null;
    }

    @Override
    protected void registerActions() {
        super.registerActions();

        Path roiFile = findROIFile();
        if (roiFile != null) {
            registerAction("Import into current image", "Annotates the currently open image with the ROI annotations.",
                    UIUtils.getIconFromResources("data-types/roi.png"), e -> {
                        if (IJ.getImage() == null) {
                            JOptionPane.showMessageDialog(this, "There is no current image open in ImageJ!", "Import ROI", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        importROI(roiFile);
                    });
        }
    }

    /**
     * Imports and displays a ROI on the current image
     *
     * @param roiFile the file
     */
    public static void importROI(Path roiFile) {
        ImagePlus imp = IJ.getImage();
        RoiManager roiManager = null;
        if (roiManager == null) {
            if (Macro.getOptions() != null && Interpreter.isBatchMode())
                roiManager = Interpreter.getBatchModeRoiManager();
            if (roiManager == null) {
                Frame frame = WindowManager.getFrame("ROI Manager");
                if (frame == null)
                    IJ.run("ROI Manager...");
                frame = WindowManager.getFrame("ROI Manager");
                if (frame == null || !(frame instanceof RoiManager)) {
                    return;
                }
                roiManager = (RoiManager) frame;
            }
        }
        for (Roi roi : ROIListData.loadRoiListFromFile(roiFile)) {
            roiManager.add(imp, roi, -1);
        }
        roiManager.runCommand("show all with labels");
    }

}
