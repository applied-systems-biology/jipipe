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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.modify;

import ij.ImagePlus;
import ij.gui.Roi;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.*;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.MeasurementColumn;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SetJIPipeDocumentation(name = "Change 2D ROI name from measurements (expression)", description = "Utilizes an expression to generate a ROI name for each individual ROI in the supplied ROI lists." +
        "The expression has access to annotations and statistics.")
@AddJIPipeInputSlot(value = ROIListData.class, name = "ROI", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Reference", create = true, optional = true)
@AddJIPipeOutputSlot(value = ROIListData.class, name = "ROI", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Modify")
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {

        ROIListData inputRois = iterationStep.getInputData("ROI", ROIListData.class, progressInfo);
        ImagePlusData inputReference = iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo);

        ROIListData result = new ROIListData();
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        ROIListData tmp = new ROIListData();

        if (includeAnnotations) {
            for (JIPipeTextAnnotation value : iterationStep.getMergedTextAnnotations().values()) {
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

        iterationStep.addOutputData(getFirstOutputSlot(), result, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Expression", description = "The expression is executed per ROI.")
    @JIPipeExpressionParameterSettings(variableSource = VariablesInfo.class)
    @JIPipeParameter("expression")
    @AddJIPipeExpressionParameterVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @AddJIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
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

    public static class VariablesInfo implements ExpressionParameterVariablesInfo {

        public static final Set<JIPipeExpressionParameterVariableInfo> VARIABLES;

        static {
            VARIABLES = new HashSet<>();
            VARIABLES.add(JIPipeExpressionParameterVariableInfo.ANNOTATIONS_VARIABLE);
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("Name", "Name", "Current name of the ROI"));
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