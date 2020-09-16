package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.misc;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColorRGBData;

import javax.swing.*;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;
import java.util.function.Supplier;

@JIPipeDocumentation(name = "Data to preview", description = "Converts any data into preview image. Does not generate a result if no previews are supported.")
@JIPipeOrganization(nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        JIPipeData data = dataBatch.getInputData(getFirstInputSlot(), JIPipeData.class);
        Component preview = data.preview(previewWidth, previewHeight);
        if (preview != null) {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    preview.setSize(previewWidth, previewHeight);
                    BufferedImage image = new BufferedImage(previewWidth, previewHeight, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = (Graphics2D) image.getGraphics();
                    preview.print(g);
                    dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlus2DColorRGBData(new ImagePlus("Preview of " + data, image)));
                });
            } catch (InterruptedException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        super.reportValidity(report);
        report.forCategory("Preview width").checkIfWithin(this, previewWidth, 1, Double.POSITIVE_INFINITY, true, false);
        report.forCategory("Preview height").checkIfWithin(this, previewHeight, 1, Double.POSITIVE_INFINITY, true, false);
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
