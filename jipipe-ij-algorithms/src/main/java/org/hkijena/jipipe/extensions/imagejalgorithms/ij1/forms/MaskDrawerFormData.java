package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.forms;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.extensions.forms.datatypes.FormData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.ImageViewerPanel;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.CalibrationPlugin;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.LUTManagerPlugin;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.PixelInfoPlugin;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.maskdrawer.MaskDrawerPlugin;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.maskdrawer.MeasurementPlugin;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import java.awt.Component;
import java.util.Arrays;
import java.util.List;

/**
 * Structural {@link FormData}
 */
public class MaskDrawerFormData extends FormData {

    private final List<JIPipeMergingDataBatch> dataBatches;
    private final DrawMaskAlgorithm drawMaskAlgorithm;
    private ImageViewerPanel imageViewerPanel = new ImageViewerPanel();
    private MaskDrawerPlugin maskDrawerPlugin;

    public MaskDrawerFormData(List<JIPipeMergingDataBatch> dataBatches, DrawMaskAlgorithm drawMaskAlgorithm) {
        this.dataBatches = dataBatches;
        this.drawMaskAlgorithm = drawMaskAlgorithm;
        initializeImageViewer();
    }

    private void initializeImageViewer() {
        maskDrawerPlugin = new MaskDrawerPlugin(imageViewerPanel);
        imageViewerPanel.setPlugins(Arrays.asList(new CalibrationPlugin(imageViewerPanel),
                new PixelInfoPlugin(imageViewerPanel),
                new LUTManagerPlugin(imageViewerPanel),
                maskDrawerPlugin,
                new MeasurementPlugin(imageViewerPanel)));
    }

    @Override
    public boolean isUsingCustomCopy() {
        return true;
    }

    @Override
    public void customCopy(FormData source, JIPipeIssueReport report) {
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
    public JIPipeData duplicate() {
        return new MaskDrawerFormData(dataBatches, drawMaskAlgorithm);
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {

    }

    @Override
    public Component getEditor(JIPipeWorkbench workbench) {
        return imageViewerPanel;
    }

    @Override
    public void loadData(JIPipeMergingDataBatch dataBatch) {
        int row = dataBatches.indexOf(dataBatch);
        ImagePlus referenceImage = dataBatch.getInputData("Reference", ImagePlusData.class, new JIPipeProgressInfo()).get(0).getImage();
        ImagePlus maskImage = drawMaskAlgorithm.getOutputSlot("Mask").getData(row, ImagePlusData.class, new JIPipeProgressInfo()).getImage();
        imageViewerPanel.setImage(referenceImage);
        maskDrawerPlugin.setMask(maskImage);
    }

    @Override
    public void writeData(JIPipeMergingDataBatch dataBatch) {

    }
}
