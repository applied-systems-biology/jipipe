package org.hkijena.jipipe.extensions.ijfilaments.nodes.process;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariable;
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

    private JIPipeExpressionParameter locationMergingFunction = new JIPipeExpressionParameter("AVG(values)");

    public SmoothFilamentsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SmoothFilamentsAlgorithm(SmoothFilamentsAlgorithm other) {
        super(other);
        this.factorX = other.factorX;
        this.factorY = other.factorY;
        this.factorZ = other.factorZ;
        this.locationMergingFunction = new JIPipeExpressionParameter(other.locationMergingFunction);
        this.enforceSameComponent = other.enforceSameComponent;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DData inputData = iterationStep.getInputData(getFirstInputSlot(), Filaments3DData.class, progressInfo);
        Filaments3DData outputData = new Filaments3DData(inputData);

        outputData.smooth(factorX, factorY, factorZ, enforceSameComponent, locationMergingFunction);

        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
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
    @JIPipeExpressionParameterSettings(hint = "per axis")
    @JIPipeExpressionParameterVariable(key = "values", description = "The list of values", name = "Values")
    @JIPipeParameter("location-merging-function")
    public JIPipeExpressionParameter getLocationMergingFunction() {
        return locationMergingFunction;
    }

    @JIPipeParameter("location-merging-function")
    public void setLocationMergingFunction(JIPipeExpressionParameter locationMergingFunction) {
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
