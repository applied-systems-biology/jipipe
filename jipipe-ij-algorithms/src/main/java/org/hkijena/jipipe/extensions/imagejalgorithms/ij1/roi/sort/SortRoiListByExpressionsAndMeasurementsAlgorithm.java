package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi.sort;

import ij.ImagePlus;
import ij.gui.Roi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.MeasurementColumn;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.NaturalOrderComparator;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.*;

@JIPipeDocumentation(name = "Sort ROI list (expression)", description = "Sorts a ROI list according to an expression-defined property. Has access to annotations and measurements.")
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", autoCreate = true, optional = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "ROI", autoCreate = true)
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Modify")
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {

        ROIListData inputRois = dataBatch.getInputData("ROI", ROIListData.class, progressInfo);
        ImagePlusData inputReference = dataBatch.getInputData("Reference", ImagePlusData.class, progressInfo);

        ExpressionVariables parameters = new ExpressionVariables();
        ROIListData tmp = new ROIListData();

        if (includeAnnotations) {
            for (JIPipeTextAnnotation value : dataBatch.getMergedTextAnnotations().values()) {
                parameters.set(value.getName(), value.getValue());
            }
        }

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

            parameters.set("index", i);
            parameters.set("num_roi", inputRois.size());

            Roi roi = inputRois.get(i);
            tmp.clear();
            tmp.add(roi);

            ResultsTableData measured = tmp.measure(referenceImage, measurements, true, measureInPhysicalUnits);
            for (int col = 0; col < measured.getColumnCount(); col++) {
                parameters.set(measured.getColumnName(col), measured.getValueAt(0, col) + "");
            }
            sortKeys.put(roi, expression.evaluate(parameters));
        }

        ROIListData outputRois = inputRois.shallowClone();
        if(reverseSortOrder)
            outputRois.sort(Comparator.comparing(sortKeys::get, NaturalOrderComparator.INSTANCE).reversed());
        else
            outputRois.sort(Comparator.comparing(sortKeys::get, NaturalOrderComparator.INSTANCE));
        dataBatch.addOutputData(getFirstOutputSlot(), outputRois, progressInfo);
    }

    @JIPipeDocumentation(name = "Expression", description = "The expression is executed per ROI.")
    @ExpressionParameterSettings(variableSource = VariableSource.class)
    @JIPipeParameter("expression")
    public StringQueryExpression getExpression() {
        return expression;
    }

    @JIPipeParameter("expression")
    public void setExpression(StringQueryExpression expression) {
        this.expression = expression;
    }

    @JIPipeDocumentation(name = "Include annotations", description = "If enabled, annotations are also available as string variables. Please note that " +
            "measurements will overwrite annotations with the same name.")
    @JIPipeParameter("include-annotations")
    public boolean isIncludeAnnotations() {
        return includeAnnotations;
    }

    @JIPipeParameter("include-annotations")
    public void setIncludeAnnotations(boolean includeAnnotations) {
        this.includeAnnotations = includeAnnotations;
    }

    @JIPipeDocumentation(name = "Measurements", description = "Measurements to be included. Please open the expression builder to see the variables generated by the measurements.")
    @JIPipeParameter("measurements")
    public ImageStatisticsSetParameter getMeasurements() {
        return measurements;
    }

    @JIPipeParameter("measurements")
    public void setMeasurements(ImageStatisticsSetParameter measurements) {
        this.measurements = measurements;
    }

    @JIPipeDocumentation(name = "Measure in physical units", description = "If true, measurements will be generated in physical units if available")
    @JIPipeParameter("measure-in-physical-units")
    public boolean isMeasureInPhysicalUnits() {
        return measureInPhysicalUnits;
    }

    @JIPipeParameter("measure-in-physical-units")
    public void setMeasureInPhysicalUnits(boolean measureInPhysicalUnits) {
        this.measureInPhysicalUnits = measureInPhysicalUnits;
    }

    @JIPipeDocumentation(name = "Reverse sort order", description = "If enabled, the sort order is reversed")
    @JIPipeParameter("reverse-sort-order")
    public boolean isReverseSortOrder() {
        return reverseSortOrder;
    }

    @JIPipeParameter("reverse-sort-order")
    public void setReverseSortOrder(boolean reverseSortOrder) {
        this.reverseSortOrder = reverseSortOrder;
    }

    public static class VariableSource implements ExpressionParameterVariableSource {

        public static final Set<ExpressionParameterVariable> VARIABLES;

        static {
            VARIABLES = new HashSet<>();
            VARIABLES.add(ExpressionParameterVariable.ANNOTATIONS_VARIABLE);
            VARIABLES.add(new ExpressionParameterVariable("Name", "Current name of the ROI", "Name"));
            VARIABLES.add(new ExpressionParameterVariable("Index", "Index of the ROI", "index"));
            VARIABLES.add(new ExpressionParameterVariable("Number of ROI", "Number of total ROI in the list", "num_roi"));
            for (MeasurementColumn column : MeasurementColumn.values()) {
                VARIABLES.add(new ExpressionParameterVariable(column.getName(), column.getDescription(), column.getColumnName()));
            }
        }

        @Override
        public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
            return VARIABLES;
        }
    }
}