package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.forms;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.extensions.forms.datatypes.FormData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imageviewer.ImageViewerPanel2D;
import org.hkijena.jipipe.extensions.imageviewer.plugins.CalibrationPlugin2D;
import org.hkijena.jipipe.extensions.imageviewer.plugins.LUTManagerPlugin2D;
import org.hkijena.jipipe.extensions.imageviewer.plugins.PixelInfoPlugin2D;
import org.hkijena.jipipe.extensions.imageviewer.plugins.maskdrawer2d.MaskDrawerPlugin2D;
import org.hkijena.jipipe.ui.JIPipeDummyWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

/**
 * Structural {@link FormData} for drawing masks
 */
public class MaskDrawerFormData extends FormData {

    private final List<JIPipeMergingDataBatch> dataBatches;
    private final DrawMaskAlgorithm drawMaskAlgorithm;
    private ImageViewerPanel2D imageViewerPanel;
    private MaskDrawerPlugin2D maskDrawerPlugin;
    private ImagePlus lazyLoadedImage;
    private ImagePlus lazyLoadedMask;

    public MaskDrawerFormData(List<JIPipeMergingDataBatch> dataBatches, DrawMaskAlgorithm drawMaskAlgorithm) {
        this.dataBatches = dataBatches;
        this.drawMaskAlgorithm = drawMaskAlgorithm;
    }

    private void initializeImageViewer() {
        imageViewerPanel = new ImageViewerPanel2D(new JIPipeDummyWorkbench());
        maskDrawerPlugin = new MaskDrawerPlugin2D(imageViewerPanel);
        imageViewerPanel.setPlugins(Arrays.asList(new CalibrationPlugin2D(imageViewerPanel),
                new PixelInfoPlugin2D(imageViewerPanel),
                new LUTManagerPlugin2D(imageViewerPanel),
                maskDrawerPlugin));
        if (lazyLoadedImage != null) {
            imageViewerPanel.setImage(lazyLoadedImage);
            maskDrawerPlugin.setMask(lazyLoadedMask);
            lazyLoadedImage = null;
            lazyLoadedMask = null;
        }
    }

    public ImageViewerPanel2D getImageViewerPanel() {
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

        MaskDrawerFormData sourceData = (MaskDrawerFormData) source;
        ImagePlus sourceMask = sourceData.maskDrawerPlugin.getMask();
        ImagePlus targetMask = maskDrawerPlugin.getMask();

        if (!ImageJUtils.imagesHaveSameSize(sourceMask, targetMask)) {
            report.reportIsInvalid("Could not copy mask due to different sizes!",
                    "The source mask is " + sourceMask + " and cannot be copied into the target " + targetMask,
                    "Ensure that the masks have the same size",
                    this);
            return;
        }

        ImageJUtils.copyBetweenImages(sourceMask, targetMask, new JIPipeProgressInfo());
        maskDrawerPlugin.recalculateMaskPreview();
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
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {

    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        // Initialize the viewer
        getImageViewerPanel();
        return new MaskDrawerFormData(dataBatches, drawMaskAlgorithm);
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
        ImagePlus maskImage = drawMaskAlgorithm.getOutputSlot("Mask").getData(row, ImagePlusGreyscaleMaskData.class, new JIPipeProgressInfo()).getImage();

        if (imageViewerPanel != null) {
            imageViewerPanel.setImage(referenceImage);
            maskDrawerPlugin.setMask(maskImage);
        } else {
            lazyLoadedImage = referenceImage;
            lazyLoadedMask = maskImage;
        }
    }

    @Override
    public void writeData(JIPipeMergingDataBatch dataBatch) {

    }
}
