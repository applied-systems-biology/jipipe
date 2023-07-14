package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.misc;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryCause;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColorRGBData;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;

@JIPipeDocumentation(name = "Data to preview", description = "Converts any data into preview image. Does not generate a result if no previews are supported.")
@JIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Convert")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Data", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlus2DColorRGBData.class, slotName = "Preview", autoCreate = true)
public class DataToPreviewAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private int previewWidth = 64;
    private int previewHeight = 64;

    public DataToPreviewAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public DataToPreviewAlgorithm(DataToPreviewAlgorithm other) {
        super(other);
        this.previewWidth = other.previewWidth;
        this.previewHeight = other.previewHeight;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        JIPipeData data = dataBatch.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo);
        Component preview = data.preview(previewWidth, previewHeight);
        if (preview != null) {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    preview.setSize(previewWidth, previewHeight);
                    BufferedImage image = new BufferedImage(previewWidth, previewHeight, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = (Graphics2D) image.getGraphics();
                    g.setColor(Color.WHITE);
                    g.fillRect(0, 0, previewWidth, previewHeight);
                    preview.print(g);
                    dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlus2DColorRGBData(new ImagePlus("Preview of " + data, image)), progressInfo);
                });
            } catch (InterruptedException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void reportValidity(JIPipeValidationReportEntryCause parentCause, JIPipeValidationReport report) {
        super.reportValidity(parentCause, report);
        report.resolve("Preview width").checkIfWithin(this, previewWidth, 1, Double.POSITIVE_INFINITY, true, false);
        report.resolve("Preview height").checkIfWithin(this, previewHeight, 1, Double.POSITIVE_INFINITY, true, false);
    }

    @JIPipeDocumentation(name = "Preview width", description = "The width of the generated image.")
    @JIPipeParameter(value = "preview-width", uiOrder = 10)
    public int getPreviewWidth() {
        return previewWidth;
    }

    @JIPipeParameter("preview-width")
    public void setPreviewWidth(int previewWidth) {
        this.previewWidth = previewWidth;
    }

    @JIPipeDocumentation(name = "Preview height", description = "The height of the generated image.")
    @JIPipeParameter(value = "preview-height", uiOrder = 11)
    public int getPreviewHeight() {
        return previewHeight;
    }

    @JIPipeParameter("preview-height")
    public void setPreviewHeight(int previewHeight) {
        this.previewHeight = previewHeight;
    }
}
