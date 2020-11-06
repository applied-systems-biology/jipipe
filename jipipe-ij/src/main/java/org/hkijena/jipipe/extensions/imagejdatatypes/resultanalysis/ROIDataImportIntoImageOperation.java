package org.hkijena.jipipe.extensions.imagejdatatypes.resultanalysis;

import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.WindowManager;
import ij.gui.Roi;
import ij.macro.Interpreter;
import ij.plugin.frame.RoiManager;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataImportOperation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeExportedDataTable;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.roi.Margin;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.Frame;
import java.nio.file.Files;
import java.nio.file.Path;

public class ROIDataImportIntoImageOperation implements JIPipeDataImportOperation {

    private Path findROIFile(Path rowStorageFolder) {
        if (rowStorageFolder != null && Files.isDirectory(rowStorageFolder)) {
            Path zipFile = PathUtils.findFileByExtensionIn(rowStorageFolder, ".zip");
            if (zipFile == null) {
                return PathUtils.findFileByExtensionIn(rowStorageFolder, ".roi");
            } else {
                return zipFile;
            }
        }
        return null;
    }

    @Override
    public JIPipeData show(JIPipeDataSlot slot, JIPipeExportedDataTable.Row row, Path rowStorageFolder, String compartmentName, String algorithmName, String displayName, JIPipeWorkbench workbench) {
        Path roiFile = findROIFile(rowStorageFolder);
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
                    return rois;
                }
                roiManager = (RoiManager) frame;
            }
        }
        for (Roi roi : rois) {
            roiManager.add(imp, roi, -1);
        }
        roiManager.runCommand("show all with labels");
        return rois;
    }

    @Override
    public String getName() {
        return "Import into current image";
    }

    @Override
    public String getDescription() {
        return "Adds ROIs into the ROI manager for the current image";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("apps/imagej.png");
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
