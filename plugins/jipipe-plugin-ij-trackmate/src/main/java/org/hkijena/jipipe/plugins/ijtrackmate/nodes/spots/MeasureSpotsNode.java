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
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFamilyStepAnnotation;
import org.hkijena.jipipe.api.nodes.taxonomy.families.TrackMateFamily;
import org.hkijena.jipipe.api.nodes.taxonomy.frameworks.TrackMateFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.tracking.TrackingAnalysisFunction;
import org.hkijena.jipipe.plugins.ijtrackmate.datatypes.SpotsCollectionData;
import org.hkijena.jipipe.plugins.ijtrackmate.datatypes.TrackCollectionData;
import org.hkijena.jipipe.plugins.ijtrackmate.nodes.tracks.TrackingNode;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

import java.util.Map;

@JIPipeNodeTaxonomyFamilyStepAnnotation(family = TrackMateFamily.class, label = "Measure spots", after = {TrackingNode.class}, purpose = "Measure the tracked spots into a results table")
@SetJIPipeDocumentation(name = "Measure spots", description = "Measures the spots and outputs the results into a table. If tracked spots are provided, the track ID will also be added.")
@ConfigureJIPipeNode(function = TrackingAnalysisFunction.class, framework = TrackMateFramework.class)
@AddJIPipeInputSlot(value = SpotsCollectionData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Output", create = true)
public class MeasureSpotsNode extends JIPipeSimpleIteratingAlgorithm {

    public MeasureSpotsNode(JIPipeNodeInfo info) {
        super(info);
    }

    public MeasureSpotsNode(MeasureSpotsNode other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        SpotsCollectionData spotsCollectionData = iterationStep.getInputData(getFirstInputSlot(), SpotsCollectionData.class, progressInfo);
        ResultsTableData tableData = new ResultsTableData();
        if (spotsCollectionData instanceof TrackCollectionData) {
            TrackCollectionData trackCollectionData = (TrackCollectionData) spotsCollectionData;
            // Compute features
            trackCollectionData.computeTrackFeatures(progressInfo.resolve("Compute features"));

            for (Integer trackID : trackCollectionData.getTrackModel().trackIDs(true)) {
                for (Spot spot : trackCollectionData.getTrackSpots(trackID)) {
                    int row = tableData.addRow();
                    tableData.setValueAt(trackCollectionData.getTrackModel().name(trackID), row, "Track name");
                    tableData.setValueAt(trackID, row, "Track index");
                    tableData.setValueAt(trackID, row, "Track ID");
                    tableData.setValueAt(spot.getName(), row, "Name");
                    for (Map.Entry<String, Double> entry : spot.getFeatures().entrySet()) {
                        String columnName = spotsCollectionData.getModel().getFeatureModel().getSpotFeatureNames().get(entry.getKey());
                        int column = tableData.getOrCreateColumnIndex(columnName, false);
                        tableData.setValueAt(entry.getValue(), row, column);
                    }
                }
            }
        } else {
            for (Spot spot : spotsCollectionData.getSpots().iterable(true)) {
                int row = tableData.addRow();
                tableData.setValueAt(spot.getName(), row, "Name");
                for (Map.Entry<String, Double> entry : spot.getFeatures().entrySet()) {
                    String columnName = spotsCollectionData.getModel().getFeatureModel().getSpotFeatureNames().get(entry.getKey());
                    int column = tableData.getOrCreateColumnIndex(columnName, false);
                    tableData.setValueAt(entry.getValue(), row, column);
                }
            }
        }
        iterationStep.addOutputData(getFirstOutputSlot(), tableData, progressInfo);
    }
}
