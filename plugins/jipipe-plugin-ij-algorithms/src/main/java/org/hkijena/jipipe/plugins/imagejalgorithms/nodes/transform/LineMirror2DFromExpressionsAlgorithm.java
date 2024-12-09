package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.transform;

import ij.ImagePlus;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariableInfo;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.LineMirror;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImagePlusPropertiesExpressionParameterVariablesInfo;

@SetJIPipeDocumentation(name = "Mirror image over line 2D (Expressions)", description = "The two endpoints of a line a provided via expressions. " +
        "The resulting line is used as axis to mirror the image pixels. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Transform")
public class LineMirror2DFromExpressionsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeExpressionParameter x1 = new JIPipeExpressionParameter("0");
    private JIPipeExpressionParameter x2 = new JIPipeExpressionParameter("width");
    private JIPipeExpressionParameter y1 = new JIPipeExpressionParameter("0");
    private JIPipeExpressionParameter y2 = new JIPipeExpressionParameter("height");
    private LineMirror.MirrorOperationMode mode = LineMirror.MirrorOperationMode.Max;

    public LineMirror2DFromExpressionsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public LineMirror2DFromExpressionsAlgorithm(LineMirror2DFromExpressionsAlgorithm other) {
        super(other);
        this.x1 = new JIPipeExpressionParameter(other.x1);
        this.x2 = new JIPipeExpressionParameter(other.x2);
        this.y1 = new JIPipeExpressionParameter(other.y1);
        this.y2 = new JIPipeExpressionParameter(other.y2);
        this.mode = other.mode;
    }

    @SetJIPipeDocumentation(name = "Mode", description = "The way how the mirror operation should be applied given the two pixel values. " +
            "You can either set both pixels to the min/max value (per channel on RGB) or only copy data from a specific side.")
    @JIPipeParameter("mode")
    public LineMirror.MirrorOperationMode getMode() {
        return mode;
    }

    @JIPipeParameter("mode")
    public void setMode(LineMirror.MirrorOperationMode mode) {
        this.mode = mode;
    }

    @SetJIPipeDocumentation(name = "X1", description = "The X of the first line coordinate")
    @JIPipeParameter(value = "x1", uiOrder = -100)
    @AddJIPipeExpressionParameterVariable(fromClass = ImagePlusPropertiesExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getX1() {
        return x1;
    }

    @JIPipeParameter("x1")
    public void setX1(JIPipeExpressionParameter x1) {
        this.x1 = x1;
    }

    @SetJIPipeDocumentation(name = "X2", description = "The X of the second line coordinate")
    @JIPipeParameter(value = "x2", uiOrder = -98)
    @AddJIPipeExpressionParameterVariable(fromClass = ImagePlusPropertiesExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getX2() {
        return x2;
    }

    @JIPipeParameter("x2")
    public void setX2(JIPipeExpressionParameter x2) {
        this.x2 = x2;
    }

    @SetJIPipeDocumentation(name = "Y1", description = "The Y of the first line coordinate")
    @JIPipeParameter(value = "y1", uiOrder = -99)
    @AddJIPipeExpressionParameterVariable(fromClass = ImagePlusPropertiesExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getY1() {
        return y1;
    }

    @JIPipeParameter("y1")
    public void setY1(JIPipeExpressionParameter y1) {
        this.y1 = y1;
    }

    @SetJIPipeDocumentation(name = "Y2", description = "The Y of the second line coordinate")
    @JIPipeParameter(value = "y2", uiOrder = -97)
    @AddJIPipeExpressionParameterVariable(fromClass = ImagePlusPropertiesExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getY2() {
        return y2;
    }

    @JIPipeParameter("y2")
    public void setY2(JIPipeExpressionParameter y2) {
        this.y2 = y2;
    }

    @Override
    public boolean isEnableDefaultCustomExpressionVariables() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus img = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getDuplicateImage();
        JIPipeExpressionVariablesMap variablesMap = new JIPipeExpressionVariablesMap();
        ImagePlusPropertiesExpressionParameterVariablesInfo.extractValues(variablesMap, img, iterationStep.getMergedTextAnnotations().values());
        variablesMap.putCustomVariables(getDefaultCustomExpressionVariables());
        int x1_ = x1.evaluateToInteger(variablesMap);
        int x2_ = x2.evaluateToInteger(variablesMap);
        int y1_ = y1.evaluateToInteger(variablesMap);
        int y2_ = y2.evaluateToInteger(variablesMap);
        ImageJUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
            LineMirror.mirrorImage(ip, x1_, y1_, x2_, y2_, mode);
        }, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), progressInfo);
    }
}
