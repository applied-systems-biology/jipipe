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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi.modify;

import ij.gui.Roi;
import ij.plugin.RoiScaler;
import net.imagej.ops.Ops;
import org.apache.commons.lang3.math.NumberUtils;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalDoubleParameter;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Change ROI properties from expressions", description = "Sets properties of all ROI to values extracted from expressions.")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Modify")
@JIPipeInputSlot(value = ROIListData.class, slotName = "Input")
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output")
public class ChangeRoiPropertiesFromExpressionsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OptionalDefaultExpressionParameter roiName = new OptionalDefaultExpressionParameter(false,"roi_name");
    private OptionalDefaultExpressionParameter positionX = new OptionalDefaultExpressionParameter(false, "x");
    private OptionalDefaultExpressionParameter positionY = new OptionalDefaultExpressionParameter(false, "y");
    private OptionalDefaultExpressionParameter positionZ = new OptionalDefaultExpressionParameter(false, "z");
    private OptionalDefaultExpressionParameter positionC = new OptionalDefaultExpressionParameter(false, "c");
    private OptionalDefaultExpressionParameter positionT = new OptionalDefaultExpressionParameter(false, "t");
    private OptionalDefaultExpressionParameter fillColor = new OptionalDefaultExpressionParameter(false, "fill_color");
    private OptionalDefaultExpressionParameter lineColor = new OptionalDefaultExpressionParameter(false, "line_color");
    private OptionalDefaultExpressionParameter lineWidth = new OptionalDefaultExpressionParameter(false, "line_width");
    private OptionalDefaultExpressionParameter scaleX = new OptionalDefaultExpressionParameter(false, "1.0");
    private OptionalDefaultExpressionParameter scaleY = new OptionalDefaultExpressionParameter(false, "1.0");
    private OptionalDefaultExpressionParameter centerScale = new OptionalDefaultExpressionParameter(false, "false");

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ChangeRoiPropertiesFromExpressionsAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", "", ROIListData.class)
                .addOutputSlot("Output", "", ROIListData.class, null)
                .seal()
                .build());
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public ChangeRoiPropertiesFromExpressionsAlgorithm(ChangeRoiPropertiesFromExpressionsAlgorithm other) {
        super(other);
        this.positionX = new OptionalDefaultExpressionParameter(other.positionX);
        this.positionY = new OptionalDefaultExpressionParameter(other.positionY);
        this.positionZ = new OptionalDefaultExpressionParameter(other.positionZ);
        this.positionC = new OptionalDefaultExpressionParameter(other.positionC);
        this.positionT = new OptionalDefaultExpressionParameter(other.positionT);
        this.fillColor = new OptionalDefaultExpressionParameter(other.fillColor);
        this.lineColor = new OptionalDefaultExpressionParameter(other.lineColor);
        this.lineWidth = new OptionalDefaultExpressionParameter(other.lineWidth);
        this.roiName = new OptionalDefaultExpressionParameter(other.roiName);
        this.scaleX = new OptionalDefaultExpressionParameter(other.scaleX);
        this.scaleY = new OptionalDefaultExpressionParameter(other.scaleY);
        this.centerScale = new OptionalDefaultExpressionParameter(other.centerScale);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROIListData data = (ROIListData) dataBatch.getInputData(getFirstInputSlot(), ROIListData.class, progressInfo).duplicate(progressInfo);

        org.hkijena.jipipe.extensions.expressions.ExpressionVariables variables = new org.hkijena.jipipe.extensions.expressions.ExpressionVariables();
        for (JIPipeTextAnnotation annotation : dataBatch.getMergedTextAnnotations().values()) {
            variables.set(annotation.getName(), annotation.getValue());
        }

        for (int i = 0; i < data.size(); i++) {
            Roi roi = data.get(i);
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
                roi.setFillColor(parseColor(fillColor.getContent().evaluate(variables)));
            if (lineColor.isEnabled())
                roi.setStrokeColor(parseColor(lineColor.getContent().evaluate(variables)));
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
                data.set(i, roi);
            }
        }

        dataBatch.addOutputData(getFirstOutputSlot(), data, progressInfo);
    }

    private Color parseColor(Object o) {
        if(o instanceof Color) {
            return (Color) o;
        }
        else if(o instanceof String) {
            return ColorUtils.parseColor((String) o);
        }
        else if(o instanceof Collection) {
            Collection<?> collection = (Collection<?>) o;
            Iterator<?> iterator = collection.iterator();
            int red = ((Number)iterator.next()).intValue();
            int green = ((Number)iterator.next()).intValue();
            int blue = ((Number)iterator.next()).intValue();
            int alpha = 255;
            if(iterator.hasNext()) {
                alpha = ((Number)iterator.next()).intValue();
            }
            return new Color(red, green, blue, alpha);
        }
        else {
            return null;
        }
    }

    @JIPipeDocumentation(name = "Location (X)", description = "The X location. The annotation value is converted to an integer.")
    @JIPipeParameter("position-x")
    @ExpressionParameterSettings(variableSource = VariableSource.class)
    public OptionalDefaultExpressionParameter getPositionX() {
        return positionX;
    }

    @JIPipeParameter("position-x")
    public void setPositionX(OptionalDefaultExpressionParameter positionX) {
        this.positionX = positionX;
    }

    @JIPipeDocumentation(name = "Location (Y)", description = "The Y location. The annotation value is converted to an integer.")
    @JIPipeParameter("position-y")
    @ExpressionParameterSettings(variableSource = VariableSource.class)
    public OptionalDefaultExpressionParameter getPositionY() {
        return positionY;
    }

    @JIPipeParameter("position-y")
    public void setPositionY(OptionalDefaultExpressionParameter positionY) {
        this.positionY = positionY;
    }

    @JIPipeDocumentation(name = "Slice position (Z)", description = "Allows to relocate the ROI to a different Z-position. " +
            "The first index is 1. If set to zero, the ROI is located on all slices. The annotation value is converted to an integer.")
    @JIPipeParameter("position-z")
    @ExpressionParameterSettings(variableSource = VariableSource.class)
    public OptionalDefaultExpressionParameter getPositionZ() {
        return positionZ;
    }

    @JIPipeParameter("position-z")
    public void setPositionZ(OptionalDefaultExpressionParameter positionZ) {
        this.positionZ = positionZ;
    }

    @JIPipeDocumentation(name = "Slice position (Channel)", description = "Allows to relocate the ROI to a different channel-position. Please note " +
            "that 'Channel' refers to an image slice and not to a pixel channel. " +
            "The first index is 1. If set to zero, the ROI is located on all channels. The annotation value is converted to an integer.")
    @JIPipeParameter("position-c")
    @ExpressionParameterSettings(variableSource = VariableSource.class)
    public OptionalDefaultExpressionParameter getPositionC() {
        return positionC;
    }

    @JIPipeParameter("position-c")
    public void setPositionC(OptionalDefaultExpressionParameter positionC) {
        this.positionC = positionC;
    }

    @JIPipeDocumentation(name = "Slice position (Frame)", description = "Allows to relocate the ROI to a different frame/time-position. " +
            "The first index is 1. If set to zero, the ROI is located on all frames. The annotation value is converted to an integer.")
    @JIPipeParameter("position-t")
    @ExpressionParameterSettings(variableSource = VariableSource.class)
    public OptionalDefaultExpressionParameter getPositionT() {
        return positionT;
    }

    @JIPipeParameter("position-t")
    public void setPositionT(OptionalDefaultExpressionParameter positionT) {
        this.positionT = positionT;
    }

    @JIPipeDocumentation(name = "Fill color", description = "Allows to change the fill color when rendered as RGB and within ImageJ. " + ColorUtils.PARSE_COLOR_DESCRIPTION)
    @JIPipeParameter("fill-color")
    @ExpressionParameterSettings(variableSource = VariableSource.class)
    public OptionalDefaultExpressionParameter getFillColor() {
        return fillColor;
    }

    @JIPipeParameter("fill-color")
    public void setFillColor(OptionalDefaultExpressionParameter fillColor) {
        this.fillColor = fillColor;
    }

    @JIPipeDocumentation(name = "Line color", description = "Allows to change the line color when rendered as RGB and within ImageJ. " + ColorUtils.PARSE_COLOR_DESCRIPTION)
    @JIPipeParameter("line-color")
    @ExpressionParameterSettings(variableSource = VariableSource.class)
    public OptionalDefaultExpressionParameter getLineColor() {
        return lineColor;
    }

    @JIPipeParameter("line-color")
    public void setLineColor(OptionalDefaultExpressionParameter lineColor) {
        this.lineColor = lineColor;
    }

    @JIPipeDocumentation(name = "Line width", description = "Allows to change the line width when rendered as RGB and within ImageJ. The annotation value is converted to an integer.")
    @JIPipeParameter("line-width")
    @ExpressionParameterSettings(variableSource = VariableSource.class)
    public OptionalDefaultExpressionParameter getLineWidth() {
        return lineWidth;
    }

    @JIPipeParameter("line-width")
    public void setLineWidth(OptionalDefaultExpressionParameter lineWidth) {
        this.lineWidth = lineWidth;
    }

    @JIPipeDocumentation(name = "ROI name", description = "Allows to change the ROI name")
    @JIPipeParameter("roi-name")
    @ExpressionParameterSettings(variableSource = VariableSource.class)
    public OptionalDefaultExpressionParameter getRoiName() {
        return roiName;
    }

    @JIPipeParameter("roi-name")
    public void setRoiName(OptionalDefaultExpressionParameter roiName) {
        this.roiName = roiName;
    }

    @JIPipeDocumentation(name = "Scale X", description = "Scales the ROI. Please note that the scale will not be saved inside the ROI. Must evaluate to a number.")
    @JIPipeParameter("scale-x")
    @ExpressionParameterSettings(variableSource = VariableSource.class)
    public OptionalDefaultExpressionParameter getScaleX() {
        return scaleX;
    }

    @JIPipeParameter("scale-x")
    public void setScaleX(OptionalDefaultExpressionParameter scaleX) {
        this.scaleX = scaleX;
    }

    @JIPipeDocumentation(name = "Scale Y", description = "Scales the ROI. Please note that the scale will not be saved inside the ROI. Must evaluate to a number.")
    @JIPipeParameter("scale-y")
    @ExpressionParameterSettings(variableSource = VariableSource.class)
    public OptionalDefaultExpressionParameter getScaleY() {
        return scaleY;
    }

    @JIPipeParameter("scale-y")
    public void setScaleY(OptionalDefaultExpressionParameter scaleY) {
        this.scaleY = scaleY;
    }

    @JIPipeDocumentation(name = "Center scale", description = "If true, each ROI is scaled relative to its center. Must evaluate to a boolean.")
    @JIPipeParameter("center-scale")
    @ExpressionParameterSettings(variableSource = VariableSource.class)
    public OptionalDefaultExpressionParameter getCenterScale() {
        return centerScale;
    }

    @JIPipeParameter("center-scale")
    public void setCenterScale(OptionalDefaultExpressionParameter centerScale) {
        this.centerScale = centerScale;
    }

    public static class VariableSource implements ExpressionParameterVariableSource {

        private static final Set<ExpressionParameterVariable> VARIABLES = new HashSet<>();

        static {
            VARIABLES.add(new ExpressionParameterVariable("<Annotations>", "The annotations of the current data batch", ""));
            VARIABLES.add(new ExpressionParameterVariable("Fill color", "The fill color of the ROI", "fill_color"));
            VARIABLES.add(new ExpressionParameterVariable("Line color", "The line color of the ROI", "line_color"));
            VARIABLES.add(new ExpressionParameterVariable("Line width", "The line width", "line_width"));
            VARIABLES.add(new ExpressionParameterVariable("Channel location", "The channel (C) location. The first index is 1. Zero indicates that that ROI applies to all locations.", "c"));
            VARIABLES.add(new ExpressionParameterVariable("Slice location", "The slice (Z) location. The first index is 1. Zero indicates that that ROI applies to all locations.", "z"));
            VARIABLES.add(new ExpressionParameterVariable("Frame location", "The frame (T) location. The first index is 1. Zero indicates that that ROI applies to all locations.", "t"));
            VARIABLES.add(new ExpressionParameterVariable("X Location", "The X location of the ROI", "x"));
            VARIABLES.add(new ExpressionParameterVariable("Y Location", "The Y location of the ROI", "y"));
            VARIABLES.add(new ExpressionParameterVariable("Name", "The ROI name", "name"));
        }

        @Override
        public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
            return VARIABLES;
        }
    }
}
