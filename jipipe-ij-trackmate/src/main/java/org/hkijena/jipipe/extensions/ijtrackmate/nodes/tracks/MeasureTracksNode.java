package org.hkijena.jipipe.extensions.ijtrackmate.nodes.tracks;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.TrackCollectionData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

@JIPipeDocumentation(name = "Measure tracks", description = "Measures the tracks and outputs the results into a table")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Tracking\nMeasure")
@JIPipeInputSlot(value = TrackCollectionData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class MeasureTracksNode extends JIPipeSimpleIteratingAlgorithm {

    public MeasureTracksNode(JIPipeNodeInfo info) {
        super(info);
    }

    public MeasureTracksNode(MeasureTracksNode other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        TrackCollectionData trackCollectionData = dataBatch.getInputData(getFirstInputSlot(), TrackCollectionData.class, progressInfo);
        ResultsTableData tableData = new ResultsTableData();

        // Compute features
        trackCollectionData.computeTrackFeatures(progressInfo.resolve("Compute features"));

        for (Integer trackID : trackCollectionData.getTrackModel().trackIDs(true)) {
            int row = tableData.addRow();
            tableData.setValueAt("Track_" + trackID, row, "Name");
            for (String trackFeature : trackCollectionData.getModel().getFeatureModel().getTrackFeatures()) {
                String columnName = trackCollectionData.getModel().getFeatureModel().getTrackFeatureNames().get(trackFeature);
                Double feature = trackCollectionData.getModel().getFeatureModel().getTrackFeature(trackID, trackFeature);
                if(feature == null)
                    feature = Double.NaN;
                int column = tableData.getOrCreateColumnIndex(columnName, false);
                tableData.setValueAt(feature, row, column);
            }
        }

        dataBatch.addOutputData(getFirstOutputSlot(), tableData, progressInfo);
    }
}
