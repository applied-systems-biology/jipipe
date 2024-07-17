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

package org.hkijena.jipipe.plugins.ijfilaments.nodes.process;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DGraphData;

@SetJIPipeDocumentation(name = "Smooth filaments", description = "Applies a smoothing operation that is based around downscaling the locations and applying the 'Remove duplicate vertices' operation. The positions are then restored.")
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Process")
@AddJIPipeInputSlot(value = Filaments3DGraphData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = Filaments3DGraphData.class, name = "Output", create = true)
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
        Filaments3DGraphData inputData = iterationStep.getInputData(getFirstInputSlot(), Filaments3DGraphData.class, progressInfo);
        Filaments3DGraphData outputData = new Filaments3DGraphData(inputData);

        outputData.smooth(factorX, factorY, factorZ, enforceSameComponent, locationMergingFunction);

        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Factor (X)", description = "The smoothing factor in the X coordinate. Set to zero to disable smoothing")
    @JIPipeParameter("factor-x")
    public double getFactorX() {
        return factorX;
    }

    @JIPipeParameter("factor-x")
    public void setFactorX(double factorX) {
        this.factorX = factorX;
    }

    @SetJIPipeDocumentation(name = "Factor (Y)", description = "The smoothing factor in the Y coordinate. Set to zero to disable smoothing")
    @JIPipeParameter("factor-y")
    public double getFactorY() {
        return factorY;
    }

    @JIPipeParameter("factor-y")
    public void setFactorY(double factorY) {
        this.factorY = factorY;
    }

    @SetJIPipeDocumentation(name = "Factor (Z)", description = "The smoothing factor in the Z plane. Set to zero to disable smoothing")
    @JIPipeParameter("factor-z")
    public double getFactorZ() {
        return factorZ;
    }

    @JIPipeParameter("factor-z")
    public void setFactorZ(double factorZ) {
        this.factorZ = factorZ;
    }

    @SetJIPipeDocumentation(name = "Location merging function", description = "A function that determines how multiple coordinates are merged together")
    @JIPipeExpressionParameterSettings(hint = "per axis")
    @AddJIPipeExpressionParameterVariable(key = "values", description = "The list of values", name = "Values")
    @JIPipeParameter("location-merging-function")
    public JIPipeExpressionParameter getLocationMergingFunction() {
        return locationMergingFunction;
    }

    @JIPipeParameter("location-merging-function")
    public void setLocationMergingFunction(JIPipeExpressionParameter locationMergingFunction) {
        this.locationMergingFunction = locationMergingFunction;
    }

    @SetJIPipeDocumentation(name = "Prevent cross-object edges", description = "If enabled, new edges will never be created across two different objects. Disable this option if there are broken filaments in the input graph.")
    @JIPipeParameter("enforce-same-component")
    public boolean isEnforceSameComponent() {
        return enforceSameComponent;
    }

    @JIPipeParameter("enforce-same-component")
    public void setEnforceSameComponent(boolean enforceSameComponent) {
        this.enforceSameComponent = enforceSameComponent;
    }
}
