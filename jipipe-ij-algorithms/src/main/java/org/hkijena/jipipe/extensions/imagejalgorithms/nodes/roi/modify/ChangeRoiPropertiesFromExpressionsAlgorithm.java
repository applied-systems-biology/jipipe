/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi.modify;

import ij.gui.Roi;
import ij.plugin.RoiScaler;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi.measure.RoiStatisticsAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.MeasurementExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Change ROI properties from expressions", description = "Sets properties of all ROI to values extracted from expressions.")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Modify")
@JIPipeInputSlot(value = ROIListData.class, slotName = "Input", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", autoCreate = true, optional = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output", autoCreate = true)
public class ChangeRoiPropertiesFromExpressionsAlgorithm extends JIPipeIteratingAlgorithm {

    private final RoiStatisticsAlgorithm roiStatisticsAlgorithm =
            JIPipe.createNode(RoiStatisticsAlgorithm.class);
    private final CustomExpressionVariablesParameter customFilterVariables;
    private OptionalJIPipeExpressionParameter roiName = new OptionalJIPipeExpressionParameter(false, "roi_name");
    private OptionalJIPipeExpressionParameter positionX = new OptionalJIPipeExpressionParameter(false, "x");
    private OptionalJIPipeExpressionParameter positionY = new OptionalJIPipeExpressionParameter(false, "y");
    private OptionalJIPipeExpressionParameter positionZ = new OptionalJIPipeExpressionParameter(false, "z");
    private OptionalJIPipeExpressionParameter positionC = new OptionalJIPipeExpressionParameter(false, "c");
    private OptionalJIPipeExpressionParameter positionT = new OptionalJIPipeExpressionParameter(false, "t");
    private OptionalJIPipeExpressionParameter fillColor = new OptionalJIPipeExpressionParameter(false, "fill_color");
    private OptionalJIPipeExpressionParameter lineColor = new OptionalJIPipeExpressionParameter(false, "line_color");
    private OptionalJIPipeExpressionParameter lineWidth = new OptionalJIPipeExpressionParameter(false, "line_width");
    private OptionalJIPipeExpressionParameter scaleX = new OptionalJIPipeExpressionParameter(false, "1.0");
    private OptionalJIPipeExpressionParameter scaleY = new OptionalJIPipeExpressionParameter(false, "1.0");
    private OptionalJIPipeExpressionParameter centerScale = new OptionalJIPipeExpressionParameter(false, "false");
    private ImageStatisticsSetParameter measurements = new ImageStatisticsSetParameter();
    private boolean measureInPhysicalUnits = true;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ChangeRoiPropertiesFromExpressionsAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.customFilterVariables = new CustomExpressionVariablesParameter(this);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public ChangeRoiPropertiesFromExpressionsAlgorithm(ChangeRoiPropertiesFromExpressionsAlgorithm other) {
        super(other);
        this.positionX = new OptionalJIPipeExpressionParameter(other.positionX);
        this.positionY = new OptionalJIPipeExpressionParameter(other.positionY);
        this.positionZ = new OptionalJIPipeExpressionParameter(other.positionZ);
        this.positionC = new OptionalJIPipeExpressionParameter(other.positionC);
        this.positionT = new OptionalJIPipeExpressionParameter(other.positionT);
        this.fillColor = new OptionalJIPipeExpressionParameter(other.fillColor);
        this.lineColor = new OptionalJIPipeExpressionParameter(other.lineColor);
        this.lineWidth = new OptionalJIPipeExpressionParameter(other.lineWidth);
        this.roiName = new OptionalJIPipeExpressionParameter(other.roiName);
        this.scaleX = new OptionalJIPipeExpressionParameter(other.scaleX);
        this.scaleY = new OptionalJIPipeExpressionParameter(other.scaleY);
        this.centerScale = new OptionalJIPipeExpressionParameter(other.centerScale);
        this.measurements = new ImageStatisticsSetParameter(other.measurements);
        this.customFilterVariables = new CustomExpressionVariablesParameter(other.customFilterVariables, this);
        this.measureInPhysicalUnits = other.measureInPhysicalUnits;
    }

