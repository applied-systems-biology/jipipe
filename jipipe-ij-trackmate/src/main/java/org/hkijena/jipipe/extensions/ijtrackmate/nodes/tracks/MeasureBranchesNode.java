package org.hkijena.jipipe.extensions.ijtrackmate.nodes.tracks;

import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.visualization.table.BranchTableView;
import fiji.plugin.trackmate.visualization.table.TablePanel;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.DefineJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.TrackCollectionData;
import org.hkijena.jipipe.extensions.ijtrackmate.utils.JIPipeLogger;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

@SetJIPipeDocumentation(name = "Branch analysis", description = "Analyzes each branch of all "
        + "tracks, and outputs in an ImageJ results "
        + "table the number of its predecessors, of "
        + "successors, and its duration.")
@DefineJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Tracking\nMeasure")
@AddJIPipeInputSlot(value = TrackCollectionData.class, slotName = "Input", create = true)
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
