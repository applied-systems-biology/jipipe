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

package org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.metadata;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.TableColumnSourceExpressionParameter;
import org.hkijena.jipipe.plugins.ij3d.datatypes.ROI3D;
import org.hkijena.jipipe.plugins.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumn;

import java.util.HashMap;
import java.util.Map;

@SetJIPipeDocumentation(name = "Set 3D ROI metadata from table", description = "Sets the 3D ROI metadata (property map) from a table. The table either has a column that indicates the ROI index or contains one row per ROI (row index is the ROI index)")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Metadata")
@AddJIPipeInputSlot(value = ROIListData.class, slotName = "ROI", create = true)
@AddJIPipeInputSlot(value = ResultsTableData.class, slotName = "Metadata", description = "Table of ROI metadata, one row per ROI", create = true)
@AddJIPipeOutputSlot(value = ROIListData.class, slotName = "Output", create = true)
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROI3DListData rois = new ROI3DListData(iterationStep.getInputData("ROI", ROI3DListData.class, progressInfo));
        ResultsTableData metadata = iterationStep.getInputData("Metadata", ResultsTableData.class, progressInfo);
        TableColumn indexColumn = roiIndexColumn.pickOrGenerateColumn(metadata, new JIPipeExpressionVariablesMap());

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
