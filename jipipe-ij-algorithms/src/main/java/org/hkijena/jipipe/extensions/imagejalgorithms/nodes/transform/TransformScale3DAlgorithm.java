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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.transform;

import ij.ImagePlus;
import ij.plugin.Resizer;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.OptionalJIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.parameters.InterpolationMethod;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImagePlusPropertiesExpressionParameterVariablesInfo;

/**
 * Wrapper around {@link ImageProcessor}
 */
@JIPipeDocumentation(name = "Scale 3D image", description = "Scales a 3D image.")
@JIPipeNode(menuPath = "Transform", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image", aliasName = "Scale... (3D)")
public class TransformScale3DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private InterpolationMethod interpolationMethod = InterpolationMethod.Bilinear;
    private OptionalJIPipeExpressionParameter xAxis = new OptionalJIPipeExpressionParameter(true, "width");
    private OptionalJIPipeExpressionParameter yAxis = new OptionalJIPipeExpressionParameter(true, "height");
    private OptionalJIPipeExpressionParameter zAxis = new OptionalJIPipeExpressionParameter(true, "MAX(num_z, num_c, num_t)");

    private TransformScale2DAlgorithm scale2DAlgorithm =
            JIPipe.createNode("ij1-transform-scale2d");

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public TransformScale3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
        xAxis.setEnabled(true);
        yAxis.setEnabled(true);
        zAxis.setEnabled(true);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public TransformScale3DAlgorithm(TransformScale3DAlgorithm other) {
        super(other);
        this.interpolationMethod = other.interpolationMethod;
        this.xAxis = new OptionalJIPipeExpressionParameter(other.xAxis);
        this.yAxis = new OptionalJIPipeExpressionParameter(other.yAxis);
        this.zAxis = new OptionalJIPipeExpressionParameter(other.zAxis);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        ImagePlus img = inputData.getImage();

        // Scale in 2D if needed
        int sx = img.getWidth();
        int sy = img.getHeight();

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        ImagePlusPropertiesExpressionParameterVariablesInfo.extractValues(variables, img, iterationStep.getMergedTextAnnotations().values());

        if (xAxis.isEnabled() && yAxis.isEnabled()) {
            variables.set("x", sx);
            sx = (int) xAxis.getContent().evaluateToNumber(variables);
            variables.set("x", sy);
            sy = (int) yAxis.getContent().evaluateToNumber(variables);
        } else if (xAxis.isEnabled()) {
            variables.set("x", sx);
            sx = (int) xAxis.getContent().evaluateToNumber(variables);
            double fac = (double) sx / img.getWidth();
            sy = (int) (sy * fac);
        } else if (yAxis.isEnabled()) {
            variables.set("x", sy);
            sy = (int) yAxis.getContent().evaluateToNumber(variables);
            double fac = (double) sy / img.getHeight();
            sx = (int) (sx * fac);
        }

        if (sx != img.getWidth() || sy != img.getHeight()) {
            scale2DAlgorithm.clearSlotData();
            scale2DAlgorithm.setxAxis(xAxis);
            scale2DAlgorithm.setyAxis(yAxis);
            scale2DAlgorithm.getFirstInputSlot().addData(new ImagePlusData(img), progressInfo);
            scale2DAlgorithm.run(progressInfo);
            img = scale2DAlgorithm.getFirstOutputSlot().getData(0, ImagePlusData.class, progressInfo).getImage();
        }

        // Scale in 3D
        if (img.getStackSize() > 1) {
            int sz = img.getStackSize();
            if (zAxis.isEnabled()) {
                sz = (int) zAxis.getContent().evaluateToNumber(variables);
            } else {
                double fac = Math.min((double) sx / img.getWidth(), (double) sy / img.getHeight());
                sz = (int) (sz * fac);
            }

            if (sz != img.getStackSize()) {
                Resizer resizer = new Resizer();
                img = resizer.zScale(img, sz, interpolationMethod.getNativeValue());
            }
        }

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), progressInfo);
    }

    @JIPipeDocumentation(name = "Interpolation", description = "The interpolation method")
    @JIPipeParameter("interpolation-method")
    public InterpolationMethod getInterpolationMethod() {
        return interpolationMethod;
    }

    @JIPipeParameter("interpolation-method")
    public void setInterpolationMethod(InterpolationMethod interpolationMethod) {
        this.interpolationMethod = interpolationMethod;
    }

    @JIPipeDocumentation(name = "X axis", description = "How the X axis should be scaled")
    @JIPipeParameter("x-axis")
    @JIPipeExpressionParameterSettings(variableSource = ImagePlusPropertiesExpressionParameterVariablesInfo.class)
    public OptionalJIPipeExpressionParameter getxAxis() {
        return xAxis;
    }

    @JIPipeParameter("x-axis")
    public void setxAxis(OptionalJIPipeExpressionParameter xAxis) {
        this.xAxis = xAxis;
    }

    @JIPipeDocumentation(name = "Y axis", description = "How the Y axis should be scaled")
    @JIPipeParameter("y-axis")
    @JIPipeExpressionParameterSettings(variableSource = ImagePlusPropertiesExpressionParameterVariablesInfo.class)
    public OptionalJIPipeExpressionParameter getyAxis() {
        return yAxis;
    }

    @JIPipeParameter("y-axis")
    public void setyAxis(OptionalJIPipeExpressionParameter yAxis) {
        this.yAxis = yAxis;
    }

    @JIPipeDocumentation(name = "Z axis", description = "How the Z axis should be scaled")
    @JIPipeParameter("z-axis")
    @JIPipeExpressionParameterSettings(variableSource = ImagePlusPropertiesExpressionParameterVariablesInfo.class)
    public OptionalJIPipeExpressionParameter getzAxis() {
        return zAxis;
    }

    @JIPipeParameter("z-axis")
    public void setzAxis(OptionalJIPipeExpressionParameter zAxis) {
        this.zAxis = zAxis;
    }
}
