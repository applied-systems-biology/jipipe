package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi;

import ij.gui.Roi;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariableInfo;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJROIUtils;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.ColorUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Roi2DPropertiesExpressionVariablesInfo implements JIPipeExpressionVariablesInfo {

    private static final Set<JIPipeExpressionParameterVariableInfo> VARIABLES = new HashSet<>();

    static {
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("fill_color", "Fill color", "The fill color of the ROI"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("line_color", "Line color", "The line color of the ROI"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("line_width", "Line width", "The line width"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("c", "Channel location", "The channel (C) location. The first index is 1. Zero indicates that that ROI applies to all locations."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("z", "Slice location", "The slice (Z) location. The first index is 1. Zero indicates that that ROI applies to all locations."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("t", "Frame location", "The frame (T) location. The first index is 1. Zero indicates that that ROI applies to all locations."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("x", "X Location", "The X location of the ROI"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("y", "Y Location", "The Y location of the ROI"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("name", "Name", "The ROI name"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("index", "Index", "The index of the ROI"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("num_roi", "Number of ROI", "The number of ROI in the list"));
    }

    public static void putVariables(Roi roi, int numRoi, ResultsTableData statistics, int index, JIPipeExpressionVariablesMap variables) {
        double x = roi.getXBase();
        double y = roi.getYBase();
        int z = roi.getZPosition();
        int c = roi.getCPosition();
        int t = roi.getTPosition();

        Map<String, String> roiProperties = ImageJROIUtils.getRoiProperties(roi);
        variables.set("metadata", roiProperties);
        for (Map.Entry<String, String> entry : roiProperties.entrySet()) {
            variables.set("metadata." + entry.getKey(), entry.getValue());
        }

        if (statistics != null) {
            for (int col = 0; col < statistics.getColumnCount(); col++) {
                variables.set(statistics.getColumnName(col), statistics.getValueAt(index, col));
            }
        }

        variables.set("index", index);
        variables.set("num_roi", numRoi);
        variables.set("x", x);
        variables.set("y", y);
        variables.set("z", z);
        variables.set("c", c);
        variables.set("t", t);
        variables.set("fill_color", roi.getFillColor() != null ? ColorUtils.colorToHexString(roi.getFillColor()) : null);
        variables.set("line_color", roi.getStrokeColor() != null ? ColorUtils.colorToHexString(roi.getStrokeColor()) : null);
        variables.set("line_width", roi.getStrokeWidth());
        variables.set("name", roi.getName());
    }

    @Override
    public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}
