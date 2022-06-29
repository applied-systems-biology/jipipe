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
import org.jgrapht.graph.DefaultWeightedEdge;

@JIPipeDocumentation(name = "Measure edges", description = "Measures the edges and outputs the results into a table")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Tracking\nMeasure")
@JIPipeInputSlot(value = TrackCollectionData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class MeasureEdgesNode extends JIPipeSimpleIteratingAlgorithm {

    public MeasureEdgesNode(JIPipeNodeInfo info) {
        super(info);
    }

    public MeasureEdgesNode(MeasureEdgesNode other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        TrackCollectionData trackCollectionData = dataBatch.getInputData(getFirstInputSlot(), TrackCollectionData.class, progressInfo);
        ResultsTableData tableData = new ResultsTableData();

        // Compute features
        trackCollectionData.computeEdgeFeatures(progressInfo.resolve("Compute features"));

        for (Integer trackID : trackCollectionData.getTracks().trackIDs(true)) {
            for (DefaultWeightedEdge trackEdge : trackCollectionData.getTracks().trackEdges(trackID)) {
                int row = tableData.addRow();
                tableData.setValueAt("Track_" + trackID, row, "Track");
                tableData.setValueAt(trackCollectionData.getTracks().getEdgeSource(trackEdge).getName(), row, "Edge source");
                tableData.setValueAt(trackCollectionData.getTracks().getEdgeTarget(trackEdge).getName(), row, "Edge target");
                tableData.setValueAt(trackCollectionData.getTracks().getEdgeWeight(trackEdge), row, "Edge weight");
                for (String edgeFeature : trackCollectionData.getModel().getFeatureModel().getEdgeFeatures()) {
                    String columnName = trackCollectionData.getModel().getFeatureModel().getEdgeFeatureNames().get(edgeFeature);
                    Double feature = trackCollectionData.getModel().getFeatureModel().getEdgeFeature(trackEdge, edgeFeature);
                    if(feature == null)
                        feature = Double.NaN;
                    int column = tableData.getOrCreateColumnIndex(columnName, false);
                    tableData.setValueAt(feature, row, column);
                }
            }
        }

        dataBatch.addOutputData(getFirstOutputSlot(), tableData, progressInfo);
    }
}