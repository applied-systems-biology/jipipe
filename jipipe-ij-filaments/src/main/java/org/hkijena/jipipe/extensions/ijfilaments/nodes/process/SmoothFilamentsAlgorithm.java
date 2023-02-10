package org.hkijena.jipipe.extensions.ijfilaments.nodes.process;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.FilamentsData;

@JIPipeDocumentation(name = "Smooth filaments", description = "Applies a smoothing operation that is based around downscaling the locations and applying the 'Remove duplicate vertices' operation. The positions are then restored.")
@JIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Process")
@JIPipeInputSlot(value = FilamentsData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = FilamentsData.class, slotName = "Output", autoCreate = true)
public class SmoothFilamentsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private double factorXY = 5;
    private double factorZ = 0;
    private double factorC = 0;
    private double factorT = 0;

    private DefaultExpressionParameter locationMergingFunction = new DefaultExpressionParameter("MEDIAN(values)");

    public SmoothFilamentsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SmoothFilamentsAlgorithm(SmoothFilamentsAlgorithm other) {
        super(other);
        this.factorXY = other.factorXY;
        this.factorZ = other.factorZ;
        this.factorC = other.factorC;
        this.factorT = other.factorT;
        this.locationMergingFunction = new DefaultExpressionParameter(other.locationMergingFunction);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        FilamentsData inputData = dataBatch.getInputData(getFirstInputSlot(), FilamentsData.class, progressInfo);
        FilamentsData outputData = new FilamentsData(inputData);

        outputData.smooth(factorXY, factorZ, factorC, factorT, locationMergingFunction);

        dataBatch.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @JIPipeDocumentation(name = "Factor (X/Y)", description = "The smoothing factor in the X/Y plane. Set to zero to disable smoothing")
    @JIPipeParameter(value = "factor-xy", uiOrder = -10)
    public double getFactorXY() {
        return factorXY;
    }

    @JIPipeParameter("factor-xy")
    public void setFactorXY(double factorXY) {
        this.factorXY = factorXY;
    }

    @JIPipeDocumentation(name = "Factor (Z)", description = "The smoothing factor in the Z plane. Set to zero to disable smoothing")
    @JIPipeParameter("factor-z")
    public double getFactorZ() {
        return factorZ;
    }

    @JIPipeParameter("factor-z")
    public void setFactorZ(double factorZ) {
        this.factorZ = factorZ;
    }

    @JIPipeDocumentation(name = "Factor (C)", description = "The smoothing factor in the channel (C) plane. Set to zero to disable smoothing")
    @JIPipeParameter("factor-c")
    public double getFactorC() {
        return factorC;
    }

    @JIPipeParameter("factor-c")
    public void setFactorC(double factorC) {
        this.factorC = factorC;
    }

    @JIPipeDocumentation(name = "Factor (T)", description = "The smoothing factor in the time (t) plane. Set to zero to disable smoothing")
    @JIPipeParameter("factor-t")
    public double getFactorT() {
        return factorT;
    }

    @JIPipeParameter("factor-t")
    public void setFactorT(double factorT) {
        this.factorT = factorT;
    }

    @JIPipeDocumentation(name = "Location merging function", description = "A function that determines how multiple coordinates are merged together")
    @ExpressionParameterSettings(hint = "per axis")
    @ExpressionParameterSettingsVariable(key = "values", description = "The list of values", name = "Values")
    @JIPipeParameter("location-merging-function")
    public DefaultExpressionParameter getLocationMergingFunction() {
        return locationMergingFunction;
    }

    @JIPipeParameter("location-merging-function")
    public void setLocationMergingFunction(DefaultExpressionParameter locationMergingFunction) {
        this.locationMergingFunction = locationMergingFunction;
    }
}