    @Override
    public void run(JIPipeProgressInfo progressInfo) {
        // Set parameters of ROI statistics algorithm
        roiStatisticsAlgorithm.setMeasurements(measurements);
        roiStatisticsAlgorithm.setMeasureInPhysicalUnits(measureInPhysicalUnits);

        // Continue with run
        super.run(progressInfo);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        ROIListData inputRois = (ROIListData) iterationStep.getInputData("Input", ROIListData.class, progressInfo).duplicate(progressInfo);
        ImagePlusData inputReference = iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo);

        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        customFilterVariables.writeToVariables(variables, true, "custom.", true, "custom");

        // Obtain statistics
        roiStatisticsAlgorithm.clearSlotData();
        roiStatisticsAlgorithm.getInputSlot("ROI").addData(inputRois, progressInfo);
        if (inputReference != null) {
            roiStatisticsAlgorithm.getInputSlot("Reference").addData(inputReference, progressInfo);
        }
        roiStatisticsAlgorithm.run(progressInfo);
        ResultsTableData statistics = roiStatisticsAlgorithm.getFirstOutputSlot().getData(0, ResultsTableData.class, progressInfo);
        roiStatisticsAlgorithm.clearSlotData();

        for (int i = 0; i < inputRois.size(); i++) {
            Roi roi = inputRois.get(i);
            double x;
            double y;
            int z;
            int c;
            int t;
            double scaleX = 1.0;
            double scaleY = 1.0;
            boolean centerScale = false;
            x = roi.getXBase();
            y = roi.getYBase();
            z = roi.getZPosition();
            c = roi.getCPosition();
            t = roi.getTPosition();

            // Make metadata accessible
            Map<String, String> roiProperties = ImageJUtils.getRoiProperties(roi);
            variables.set("metadata", roiProperties);
            for (Map.Entry<String, String> entry : roiProperties.entrySet()) {
                variables.set("metadata." + entry.getKey(), entry.getValue());
            }

            for (int col = 0; col < statistics.getColumnCount(); col++) {
                variables.set(statistics.getColumnName(col), statistics.getValueAt(i, col));
            }

            variables.set("index", i);
            variables.set("num_roi", inputRois.size());
            variables.set("x", x);
            variables.set("y", y);
            variables.set("z", z);
            variables.set("c", c);
            variables.set("t", t);
            variables.set("fill_color", roi.getFillColor() != null ? ColorUtils.colorToHexString(roi.getFillColor()) : null);
            variables.set("line_color", roi.getStrokeColor() != null ? ColorUtils.colorToHexString(roi.getStrokeColor()) : null);
            variables.set("line_width", roi.getStrokeWidth());
            variables.set("name", roi.getName());

            if (positionX.isEnabled())
                x = positionX.getContent().evaluateToNumber(variables);
            if (positionY.isEnabled())
                y = positionY.getContent().evaluateToNumber(variables);
            if (positionZ.isEnabled())
                z = (int) positionZ.getContent().evaluateToNumber(variables);
            if (positionC.isEnabled())
                c = (int) positionC.getContent().evaluateToNumber(variables);
            if (positionT.isEnabled())
                t = (int) positionT.getContent().evaluateToNumber(variables);
            roi.setPosition(c, z, t);
            roi.setLocation(x, y);

            if (fillColor.isEnabled())
                roi.setFillColor(fillColor.getContent().evaluateToColor(variables));
            if (lineColor.isEnabled())
                roi.setStrokeColor(lineColor.getContent().evaluateToColor(variables));
            if (lineWidth.isEnabled())
                roi.setStrokeWidth(lineWidth.getContent().evaluateToNumber(variables));
            if (roiName.isEnabled())
                roi.setName(StringUtils.nullToEmpty(roiName.getContent().evaluateToString(variables)));

            if (this.scaleX.isEnabled()) {
                scaleX = this.scaleX.getContent().evaluateToNumber(variables);
            }
            if (this.scaleY.isEnabled()) {
                scaleY = this.scaleY.getContent().evaluateToNumber(variables);
            }
            if (this.centerScale.isEnabled()) {
                centerScale = (boolean) this.centerScale.getContent().evaluate(variables);
            }
            if (scaleX != 1.0 || scaleY != 1.0) {
                roi = RoiScaler.scale(roi, scaleX, scaleY, centerScale);
                inputRois.set(i, roi);
            }
        }

