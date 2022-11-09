package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi.modify;

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
import org.hkijena.jipipe.utils.StringUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@JIPipeDocumentation(name = "Change ROI name from measurements (expression)", description = "Utilizes an expression to generate a ROI name for each individual ROI in the supplied ROI lists." +
        "The expression has access to annotations and statistics.")
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", autoCreate = true, optional = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "ROI", autoCreate = true)
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Modify")
public class ChangeRoiNameFromExpressionsAndMeasurementsAlgorithm extends JIPipeIteratingAlgorithm {

    private StringQueryExpression expression = new StringQueryExpression();
    private boolean includeAnnotations = true;
    private ImageStatisticsSetParameter measurements = new ImageStatisticsSetParameter();
    private boolean measureInPhysicalUnits = true;

    public ChangeRoiNameFromExpressionsAndMeasurementsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ChangeRoiNameFromExpressionsAndMeasurementsAlgorithm(ChangeRoiNameFromExpressionsAndMeasurementsAlgorithm other) {
        super(other);
        this.expression = new StringQueryExpression(other.expression);
        this.includeAnnotations = other.includeAnnotations;
        this.measurements = new ImageStatisticsSetParameter(other.measurements);
        this.measureInPhysicalUnits = other.measureInPhysicalUnits;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {

        ROIListData inputRois = dataBatch.getInputData("ROI", ROIListData.class, progressInfo);
        ImagePlusData inputReference = dataBatch.getInputData("Reference", ImagePlusData.class, progressInfo);

        ROIListData result = new ROIListData();
        ExpressionVariables variables = new ExpressionVariables();
        ROIListData tmp = new ROIListData();

        if (includeAnnotations) {
            for (JIPipeTextAnnotation value : dataBatch.getMergedTextAnnotations().values()) {
                variables.set(value.getName(), value.getValue());
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
        for (Roi roi : inputRois) {
            tmp.clear();
            tmp.add(roi);

            ResultsTableData measured = tmp.measure(referenceImage, measurements, true, measureInPhysicalUnits);
            for (int col = 0; col < measured.getColumnCount(); col++) {
                variables.set(measured.getColumnName(col), measured.getValueAt(0, col));
            }

            // Make metadata accessible
            Map<String, String> roiProperties = ImageJUtils.getRoiProperties(roi);
            variables.set("metadata", roiProperties);
            for (Map.Entry<String, String> entry : roiProperties.entrySet()) {
                variables.set("metadata." + entry.getKey(), entry.getValue());
            }

            String newName = StringUtils.nullToEmpty(expression.evaluate(variables));
            Roi copy = (Roi) roi.clone();
            copy.setName(newName);
            result.add(copy);
        }

        dataBatch.addOutputData(getFirstOutputSlot(), result, progressInfo);
    }

    @JIPipeDocumentation(name = "Expression", description = "The expression is executed per ROI.")
    @ExpressionParameterSettings(variableSource = VariableSource.class)
    @JIPipeParameter("expression")
    @ExpressionParameterSettingsVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @ExpressionParameterSettingsVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
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
    @JIPipeParameter(value = "measurements", important = true)
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

    public static class VariableSource implements ExpressionParameterVariableSource {

        public static final Set<ExpressionParameterVariable> VARIABLES;

        static {
            VARIABLES = new HashSet<>();
            VARIABLES.add(ExpressionParameterVariable.ANNOTATIONS_VARIABLE);
            VARIABLES.add(new ExpressionParameterVariable("Name", "Current name of the ROI", "Name"));
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