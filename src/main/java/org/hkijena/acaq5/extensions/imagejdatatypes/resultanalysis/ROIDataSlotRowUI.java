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
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ACAQROIData;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.resultanalysis.ACAQDefaultResultDataSlotRowUI;
import org.hkijena.acaq5.utils.PathUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class ROIDataSlotRowUI extends ACAQDefaultResultDataSlotRowUI {

    public ROIDataSlotRowUI(ACAQWorkbenchUI workbenchUI, ACAQDataSlot slot, ACAQExportedDataTable.Row row) {
        super(workbenchUI, slot, row);
    }

    private Path findROIFile() {
        if (getRowStorageFolder() != null && Files.isDirectory(getRowStorageFolder())) {
            return PathUtils.findFileByExtensionIn(getRowStorageFolder(), ".zip");
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
                        importROI(roiFile);
                    });
        }
    }

    private void importROI(Path roiFile) {
        if (IJ.getImage() == null) {
            JOptionPane.showMessageDialog(this, "There is no current image open in ImageJ!", "Import ROI", JOptionPane.ERROR_MESSAGE);
            return;
        }

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
        for (Roi roi : ACAQROIData.loadRoiListFromFile(roiFile)) {
            if (imp.getStackSize() > 1) {
                int n = imp.getCurrentSlice();
                if (imp.isHyperStack()) {
                    int[] pos = imp.convertIndexToPosition(n);
                    roi.setPosition(pos[0], pos[1], pos[2]);
                } else
                    roi.setPosition(n);
            }
            roiManager.add(imp, roi, -1);
        }
        roiManager.runCommand("show all with labels");
    }
}
