package org.hkijena.acaq5.extension.ui.resultanalysis;

import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.WindowManager;
import ij.gui.Roi;
import ij.macro.Interpreter;
import ij.plugin.frame.RoiManager;
import org.hkijena.acaq5.api.ACAQRunSample;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.extension.api.datasources.ACAQROIDataFromFile;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.resultanalysis.ACAQDefaultDataSlotResultUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ROIDataSlotResultUI extends ACAQDefaultDataSlotResultUI {

    public ROIDataSlotResultUI(ACAQWorkbenchUI workbenchUI, ACAQRunSample sample, ACAQDataSlot<?> slot) {
        super(workbenchUI, sample, slot);
    }

    private Path findROIFile() {
        if (getSlot().getStoragePath() != null && Files.isDirectory(getSlot().getStoragePath())) {
            try {
                return Files.list(getSlot().getStoragePath()).filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".zip")).findFirst().orElse(null);
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    @Override
    protected void registerActions() {
        super.registerActions();

        Path roiFile = findROIFile();
        if (roiFile != null) {
            registerAction("Import into current image", UIUtils.getIconFromResources("data-types/roi.png"), e -> {
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
        for (Roi roi : ACAQROIDataFromFile.loadFromFile(roiFile)) {
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
