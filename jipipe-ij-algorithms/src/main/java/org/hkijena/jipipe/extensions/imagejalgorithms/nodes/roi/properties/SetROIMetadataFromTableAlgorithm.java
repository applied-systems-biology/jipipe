package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi.properties;

import ij.gui.Roi;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.DefineJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.TableColumnSourceExpressionParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;

import java.util.HashMap;
import java.util.Map;

@SetJIPipeDocumentation(name = "Set ROI metadata from table", description = "Sets the ROI metadata (property map) from a table. The table either has a column that indicates the ROI index or contains one row per ROI (row index is the ROI index)")
@DefineJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Metadata")
@AddJIPipeInputSlot(value = ROIListData.class, slotName = "ROI", create = true)
@AddJIPipeInputSlot(value = ResultsTableData.class, slotName = "Metadata", description = "Table of ROI metadata, one row per ROI", create = true)
@AddJIPipeOutputSlot(value = ROIListData.class, slotName = "Output", create = true)
public class SetROIMetadataFromTableAlgorithm extends JIPipeIteratingAlgorithm {

    private TableColumnSourceExpressionParameter roiIndexColumn = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.Generate, "row");
    private boolean clearBeforeWrite = false;
    private boolean ignoreMissingRoiIndices = false;

    public SetROIMetadataFromTableAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SetROIMetadataFromTableAlgorithm(SetROIMetadataFromTableAlgorithm other) {
        super(other);
        this.roiIndexColumn = new TableColumnSourceExpressionParameter(other.roiIndexColumn);
        this.clearBeforeWrite = other.clearBeforeWrite;
        this.ignoreMissingRoiIndices = other.ignoreMissingRoiIndices;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROIListData rois = new ROIListData(iterationStep.getInputData("ROI", ROIListData.class, progressInfo));
        ResultsTableData metadata = iterationStep.getInputData("Metadata", ResultsTableData.class, progressInfo);
        TableColumn indexColumn = roiIndexColumn.pickOrGenerateColumn(metadata);

        for (int i = 0; i < indexColumn.getRows(); i++) {
            int roiIndex = (int) indexColumn.getRowAsDouble(i);
            if (roiIndex < 0 || roiIndex >= rois.size()) {
                if (ignoreMissingRoiIndices)
                    continue;
                throw new IndexOutOfBoundsException("There is no ROI with index " + roiIndex);
            }
            Roi roi = rois.get(i);
            Map<String, String> properties = clearBeforeWrite ? new HashMap<>() : ImageJUtils.getRoiProperties(roi);
            for (String columnName : metadata.getColumnNames()) {
                if (roiIndexColumn.getKey() == TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn && columnName.equals(indexColumn.getLabel()))
                    continue;
                properties.put(columnName, metadata.getValueAsString(i, columnName));
            }
            ImageJUtils.setRoiProperties(roi, properties);
        }

        iterationStep.addOutputData(getFirstOutputSlot(), rois, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Ignore missing ROI indices", description = "If enabled, missing ROI indices are ignored (a log message is written)")
    @JIPipeParameter("ignore-missing-roi-indices")
    public boolean isIgnoreMissingRoiIndices() {
        return ignoreMissingRoiIndices;
    }

    @JIPipeParameter("ignore-missing-roi-indices")
    public void setIgnoreMissingRoiIndices(boolean ignoreMissingRoiIndices) {
        this.ignoreMissingRoiIndices = ignoreMissingRoiIndices;
    }

    @SetJIPipeDocumentation(name = "ROI index", description = "Determines how the table column is associated to the index of the ROI (zero-based). Defaults to assuming that the row index is the ROI index.")
    @JIPipeParameter("roi-index-column")
    public TableColumnSourceExpressionParameter getRoiIndexColumn() {
        return roiIndexColumn;
    }

    @JIPipeParameter("roi-index-column")
    public void setRoiIndexColumn(TableColumnSourceExpressionParameter roiIndexColumn) {
        this.roiIndexColumn = roiIndexColumn;
    }

    @SetJIPipeDocumentation(name = "Clear properties before write", description = "If enabled, all existing ROI properties are deleted before writing the new properties")
    @JIPipeParameter("clear-before-write")
    public boolean isClearBeforeWrite() {
        return clearBeforeWrite;
    }

    @JIPipeParameter("clear-before-write")
    public void setClearBeforeWrite(boolean clearBeforeWrite) {
        this.clearBeforeWrite = clearBeforeWrite;
    }
}
