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
import org.hkijena.jipipe.plugins.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DGraphData;
import org.hkijena.jipipe.plugins.ijfilaments.parameters.CycleFinderAlgorithm;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentEdge;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalStringParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.jgrapht.alg.cycle.PatonCycleBase;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SetJIPipeDocumentation(name = "Filament cycles to components", description = "Finds all cycles in the input filament graph and makes each cycle a component. Unlike 'Split filaments into cycles', this node outputs one filament graph where each component is a cycle.")
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Modify")
@AddJIPipeInputSlot(value = Filaments3DGraphData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = Filaments3DGraphData.class, name = "Output", create = true)
public class FilamentCyclesToComponentsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OptionalStringParameter cycleIdAnnotation = new OptionalStringParameter("Cycle", true);
    private CycleFinderAlgorithm cycleFinderAlgorithm = CycleFinderAlgorithm.PatonCycleBasis;

    public FilamentCyclesToComponentsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public FilamentCyclesToComponentsAlgorithm(FilamentCyclesToComponentsAlgorithm other) {
        super(other);
        this.cycleIdAnnotation = new OptionalTextAnnotationNameParameter(other.cycleIdAnnotation);
        this.cycleFinderAlgorithm = other.cycleFinderAlgorithm;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DGraphData inputData = iterationStep.getInputData(getFirstInputSlot(), Filaments3DGraphData.class, progressInfo);
        Filaments3DGraphData outputData = new Filaments3DGraphData();

        PatonCycleBase<FilamentVertex, FilamentEdge> patonCycleBase = new PatonCycleBase<>(inputData);
        progressInfo.log("Finding cycles ...");
        Set<List<FilamentEdge>> cycles = cycleFinderAlgorithm.findCycles(inputData);
        progressInfo.log("Detected " + cycles.size() + " cycles");
        int componentId = 0;
        for (List<FilamentEdge> cycle : cycles) {

            Map<FilamentVertex, FilamentVertex> copyMap = new IdentityHashMap<>();

            for (FilamentEdge filamentEdge : cycle) {
                FilamentVertex edgeSource = inputData.getEdgeSource(filamentEdge);
                FilamentVertex edgeTarget = inputData.getEdgeTarget(filamentEdge);
                FilamentVertex copyEdgeSource = copyMap.get(edgeSource);
                FilamentVertex copyEdgeTarget = copyMap.get(edgeTarget);

                if (copyEdgeSource == null) {
                    copyEdgeSource = new FilamentVertex(edgeSource);
                    if (cycleIdAnnotation.isEnabled()) {
                        copyEdgeSource.getMetadata().put(cycleIdAnnotation.getContent(), String.valueOf(componentId));
                    }
                    copyMap.put(edgeSource, copyEdgeSource);
                }

                if (copyEdgeTarget == null) {
                    copyEdgeTarget = new FilamentVertex(edgeSource);
                    if (cycleIdAnnotation.isEnabled()) {
                        copyEdgeTarget.getMetadata().put(cycleIdAnnotation.getContent(), String.valueOf(componentId));
                    }
                    copyMap.put(edgeTarget, copyEdgeTarget);
                }

                outputData.addVertex(copyEdgeSource);
                outputData.addVertex(copyEdgeTarget);
                outputData.addEdge(copyEdgeSource, copyEdgeTarget, new FilamentEdge(filamentEdge));
            }

            componentId++;

        }

        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Set cycle ID as metadata", description = "If enabled, cycle ID is stored into the metadata ")
    @JIPipeParameter("cycle-id-metadata")
    public OptionalStringParameter getCycleIdAnnotation() {
        return cycleIdAnnotation;
    }

    @JIPipeParameter("cycle-id-metadata")
    public void setCycleIdAnnotation(OptionalStringParameter cycleIdAnnotation) {
        this.cycleIdAnnotation = cycleIdAnnotation;
    }

    @SetJIPipeDocumentation(name = "Cycle finder algorithm", description = "The algorithm used for finding cycles. See https://jgrapht.org/javadoc/org.jgrapht.core/org/jgrapht/alg/cycle/package-summary.html for more information.")
    @JIPipeParameter("cycle-finder")
    public CycleFinderAlgorithm getCycleFinderAlgorithm() {
        return cycleFinderAlgorithm;
    }

    @JIPipeParameter("cycle-finder")
    public void setCycleFinderAlgorithm(CycleFinderAlgorithm cycleFinderAlgorithm) {
        this.cycleFinderAlgorithm = cycleFinderAlgorithm;
    }
}
