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

package org.hkijena.jipipe.plugins.ijfilaments.nodes.split;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DGraphData;
import org.hkijena.jipipe.plugins.ijfilaments.parameters.CycleFinderAlgorithm;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentEdge;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SetJIPipeDocumentation(name = "Split filaments into cycles", description = "Splits the filament graph into cycles and outputs one graph per cycle")
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Split")
@AddJIPipeInputSlot(value = Filaments3DGraphData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = Filaments3DGraphData.class, name = "Output", create = true)
public class SplitFilamentsIntoCyclesAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OptionalTextAnnotationNameParameter cycleIdAnnotation = new OptionalTextAnnotationNameParameter("#Cycle", true);
    private CycleFinderAlgorithm cycleFinderAlgorithm = CycleFinderAlgorithm.PatonCycleBasis;

    public SplitFilamentsIntoCyclesAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SplitFilamentsIntoCyclesAlgorithm(SplitFilamentsIntoCyclesAlgorithm other) {
        super(other);
        this.cycleIdAnnotation = new OptionalTextAnnotationNameParameter(other.cycleIdAnnotation);
        this.cycleFinderAlgorithm = other.cycleFinderAlgorithm;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DGraphData inputData = iterationStep.getInputData(getFirstInputSlot(), Filaments3DGraphData.class, progressInfo);
        progressInfo.log("Finding cycles ...");
        Set<List<FilamentEdge>> cycles = cycleFinderAlgorithm.findCycles(inputData);
        progressInfo.log("Detected " + cycles.size() + " cycles");
        int componentId = 0;
        for (List<FilamentEdge> cycle : cycles) {
            Filaments3DGraphData outputData = new Filaments3DGraphData();
            List<JIPipeTextAnnotation> annotationList = new ArrayList<>();
            cycleIdAnnotation.addAnnotationIfEnabled(annotationList, componentId + "");

            for (FilamentEdge filamentEdge : cycle) {
                FilamentVertex edgeSource = inputData.getEdgeSource(filamentEdge);
                FilamentVertex edgeTarget = inputData.getEdgeTarget(filamentEdge);
                outputData.addVertex(edgeSource);
                outputData.addVertex(edgeTarget);
                outputData.addEdge(edgeSource, edgeTarget, new FilamentEdge(filamentEdge));
            }

            componentId++;

            iterationStep.addOutputData(getFirstOutputSlot(), outputData, annotationList, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        }
    }

    @SetJIPipeDocumentation(name = "Annotate with cycle index", description = "If enabled, the numeric index of the cycle is written into the specified annotation")
    @JIPipeParameter("cycle-id-annotation")
    public OptionalTextAnnotationNameParameter getCycleIdAnnotation() {
        return cycleIdAnnotation;
    }

    @JIPipeParameter("cycle-id-annotation")
    public void setCycleIdAnnotation(OptionalTextAnnotationNameParameter cycleIdAnnotation) {
        this.cycleIdAnnotation = cycleIdAnnotation;
    }
}
