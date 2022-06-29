package org.hkijena.jipipe.extensions.ijtrackmate.nodes.spots;

import fiji.plugin.trackmate.Spot;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.SpotsCollectionData;
import org.hkijena.jipipe.extensions.ijtrackmate.utils.SpotFeatureVariableSource;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.util.Map;

@JIPipeDocumentation(name = "Measure spots", description = "Measures the spots and outputs the results into a table")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Tracking\nMeasure")
@JIPipeInputSlot(value = SpotsCollectionData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class MeasureSpotsNode extends JIPipeSimpleIteratingAlgorithm {

    public MeasureSpotsNode(JIPipeNodeInfo info) {
        super(info);
    }

    public MeasureSpotsNode(MeasureSpotsNode other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        SpotsCollectionData spotsCollectionData = dataBatch.getInputData(getFirstInputSlot(), SpotsCollectionData.class, progressInfo);
        ResultsTableData tableData = new ResultsTableData();
        for (Spot spot : spotsCollectionData.getSpots().iterable(false)) {
            int row = tableData.addRow();
            tableData.setValueAt(spot.getName(), row, "Name");
            for (Map.Entry<String, Double> entry : spot.getFeatures().entrySet()) {
                String columnName = spotsCollectionData.getModel().getFeatureModel().getSpotFeatureNames().get(entry.getKey());
                int column = tableData.getOrCreateColumnIndex(columnName, false);
                tableData.setValueAt(entry.getValue(), row, column);
            }
        }
        dataBatch.addOutputData(getFirstOutputSlot(), tableData, progressInfo);
    }
}
