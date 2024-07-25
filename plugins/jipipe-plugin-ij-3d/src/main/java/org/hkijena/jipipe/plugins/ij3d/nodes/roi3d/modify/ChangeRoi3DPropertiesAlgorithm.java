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

package org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.modify;

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
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.ij3d.datatypes.ROI3D;
import org.hkijena.jipipe.plugins.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.plugins.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalDoubleParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalStringParameter;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;

@SetJIPipeDocumentation(name = "Set 3D ROI properties", description = "Sets the properties of all 3D ROI")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Modify")
@AddJIPipeInputSlot(value = ROI3DListData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ROI3DListData.class, name = "Output", create = true)
public class ChangeRoi3DPropertiesAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    private OptionalStringParameter roiName = new OptionalStringParameter("", false);
    private OptionalStringParameter roiComment = new OptionalStringParameter("", false);
    private OptionalDoubleParameter centerX = new OptionalDoubleParameter(0, false);
    private OptionalDoubleParameter centerY = new OptionalDoubleParameter(0, false);
    private OptionalDoubleParameter centerZ = new OptionalDoubleParameter(0, false);
    private OptionalIntegerParameter channelLocation = new OptionalIntegerParameter(false, 0);
    private OptionalIntegerParameter frameLocation = new OptionalIntegerParameter(false, 0);
    private OptionalColorParameter fillColor = new OptionalColorParameter(Color.RED, false);

    public ChangeRoi3DPropertiesAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ChangeRoi3DPropertiesAlgorithm(ChangeRoi3DPropertiesAlgorithm other) {
        super(other);
        this.roiName = other.roiName;
        this.roiComment = other.roiComment;
        this.centerX = other.centerX;
        this.centerY = other.centerY;
        this.centerZ = other.centerZ;
        this.channelLocation = other.channelLocation;
        this.frameLocation = other.frameLocation;
        this.fillColor = other.fillColor;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROI3DListData outputROI = new ROI3DListData(iterationStep.getInputData("Input", ROI3DListData.class, progressInfo));

        for (int row = 0; row < outputROI.size(); row++) {
            ROI3D roi = outputROI.get(row);

            // Filter
            if (roiName.isEnabled()) {
                roi.getObject3D().setName(StringUtils.nullToEmpty(roiName.getContent()));
            }
            if (roiComment.isEnabled()) {
                roi.getObject3D().setComment(StringUtils.nullToEmpty(roiComment.getContent()));
            }
            if (centerX.isEnabled() || centerY.isEnabled() || centerZ.isEnabled()) {
                double x = roi.getObject3D().getCenterX();
                double y = roi.getObject3D().getCenterY();
                double z = roi.getObject3D().getCenterZ();
                if (centerX.isEnabled()) {
                    x = centerX.getContent();
                }
                if (centerY.isEnabled()) {
                    y = centerY.getContent();
                }
                if (centerZ.isEnabled()) {
                    z = centerZ.getContent();
                }
                roi.getObject3D().setNewCenter(x, y, z);
            }
            if (channelLocation.isEnabled()) {
                roi.setChannel(channelLocation.getContent());
            }
            if (frameLocation.isEnabled()) {
                roi.setFrame(frameLocation.getContent());
            }
            if (fillColor.isEnabled()) {
                roi.setFillColor(fillColor.getContent());
            }
        }

        iterationStep.addOutputData(getFirstOutputSlot(), outputROI, progressInfo);
    }

    @SetJIPipeDocumentation(name = "ROI name", description = "If true, override the ROI name")
    @JIPipeParameter("roi-name")
    public OptionalStringParameter getRoiName() {
        return roiName;
    }

    @JIPipeParameter("roi-name")
    public void setRoiName(OptionalStringParameter roiName) {
        this.roiName = roiName;
    }

    @SetJIPipeDocumentation(name = "ROI comment", description = "If true, override the ROI's comment field")
    @JIPipeParameter("roi-comment")
    public OptionalStringParameter getRoiComment() {
        return roiComment;
    }

    @JIPipeParameter("roi-comment")
    public void setRoiComment(OptionalStringParameter roiComment) {
        this.roiComment = roiComment;
    }

    @SetJIPipeDocumentation(name = "Center (X)", description = "If true, override the ROI's center X location")
    @JIPipeParameter("center-x")
    public OptionalDoubleParameter getCenterX() {
        return centerX;
    }

    @JIPipeParameter("center-x")
    public void setCenterX(OptionalDoubleParameter centerX) {
        this.centerX = centerX;
    }

    @SetJIPipeDocumentation(name = "Center (Y)", description = "If true, override the ROI's center Y location")
    @JIPipeParameter("center-y")
    public OptionalDoubleParameter getCenterY() {
        return centerY;
    }

    @JIPipeParameter("center-y")
    public void setCenterY(OptionalDoubleParameter centerY) {
        this.centerY = centerY;
    }

    @SetJIPipeDocumentation(name = "Center (Z)", description = "If true, override the ROI's center Z location")
    @JIPipeParameter("center-z")
    public OptionalDoubleParameter getCenterZ() {
        return centerZ;
    }

    @JIPipeParameter("center-z")
    public void setCenterZ(OptionalDoubleParameter centerZ) {
        this.centerZ = centerZ;
    }

    @SetJIPipeDocumentation(name = "Channel", description = "If true, override the ROI's channel location (0 = all channels)")
    @JIPipeParameter("channel-location")
    public OptionalIntegerParameter getChannelLocation() {
        return channelLocation;
    }

    @JIPipeParameter("channel-location")
    public void setChannelLocation(OptionalIntegerParameter channelLocation) {
        this.channelLocation = channelLocation;
    }

    @SetJIPipeDocumentation(name = "Frame", description = "If true, override the ROI's frame location (0 = all frames)")
    @JIPipeParameter("frame-location")
    public OptionalIntegerParameter getFrameLocation() {
        return frameLocation;
    }

    @JIPipeParameter("frame-location")
    public void setFrameLocation(OptionalIntegerParameter frameLocation) {
        this.frameLocation = frameLocation;
    }

    @SetJIPipeDocumentation(name = "Fill color", description = "If true, override the ROI's fill color (you can return a color or a HEX string)")
    @JIPipeParameter("fill-color")
    public OptionalColorParameter getFillColor() {
        return fillColor;
    }

    @JIPipeParameter("fill-color")
    public void setFillColor(OptionalColorParameter fillColor) {
        this.fillColor = fillColor;
    }
}
