package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImagePlusPropertiesExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.parameters.primitives.BooleanParameterSettings;

@JIPipeDocumentation(name = "Set hyperstack dimensions", description = "Sets the exact hyperstack dimensions of the incoming images. If you provide " +
        "a lower size, planes will be removed. If you provide a larger dimension, planes are either set to black or copied from the slices with the highest index.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Transform")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true, inheritedSlot = "Input")
public class TransformSetHyperstackDimensionsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private DefaultExpressionParameter zAxis = new DefaultExpressionParameter("num_z");
    private DefaultExpressionParameter cAxis = new DefaultExpressionParameter("num_c");
    private DefaultExpressionParameter tAxis = new DefaultExpressionParameter("num_t");
    private boolean copySlices = true;

    public TransformSetHyperstackDimensionsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public TransformSetHyperstackDimensionsAlgorithm(TransformSetHyperstackDimensionsAlgorithm other) {
        super(other);
        this.zAxis = new DefaultExpressionParameter(other.zAxis);
        this.cAxis = new DefaultExpressionParameter(other.cAxis);
        this.tAxis = new DefaultExpressionParameter(other.tAxis);
        this.copySlices = other.copySlices;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus image = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();
        ExpressionVariables variables = new ExpressionVariables();
        ImagePlusPropertiesExpressionParameterVariableSource.extractValues(variables, image);

        int z = (int) zAxis.evaluateToNumber(variables);
        int c = (int) cAxis.evaluateToNumber(variables);
        int t = (int) tAxis.evaluateToNumber(variables);

        ImagePlus outputImage = ImageJUtils.ensureSize(image, c, z, t, copySlices);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(outputImage), progressInfo);
    }

    @JIPipeDocumentation(name = "Z axis", description = "Expression that returns the size of the Z axis")
    @JIPipeParameter(value = "z-axis", uiOrder = -50)
    public DefaultExpressionParameter getzAxis() {
        return zAxis;
    }

    @JIPipeParameter("z-axis")
    public void setzAxis(DefaultExpressionParameter zAxis) {
        this.zAxis = zAxis;
    }

    @JIPipeDocumentation(name = "C axis", description = "Expression that returns the size of the channel axis")
    @JIPipeParameter(value = "c-axis", uiOrder = -49)
    public DefaultExpressionParameter getcAxis() {
        return cAxis;
    }

    @JIPipeParameter("c-axis")
    public void setcAxis(DefaultExpressionParameter cAxis) {
        this.cAxis = cAxis;
    }

    @JIPipeDocumentation(name = "T axis", description = "Expression that returns the size of the time axis")
    @JIPipeParameter(value = "t-axis", uiOrder = -48)
    public DefaultExpressionParameter gettAxis() {
        return tAxis;
    }

    @JIPipeParameter("t-axis")
    public void settAxis(DefaultExpressionParameter tAxis) {
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
