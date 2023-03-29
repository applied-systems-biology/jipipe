package org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.metadata;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.TableColumnSourceExpressionParameter;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3D;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;

import java.util.HashMap;
import java.util.Map;

@JIPipeDocumentation(name = "Set 3D ROI metadata from table", description = "Sets the 3D ROI metadata (property map) from a table. The table either has a column that indicates the ROI index or contains one row per ROI (row index is the ROI index)")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Metadata")
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI", autoCreate = true)
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Metadata", description = "Table of ROI metadata, one row per ROI", autoCreate = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output", autoCreate = true)
public class SetROI3DMetadataFromTableAlgorithm extends JIPipeIteratingAlgorithm {

    private TableColumnSourceExpressionParameter roiIndexColumn = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.Generate, "row");
    private boolean clearBeforeWrite = false;
    private boolean ignoreMissingRoiIndices = false;

    public SetROI3DMetadataFromTableAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SetROI3DMetadataFromTableAlgorithm(SetROI3DMetadataFromTableAlgorithm other) {
        super(other);
        this.roiIndexColumn = new TableColumnSourceExpressionParameter(other.roiIndexColumn);
        this.clearBeforeWrite = other.clearBeforeWrite;
        this.ignoreMissingRoiIndices = other.ignoreMissingRoiIndices;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROI3DListData rois = new ROI3DListData(dataBatch.getInputData("ROI", ROI3DListData.class, progressInfo));
        ResultsTableData metadata = dataBatch.getInputData("Metadata", ResultsTableData.class, progressInfo);
        TableColumn indexColumn = roiIndexColumn.pickOrGenerateColumn(metadata);

        for (int i = 0; i < indexColumn.getRows(); i++) {
            int roiIndex = (int) indexColumn.getRowAsDouble(i);
            if (roiIndex < 0 || roiIndex >= rois.size()) {
                if (ignoreMissingRoiIndices)
                    continue;
                throw new IndexOutOfBoundsException("There is no ROI with index " + roiIndex);
            }
            ROI3D roi = rois.get(i);
            Map<String, String> properties = clearBeforeWrite ? new HashMap<>() : roi.getMetadata();
            for (String columnName : metadata.getColumnNames()) {
                if (roiIndexColumn.getKey() == TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn && columnName.equals(indexColumn.getLabel()))
                    continue;
                properties.put(columnName, metadata.getValueAsString(i, columnName));
            }
            roi.setMetadata(properties);
        }

        dataBatch.addOutputData(getFirstOutputSlot(), rois, progressInfo);
    }

    @JIPipeDocumentation(name = "Ignore missing ROI indices", description = "If enabled, missing ROI indices are ignored (a log message is written)")
    @JIPipeParameter("ignore-missing-roi-indices")
    public boolean isIgnoreMissingRoiIndices() {
        return ignoreMissingRoiIndices;
    }

    @JIPipeParameter("ignore-missing-roi-indices")
    public void setIgnoreMissingRoiIndices(boolean ignoreMissingRoiIndices) {
        this.ignoreMissingRoiIndices = ignoreMissingRoiIndices;
    }

    @JIPipeDocumentation(name = "ROI index", description = "Determines how the table column is associated to the index of the ROI (zero-based). Defaults to assuming that the row index is the ROI index.")
    @JIPipeParameter("roi-index-column")
    public TableColumnSourceExpressionParameter getRoiIndexColumn() {
        return roiIndexColumn;
    }

    @JIPipeParameter("roi-index-column")
    public void setRoiIndexColumn(TableColumnSourceExpressionParameter roiIndexColumn) {
        this.roiIndexColumn = roiIndexColumn;
    }

    @JIPipeDocumentation(name = "Clear properties before write", description = "If enabled, all existing ROI properties are deleted before writing the new properties")
    @JIPipeParameter("clear-before-write")
    public boolean isClearBeforeWrite() {
        return clearBeforeWrite;
    }

    @JIPipeParameter("clear-before-write")
    public void setClearBeforeWrite(boolean clearBeforeWrite) {
        this.clearBeforeWrite = clearBeforeWrite;
    }
}
