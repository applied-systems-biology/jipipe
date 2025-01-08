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
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentEdge;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentVertex;
import org.jgrapht.alg.connectivity.ConnectivityInspector;

import java.util.Set;

@SetJIPipeDocumentation(name = "Set filament vertex metadata to component index", description = "Finds all connected components and writes the component index into a metadata field")
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Modify")
@AddJIPipeInputSlot(value = Filaments3DGraphData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = Filaments3DGraphData.class, name = "Output", create = true)
public class SetFilamentMetadataToComponentIndexAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private String metadataKey = "component_index";

    public SetFilamentMetadataToComponentIndexAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SetFilamentMetadataToComponentIndexAlgorithm(SetFilamentMetadataToComponentIndexAlgorithm other) {
        super(other);
        this.metadataKey = other.metadataKey;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DGraphData filaments = (Filaments3DGraphData) iterationStep.getInputData(getFirstInputSlot(), Filaments3DGraphData.class, progressInfo).duplicate(progressInfo);
        ConnectivityInspector<FilamentVertex, FilamentEdge> connectivityInspector = new ConnectivityInspector<>(filaments);
        int componentId = 0;
        for (Set<FilamentVertex> connectedSet : connectivityInspector.connectedSets()) {
            for (FilamentVertex vertex : connectedSet) {
                vertex.setMetadata(metadataKey, componentId);
            }
            componentId++;
        }
        iterationStep.addOutputData(getFirstOutputSlot(), filaments, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Metadata key", description = "The vertex metadata key where the values will be written")
    @JIPipeParameter(value = "metadata-key", important = true)
    public String getMetadataKey() {
        return metadataKey;
    }

    @JIPipeParameter("metadata-key")
    public void setMetadataKey(String metadataKey) {
        this.metadataKey = metadataKey;
    }
}
