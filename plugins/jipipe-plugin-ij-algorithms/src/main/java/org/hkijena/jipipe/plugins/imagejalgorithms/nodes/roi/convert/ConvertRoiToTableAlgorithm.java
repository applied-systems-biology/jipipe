package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.convert;

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
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;

@SetJIPipeDocumentation(name = "Convert 2D ROI to table", description = "Writes the point coordinates and other metadata of the incoming ROI into a table")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Convert")
@AddJIPipeInputSlot(value = ROI2DListData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Output", create = true)
public class ConvertRoiToTableAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public ConvertRoiToTableAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ConvertRoiToTableAlgorithm(ConvertRoiToTableAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROI2DListData inputData = iterationStep.getInputData(getFirstInputSlot(), ROI2DListData.class, progressInfo);
        ResultsTableData outputData = new ResultsTableData();

        for (int i = 0; i < inputData.size(); i++) {
            Roi roi = inputData.get(i);
            Map<String, Object> data = new HashMap<>();
            data.put("ROI.Index", i);
            data.put("ROI.Name", roi.getName());
            data.put("ROI.X", roi.getXBase());
            data.put("ROI.Y", roi.getYBase());
            data.put("ROI.BX", roi.getBounds().getMinX());
            data.put("ROI.BY", roi.getBounds().getMinY());
            data.put("ROI.BWidth", roi.getBounds().getWidth());
            data.put("ROI.BHeight", roi.getBounds().getHeight());
            data.put("ROI.C", roi.getCPosition());
            data.put("ROI.Z", roi.getZPosition());
            data.put("ROI.T", roi.getTPosition());
            for (Map.Entry<String, String> entry : ImageJUtils.getRoiProperties(roi).entrySet()) {
                data.put("ROI.Metadata." + entry.getKey(), entry.getValue());
            }

            for (Point2D point : ImageJUtils.getContourPoints(roi)) {
                data.put("X", point.getX());
                data.put("Y", point.getY());
                outputData.addRow(data);
            }
        }

        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }
}
