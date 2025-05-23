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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.convert;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.contrast.HistogramContrastEnhancerAlgorithm;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.primitives.vectors.Vector2dParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.vectors.VectorParameterSettings;
import org.hkijena.jipipe.utils.ImageJCalibrationMode;

@SetJIPipeDocumentation(name = "Convert image to 8-bit (ImageJ auto-contrast)", description = "Converts an image into 8-bit while applying ImageJ's automatic contrast functions per slice.")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscale8UData.class, name = "Output", create = true)
@ConfigureJIPipeNode(menuPath = "Convert", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nType", aliasName = "8-bit (per-slice auto-contrast)")
public class ConvertImageTo8BitAutoContrastAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private ImageJCalibrationMode calibrationMode = ImageJCalibrationMode.AutomaticImageJ;
    private Vector2dParameter customRange = new Vector2dParameter(0, 1);
    private boolean stretchHistogram = true;

    public ConvertImageTo8BitAutoContrastAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ConvertImageTo8BitAutoContrastAlgorithm(ConvertImageTo8BitAutoContrastAlgorithm other) {
        super(other);
        this.calibrationMode = other.calibrationMode;
        this.customRange = new Vector2dParameter(other.customRange);
        this.stretchHistogram = other.stretchHistogram;
    }

    @SetJIPipeDocumentation(name = "Stretch histogram", description = "If enabled, enhance the contrast of the output by applying histogram stretching (+ normalization). " +
            "You can also use 'Histogram-based contrast enhancer' to gain access to more settings.")
    @JIPipeParameter("stretch-histogram")
    public boolean isStretchHistogram() {
        return stretchHistogram;
    }

    @JIPipeParameter("stretch-histogram")
    public void setStretchHistogram(boolean stretchHistogram) {
        this.stretchHistogram = stretchHistogram;
    }

    @SetJIPipeDocumentation(name = "Calibration method", description = "The method to apply for calibration.")
    @JIPipeParameter("calibration-mode")
    public ImageJCalibrationMode getCalibrationMode() {
        return calibrationMode;
    }

    @JIPipeParameter("calibration-mode")
    public void setCalibrationMode(ImageJCalibrationMode calibrationMode) {
        this.calibrationMode = calibrationMode;
        emitParameterUIChangedEvent();
    }

    @SetJIPipeDocumentation(name = "Custom range", description = "The custom range if the calibration mode is set to 'Custom'")
    @JIPipeParameter("custom-range")
    @VectorParameterSettings(xLabel = "Min", yLabel = "Max")
    public Vector2dParameter getCustomRange() {
        return customRange;
    }

    @JIPipeParameter("custom-range")
    public void setCustomRange(Vector2dParameter customRange) {
        this.customRange = customRange;
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if ("custom-range".equalsIgnoreCase(access.getKey())) {
            return calibrationMode == ImageJCalibrationMode.Custom;
        }
        return super.isParameterUIVisible(tree, access);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus image = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();
        HistogramContrastEnhancerAlgorithm histogramContrastEnhancerAlgorithm = JIPipe.createNode(HistogramContrastEnhancerAlgorithm.class);
        ImagePlus result = ImageJIterationUtils.generateForEachIndexedZCTSlice(image, (ip, index) -> {
            // 8-bit is the lowest, so always write the calibration first and then convert
            ImagePlus imp = new ImagePlus("slice", ip.duplicate());
            Vector2dParameter minMax = ImageJUtils.calibrate(imp, calibrationMode, customRange.getX(), customRange.getY());
            ImageJUtils.writeCalibrationToPixels(imp.getProcessor(), minMax.getX(), minMax.getY());
            ImageProcessor resultIp = ImageJUtils.convertToGreyscale8UIfNeeded(imp).getProcessor();
            if (stretchHistogram) {
                histogramContrastEnhancerAlgorithm.stretchHistogram(resultIp, 0.35, true);
            }
            return resultIp;
        }, progressInfo);
        result.copyScale(image);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscale8UData(result), progressInfo);
    }
}
