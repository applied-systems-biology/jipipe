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

package org.hkijena.jipipe.plugins.ijfilaments.nodes.modify;

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
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.plugins.ijfilaments.parameters.VertexMaskParameter;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentVertex;

import java.util.HashSet;
import java.util.Set;

@SetJIPipeDocumentation(name = "Erode end vertices", description = "Iteratively removes vertices that have a degree of at most 1")
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Modify")
@AddJIPipeInputSlot(value = Filaments3DData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = Filaments3DData.class, slotName = "Output", create = true)
public class ErodeEndVerticesAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final VertexMaskParameter vertexMask;
    private int iterations = 1;

    public ErodeEndVerticesAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.vertexMask = new VertexMaskParameter();
        registerSubParameter(vertexMask);
    }

    public ErodeEndVerticesAlgorithm(ErodeEndVerticesAlgorithm other) {
        super(other);
        this.iterations = other.iterations;
        this.vertexMask = new VertexMaskParameter(other.vertexMask);
        registerSubParameter(vertexMask);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DData graph = (Filaments3DData) iterationStep.getInputData(getFirstInputSlot(), Filaments3DData.class, progressInfo).duplicate(progressInfo);
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        getDefaultCustomExpressionVariables().writeToVariables(variables);

        for (int i = 0; i < iterations; i++) {
            JIPipeProgressInfo iterationProgress = progressInfo.resolveAndLog("Iteration", i, iterations);
            Set<FilamentVertex> toDelete = new HashSet<>();
            for (FilamentVertex vertex : vertexMask.filter(graph, graph.vertexSet(), variables)) {
                if (graph.degreeOf(vertex) == 1) {
                    toDelete.add(vertex);
                }
            }

            for (FilamentVertex vertex : toDelete) {
                graph.removeVertex(vertex);
            }

            iterationProgress.log("Removed " + toDelete.size() + " vertices");
        }

        iterationStep.addOutputData(getFirstOutputSlot(), graph, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Iterations", description = "The number of erosion iterations")
    @JIPipeParameter("iterations")
    public int getIterations() {
        return iterations;
    }

    @JIPipeParameter("iterations")
    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    @SetJIPipeDocumentation(name = "Vertex mask", description = "Additional mask applied to the vertices. If the vertex mask returns FALSE, the vertex is not eroded.")
    @JIPipeParameter("vertex-filter")
    public VertexMaskParameter getVertexMask() {
        return vertexMask;
    }

    @Override
    public boolean isEnableDefaultCustomExpressionVariables() {
        return true;
    }
}
