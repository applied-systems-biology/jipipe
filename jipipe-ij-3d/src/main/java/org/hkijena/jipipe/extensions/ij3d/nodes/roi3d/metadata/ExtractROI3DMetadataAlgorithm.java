package org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.metadata;

import ij.gui.Roi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3D;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalStringParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.Map;

@JIPipeDocumentation(name = "Extract 3D ROI metadata as table", description = "Extracts the metadata (properties map) of each 3D ROI and writes them into a table")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Metadata")
@JIPipeInputSlot(value = ROI3DListData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROI3DListData rois = dataBatch.getInputData(getFirstInputSlot(), ROI3DListData.class, progressInfo);
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
        dataBatch.addOutputData(getFirstOutputSlot(), table, progressInfo);
    }

    @JIPipeDocumentation(name = "Add ROI name", description = "If enabled, add a column with the ROI name")
    @JIPipeParameter("name-column")
    public OptionalStringParameter getNameColumn() {
        return nameColumn;
    }

    @JIPipeParameter("name-column")
    public void setNameColumn(OptionalStringParameter nameColumn) {
        this.nameColumn = nameColumn;
    }

    @JIPipeDocumentation(name = "Add ROI index", description = "If enabled, add a column with the ROI index")
    @JIPipeParameter("index-column")
    public OptionalStringParameter getIndexColumn() {
        return indexColumn;
    }

    @JIPipeParameter("index-column")
    public void setIndexColumn(OptionalStringParameter indexColumn) {
        this.indexColumn = indexColumn;
    }
}