package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi;

import ij.ImagePlus;
import ij.gui.Roi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.MeasurementColumn;
import org.hkijena.jipipe.extensions.parameters.expressions.*;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@JIPipeDocumentation(name = "Set ROI name by expression", description = "Utilizes an expression to generate a ROI name for each individual ROI in the supplied ROI lists." +
        "The expression has access to annotations and statistics.")
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference")
@JIPipeOutputSlot(value = ROIListData.class, slotName = "ROI")
@JIPipeOrganization(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Modify")
public class GenerateROINameAlgorithm extends ImageRoiProcessorAlgorithm {

    private StringQueryExpression expression = new StringQueryExpression();
    private boolean includeAnnotations = true;
    private ImageStatisticsSetParameter measurements = new ImageStatisticsSetParameter();

    public GenerateROINameAlgorithm(JIPipeNodeInfo info) {
        super(info, ROIListData.class, "ROI");
    }

    public GenerateROINameAlgorithm(GenerateROINameAlgorithm other) {
        super(other);
        this.expression = new StringQueryExpression(other.expression);
        this.includeAnnotations = other.includeAnnotations;
        this.measurements = new ImageStatisticsSetParameter(other.measurements);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROIListData result = new ROIListData();
        ExpressionParameters parameters = new ExpressionParameters();
        ROIListData tmp = new ROIListData();

        if (includeAnnotations) {
            for (JIPipeAnnotation value : dataBatch.getAnnotations().values()) {
                parameters.set(value.getName(), value.getValue());
            }
        }

        Map<ImagePlusData, ROIListData> groupedByReference = getReferenceImage(dataBatch, progressInfo);
        for (Map.Entry<ImagePlusData, ROIListData> referenceEntry : groupedByReference.entrySet()) {
            ImagePlus referenceImage = null;
            if (referenceEntry.getKey() != null) {
                referenceImage = referenceEntry.getKey().getImage();
            }
            for (Roi roi : referenceEntry.getValue()) {
                tmp.clear();
                tmp.add(roi);

                ResultsTableData measured = tmp.measure(referenceImage, measurements, true);
                for (int col = 0; col < measured.getColumnCount(); col++) {
                    parameters.set(measured.getColumnName(col), measured.getValueAt(0, col) + "");
                }

                String newName = StringUtils.nullToEmpty(expression.evaluate(parameters));
                Roi copy = (Roi) roi.clone();
                copy.setName(newName);
                result.add(copy);
            }
        }

        dataBatch.addOutputData(getFirstOutputSlot(), result, progressInfo);
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

    public static class VariableSource implements ExpressionParameterVariableSource {

        public static final Set<ExpressionParameterVariable> VARIABLES;

        static {
            VARIABLES = new HashSet<>();
            VARIABLES.add(new ExpressionParameterVariable("<Annotations>",
                    "Annotations of the source ROI list are available (use Update Cache to find the list of annotations)",
                    ""));
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