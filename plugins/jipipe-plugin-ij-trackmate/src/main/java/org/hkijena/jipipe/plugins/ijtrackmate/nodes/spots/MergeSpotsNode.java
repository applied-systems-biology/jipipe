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

package org.hkijena.jipipe.plugins.ijtrackmate.nodes.spots;

import fiji.plugin.trackmate.Spot;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.plugins.ijtrackmate.datatypes.SpotsCollectionData;

import java.util.List;

@SetJIPipeDocumentation(name = "Merge spots", description = "Merges spot lists. Please ensure that the spots are sourced from the same image.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Tracking\nSplit/Merge")
@AddJIPipeInputSlot(value = SpotsCollectionData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = SpotsCollectionData.class, name = "Output", create = true)
public class MergeSpotsNode extends JIPipeMergingAlgorithm {

    public MergeSpotsNode(JIPipeNodeInfo info) {
        super(info);
    }

    public MergeSpotsNode(MergeSpotsNode other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        List<SpotsCollectionData> spotCollections = iterationStep.getInputData(getFirstInputSlot(), SpotsCollectionData.class, progressInfo);
        if (spotCollections.isEmpty())
            return;
        if (spotCollections.size() == 1) {
            iterationStep.addOutputData(getFirstOutputSlot(), spotCollections.get(0), progressInfo);
            return;
        }
        SpotsCollectionData newCollection = new SpotsCollectionData(spotCollections.get(0));
        for (int i = 1; i < spotCollections.size(); i++) {
            SpotsCollectionData sourceCollection = spotCollections.get(i);
            for (Spot spot : sourceCollection.getSpots().iterable(true)) {
                int frame = spot.getFeature(Spot.FRAME).intValue();
                newCollection.getSpots().add(spot, frame);
            }
        }
        iterationStep.addOutputData(getFirstOutputSlot(), newCollection, progressInfo);
    }
}
