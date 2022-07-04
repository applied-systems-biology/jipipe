package org.hkijena.jipipe.extensions.ijtrackmate.nodes.spots;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.variables.AnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.SpotDetectorData;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.SpotsCollectionData;
import org.hkijena.jipipe.extensions.ijtrackmate.utils.SpotFeatureVariableSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;

import java.util.Map;

@JIPipeDocumentation(name = "Filter spots", description = "Filter TrackMate spots via expressions")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Tracking\nFilter")
@JIPipeInputSlot(value = SpotsCollectionData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = SpotsCollectionData.class, slotName = "Output",autoCreate = true)
public class SpotFilterNode extends JIPipeSimpleIteratingAlgorithm {

    private DefaultExpressionParameter filter = new DefaultExpressionParameter("quality > 30");

    public SpotFilterNode(JIPipeNodeInfo info) {
        super(info);
    }

    public SpotFilterNode(SpotFilterNode other) {
        super(other);
        this.filter = new DefaultExpressionParameter(other.filter);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        SpotsCollectionData spotsCollectionData = new SpotsCollectionData(dataBatch.getInputData(getFirstInputSlot(), SpotsCollectionData.class, progressInfo));
        SpotCollection newCollection = new SpotCollection();
        SpotCollection oldCollection = spotsCollectionData.getSpots();
        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());
        variables.set("n_spots", oldCollection.getNSpots(true));
        int index = 0;
        for (Spot spot : oldCollection.iterable(true)) {
            variables.set("name", spot.getName());
            variables.set("id", spot.ID());
            variables.set("index", index);
            for (Map.Entry<String, Double> entry : spot.getFeatures().entrySet()) {
                String variableName = SpotFeatureVariableSource.keyToVariable(entry.getKey());
                variables.set(variableName, entry.getValue());
            }
            if(filter.test(variables)) {
                newCollection.add(spot, spot.getFeature(Spot.FRAME).intValue());
            }
        }
        spotsCollectionData.getModel().setSpots(newCollection, true);
        dataBatch.addOutputData(getFirstOutputSlot(), spotsCollectionData, progressInfo);
        ++index;
    }

    @JIPipeDocumentation(name = "Filter", description = "The expression is executed per spot. If it returns TRUE, the spot is kept.")
    @JIPipeParameter(value = "filter", important = true)
    @ExpressionParameterSettingsVariable(fromClass = SpotFeatureVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = AnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(name = "Spot ID", key = "id", description = "Numeric spot ID. Please note that the ID is not necessarily consecutive.")
    @ExpressionParameterSettingsVariable(name = "Spot index", key = "index", description = "Numeric index.")
    @ExpressionParameterSettingsVariable(name = "Number of spots", key = "n_spots", description = "The total number of spots")
    public DefaultExpressionParameter getFilter() {
        return filter;
    }

    @JIPipeParameter("filter")
    public void setFilter(DefaultExpressionParameter filter) {
        this.filter = filter;
    }
}
