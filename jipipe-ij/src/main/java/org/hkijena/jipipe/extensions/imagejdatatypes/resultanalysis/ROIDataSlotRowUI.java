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

package org.hkijena.jipipe.extensions.imagejdatatypes.resultanalysis;

import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.WindowManager;
import ij.gui.Roi;
import ij.macro.Interpreter;
import ij.plugin.frame.RoiManager;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeExportedDataTable;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.roi.Margin;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeDefaultResultDataSlotRowUI;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.UIUtils;

import java.awt.Frame;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Result UI for {@link ROIListData}
 */
public class ROIDataSlotRowUI extends JIPipeDefaultResultDataSlotRowUI {

    /**
     * @param workbenchUI the workbench
     * @param slot        the data slot
     * @param row         the slot row
     */
    public ROIDataSlotRowUI(JIPipeProjectWorkbench workbenchUI, JIPipeDataSlot slot, JIPipeExportedDataTable.Row row) {
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
                    UIUtils.getIconFromResources("data-types/roi.png"), e -> importROI(roiFile));
        }
    }

    /**
     * Imports and displays a ROI on the current image
     *
     * @param roiFile the file
     */
    public static void importROI(Path roiFile) {
        ROIListData rois = new ROIListData(ROIListData.loadRoiListFromFile(roiFile));
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp == null) {
            imp = rois.toMask(new Margin(), false, true, 1);
            imp.show();
        }
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
        for (Roi roi : rois) {
            roiManager.add(imp, roi, -1);
        }
        roiManager.runCommand("show all with labels");
    }

}
