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
import ij.plugin.RoiRotator;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.*;
import org.hkijena.jipipe.plugins.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.measure.RoiStatisticsAlgorithm;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.Image5DExpressionParameterVariablesInfo2;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.MeasurementExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.ColorUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SetJIPipeDocumentation(name = "Transform 2D ROI", description = "Applies rotation, scaling, and translation of 2D ROI.")
@AddJIPipeNodeAlias(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Transform", aliasName = "Rotate 2D ROI")
@AddJIPipeNodeAlias(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Transform", aliasName = "Translate 2D ROI")
@AddJIPipeNodeAlias(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Transform", aliasName = "Scale 2D ROI")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Modify")
@AddJIPipeInputSlot(value = ROI2DListData.class, name = "Input", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Reference", create = true, optional = true)
@AddJIPipeOutputSlot(value = ROI2DListData.class, name = "Output", create = true)
public class TransformRoiFromExpressionsAlgorithm extends JIPipeIteratingAlgorithm {

    private final RoiStatisticsAlgorithm roiStatisticsAlgorithm =
            JIPipe.createNode(RoiStatisticsAlgorithm.class);
    private OptionalJIPipeExpressionParameter positionX = new OptionalJIPipeExpressionParameter(false, "x");
    private OptionalJIPipeExpressionParameter positionY = new OptionalJIPipeExpressionParameter(false, "y");
    private OptionalJIPipeExpressionParameter scaleX = new OptionalJIPipeExpressionParameter(false, "1.0");
    private OptionalJIPipeExpressionParameter scaleY = new OptionalJIPipeExpressionParameter(false, "1.0");
    private OptionalJIPipeExpressionParameter angle = new OptionalJIPipeExpressionParameter(false, "0");
    private JIPipeExpressionParameter originX = new JIPipeExpressionParameter("x + Width / 2");
    private JIPipeExpressionParameter originY = new JIPipeExpressionParameter("y + Height / 2");
    private ImageStatisticsSetParameter measurements = new ImageStatisticsSetParameter();
    private boolean measureInPhysicalUnits = true;

    public TransformRoiFromExpressionsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public TransformRoiFromExpressionsAlgorithm(TransformRoiFromExpressionsAlgorithm other) {
        super(other);
        this.positionX = new OptionalJIPipeExpressionParameter(other.positionX);
        this.positionY = new OptionalJIPipeExpressionParameter(other.positionY);
        this.scaleX = new OptionalJIPipeExpressionParameter(other.scaleX);
        this.scaleY = new OptionalJIPipeExpressionParameter(other.scaleY);
        this.measurements = new ImageStatisticsSetParameter(other.measurements);
        this.angle = new OptionalJIPipeExpressionParameter(other.angle);
        this.originX = new JIPipeExpressionParameter(other.originX);
        this.originY = new JIPipeExpressionParameter(other.originY);
        this.measureInPhysicalUnits = other.measureInPhysicalUnits;
    }

    @Override
    public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        // Set parameters of ROI statistics algorithm
        roiStatisticsAlgorithm.setMeasurements(measurements);
        roiStatisticsAlgorithm.setMeasureInPhysicalUnits(measureInPhysicalUnits);

        // Continue with run
        super.run(runContext, progressInfo);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROI2DListData inputRois = (ROI2DListData) iterationStep.getInputData("Input", ROI2DListData.class, progressInfo).duplicate(progressInfo);
        ImagePlusData inputReference = iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo);

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap(iterationStep);

        // Obtain statistics
        roiStatisticsAlgorithm.clearSlotData(false, progressInfo);
        roiStatisticsAlgorithm.getInputSlot("ROI").addData(inputRois, progressInfo);
        if (inputReference != null) {
            roiStatisticsAlgorithm.getInputSlot("Reference").addData(inputReference, progressInfo);
        }
        roiStatisticsAlgorithm.run(runContext, progressInfo);
        ResultsTableData statistics = roiStatisticsAlgorithm.getFirstOutputSlot().getData(0, ResultsTableData.class, progressInfo);
        roiStatisticsAlgorithm.clearSlotData(false, progressInfo);

        // Add image info
        ImagePlus referenceImg;
        if(inputReference != null) {
            referenceImg = inputReference.getImage();
        }
        else {
            referenceImg = inputRois.createDummyImage();
        }

        Image5DExpressionParameterVariablesInfo2.writeToVariables(referenceImg, variables);

        for (int i = 0; i < inputRois.size(); i++) {
            Roi roi = inputRois.get(i);
            double x;
            double y;
            int z;
            int c;
            int t;
            double scaleX = 1.0;
            double scaleY = 1.0;
            double angle = 0;
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

            double centerX = this.originX.evaluateToDouble(variables);
            double centerY = this.originY.evaluateToDouble(variables);

            if (positionX.isEnabled())
                x = positionX.getContent().evaluateToNumber(variables);
            if (positionY.isEnabled())
                y = positionY.getContent().evaluateToNumber(variables);
            roi.setLocation(x, y);

            if (this.scaleX.isEnabled()) {
                scaleX = this.scaleX.getContent().evaluateToNumber(variables);
            }
            if (this.scaleY.isEnabled()) {
                scaleY = this.scaleY.getContent().evaluateToNumber(variables);
            }
            if(this.angle.isEnabled()) {
                angle = this.angle.getContent().evaluateToNumber(variables);
            }
            if(angle != 0) {
                roi = RoiRotator.rotate(roi, angle, centerX, centerY);
                inputRois.set(i, roi);
            }
            if (scaleX != 1.0 || scaleY != 1.0) {
                roi = ImageJUtils.scaleRoiAroundOrigin(roi, scaleX, scaleY, centerX, centerY);
                inputRois.set(i, roi);
            }

        }

        iterationStep.addOutputData(getFirstOutputSlot(), inputRois, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Origin (Y)", description = "Origin (Y) location for rotation and scaling")
    @JIPipeParameter("origin-y")
    @JIPipeExpressionParameterSettings(variableSource = VariablesInfo.class, hint = "per ROI")
    @AddJIPipeExpressionParameterVariable(fromClass = Image5DExpressionParameterVariablesInfo2.class)
    @AddJIPipeExpressionParameterVariable(fromClass = MeasurementExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @AddJIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public JIPipeExpressionParameter getOriginY() {
        return originY;
    }

    @JIPipeParameter("origin-y")
    public void setOriginY(JIPipeExpressionParameter originY) {
        this.originY = originY;
    }

    @SetJIPipeDocumentation(name = "Origin (X)", description = "Origin (X) location for rotation and scaling")
    @JIPipeParameter("origin-x")
    @JIPipeExpressionParameterSettings(variableSource = VariablesInfo.class, hint = "per ROI")
    @AddJIPipeExpressionParameterVariable(fromClass = Image5DExpressionParameterVariablesInfo2.class)
    @AddJIPipeExpressionParameterVariable(fromClass = MeasurementExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @AddJIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public JIPipeExpressionParameter getOriginX() {
        return originX;
    }

    @JIPipeParameter("origin-x")
    public void setOriginX(JIPipeExpressionParameter originX) {
        this.originX = originX;
    }

    @SetJIPipeDocumentation(name = "Angle (°)", description = "Rotation angle in degree")
    @JIPipeParameter("angle")
    @JIPipeExpressionParameterSettings(variableSource = VariablesInfo.class, hint = "per ROI")
    @AddJIPipeExpressionParameterVariable(fromClass = Image5DExpressionParameterVariablesInfo2.class)
    @AddJIPipeExpressionParameterVariable(fromClass = MeasurementExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @AddJIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public OptionalJIPipeExpressionParameter getAngle() {
        return angle;
    }

    @JIPipeParameter("angle")
    public void setAngle(OptionalJIPipeExpressionParameter angle) {
        this.angle = angle;
    }

    @SetJIPipeDocumentation(name = "Location (X)", description = "The X location. The annotation value is converted to an integer.")
    @JIPipeParameter("position-x")
    @JIPipeExpressionParameterSettings(variableSource = VariablesInfo.class, hint = "per ROI")
    @AddJIPipeExpressionParameterVariable(fromClass = Image5DExpressionParameterVariablesInfo2.class)
    @AddJIPipeExpressionParameterVariable(fromClass = MeasurementExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @AddJIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public OptionalJIPipeExpressionParameter getPositionX() {
        return positionX;
    }

    @JIPipeParameter("position-x")
    public void setPositionX(OptionalJIPipeExpressionParameter positionX) {
        this.positionX = positionX;
    }

    @SetJIPipeDocumentation(name = "Location (Y)", description = "The Y location. The annotation value is converted to an integer.")
    @JIPipeParameter("position-y")
    @JIPipeExpressionParameterSettings(variableSource = VariablesInfo.class, hint = "per ROI")
    @AddJIPipeExpressionParameterVariable(fromClass = Image5DExpressionParameterVariablesInfo2.class)
    @AddJIPipeExpressionParameterVariable(fromClass = MeasurementExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @AddJIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public OptionalJIPipeExpressionParameter getPositionY() {
        return positionY;
    }

    @JIPipeParameter("position-y")
    public void setPositionY(OptionalJIPipeExpressionParameter positionY) {
        this.positionY = positionY;
    }

    @SetJIPipeDocumentation(name = "Scale X", description = "Scales the ROI. Please note that the scale will not be saved inside the ROI. Must evaluate to a number.")
    @JIPipeParameter("scale-x")
    @JIPipeExpressionParameterSettings(variableSource = VariablesInfo.class, hint = "per ROI")
    @AddJIPipeExpressionParameterVariable(fromClass = Image5DExpressionParameterVariablesInfo2.class)
    @AddJIPipeExpressionParameterVariable(fromClass = MeasurementExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @AddJIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public OptionalJIPipeExpressionParameter getScaleX() {
        return scaleX;
    }

    @JIPipeParameter("scale-x")
    public void setScaleX(OptionalJIPipeExpressionParameter scaleX) {
        this.scaleX = scaleX;
    }

    @SetJIPipeDocumentation(name = "Scale Y", description = "Scales the ROI. Please note that the scale will not be saved inside the ROI. Must evaluate to a number.")
    @JIPipeParameter("scale-y")
    @JIPipeExpressionParameterSettings(variableSource = VariablesInfo.class, hint = "per ROI")
    @AddJIPipeExpressionParameterVariable(fromClass = Image5DExpressionParameterVariablesInfo2.class)
    @AddJIPipeExpressionParameterVariable(fromClass = MeasurementExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @AddJIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public OptionalJIPipeExpressionParameter getScaleY() {
        return scaleY;
    }

    @JIPipeParameter("scale-y")
    public void setScaleY(OptionalJIPipeExpressionParameter scaleY) {
        this.scaleY = scaleY;
    }

    @SetJIPipeDocumentation(name = "Measurements", description = "The measurements to calculate." + "<br/><br/>" + ImageStatisticsSetParameter.ALL_DESCRIPTIONS)
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

    public static class VariablesInfo implements JIPipeExpressionVariablesInfo {

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

        @Override
        public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
            return VARIABLES;
        }
    }
}
