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

package org.hkijena.jipipe.extensions.ijtrackmate.nodes.tracks;

import fiji.plugin.trackmate.Spot;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeMultiDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.TrackCollectionData;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.List;

@JIPipeDocumentation(name = "Merge tracks", description = "Merges track lists. Please ensure that the spots are sourced from the same image.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Tracking\nSplit/Merge")
@JIPipeInputSlot(value = TrackCollectionData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = TrackCollectionData.class, slotName = "Output", autoCreate = true)
public class MergeTracksNode extends JIPipeMergingAlgorithm {

    public MergeTracksNode(JIPipeNodeInfo info) {
        super(info);
    }

    public MergeTracksNode(MergeTracksNode other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeMultiDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        List<TrackCollectionData> spotCollections = dataBatch.getInputData(getFirstInputSlot(), TrackCollectionData.class, progressInfo);
        if (spotCollections.isEmpty())
            return;
        if (spotCollections.size() == 1) {
            dataBatch.addOutputData(getFirstOutputSlot(), spotCollections.get(0), progressInfo);
            return;
        }
        TrackCollectionData newCollection = new TrackCollectionData(spotCollections.get(0));
        newCollection.getModel().clearTracks(true);
        for (int i = 1; i < spotCollections.size(); i++) {
            // Copy over spots that do not exist
            TrackCollectionData sourceCollection = spotCollections.get(i);
            for (Spot spot : sourceCollection.getSpots().iterable(true)) {
                int frame = spot.getFeature(Spot.FRAME).intValue();
                Spot closestSpot = newCollection.getSpots().getClosestSpot(spot, frame, true);
                if (closestSpot == null || closestSpot.squareDistanceTo(spot) > 0.00001d) {
                    // Add the missing spot
                    newCollection.getSpots().add(spot, frame);
                }
            }
            newCollection.getModel().beginUpdate();
            for (DefaultWeightedEdge edge : sourceCollection.getTrackModel().edgeSet()) {
                Spot sourceCollectionSource = sourceCollection.getTrackModel().getEdgeSource(edge);
                Spot sourceCollectionTarget = sourceCollection.getTrackModel().getEdgeTarget(edge);
                Spot newCollectionSource = newCollection.getSpots().getClosestSpot(sourceCollectionSource, sourceCollectionSource.getFeature(Spot.FRAME).intValue(), true);
                Spot newCollectionTarget = newCollection.getSpots().getClosestSpot(sourceCollectionTarget, sourceCollectionTarget.getFeature(Spot.FRAME).intValue(), true);
                newCollection.getModel().addEdge(newCollectionSource, newCollectionTarget, sourceCollection.getTrackModel().getEdgeWeight(edge));
            }
            newCollection.getModel().endUpdate();
        }
        dataBatch.addOutputData(getFirstOutputSlot(), newCollection, progressInfo);
    }
}
