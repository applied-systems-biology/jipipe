package org.hkijena.jipipe.extensions.ijfilaments.nodes.process;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;

@JIPipeDocumentation(name = "Smooth filaments", description = "Applies a smoothing operation that is based around downscaling the locations and applying the 'Remove duplicate vertices' operation. The positions are then restored.")
@JIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Process")
@JIPipeInputSlot(value = Filaments3DData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = Filaments3DData.class, slotName = "Output", autoCreate = true)
public class SmoothFilamentsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private double factorX = 5;

    private double factorY = 5;
    private double factorZ = 0;
    private boolean enforceSameComponent = true;

    private DefaultExpressionParameter locationMergingFunction = new DefaultExpressionParameter("AVG(values)");

    public SmoothFilamentsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SmoothFilamentsAlgorithm(SmoothFilamentsAlgorithm other) {
        super(other);
        this.factorX = other.factorX;
        this.factorY = other.factorY;
        this.factorZ = other.factorZ;
        this.locationMergingFunction = new DefaultExpressionParameter(other.locationMergingFunction);
        this.enforceSameComponent = other.enforceSameComponent;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Filaments3DData inputData = dataBatch.getInputData(getFirstInputSlot(), Filaments3DData.class, progressInfo);
        Filaments3DData outputData = new Filaments3DData(inputData);

        outputData.smooth(factorX, factorY, factorZ, enforceSameComponent, locationMergingFunction);

        dataBatch.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @JIPipeDocumentation(name = "Factor (X)", description = "The smoothing factor in the X coordinate. Set to zero to disable smoothing")
    @JIPipeParameter("factor-x")
    public double getFactorX() {
        return factorX;
    }

    @JIPipeParameter("factor-x")
    public void setFactorX(double factorX) {
        this.factorX = factorX;
    }

    @JIPipeDocumentation(name = "Factor (Y)", description = "The smoothing factor in the Y coordinate. Set to zero to disable smoothing")
    @JIPipeParameter("factor-y")
    public double getFactorY() {
        return factorY;
    }

    @JIPipeParameter("factor-y")
    public void setFactorY(double factorY) {
        this.factorY = factorY;
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

    @JIPipeDocumentation(name = "Prevent cross-object edges", description = "If enabled, new edges will never be created across two different objects. Disable this option if there are broken filaments in the input graph.")
    @JIPipeParameter("enforce-same-component")
    public boolean isEnforceSameComponent() {
        return enforceSameComponent;
    }

    @JIPipeParameter("enforce-same-component")
    public void setEnforceSameComponent(boolean enforceSameComponent) {
        this.enforceSameComponent = enforceSameComponent;
    }
}
