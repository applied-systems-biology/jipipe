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

package org.hkijena.jipipe.extensions.ijfilaments.nodes.process;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;

@SetJIPipeDocumentation(name = "Remove duplicate vertices", description = "Detects vertices with the same location and removes all duplicates. Edges are preserved. The metadata of deleted vertices will be removed.")
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Process")
@AddJIPipeInputSlot(value = Filaments3DData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = Filaments3DData.class, slotName = "Output", create = true)
public class RemoveDuplicateVerticesAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private boolean onlySameComponent = true;

    public RemoveDuplicateVerticesAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public RemoveDuplicateVerticesAlgorithm(RemoveDuplicateVerticesAlgorithm other) {
        super(other);
        this.onlySameComponent = other.onlySameComponent;
    }

    @SetJIPipeDocumentation(name = "Only merge if in same component", description = "If enabled, vertices will be only merged if they are in the same component.")
    @JIPipeParameter("only-same-component")
    public boolean isOnlySameComponent() {
        return onlySameComponent;
    }

    @JIPipeParameter("only-same-component")
    public void setOnlySameComponent(boolean onlySameComponent) {
        this.onlySameComponent = onlySameComponent;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DData inputData = iterationStep.getInputData(getFirstInputSlot(), Filaments3DData.class, progressInfo);
        Filaments3DData outputData = new Filaments3DData(inputData);
        outputData.removeDuplicateVertices(onlySameComponent);
        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }
}
