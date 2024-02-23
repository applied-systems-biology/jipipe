package org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.metadata;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3D;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalStringParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.Map;

@SetJIPipeDocumentation(name = "Extract 3D ROI metadata as table", description = "Extracts the metadata (properties map) of each 3D ROI and writes them into a table")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Metadata")
@AddJIPipeInputSlot(value = ROI3DListData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", create = true)
public class ExtractROI3DMetadataAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OptionalStringParameter nameColumn = new OptionalStringParameter("ROI Name", false);

    private OptionalStringParameter indexColumn = new OptionalStringParameter("ROI Index", false);


    public ExtractROI3DMetadataAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ExtractROI3DMetadataAlgorithm(ExtractROI3DMetadataAlgorithm other) {
        super(other);
        nameColumn = new OptionalStringParameter(other.nameColumn);
        indexColumn = new OptionalStringParameter(other.indexColumn);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROI3DListData rois = iterationStep.getInputData(getFirstInputSlot(), ROI3DListData.class, progressInfo);
        ResultsTableData table = new ResultsTableData();
        for (int i = 0; i < rois.size(); i++) {
            ROI3D roi = rois.get(i);
            Map<String, String> map = roi.getMetadata();
            int row = table.addRow();
            if (nameColumn.isEnabled()) {
                table.setValueAt(StringUtils.orElse(roi.getObject3D().getName(), "Unnamed"), row, nameColumn.getContent());
            }
            if (indexColumn.isEnabled()) {
                table.setValueAt(i, row, indexColumn.getContent());
            }
            for (Map.Entry<String, String> entry : map.entrySet()) {
                table.setValueAt(entry.getValue(), row, entry.getKey());
            }
        }
        iterationStep.addOutputData(getFirstOutputSlot(), table, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Add ROI name", description = "If enabled, add a column with the ROI name")
    @JIPipeParameter("name-column")
    public OptionalStringParameter getNameColumn() {
        return nameColumn;
    }

    @JIPipeParameter("name-column")
    public void setNameColumn(OptionalStringParameter nameColumn) {
        this.nameColumn = nameColumn;
    }

    @SetJIPipeDocumentation(name = "Add ROI index", description = "If enabled, add a column with the ROI index")
    @JIPipeParameter("index-column")
    public OptionalStringParameter getIndexColumn() {
        return indexColumn;
    }

    @JIPipeParameter("index-column")
    public void setIndexColumn(OptionalStringParameter indexColumn) {
        this.indexColumn = indexColumn;
    }
}
