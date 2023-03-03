package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.forms;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.extensions.forms.datatypes.FormData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imageviewer.ImageViewerPanel;
import org.hkijena.jipipe.extensions.imageviewer.plugins2d.CalibrationPlugin2D;
import org.hkijena.jipipe.extensions.imageviewer.plugins2d.LUTManagerPlugin2D;
import org.hkijena.jipipe.extensions.imageviewer.plugins2d.PixelInfoPlugin2D;
import org.hkijena.jipipe.extensions.imageviewer.plugins2d.maskdrawer.MaskToROIDrawerPlugin2D;
import org.hkijena.jipipe.extensions.imageviewer.plugins2d.roimanager.ROIManagerPlugin2D;
import org.hkijena.jipipe.ui.JIPipeDummyWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Structural {@link FormData} for drawing ROIs
 */
public class ROIDrawerFormData extends FormData {

    private final List<JIPipeMergingDataBatch> dataBatches;
    private final DrawROIAlgorithm drawROIAlgorithm;
    private ImageViewerPanel imageViewerPanel;

    private ROIManagerPlugin2D roiManagerPlugin;
    private MaskToROIDrawerPlugin2D maskDrawerPlugin;
    private ImagePlus lazyLoadedImage;
    private ROIListData lazyLoadedROIs;

    public ROIDrawerFormData(List<JIPipeMergingDataBatch> dataBatches, DrawROIAlgorithm drawROIAlgorithm) {
        this.dataBatches = dataBatches;
        this.drawROIAlgorithm = drawROIAlgorithm;
    }

    private void initializeImageViewer() {
        imageViewerPanel = new ImageViewerPanel(new JIPipeDummyWorkbench(), Arrays.asList(CalibrationPlugin2D.class,
                PixelInfoPlugin2D.class,
                LUTManagerPlugin2D.class,
                ROIManagerPlugin2D.class,
                MaskToROIDrawerPlugin2D.class), Collections.emptyMap());
        maskDrawerPlugin = imageViewerPanel.getPlugin(MaskToROIDrawerPlugin2D.class);
        roiManagerPlugin = imageViewerPanel.getPlugin(ROIManagerPlugin2D.class);
        if (lazyLoadedImage != null) {
            imageViewerPanel.setImage(lazyLoadedImage);
            roiManagerPlugin.setRois(lazyLoadedROIs, false);
            lazyLoadedImage = null;
            lazyLoadedROIs = null;
        }
    }

    public ImageViewerPanel getImageViewerPanel() {
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
    public void customCopy(FormData source, JIPipeIssueReport report) {
        // Initialize the viewer
        getImageViewerPanel();

        ROIDrawerFormData sourceData = (ROIDrawerFormData) source;
        ROIListData sourceROIs = sourceData.roiManagerPlugin.getRois();
        roiManagerPlugin.setRois(new ROIListData(sourceROIs), false);
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
    public void reportValidity(JIPipeIssueReport report) {

    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        // Initialize the viewer
        getImageViewerPanel();
        return new ROIDrawerFormData(dataBatches, drawROIAlgorithm);
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {

    }

    @Override
    public Component getEditor(JIPipeWorkbench workbench) {
        return getImageViewerPanel();
    }

    @Override
    public void loadData(JIPipeMergingDataBatch dataBatch) {
        int row = dataBatches.indexOf(dataBatch);
        ImagePlus referenceImage = dataBatch.getInputData("Reference", ImagePlusData.class, new JIPipeProgressInfo()).get(0).getImage();
        ROIListData rois = drawROIAlgorithm.getOutputSlot("ROI").getData(row, ROIListData.class, new JIPipeProgressInfo());

        if (imageViewerPanel != null) {
            imageViewerPanel.setImage(referenceImage);
            roiManagerPlugin.setRois(rois, false);
        } else {
            lazyLoadedImage = referenceImage;
            lazyLoadedROIs = rois;
        }
    }

    @Override
    public void writeData(JIPipeMergingDataBatch dataBatch) {

    }
}
