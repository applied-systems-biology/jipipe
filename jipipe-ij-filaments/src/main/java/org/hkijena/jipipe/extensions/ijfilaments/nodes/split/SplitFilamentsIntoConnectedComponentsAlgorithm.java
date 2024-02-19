package org.hkijena.jipipe.extensions.ijfilaments.nodes.split;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.DefineJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentEdge;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.AsSubgraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SetJIPipeDocumentation(name = "Split filaments into connected components", description = "Splits the filament graph into connected components and outputs one graph per component")
@DefineJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Split")
@AddJIPipeInputSlot(value = Filaments3DData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = Filaments3DData.class, slotName = "Output", create = true)
public class SplitFilamentsIntoConnectedComponentsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OptionalAnnotationNameParameter componentIdAnnotation = new OptionalAnnotationNameParameter("#Component", true);

    public SplitFilamentsIntoConnectedComponentsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SplitFilamentsIntoConnectedComponentsAlgorithm(SplitFilamentsIntoConnectedComponentsAlgorithm other) {
        super(other);
        this.componentIdAnnotation = new OptionalAnnotationNameParameter(other.componentIdAnnotation);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DData inputData = iterationStep.getInputData(getFirstInputSlot(), Filaments3DData.class, progressInfo);
        ConnectivityInspector<FilamentVertex, FilamentEdge> connectivityInspector = new ConnectivityInspector<>(inputData);
        int componentId = 0;
        for (Set<FilamentVertex> connectedSet : connectivityInspector.connectedSets()) {
            Filaments3DData outputData = new Filaments3DData();
            List<JIPipeTextAnnotation> annotationList = new ArrayList<>();
            componentIdAnnotation.addAnnotationIfEnabled(annotationList, componentId + "");

            AsSubgraph<FilamentVertex, FilamentEdge> subgraph = new AsSubgraph<>(inputData, connectedSet);
            for (FilamentVertex filamentVertex : connectedSet) {
                outputData.addVertex(filamentVertex);
            }
            for (FilamentEdge edge : subgraph.edgeSet()) {
                outputData.addEdge(subgraph.getEdgeSource(edge), subgraph.getEdgeTarget(edge), edge);
            }

            componentId++;

            iterationStep.addOutputData(getFirstOutputSlot(), outputData, annotationList, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        }
    }

    @SetJIPipeDocumentation(name = "Annotate with component index", description = "If enabled, the numeric index of the component is written into the specified annotation")
    @JIPipeParameter("component-id-annotation")
    public OptionalAnnotationNameParameter getComponentIdAnnotation() {
        return componentIdAnnotation;
    }

    @JIPipeParameter("component-id-annotation")
    public void setComponentIdAnnotation(OptionalAnnotationNameParameter componentIdAnnotation) {
        this.componentIdAnnotation = componentIdAnnotation;
    }
}
