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

package org.hkijena.jipipe.plugins.imagejdatatypes.display;

import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.WindowManager;
import ij.gui.Roi;
import ij.macro.Interpreter;
import ij.plugin.frame.RoiManager;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.desktop.api.data.JIPipeDesktopDataDisplayOperation;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.roi.Margin;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.Optional;

public class AddToROIManagerDataDisplayOperation implements JIPipeDesktopDataDisplayOperation {

    @Override
    public String getId() {
        return "jipipe:add-to-roi-manager";
    }

    @Override
    public void display(JIPipeData data, String displayName, JIPipeDesktopWorkbench desktopWorkbench, JIPipeDataSource source) {
        ROI2DListData rois = (ROI2DListData) data;
        Map<Optional<ImagePlus>, ROI2DListData> byImage = rois.groupByReferenceImage();

        RoiManager roiManager = null;
        if (Macro.getOptions() != null && Interpreter.isBatchMode())
            roiManager = Interpreter.getBatchModeRoiManager();
        if (roiManager == null) {
            Frame frame = WindowManager.getFrame("ROI Manager");
            if (frame == null)
                IJ.run("ROI Manager...");
            frame = WindowManager.getFrame("ROI Manager");
            if (!(frame instanceof RoiManager)) {
                return;
            }
            roiManager = (RoiManager) frame;
        }

        ImagePlus fallbackImage = WindowManager.getCurrentImage();
        Margin margin = new Margin();
        margin.getWidth().ensureExactValue(false);
        margin.getHeight().ensureExactValue(false);

        if (roiManager.getCount() > 0) {
            int result = JOptionPane.showOptionDialog(desktopWorkbench.getWindow(),
                    "The current ROI manager already contains ROI. What should be done?",
                    "Show ROI",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new Object[]{"Add to list", "New list", "Cancel"},
                    "Add to list");
            if (result == JOptionPane.NO_OPTION) {
                roiManager.reset();
            } else if (result == JOptionPane.CANCEL_OPTION) {
                return;
            }
        }

        for (Map.Entry<Optional<ImagePlus>, ROI2DListData> entry : byImage.entrySet()) {
            if (!entry.getKey().isPresent()) {
                if (fallbackImage == null) {
                    fallbackImage = entry.getValue().toMask(margin, false, true, 1);
                    fallbackImage.setTitle("Auto-generated based on ROI");
                    fallbackImage.show();
                }
                for (Roi roi : entry.getValue()) {
                    roiManager.add(fallbackImage, (Roi) roi.clone(), -1);
                }
            } else {
                ImagePlus target = ImageJUtils.duplicate(entry.getKey().get());
                target.setTitle(entry.getKey().get().getTitle());
                target.show();
                for (Roi roi : entry.getValue()) {
                    roiManager.add(target, (Roi) roi.clone(), -1);
                }
            }
        }

        roiManager.runCommand("show all with labels");
    }

    @Override
    public String getName() {
        return "Add to ROI manager";
    }

    @Override
    public String getDescription() {
        return "Adds the ROI list into the ImageJ ROI manager. Requires that you have opened an ImageJ image window.";
    }

    @Override
    public int getOrder() {
        return 5;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("apps/imagej.png");
    }
}
