package org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.process;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;

@JIPipeDocumentation(name = "Remove border 3D ROI", description = "Removes 3D ROI at the image borders")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class)
@JIPipeInputSlot(value = ROI3DListData.class, slotName = "Input", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", autoCreate = true)
@JIPipeOutputSlot(value = ROI3DListData.class, slotName = "Output", autoCreate = true)
public class RemoveBorderRoi3DAlgorithm extends JIPipeIteratingAlgorithm {
    private boolean removeInX = true;

    private boolean removeInY = true;
    private boolean removeInZ = true;
    private double borderDistance = 0;

    public RemoveBorderRoi3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public RemoveBorderRoi3DAlgorithm(RemoveBorderRoi3DAlgorithm other) {
        super(other);
        this.removeInX = other.removeInX;
        this.removeInY = other.removeInY;
        this.removeInZ = other.removeInZ;
        this.borderDistance = other.borderDistance;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        ROI3DListData data = iterationStep.getInputData("Input", ROI3DListData.class, progressInfo).shallowCopy();
        ImagePlus reference = iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo).getImage();
        data.removeIf(roi3D -> {
            if (removeInX) {
                int xMin = roi3D.getObject3D().getXmin();
                int xMax = roi3D.getObject3D().getXmax();
                if (xMin <= borderDistance) {
                    return true;
                }
                if (xMax >= reference.getWidth() - borderDistance - 1) {
                    return true;
                }
            }
            if (removeInY) {
                int yMin = roi3D.getObject3D().getYmin();
                int yMax = roi3D.getObject3D().getYmax();
                if (yMin <= borderDistance) {
                    return true;
                }
                if (yMax >= reference.getHeight() - borderDistance - 1) {
                    return true;
                }
            }
            if (removeInZ) {
                int zMin = roi3D.getObject3D().getZmin();
                int zMax = roi3D.getObject3D().getZmax();
                if (zMin <= borderDistance) {
                    return true;
                }
                if (zMax >= reference.getNSlices() - borderDistance - 1) {
                    return true;
                }
            }
            return false;
        });
        iterationStep.addOutputData(getFirstOutputSlot(), data, progressInfo);
    }

    @JIPipeDocumentation(name = "Check X coordinate", description = "If enabled, check if the object's X coordinate")
    @JIPipeParameter("remove-in-x")
    public boolean isRemoveInX() {
        return removeInX;
    }

    @JIPipeParameter("remove-in-x")
    public void setRemoveInX(boolean removeInX) {
        this.removeInX = removeInX;
    }

    @JIPipeDocumentation(name = "Check Y coordinate", description = "If enabled, check if the object's Y coordinate")
    @JIPipeParameter("remove-in-y")
    public boolean isRemoveInY() {
        return removeInY;
    }

    @JIPipeParameter("remove-in-y")
    public void setRemoveInY(boolean removeInY) {
        this.removeInY = removeInY;
    }

    @JIPipeDocumentation(name = "Check Z coordinate", description = "If enabled, check if the object's Z coordinate")
    @JIPipeParameter("remove-in-z")
    public boolean isRemoveInZ() {
        return removeInZ;
    }

    @JIPipeParameter("remove-in-z")
    public void setRemoveInZ(boolean removeInZ) {
        this.removeInZ = removeInZ;
    }

    @JIPipeDocumentation(name = "Border distance", description = "The maximum distance to the border (defaults to zero)")
    @JIPipeParameter("border-distance")
    public double getBorderDistance() {
        return borderDistance;
    }

    @JIPipeParameter("border-distance")
    public void setBorderDistance(double borderDistance) {
        this.borderDistance = borderDistance;
    }
}
