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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.forms;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.CustomValidationReportContext;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopDummyWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.plugins.forms.datatypes.FormData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imageviewer.JIPipeImageViewer;
import org.hkijena.jipipe.plugins.imageviewer.plugins2d.CalibrationPlugin2D;
import org.hkijena.jipipe.plugins.imageviewer.plugins2d.LUTManagerPlugin2D;
import org.hkijena.jipipe.plugins.imageviewer.plugins2d.PixelInfoPlugin2D;
import org.hkijena.jipipe.plugins.imageviewer.plugins2d.maskdrawer.MaskToROIDrawerPlugin2D;
import org.hkijena.jipipe.plugins.imageviewer.plugins2d.roimanager.ROIManagerPlugin2D;

import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Structural {@link FormData} for drawing ROIs
 */
public class ROIDrawerFormData extends FormData {

    private final List<JIPipeMultiIterationStep> iterationSteps;
    private final DrawROIAlgorithm drawROIAlgorithm;
    private JIPipeImageViewer imageViewerPanel;

    private ROIManagerPlugin2D roiManagerPlugin;
    private MaskToROIDrawerPlugin2D maskDrawerPlugin;
    private ImagePlus lazyLoadedImage;
    private ROI2DListData lazyLoadedROIs;

    public ROIDrawerFormData(List<JIPipeMultiIterationStep> iterationSteps, DrawROIAlgorithm drawROIAlgorithm) {
        this.iterationSteps = iterationSteps;
        this.drawROIAlgorithm = drawROIAlgorithm;
    }

    private void initializeImageViewer() {
        imageViewerPanel = new JIPipeImageViewer(new JIPipeDesktopDummyWorkbench(), Arrays.asList(CalibrationPlugin2D.class,
                PixelInfoPlugin2D.class,
                LUTManagerPlugin2D.class,
                ROIManagerPlugin2D.class,
                MaskToROIDrawerPlugin2D.class), Collections.emptyMap());
        maskDrawerPlugin = imageViewerPanel.getPlugin(MaskToROIDrawerPlugin2D.class);
        roiManagerPlugin = imageViewerPanel.getPlugin(ROIManagerPlugin2D.class);
        if (lazyLoadedImage != null) {
            imageViewerPanel.setImageData(new ImagePlusData(lazyLoadedImage));
            roiManagerPlugin.setRois(lazyLoadedROIs, false);
            lazyLoadedImage = null;
            lazyLoadedROIs = null;
        }
    }

    public JIPipeImageViewer getImageViewerPanel() {
        if (imageViewerPanel == null || maskDrawerPlugin == null) {
            initializeImageViewer();
        }
        return imageViewerPanel;
    }

    @Override
    public boolean isUsingCustomCopy() {
        return true;
    }

    @Override
    public void customCopy(FormData source, CustomValidationReportContext context, JIPipeValidationReport report) {
        // Initialize the viewer
        getImageViewerPanel();

        ROIDrawerFormData sourceData = (ROIDrawerFormData) source;
        ROI2DListData sourceROIs = sourceData.roiManagerPlugin.getRois();
        roiManagerPlugin.setRois(new ROI2DListData(sourceROIs), false);
    }

    @Override
    public boolean isUsingCustomReset() {
        return true;
    }

    @Override
    public void customReset() {
        // Initialize the viewer
        getImageViewerPanel();

        ImagePlus targetMask = maskDrawerPlugin.getMask();
        ImageJUtils.forEachIndexedZCTSlice(targetMask, (targetProcessor, index) -> {
            targetProcessor.setValue(0);
            targetProcessor.fillRect(0, 0, targetProcessor.getWidth(), targetProcessor.getHeight());
        }, new JIPipeProgressInfo());
        maskDrawerPlugin.recalculateMaskPreview();

        roiManagerPlugin.clearROIs(false);
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {

    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        // Initialize the viewer
        getImageViewerPanel();
        return new ROIDrawerFormData(iterationSteps, drawROIAlgorithm);
    }

    @Override
    public Component getEditor(JIPipeDesktopWorkbench workbench) {
        return getImageViewerPanel();
    }

    @Override
    public void loadData(JIPipeMultiIterationStep iterationStep) {
        int row = iterationSteps.indexOf(iterationStep);
        ImagePlus referenceImage = iterationStep.getInputData("Reference", ImagePlusData.class, new JIPipeProgressInfo()).get(0).getImage();
        ROI2DListData rois = drawROIAlgorithm.getOutputSlot("ROI").getData(row, ROI2DListData.class, new JIPipeProgressInfo());

        if (imageViewerPanel != null) {
            imageViewerPanel.setImagePlus(referenceImage);
            roiManagerPlugin.setRois(rois, false);
        } else {
            lazyLoadedImage = referenceImage;
            lazyLoadedROIs = rois;
        }
    }

    @Override
    public void writeData(JIPipeMultiIterationStep iterationStep) {

    }
}