        iterationStep.addOutputData(getFirstOutputSlot(), inputRois, progressInfo);
    }

    @JIPipeDocumentation(name = "Location (X)", description = "The X location. The annotation value is converted to an integer.")
    @JIPipeParameter("position-x")
    @ExpressionParameterSettings(variableSource = VariableSource.class, hint = "per ROI")
    @ExpressionParameterSettingsVariable(fromClass = MeasurementExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @ExpressionParameterSettingsVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @ExpressionParameterSettingsVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public OptionalJIPipeExpressionParameter getPositionX() {
        return positionX;
    }

    @JIPipeParameter("position-x")
    public void setPositionX(OptionalJIPipeExpressionParameter positionX) {
        this.positionX = positionX;
    }

    @JIPipeDocumentation(name = "Location (Y)", description = "The Y location. The annotation value is converted to an integer.")
    @JIPipeParameter("position-y")
    @ExpressionParameterSettings(variableSource = VariableSource.class, hint = "per ROI")
    @ExpressionParameterSettingsVariable(fromClass = MeasurementExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @ExpressionParameterSettingsVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @ExpressionParameterSettingsVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public OptionalJIPipeExpressionParameter getPositionY() {
        return positionY;
    }

    @JIPipeParameter("position-y")
    public void setPositionY(OptionalJIPipeExpressionParameter positionY) {
        this.positionY = positionY;
    }

    @JIPipeDocumentation(name = "Slice position (Z)", description = "Allows to relocate the ROI to a different Z-position. " +
            "The first index is 1. If set to zero, the ROI is located on all slices. The annotation value is converted to an integer.")
    @JIPipeParameter("position-z")
    @ExpressionParameterSettings(variableSource = VariableSource.class, hint = "per ROI")
    @ExpressionParameterSettingsVariable(fromClass = MeasurementExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @ExpressionParameterSettingsVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @ExpressionParameterSettingsVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public OptionalJIPipeExpressionParameter getPositionZ() {
        return positionZ;
    }

    @JIPipeParameter("position-z")
    public void setPositionZ(OptionalJIPipeExpressionParameter positionZ) {
        this.positionZ = positionZ;
    }

    @JIPipeDocumentation(name = "Slice position (Channel)", description = "Allows to relocate the ROI to a different channel-position. Please note " +
            "that 'Channel' refers to an image slice and not to a pixel channel. " +
            "The first index is 1. If set to zero, the ROI is located on all channels. The annotation value is converted to an integer.")
    @JIPipeParameter("position-c")
    @ExpressionParameterSettings(variableSource = VariableSource.class, hint = "per ROI")
    @ExpressionParameterSettingsVariable(fromClass = MeasurementExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @ExpressionParameterSettingsVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @ExpressionParameterSettingsVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public OptionalJIPipeExpressionParameter getPositionC() {
        return positionC;
    }

    @JIPipeParameter("position-c")
    public void setPositionC(OptionalJIPipeExpressionParameter positionC) {
        this.positionC = positionC;
    }

    @JIPipeDocumentation(name = "Slice position (Frame)", description = "Allows to relocate the ROI to a different frame/time-position. " +
            "The first index is 1. If set to zero, the ROI is located on all frames. The annotation value is converted to an integer.")
    @JIPipeParameter("position-t")
    @ExpressionParameterSettings(variableSource = VariableSource.class, hint = "per ROI")
    @ExpressionParameterSettingsVariable(fromClass = MeasurementExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @ExpressionParameterSettingsVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @ExpressionParameterSettingsVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public OptionalJIPipeExpressionParameter getPositionT() {
        return positionT;
    }

    @JIPipeParameter("position-t")
    public void setPositionT(OptionalJIPipeExpressionParameter positionT) {
        this.positionT = positionT;
    }

    @JIPipeDocumentation(name = "Fill color", description = "Allows to change the fill color when rendered as RGB and within ImageJ. " + ColorUtils.PARSE_COLOR_DESCRIPTION)
    @JIPipeParameter("fill-color")
    @ExpressionParameterSettings(variableSource = VariableSource.class, hint = "per ROI")
    @ExpressionParameterSettingsVariable(fromClass = MeasurementExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @ExpressionParameterSettingsVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @ExpressionParameterSettingsVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public OptionalJIPipeExpressionParameter getFillColor() {
        return fillColor;
    }

    @JIPipeParameter("fill-color")
    public void setFillColor(OptionalJIPipeExpressionParameter fillColor) {
        this.fillColor = fillColor;
    }

    @JIPipeDocumentation(name = "Line color", description = "Allows to change the line color when rendered as RGB and within ImageJ. " + ColorUtils.PARSE_COLOR_DESCRIPTION)
    @JIPipeParameter("line-color")
    @ExpressionParameterSettings(variableSource = VariableSource.class, hint = "per ROI")
    @ExpressionParameterSettingsVariable(fromClass = MeasurementExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @ExpressionParameterSettingsVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @ExpressionParameterSettingsVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public OptionalJIPipeExpressionParameter getLineColor() {
        return lineColor;
    }

    @JIPipeParameter("line-color")
    public void setLineColor(OptionalJIPipeExpressionParameter lineColor) {
        this.lineColor = lineColor;
    }

    @JIPipeDocumentation(name = "Line width", description = "Allows to change the line width when rendered as RGB and within ImageJ. The annotation value is converted to an integer.")
    @JIPipeParameter("line-width")
    @ExpressionParameterSettings(variableSource = VariableSource.class, hint = "per ROI")
    @ExpressionParameterSettingsVariable(fromClass = MeasurementExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @ExpressionParameterSettingsVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @ExpressionParameterSettingsVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public OptionalJIPipeExpressionParameter getLineWidth() {
        return lineWidth;
    }

    @JIPipeParameter("line-width")
    public void setLineWidth(OptionalJIPipeExpressionParameter lineWidth) {
        this.lineWidth = lineWidth;
    }

    @JIPipeDocumentation(name = "ROI name", description = "Allows to change the ROI name")
    @JIPipeParameter("roi-name")
    @ExpressionParameterSettings(variableSource = VariableSource.class, hint = "per ROI")
    @ExpressionParameterSettingsVariable(fromClass = MeasurementExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @ExpressionParameterSettingsVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @ExpressionParameterSettingsVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public OptionalJIPipeExpressionParameter getRoiName() {
        return roiName;
    }

    @JIPipeParameter("roi-name")
    public void setRoiName(OptionalJIPipeExpressionParameter roiName) {
        this.roiName = roiName;
    }

    @JIPipeDocumentation(name = "Scale X", description = "Scales the ROI. Please note that the scale will not be saved inside the ROI. Must evaluate to a number.")
    @JIPipeParameter("scale-x")
    @ExpressionParameterSettings(variableSource = VariableSource.class, hint = "per ROI")
    @ExpressionParameterSettingsVariable(fromClass = MeasurementExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @ExpressionParameterSettingsVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @ExpressionParameterSettingsVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public OptionalJIPipeExpressionParameter getScaleX() {
        return scaleX;
    }

    @JIPipeParameter("scale-x")
    public void setScaleX(OptionalJIPipeExpressionParameter scaleX) {
        this.scaleX = scaleX;
    }

    @JIPipeDocumentation(name = "Scale Y", description = "Scales the ROI. Please note that the scale will not be saved inside the ROI. Must evaluate to a number.")
    @JIPipeParameter("scale-y")
    @ExpressionParameterSettings(variableSource = VariableSource.class, hint = "per ROI")
    @ExpressionParameterSettingsVariable(fromClass = MeasurementExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @ExpressionParameterSettingsVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @ExpressionParameterSettingsVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public OptionalJIPipeExpressionParameter getScaleY() {
        return scaleY;
    }

    @JIPipeParameter("scale-y")
    public void setScaleY(OptionalJIPipeExpressionParameter scaleY) {
        this.scaleY = scaleY;
    }

    @JIPipeDocumentation(name = "Center scale", description = "If true, each ROI is scaled relative to its center. Must evaluate to a boolean.")
    @JIPipeParameter("center-scale")
    @ExpressionParameterSettings(variableSource = VariableSource.class, hint = "per ROI")
    @ExpressionParameterSettingsVariable(fromClass = MeasurementExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @ExpressionParameterSettingsVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @ExpressionParameterSettingsVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public OptionalJIPipeExpressionParameter getCenterScale() {
        return centerScale;
    }

    @JIPipeParameter("center-scale")
    public void setCenterScale(OptionalJIPipeExpressionParameter centerScale) {
        this.centerScale = centerScale;
    }

    @JIPipeDocumentation(name = "Measurements", description = "The measurements to calculate.")
    @JIPipeParameter(value = "measurements", important = true)
    public ImageStatisticsSetParameter getMeasurements() {
        return measurements;
    }

    @JIPipeParameter("measurements")
    public void setMeasurements(ImageStatisticsSetParameter measurements) {
        this.measurements = measurements;
    }

    @JIPipeDocumentation(name = "Custom expression variables", description = "Here you can add parameters that will be included into the expression as variables <code>custom.[key]</code>. Alternatively, you can access them via <code>GET_ITEM(custom, \"[key]\")</code>.")
    @JIPipeParameter(value = "custom-filter-variables", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/insert-math-expression.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/insert-math-expression.png", persistence = JIPipeParameterPersistence.NestedCollection)
    public CustomExpressionVariablesParameter getCustomFilterVariables() {
        return customFilterVariables;
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

        private static final Set<ExpressionParameterVariable> VARIABLES = new HashSet<>();

        static {
            VARIABLES.add(new ExpressionParameterVariable("Fill color", "The fill color of the ROI", "fill_color"));
            VARIABLES.add(new ExpressionParameterVariable("Line color", "The line color of the ROI", "line_color"));
            VARIABLES.add(new ExpressionParameterVariable("Line width", "The line width", "line_width"));
            VARIABLES.add(new ExpressionParameterVariable("Channel location", "The channel (C) location. The first index is 1. Zero indicates that that ROI applies to all locations.", "c"));
            VARIABLES.add(new ExpressionParameterVariable("Slice location", "The slice (Z) location. The first index is 1. Zero indicates that that ROI applies to all locations.", "z"));
            VARIABLES.add(new ExpressionParameterVariable("Frame location", "The frame (T) location. The first index is 1. Zero indicates that that ROI applies to all locations.", "t"));
            VARIABLES.add(new ExpressionParameterVariable("X Location", "The X location of the ROI", "x"));
            VARIABLES.add(new ExpressionParameterVariable("Y Location", "The Y location of the ROI", "y"));
            VARIABLES.add(new ExpressionParameterVariable("Name", "The ROI name", "name"));
            VARIABLES.add(new ExpressionParameterVariable("Index", "The index of the ROI", "index"));
            VARIABLES.add(new ExpressionParameterVariable("Number of ROI", "The number of ROI in the list", "num_roi"));
        }

        @Override
        public Set<ExpressionParameterVariable> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
            return VARIABLES;
        }
    }
}
