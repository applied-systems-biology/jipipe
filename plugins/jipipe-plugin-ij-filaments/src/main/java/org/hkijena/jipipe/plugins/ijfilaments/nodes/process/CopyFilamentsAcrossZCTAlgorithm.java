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
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DGraphData;
import org.hkijena.jipipe.plugins.ijfilaments.parameters.VertexMaskParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.HyperstackDimension;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImagePlusPropertiesExpressionParameterVariablesInfo;

@SetJIPipeDocumentation(name = "Copy filaments across Z/C/T", description = "Copies a filament graph to other Z locations, channels, and frames. The newly created vertices can be optionally connected to the neighboring source vertex.")
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Process")
@AddJIPipeInputSlot(value = Filaments3DGraphData.class, name = "Input", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Reference", optional = true, create = true)
@AddJIPipeOutputSlot(value = Filaments3DGraphData.class, name = "Output", create = true)
public class CopyFilamentsAcrossZCTAlgorithm extends JIPipeIteratingAlgorithm {

    private HyperstackDimension dimension = HyperstackDimension.Frame;
    private JIPipeExpressionParameter locations = new JIPipeExpressionParameter("RANGE(0, num_t)");
    private final VertexMaskParameter vertexMask;

    public CopyFilamentsAcrossZCTAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.vertexMask = new VertexMaskParameter();
        registerSubParameter(vertexMask);
    }

    public CopyFilamentsAcrossZCTAlgorithm(CopyFilamentsAcrossZCTAlgorithm other) {
        super(other);
        this.dimension = other.dimension;
        this.locations = new JIPipeExpressionParameter(other.locations);
        this.vertexMask = new VertexMaskParameter(other.vertexMask);
        registerSubParameter(vertexMask);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DGraphData filaments = (Filaments3DGraphData) iterationStep.getInputData("Input", Filaments3DGraphData.class, progressInfo).duplicate(progressInfo);
        ImagePlusData referenceImageData = iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo);

    }

    @SetJIPipeDocumentation(name = "Direction", description = "The direction in which to expand each vertex.")
    @JIPipeParameter("dimension")
    public HyperstackDimension getDimension() {
        return dimension;
    }

    @JIPipeParameter("dimension")
    public void setDimension(HyperstackDimension dimension) {
        this.dimension = dimension;
    }

    @SetJIPipeDocumentation(name = "Locations (in direction)", description = "Expression that determines the locations in the selected direction where the vertex will be present.")
    @JIPipeParameter("locations")
    @AddJIPipeExpressionParameterVariable(fromClass = ImagePlusPropertiesExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getLocations() {
        return locations;
    }

    @JIPipeParameter("locations")
    public void setLocations(JIPipeExpressionParameter locations) {
        this.locations = locations;
    }

    @SetJIPipeDocumentation(name = "Vertex mask", description = "Used to filter vertices")
    @JIPipeParameter("vertex-filter")
    public VertexMaskParameter getVertexMask() {
        return vertexMask;
    }
}
