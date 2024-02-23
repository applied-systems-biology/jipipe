package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi.sort;

import ij.ImagePlus;
import ij.gui.Roi;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.MeasurementColumn;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.NaturalOrderComparator;

import java.util.*;

@SetJIPipeDocumentation(name = "Sort ROI list (expression)", description = "Sorts a ROI list according to an expression-defined property. Has access to annotations and measurements.")
@AddJIPipeInputSlot(value = ROIListData.class, slotName = "ROI", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", create = true, optional = true)
@AddJIPipeOutputSlot(value = ROIListData.class, slotName = "ROI", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Modify")
public class SortRoiListByExpressionsAndMeasurementsAlgorithm extends JIPipeIteratingAlgorithm {

    private StringQueryExpression expression = new StringQueryExpression();
    private boolean includeAnnotations = true;
    private ImageStatisticsSetParameter measurements = new ImageStatisticsSetParameter();
    private boolean measureInPhysicalUnits = true;
    private boolean reverseSortOrder = false;

    public SortRoiListByExpressionsAndMeasurementsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SortRoiListByExpressionsAndMeasurementsAlgorithm(SortRoiListByExpressionsAndMeasurementsAlgorithm other) {
        super(other);
        this.expression = new StringQueryExpression(other.expression);
        this.includeAnnotations = other.includeAnnotations;
        this.measurements = new ImageStatisticsSetParameter(other.measurements);
        this.measureInPhysicalUnits = other.measureInPhysicalUnits;
        this.reverseSortOrder = other.reverseSortOrder;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {

        ROIListData inputRois = iterationStep.getInputData("ROI", ROIListData.class, progressInfo);
        ImagePlusData inputReference = iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo);

        JIPipeExpressionVariablesMap variablesMap = new JIPipeExpressionVariablesMap();
        ROIListData tmp = new ROIListData();

        if (includeAnnotations) {
            variablesMap.putAnnotations(iterationStep.getMergedTextAnnotations());
        }
        variablesMap.putCustomVariables(getDefaultCustomExpressionVariables());

        ImagePlus referenceImage = null;
        if (inputReference != null) {
            referenceImage = inputReference.getImage();
        }
        if (referenceImage != null) {
            // This is needed, as measuring messes with the image
            referenceImage = ImageJUtils.duplicate(referenceImage);
        }
        Map<Roi, Object> sortKeys = new IdentityHashMap<>();
        for (int i = 0; i < inputRois.size(); i++) {

            variablesMap.set("index", i);
            variablesMap.set("num_roi", inputRois.size());

            Roi roi = inputRois.get(i);
            tmp.clear();
            tmp.add(roi);

            ResultsTableData measured = tmp.measure(referenceImage, measurements, true, measureInPhysicalUnits);
            for (int col = 0; col < measured.getColumnCount(); col++) {
                variablesMap.set(measured.getColumnName(col), measured.getValueAt(0, col));
            }
            sortKeys.put(roi, expression.evaluate(variablesMap));
        }

        ROIListData outputRois = inputRois.shallowClone();
        if (reverseSortOrder)
            outputRois.sort(Comparator.comparing(sortKeys::get, NaturalOrderComparator.INSTANCE).reversed());
        else
            outputRois.sort(Comparator.comparing(sortKeys::get, NaturalOrderComparator.INSTANCE));
        iterationStep.addOutputData(getFirstOutputSlot(), outputRois, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Expression", description = "The expression is executed per ROI.")
    @JIPipeExpressionParameterSettings(variableSource = VariablesInfo.class, hint = "per ROI")
    @JIPipeParameter("expression")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    public StringQueryExpression getExpression() {
        return expression;
    }

    @JIPipeParameter("expression")
    public void setExpression(StringQueryExpression expression) {
        this.expression = expression;
    }

    @SetJIPipeDocumentation(name = "Include annotations", description = "If enabled, annotations are also available as string variables. Please note that " +
            "measurements will overwrite annotations with the same name.")
    @JIPipeParameter("include-annotations")
    public boolean isIncludeAnnotations() {
        return includeAnnotations;
    }

    @JIPipeParameter("include-annotations")
    public void setIncludeAnnotations(boolean includeAnnotations) {
        this.includeAnnotations = includeAnnotations;
    }

    @SetJIPipeDocumentation(name = "Measurements", description = "Measurements to be included. Please open the expression builder to see the variables generated by the measurements.")
    @JIPipeParameter(value = "measurements", important = true)
    public ImageStatisticsSetParameter getMeasurements() {
        return measurements;
    }

    @JIPipeParameter("measurements")
    public void setMeasurements(ImageStatisticsSetParameter measurements) {
        this.measurements = measurements;
    }

    @SetJIPipeDocumentation(name = "Measure in physical units", description = "If true, measurements will be generated in physical units if available")
    @JIPipeParameter("measure-in-physical-units")
    public boolean isMeasureInPhysicalUnits() {
        return measureInPhysicalUnits;
    }

    @JIPipeParameter("measure-in-physical-units")
    public void setMeasureInPhysicalUnits(boolean measureInPhysicalUnits) {
        this.measureInPhysicalUnits = measureInPhysicalUnits;
    }

    @SetJIPipeDocumentation(name = "Reverse sort order", description = "If enabled, the sort order is reversed")
    @JIPipeParameter("reverse-sort-order")
    public boolean isReverseSortOrder() {
        return reverseSortOrder;
    }

    @JIPipeParameter("reverse-sort-order")
    public void setReverseSortOrder(boolean reverseSortOrder) {
        this.reverseSortOrder = reverseSortOrder;
    }

    @Override
    public boolean isEnableDefaultCustomExpressionVariables() {
        return true;
    }

    public static class VariablesInfo implements ExpressionParameterVariablesInfo {

        public static final Set<JIPipeExpressionParameterVariableInfo> VARIABLES;

        static {
            VARIABLES = new HashSet<>();
            VARIABLES.add(JIPipeExpressionParameterVariableInfo.ANNOTATIONS_VARIABLE);
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("Name", "Name", "Current name of the ROI"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("index", "Index", "Index of the ROI"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("num_roi", "Number of ROI", "Number of total ROI in the list"));
            for (MeasurementColumn column : MeasurementColumn.values()) {
                VARIABLES.add(new JIPipeExpressionParameterVariableInfo(column.getColumnName(), column.getName(), column.getDescription()));
            }
        }

        @Override
        public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
            return VARIABLES;
        }
    }
}