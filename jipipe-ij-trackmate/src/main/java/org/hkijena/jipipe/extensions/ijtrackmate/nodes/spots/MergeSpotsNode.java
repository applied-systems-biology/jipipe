/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 *
 */

package org.hkijena.jipipe.extensions.ijtrackmate.nodes.spots;

import fiji.plugin.trackmate.Spot;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.nodes.utils.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.SpotsCollectionData;

import java.util.List;

@JIPipeDocumentation(name = "Merge spots", description = "Merges spot lists. Please ensure that the spots are sourced from the same image.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Tracking\nSplit/Merge")
@JIPipeInputSlot(value = SpotsCollectionData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = SpotsCollectionData.class, slotName = "Output", autoCreate = true)
public class MergeSpotsNode extends JIPipeMergingAlgorithm {

    public MergeSpotsNode(JIPipeNodeInfo info) {
        super(info);
    }

    public MergeSpotsNode(MergeSpotsNode other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        List<SpotsCollectionData> spotCollections = dataBatch.getInputData(getFirstInputSlot(), SpotsCollectionData.class, progressInfo);
        if (spotCollections.isEmpty())
            return;
        if (spotCollections.size() == 1) {
            dataBatch.addOutputData(getFirstOutputSlot(), spotCollections.get(0), progressInfo);
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
        dataBatch.addOutputData(getFirstOutputSlot(), newCollection, progressInfo);
    }
}
