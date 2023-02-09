package org.hkijena.jipipe.extensions.ijfilaments.nodes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.FilamentsData;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentEdge;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.AsSubgraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@JIPipeDocumentation(name = "Merge filaments", description = "Merges multiple filament graphs into one")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Filaments\nSplit")
@JIPipeInputSlot(value = FilamentsData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = FilamentsData.class, slotName = "Output", autoCreate = true)
public class MergeFilamentsAlgorithm extends JIPipeMergingAlgorithm {

    public MergeFilamentsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public MergeFilamentsAlgorithm(MergeFilamentsAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        FilamentsData outputData = new FilamentsData();
        for (FilamentsData data : dataBatch.getInputData(getFirstInputSlot(), FilamentsData.class, progressInfo)) {
            outputData.mergeWith(data);
        }
        dataBatch.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

}
