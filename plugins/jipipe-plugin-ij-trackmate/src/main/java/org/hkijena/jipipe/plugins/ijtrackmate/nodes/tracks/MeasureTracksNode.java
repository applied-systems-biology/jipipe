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

package org.hkijena.jipipe.plugins.ijtrackmate.nodes.tracks;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.plugins.ijtrackmate.datatypes.TrackCollectionData;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

@SetJIPipeDocumentation(name = "Measure tracks", description = "Measures the tracks and outputs the results into a table")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Tracking\nMeasure")
@AddJIPipeInputSlot(value = TrackCollectionData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", create = true)
public class MeasureTracksNode extends JIPipeSimpleIteratingAlgorithm {

    public MeasureTracksNode(JIPipeNodeInfo info) {
        super(info);
    }

    public MeasureTracksNode(MeasureTracksNode other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        TrackCollectionData trackCollectionData = iterationStep.getInputData(getFirstInputSlot(), TrackCollectionData.class, progressInfo);
        ResultsTableData tableData = new ResultsTableData();

        // Compute features
        trackCollectionData.computeTrackFeatures(progressInfo.resolve("Compute features"));

        for (Integer trackID : trackCollectionData.getTrackModel().trackIDs(true)) {
            int row = tableData.addRow();
            tableData.setValueAt("Track_" + trackID, row, "Name");
            for (String trackFeature : trackCollectionData.getModel().getFeatureModel().getTrackFeatures()) {
                String columnName = trackCollectionData.getModel().getFeatureModel().getTrackFeatureNames().get(trackFeature);
                Double feature = trackCollectionData.getModel().getFeatureModel().getTrackFeature(trackID, trackFeature);
                if (feature == null)
                    feature = Double.NaN;
                int column = tableData.getOrCreateColumnIndex(columnName, false);
                tableData.setValueAt(feature, row, column);
            }
        }

        iterationStep.addOutputData(getFirstOutputSlot(), tableData, progressInfo);
    }
}
