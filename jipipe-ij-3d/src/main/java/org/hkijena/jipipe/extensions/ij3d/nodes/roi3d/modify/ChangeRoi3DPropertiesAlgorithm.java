package org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.modify;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3D;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalDoubleParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalIntegerParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalStringParameter;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;

@JIPipeDocumentation(name = "Change 3D ROI properties", description = "Sets the properties of all 3D ROI")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Modify")
@JIPipeInputSlot(value = ROI3DListData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ROI3DListData.class, slotName = "Output", autoCreate = true)
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROI3DListData outputROI = new ROI3DListData(dataBatch.getInputData("Input", ROI3DListData.class, progressInfo));

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

        dataBatch.addOutputData(getFirstOutputSlot(), outputROI, progressInfo);
    }

    @JIPipeDocumentation(name = "ROI name", description = "If true, override the ROI name")
    @JIPipeParameter("roi-name")
    public OptionalStringParameter getRoiName() {
        return roiName;
    }

    @JIPipeParameter("roi-name")
    public void setRoiName(OptionalStringParameter roiName) {
        this.roiName = roiName;
    }

    @JIPipeDocumentation(name = "ROI comment", description = "If true, override the ROI's comment field")
    @JIPipeParameter("roi-comment")
    public OptionalStringParameter getRoiComment() {
        return roiComment;
    }

    @JIPipeParameter("roi-comment")
    public void setRoiComment(OptionalStringParameter roiComment) {
        this.roiComment = roiComment;
    }

    @JIPipeDocumentation(name = "Center (X)", description = "If true, override the ROI's center X location")
    @JIPipeParameter("center-x")
    public OptionalDoubleParameter getCenterX() {
        return centerX;
    }

    @JIPipeParameter("center-x")
    public void setCenterX(OptionalDoubleParameter centerX) {
        this.centerX = centerX;
    }

    @JIPipeDocumentation(name = "Center (Y)", description = "If true, override the ROI's center Y location")
    @JIPipeParameter("center-y")
    public OptionalDoubleParameter getCenterY() {
        return centerY;
    }

    @JIPipeParameter("center-y")
    public void setCenterY(OptionalDoubleParameter centerY) {
        this.centerY = centerY;
    }

    @JIPipeDocumentation(name = "Center (Z)", description = "If true, override the ROI's center Z location")
    @JIPipeParameter("center-z")
    public OptionalDoubleParameter getCenterZ() {
        return centerZ;
    }

    @JIPipeParameter("center-z")
    public void setCenterZ(OptionalDoubleParameter centerZ) {
        this.centerZ = centerZ;
    }

    @JIPipeDocumentation(name = "Channel", description = "If true, override the ROI's channel location (0 = all channels)")
    @JIPipeParameter("channel-location")
    public OptionalIntegerParameter getChannelLocation() {
        return channelLocation;
    }

    @JIPipeParameter("channel-location")
    public void setChannelLocation(OptionalIntegerParameter channelLocation) {
        this.channelLocation = channelLocation;
    }

    @JIPipeDocumentation(name = "Frame", description = "If true, override the ROI's frame location (0 = all frames)")
    @JIPipeParameter("frame-location")
    public OptionalIntegerParameter getFrameLocation() {
        return frameLocation;
    }

    @JIPipeParameter("frame-location")
    public void setFrameLocation(OptionalIntegerParameter frameLocation) {
        this.frameLocation = frameLocation;
    }

    @JIPipeDocumentation(name = "Fill color", description = "If true, override the ROI's fill color (you can return a color or a HEX string)")
    @JIPipeParameter("fill-color")
    public OptionalColorParameter getFillColor() {
        return fillColor;
    }

    @JIPipeParameter("fill-color")
    public void setFillColor(OptionalColorParameter fillColor) {
        this.fillColor = fillColor;
    }
}
