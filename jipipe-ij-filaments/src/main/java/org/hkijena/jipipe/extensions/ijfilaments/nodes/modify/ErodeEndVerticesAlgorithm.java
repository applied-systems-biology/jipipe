package org.hkijena.jipipe.extensions.ijfilaments.nodes.modify;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.expressions.CustomExpressionVariablesParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.extensions.ijfilaments.parameters.VertexMaskParameter;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.util.HashSet;
import java.util.Set;

@JIPipeDocumentation(name = "Erode end vertices", description = "Iteratively removes vertices that have a degree of at most 1")
@JIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Modify")
@JIPipeInputSlot(value = Filaments3DData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = Filaments3DData.class, slotName = "Output", autoCreate = true)
public class ErodeEndVerticesAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private int iterations = 1;
    private final VertexMaskParameter vertexMask;
    private final CustomExpressionVariablesParameter customExpressionVariables;

    public ErodeEndVerticesAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.customExpressionVariables = new CustomExpressionVariablesParameter(this);
        this.vertexMask = new VertexMaskParameter();
        registerSubParameter(vertexMask);
    }

    public ErodeEndVerticesAlgorithm(ErodeEndVerticesAlgorithm other) {
        super(other);
        this.customExpressionVariables = new CustomExpressionVariablesParameter(other.customExpressionVariables, this);
        this.iterations = other.iterations;
        this.vertexMask = new VertexMaskParameter(other.vertexMask);
        registerSubParameter(vertexMask);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        Filaments3DData graph = (Filaments3DData) iterationStep.getInputData(getFirstInputSlot(), Filaments3DData.class, progressInfo).duplicate(progressInfo);
        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        customExpressionVariables.writeToVariables(variables, true, "custom", true, "custom");

        for (int i = 0; i < iterations; i++) {
            JIPipeProgressInfo iterationProgress = progressInfo.resolveAndLog("Iteration", i, iterations);
            Set<FilamentVertex> toDelete = new HashSet<>();
            for (FilamentVertex vertex : vertexMask.filter(graph, graph.vertexSet(), variables)) {
                if(graph.degreeOf(vertex) == 1) {
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

    @JIPipeDocumentation(name = "Iterations", description = "The number of erosion iterations")
    @JIPipeParameter("iterations")
    public int getIterations() {
        return iterations;
    }

    @JIPipeParameter("iterations")
    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    @JIPipeDocumentation(name = "Vertex mask", description = "Additional mask applied to the vertices. If the vertex mask returns FALSE, the vertex is not eroded.")
    @JIPipeParameter("vertex-filter")
    public VertexMaskParameter getVertexMask() {
        return vertexMask;
    }

    @JIPipeDocumentation(name = "Custom expression variables", description = "Here you can add parameters that will be included into the expression as variables <code>custom.[key]</code>. Alternatively, you can access them via <code>GET_ITEM(custom, \"[key]\")</code>.")
    @JIPipeParameter(value = "custom-filter-variables", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/insert-math-expression.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/insert-math-expression.png", persistence = JIPipeParameterPersistence.NestedCollection)
    public CustomExpressionVariablesParameter getCustomExpressionVariables() {
        return customExpressionVariables;
    }
}
