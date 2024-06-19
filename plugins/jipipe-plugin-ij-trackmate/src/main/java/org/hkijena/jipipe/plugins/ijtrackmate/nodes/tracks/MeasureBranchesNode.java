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

import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.visualization.table.BranchTableView;
import fiji.plugin.trackmate.visualization.table.TablePanel;
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
import org.hkijena.jipipe.plugins.ijtrackmate.utils.JIPipeLogger;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

@SetJIPipeDocumentation(name = "Branch analysis", description = "Analyzes each branch of all "
        + "tracks, and outputs in an ImageJ results "
        + "table the number of its predecessors, of "
        + "successors, and its duration.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Tracking\nMeasure")
@AddJIPipeInputSlot(value = TrackCollectionData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", create = true)
public class MeasureBranchesNode extends JIPipeSimpleIteratingAlgorithm {

    public MeasureBranchesNode(JIPipeNodeInfo info) {
        super(info);
    }

    public MeasureBranchesNode(MeasureBranchesNode other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        TrackCollectionData trackCollectionData = iterationStep.getInputData(getFirstInputSlot(), TrackCollectionData.class, progressInfo);
        trackCollectionData.getModel().setLogger(new JIPipeLogger(progressInfo.resolve("TrackMate")));

        TablePanel<BranchTableView.Branch> branchTable = BranchTableView.createBranchTable(trackCollectionData.getModel(), new SelectionModel(trackCollectionData.getModel()));
        ResultsTableData targetTable = ResultsTableData.fromTableModel(branchTable.getTable().getModel(), branchTable.getTable().getColumnModel(), true);
        iterationStep.addOutputData(getFirstOutputSlot(), targetTable, progressInfo);
    }
}
