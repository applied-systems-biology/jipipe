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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.misc;

import ij.ImagePlus;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColorRGBData;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;

@SetJIPipeDocumentation(name = "Data to preview", description = "Converts any data into preview image. Does not generate a result if no previews are supported.")
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Convert")
@AddJIPipeInputSlot(value = JIPipeData.class, slotName = "Data", create = true)
@AddJIPipeOutputSlot(value = ImagePlus2DColorRGBData.class, slotName = "Preview", create = true)
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        JIPipeData data = iterationStep.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo);
        Component preview = data.createThumbnail(previewWidth, previewHeight, progressInfo).renderToComponent(previewWidth, previewHeight);
        if (preview != null) {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    preview.setSize(previewWidth, previewHeight);
                    BufferedImage image = new BufferedImage(previewWidth, previewHeight, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = (Graphics2D) image.getGraphics();
                    g.setColor(Color.WHITE);
                    g.fillRect(0, 0, previewWidth, previewHeight);
                    preview.print(g);
                    iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlus2DColorRGBData(new ImagePlus("Preview of " + data, image)), progressInfo);
                });
            } catch (InterruptedException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        super.reportValidity(reportContext, report);
        if (previewWidth < 1) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new ParameterValidationReportContext(reportContext, this, "Preview width", "preview-width"),
                    "Preview width too small!",
                    "The preview width must be greater than zero!"));
        }
        if (previewHeight < 1) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new ParameterValidationReportContext(reportContext, this, "Preview height", "preview-height"),
                    "Preview height too small!",
                    "The preview height must be greater than zero!"));
        }
    }

    @SetJIPipeDocumentation(name = "Preview width", description = "The width of the generated image.")
    @JIPipeParameter(value = "preview-width", uiOrder = 10)
    public int getPreviewWidth() {
        return previewWidth;
    }

    @JIPipeParameter("preview-width")
    public void setPreviewWidth(int previewWidth) {
        this.previewWidth = previewWidth;
    }

    @SetJIPipeDocumentation(name = "Preview height", description = "The height of the generated image.")
    @JIPipeParameter(value = "preview-height", uiOrder = 11)
    public int getPreviewHeight() {
        return previewHeight;
    }

    @JIPipeParameter("preview-height")
    public void setPreviewHeight(int previewHeight) {
        this.previewHeight = previewHeight;
    }
}
