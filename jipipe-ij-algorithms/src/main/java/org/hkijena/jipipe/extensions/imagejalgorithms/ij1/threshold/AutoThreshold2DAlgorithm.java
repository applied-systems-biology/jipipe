/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.threshold;

import com.fathzer.soft.javaluator.StaticVariableSet;
import ij.ImagePlus;
import ij.process.AutoThresholder;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameter;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalAnnotationNameParameter;

import java.util.ArrayList;
import java.util.List;

import static org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension.ADD_MASK_QUALIFIER;

/**
 * Segmenter node that thresholds via an auto threshold
 */
@JIPipeDocumentation(name = "Auto threshold 2D", description = "Applies an auto-thresholding algorithm. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeOrganization(menuPath = "Threshold", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscale8UData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output")
public class AutoThreshold2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private AutoThresholder.Method method = AutoThresholder.Method.Default;
    private boolean darkBackground = true;
    private OptionalAnnotationNameParameter thresholdAnnotation = new OptionalAnnotationNameParameter("Threshold", true);
    private boolean applyThresholdPerSlice = true;
    private DefaultExpressionParameter thresholdCombinationExpression = new DefaultExpressionParameter("MIN(thresholds)");

    /**
     * @param info the info
     */
    public AutoThreshold2DAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusGreyscale8UData.class)
                .addOutputSlot("Output", ImagePlusGreyscaleMaskData.class, "Input", ADD_MASK_QUALIFIER)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public AutoThreshold2DAlgorithm(AutoThreshold2DAlgorithm other) {
        super(other);
        this.method = other.method;
        this.darkBackground = other.darkBackground;
        this.thresholdAnnotation = new OptionalAnnotationNameParameter(other.thresholdAnnotation);
        this.applyThresholdPerSlice = other.applyThresholdPerSlice;
        this.thresholdCombinationExpression = new DefaultExpressionParameter(thresholdCombinationExpression);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscale8UData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
        AutoThresholder autoThresholder = new AutoThresholder();
        if(applyThresholdPerSlice) {
            List<Integer> thresholds = new ArrayList<>();
            ImageJUtils.forEachSlice(img, ip -> {
                if (!darkBackground)
                    ip.invert();
                int threshold = autoThresholder.getThreshold(method, ip.getHistogram());
                ip.threshold(threshold);
                thresholds.add(threshold);
            }, progressInfo);
            List<JIPipeAnnotation> annotations = new ArrayList<>();
            if(thresholdAnnotation.isEnabled()) {
                StaticVariableSet<Object> variableSet = new StaticVariableSet<>();
                variableSet.set("thresholds", thresholds);
                String result = thresholdCombinationExpression.evaluate(variableSet) + "";
                annotations.add(thresholdAnnotation.createAnnotation(result));
            }
            dataBatch.addOutputData(getFirstOutputSlot(),
                    new ImagePlusGreyscaleMaskData(img),
                    annotations,
                    JIPipeAnnotationMergeStrategy.Merge,
                    progressInfo);
        }
        else {
            List<Integer> thresholds = new ArrayList<>();
            ImageJUtils.forEachSlice(img, ip -> {
                if (!darkBackground)
                    ip.invert();
                int threshold = autoThresholder.getThreshold(method, ip.getHistogram());
                thresholds.add(threshold);
            }, progressInfo);

            // Combine thresholds
            StaticVariableSet<Object> variableSet = new StaticVariableSet<>();
            variableSet.set("thresholds", thresholds);
            Number combined = (Number) thresholdCombinationExpression.evaluate(variableSet);
            int threshold = Math.min(255, Math.max(0, combined.intValue()));
            List<JIPipeAnnotation> annotations = new ArrayList<>();
            if(thresholdAnnotation.isEnabled()) {
                annotations.add(thresholdAnnotation.createAnnotation("" + threshold));
            }
            ImageJUtils.forEachSlice(img, ip -> {
               ip.threshold(threshold);
            }, progressInfo);
            dataBatch.addOutputData(getFirstOutputSlot(),
                    new ImagePlusGreyscaleMaskData(img),
                    annotations,
                    JIPipeAnnotationMergeStrategy.Merge,
                    progressInfo);
        }
    }

    @JIPipeParameter("method")
    @JIPipeDocumentation(name = "Method")
    public AutoThresholder.Method getMethod() {
        return method;
    }

    @JIPipeParameter("method")
    public void setMethod(AutoThresholder.Method method) {
        this.method = method;
    }

    @JIPipeDocumentation(name = "Dark background", description = "If the background color is dark. Disable this if your image has a bright background.")
    @JIPipeParameter("dark-background")
    public boolean isDarkBackground() {
        return darkBackground;
    }

    @JIPipeParameter("dark-background")
    public void setDarkBackground(boolean darkBackground) {
        this.darkBackground = darkBackground;
    }

    @JIPipeDocumentation(name = "Threshold annotation", description = "Puts the generated threshold(s) into an annotation.")
    @JIPipeParameter("threshold-annotation")
    public OptionalAnnotationNameParameter getThresholdAnnotation() {
        return thresholdAnnotation;
    }

    @JIPipeParameter("threshold-annotation")
    public void setThresholdAnnotation(OptionalAnnotationNameParameter thresholdAnnotation) {
        this.thresholdAnnotation = thresholdAnnotation;
    }

    @JIPipeDocumentation(name = "Threshold per slice", description = "If enabled, the auto-thresholding is applied per slice.")
    @JIPipeParameter("threshold-per-slice")
    public boolean isApplyThresholdPerSlice() {
        return applyThresholdPerSlice;
    }

    @JIPipeParameter("threshold-per-slice")
    public void setApplyThresholdPerSlice(boolean applyThresholdPerSlice) {
        this.applyThresholdPerSlice = applyThresholdPerSlice;
    }

    @JIPipeDocumentation(name = "Threshold combination function", description = "Used if 'Threshold per slice' is turned off, or if an annotation is generated. " +
            "This expression combines multiple thresholds into one numeric threshold. Can output string if 'Threshold per slice' is enabled, as the expression" +
            " is only used for annotations.")
    @ExpressionParameterSettings(variableSource = ThresholdsExpressionParameterVariableSource.class)
    @JIPipeParameter("threshold-combine-expression")
    public DefaultExpressionParameter getThresholdCombinationExpression() {
        return thresholdCombinationExpression;
    }

    @JIPipeParameter("threshold-combine-expression")
    public void setThresholdCombinationExpression(DefaultExpressionParameter thresholdCombinationExpression) {
        this.thresholdCombinationExpression = thresholdCombinationExpression;
    }
}
