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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.properties;

import ij.gui.Roi;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJROIUtils;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalStringParameter;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.Map;

@SetJIPipeDocumentation(name = "Extract 2D ROI metadata as table", description = "Extracts the metadata (properties map) of each ROI and writes them into a table")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Metadata")
@AddJIPipeInputSlot(value = ROI2DListData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Output", create = true)
public class ExtractROIMetadataAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OptionalStringParameter nameColumn = new OptionalStringParameter("ROI Name", false);

    private OptionalStringParameter indexColumn = new OptionalStringParameter("ROI Index", false);


    public ExtractROIMetadataAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ExtractROIMetadataAlgorithm(ExtractROIMetadataAlgorithm other) {
        super(other);
        nameColumn = new OptionalStringParameter(other.nameColumn);
        indexColumn = new OptionalStringParameter(other.indexColumn);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROI2DListData rois = iterationStep.getInputData(getFirstInputSlot(), ROI2DListData.class, progressInfo);
        ResultsTableData table = new ResultsTableData();
        for (int i = 0; i < rois.size(); i++) {
            Roi roi = rois.get(i);
            Map<String, String> map = ImageJROIUtils.getRoiProperties(roi);
            int row = table.addRow();
            if (nameColumn.isEnabled()) {
                table.setValueAt(StringUtils.orElse(roi.getName(), "Unnamed"), row, nameColumn.getContent());
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
