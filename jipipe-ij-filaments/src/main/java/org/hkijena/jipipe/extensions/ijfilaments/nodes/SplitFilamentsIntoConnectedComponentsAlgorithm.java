package org.hkijena.jipipe.extensions.ijfilaments.nodes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.FilamentsData;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentEdge;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.AsSubgraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@JIPipeDocumentation(name = "Split filaments into connected components", description = "Splits the filament graph into connected components and outputs one graph per component")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Filaments\nMerge")
@JIPipeInputSlot(value = FilamentsData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = FilamentsData.class, slotName = "Output", autoCreate = true)
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        FilamentsData inputData = dataBatch.getInputData(getFirstInputSlot(), FilamentsData.class, progressInfo);
        ConnectivityInspector<FilamentVertex, FilamentEdge> connectivityInspector = new ConnectivityInspector<>(inputData);
        int componentId = 0;
        for (Set<FilamentVertex> connectedSet : connectivityInspector.connectedSets()) {
            FilamentsData outputData = new FilamentsData();
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

            dataBatch.addOutputData(getFirstOutputSlot(), outputData, annotationList, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        }
    }

    @JIPipeDocumentation(name = "Annotate with component index", description = "If enabled, the numeric index of the component is written into the specified annotation")
    @JIPipeParameter("component-id-annotation")
    public OptionalAnnotationNameParameter getComponentIdAnnotation() {
        return componentIdAnnotation;
    }

    @JIPipeParameter("component-id-annotation")
    public void setComponentIdAnnotation(OptionalAnnotationNameParameter componentIdAnnotation) {
        this.componentIdAnnotation = componentIdAnnotation;
    }
}
