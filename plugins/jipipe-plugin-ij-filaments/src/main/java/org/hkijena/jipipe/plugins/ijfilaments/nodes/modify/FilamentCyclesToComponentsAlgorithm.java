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
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentEdge;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalStringParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.jgrapht.alg.cycle.PatonCycleBase;
import org.jgrapht.alg.interfaces.CycleBasisAlgorithm;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@SetJIPipeDocumentation(name = "Filament cycles to components", description = "Finds all cycles in the input filament graph and makes each cycle a component. Unlike 'Split filaments into cycles', this node outputs one filament graph where each component is a cycle.")
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Modify")
@AddJIPipeInputSlot(value = Filaments3DData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = Filaments3DData.class, name = "Output", create = true)
public class FilamentCyclesToComponentsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OptionalStringParameter cycleIdAnnotation = new OptionalStringParameter("Cycle", true);

    public FilamentCyclesToComponentsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public FilamentCyclesToComponentsAlgorithm(FilamentCyclesToComponentsAlgorithm other) {
        super(other);
        this.cycleIdAnnotation = new OptionalTextAnnotationNameParameter(other.cycleIdAnnotation);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DData inputData = iterationStep.getInputData(getFirstInputSlot(), Filaments3DData.class, progressInfo);
        Filaments3DData outputData = new Filaments3DData();

        PatonCycleBase<FilamentVertex, FilamentEdge> patonCycleBase = new PatonCycleBase<>(inputData);
        progressInfo.log("Finding cycle basis (Paton) ...");
        CycleBasisAlgorithm.CycleBasis<FilamentVertex, FilamentEdge> cycleBasis = patonCycleBase.getCycleBasis();
        progressInfo.log("Detected " + cycleBasis.getCycles().size() + " cycles");
        int componentId = 0;
        for (List<FilamentEdge> cycle : cycleBasis.getCycles()) {

            Map<FilamentVertex, FilamentVertex> copyMap = new IdentityHashMap<>();

            for (FilamentEdge filamentEdge : cycle) {
                FilamentVertex edgeSource = inputData.getEdgeSource(filamentEdge);
                FilamentVertex edgeTarget = inputData.getEdgeTarget(filamentEdge);
                FilamentVertex copyEdgeSource = copyMap.get(edgeSource);
                FilamentVertex copyEdgeTarget = copyMap.get(edgeTarget);

                if (copyEdgeSource == null) {
                    copyEdgeSource = new FilamentVertex(edgeSource);
                    if(cycleIdAnnotation.isEnabled()) {
                        copyEdgeSource.getMetadata().put(cycleIdAnnotation.getContent(), String.valueOf(componentId));
                    }
                    copyMap.put(edgeSource, copyEdgeSource);
                }

                if (copyEdgeTarget == null) {
                    copyEdgeTarget = new FilamentVertex(edgeSource);
                    if(cycleIdAnnotation.isEnabled()) {
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
}
