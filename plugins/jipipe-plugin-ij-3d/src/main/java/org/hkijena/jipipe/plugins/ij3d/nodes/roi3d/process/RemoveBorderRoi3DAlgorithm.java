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

package org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.process;

import ij.ImagePlus;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;

@SetJIPipeDocumentation(name = "Remove border 3D ROI", description = "Removes 3D ROI at the image borders")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ROI3DListData.class, name = "Input", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Reference", create = true)
@AddJIPipeOutputSlot(value = ROI3DListData.class, name = "Output", create = true)
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
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

    @SetJIPipeDocumentation(name = "Check X coordinate", description = "If enabled, check if the object's X coordinate")
    @JIPipeParameter("remove-in-x")
    public boolean isRemoveInX() {
        return removeInX;
    }

    @JIPipeParameter("remove-in-x")
    public void setRemoveInX(boolean removeInX) {
        this.removeInX = removeInX;
    }

    @SetJIPipeDocumentation(name = "Check Y coordinate", description = "If enabled, check if the object's Y coordinate")
    @JIPipeParameter("remove-in-y")
    public boolean isRemoveInY() {
        return removeInY;
    }

    @JIPipeParameter("remove-in-y")
    public void setRemoveInY(boolean removeInY) {
        this.removeInY = removeInY;
    }

    @SetJIPipeDocumentation(name = "Check Z coordinate", description = "If enabled, check if the object's Z coordinate")
    @JIPipeParameter("remove-in-z")
    public boolean isRemoveInZ() {
        return removeInZ;
    }

    @JIPipeParameter("remove-in-z")
    public void setRemoveInZ(boolean removeInZ) {
        this.removeInZ = removeInZ;
    }

    @SetJIPipeDocumentation(name = "Border distance", description = "The maximum distance to the border (defaults to zero)")
    @JIPipeParameter("border-distance")
    public double getBorderDistance() {
        return borderDistance;
    }

    @JIPipeParameter("border-distance")
    public void setBorderDistance(double borderDistance) {
        this.borderDistance = borderDistance;
    }
}
