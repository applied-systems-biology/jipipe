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

package org.hkijena.jipipe.extensions.ijtrackmate.nodes.tracks;

import fiji.plugin.trackmate.tracking.SpotTracker;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.SpotTrackerData;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.SpotsCollectionData;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.TrackCollectionData;
import org.hkijena.jipipe.extensions.ijtrackmate.utils.JIPipeLogger;

@SetJIPipeDocumentation(name = "Track spots", description = "Track spots using TrackMate")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Tracking")
@AddJIPipeInputSlot(value = SpotsCollectionData.class, slotName = "Spots", description = "The detected spots", create = true)
@AddJIPipeInputSlot(value = SpotTrackerData.class, slotName = "Spot tracker", description = "The algorithm that tracks the spots", create = true)
@AddJIPipeOutputSlot(value = TrackCollectionData.class, slotName = "Tracks", description = "The detected tracks", create = true)
public class TrackingNode extends JIPipeIteratingAlgorithm {

    private int numThreads = 1;

    public TrackingNode(JIPipeNodeInfo info) {
        super(info);
    }

    public TrackingNode(TrackingNode other) {
        super(other);
        numThreads = other.numThreads;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        SpotsCollectionData spotsCollectionData = iterationStep.getInputData("Spots", SpotsCollectionData.class, progressInfo);
        SpotTrackerData spotTrackerData = iterationStep.getInputData("Spot tracker", SpotTrackerData.class, progressInfo);
        final SpotTracker tracker = spotTrackerData.getTrackerFactory().create(spotsCollectionData.getSpots(), spotTrackerData.getSettings());
        tracker.setNumThreads(numThreads);
        tracker.setLogger(new JIPipeLogger(progressInfo.resolve("TrackMate")));
        if (!tracker.checkInput())
            throw new RuntimeException("Tracker input is invalid: " + tracker.getErrorMessage());
        if (!tracker.process())
            throw new RuntimeException("Error while tracking: " + tracker.getErrorMessage());
        TrackCollectionData trackCollectionData = new TrackCollectionData(spotsCollectionData);
        trackCollectionData.getModel().setTracks(tracker.getResult(), true);
        iterationStep.addOutputData(getFirstOutputSlot(), trackCollectionData, progressInfo);
    }
}
