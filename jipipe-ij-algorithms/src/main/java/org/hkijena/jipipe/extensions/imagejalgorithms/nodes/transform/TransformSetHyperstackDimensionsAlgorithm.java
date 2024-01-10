package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.transform;

import ij.ImagePlus;
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
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImagePlusPropertiesExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.parameters.library.primitives.BooleanParameterSettings;

@JIPipeDocumentation(name = "Set hyperstack dimensions", description = "Sets the exact hyperstack dimensions of the incoming images. If you provide " +
        "a lower size, planes will be removed. If you provide a larger dimension, planes are either set to black or copied from the slices with the highest index.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Transform")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nHyperstacks")
public class TransformSetHyperstackDimensionsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeExpressionParameter zAxis = new JIPipeExpressionParameter("num_z");
    private JIPipeExpressionParameter cAxis = new JIPipeExpressionParameter("num_c");
    private JIPipeExpressionParameter tAxis = new JIPipeExpressionParameter("num_t");
    private boolean copySlices = true;

    public TransformSetHyperstackDimensionsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public TransformSetHyperstackDimensionsAlgorithm(TransformSetHyperstackDimensionsAlgorithm other) {
        super(other);
        this.zAxis = new JIPipeExpressionParameter(other.zAxis);
        this.cAxis = new JIPipeExpressionParameter(other.cAxis);
        this.tAxis = new JIPipeExpressionParameter(other.tAxis);
        this.copySlices = other.copySlices;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        ImagePlus image = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();
        ExpressionVariables variables = new ExpressionVariables();
        ImagePlusPropertiesExpressionParameterVariableSource.extractValues(variables, image, iterationStep.getMergedTextAnnotations().values());

        int z = (int) zAxis.evaluateToNumber(variables);
        int c = (int) cAxis.evaluateToNumber(variables);
        int t = (int) tAxis.evaluateToNumber(variables);

        ImagePlus outputImage = ImageJUtils.ensureSize(image, c, z, t, copySlices);
        outputImage.setCalibration(image.getCalibration());
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(outputImage), progressInfo);
    }

    @JIPipeDocumentation(name = "Z axis", description = "Expression that returns the size of the Z axis")
    @JIPipeParameter(value = "z-axis", uiOrder = -50)
    @ExpressionParameterSettings(variableSource = ImagePlusPropertiesExpressionParameterVariableSource.class)
    public JIPipeExpressionParameter getzAxis() {
        return zAxis;
    }

    @JIPipeParameter("z-axis")
    public void setzAxis(JIPipeExpressionParameter zAxis) {
        this.zAxis = zAxis;
    }

    @JIPipeDocumentation(name = "C axis", description = "Expression that returns the size of the channel axis")
    @JIPipeParameter(value = "c-axis", uiOrder = -49)
    @ExpressionParameterSettings(variableSource = ImagePlusPropertiesExpressionParameterVariableSource.class)
    public JIPipeExpressionParameter getcAxis() {
        return cAxis;
    }

    @JIPipeParameter("c-axis")
    public void setcAxis(JIPipeExpressionParameter cAxis) {
        this.cAxis = cAxis;
    }

    @JIPipeDocumentation(name = "T axis", description = "Expression that returns the size of the time axis")
    @JIPipeParameter(value = "t-axis", uiOrder = -48)
    @ExpressionParameterSettings(variableSource = ImagePlusPropertiesExpressionParameterVariableSource.class)
    public JIPipeExpressionParameter gettAxis() {
        return tAxis;
    }

    @JIPipeParameter("t-axis")
    public void settAxis(JIPipeExpressionParameter tAxis) {
        this.tAxis = tAxis;
    }

    @JIPipeDocumentation(name = "Newly generated slices", description = "Determines how new slices are generated, if needed. You can either repeat the last available slice or make new slices zero/black.")
    @BooleanParameterSettings(comboBoxStyle = true, trueLabel = "Repeat last available", falseLabel = "Create empty slice")
    @JIPipeParameter("copy-slices")
    public boolean isCopySlices() {
        return copySlices;
    }

    @JIPipeParameter("copy-slices")
    public void setCopySlices(boolean copySlices) {
        this.copySlices = copySlices;
    }
}
