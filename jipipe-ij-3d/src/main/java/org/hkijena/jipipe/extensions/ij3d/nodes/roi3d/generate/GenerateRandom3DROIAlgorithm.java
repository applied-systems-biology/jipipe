package org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.generate;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.parameters.library.quantities.Quantity;

import java.util.Objects;

@JIPipeDocumentation(name = "Generate random 3D spots", description = "Randomly generates 3D ROI spots")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeOutputSlot(value = ROI3DListData.class, slotName = "Output", autoCreate = true)
public class GenerateRandom3DROIAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private int objectCount = 10;
    private double minDistance = 5;

    private Quantity scaleXY = new Quantity(1, "px");

    private Quantity scaleZ = new Quantity(1, "px");

    public GenerateRandom3DROIAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public GenerateRandom3DROIAlgorithm(GenerateRandom3DROIAlgorithm other) {
        super(other);
        this.objectCount = other.objectCount;
        this.minDistance = other.minDistance;
        this.scaleXY = new Quantity(other.scaleXY);
        this.scaleZ = new Quantity(other.scaleZ);
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        super.reportValidity(report);
        if(!Objects.equals(scaleXY.getUnit(), scaleZ.getUnit())) {
            report.reportIsInvalid("Units are not equal: " + scaleXY.getUnit() + " <> " + scaleZ.getUnit(),
                    "The units of the scale parameters need to be equal!",
                    "Set the same unit",
                    this);
        }
    }

    @JIPipeDocumentation(name = "Number of objects", description = "The number of objects to generate")
    @JIPipeParameter("object-count")
    public int getObjectCount() {
        return objectCount;
    }

    @JIPipeParameter("object-count")
    public void setObjectCount(int objectCount) {
        this.objectCount = objectCount;
    }

    @JIPipeDocumentation(name = "Minimum distance", description = "The minimum distance between objects")
    @JIPipeParameter("min-distance")
    public double getMinDistance() {
        return minDistance;
    }

    @JIPipeParameter("min-distance")
    public void setMinDistance(double minDistance) {
        this.minDistance = minDistance;
    }

    @JIPipeDocumentation(name = "Physical size (X/Y)", description = "The physical size / scale in the X/Y-direction. The unit needs to be the same as in the Z direction.")
    @JIPipeParameter("scale-xy")
    public Quantity getScaleXY() {
        return scaleXY;
    }

    @JIPipeParameter("scale-xy")
    public void setScaleXY(Quantity scaleXY) {
        this.scaleXY = scaleXY;
    }

    @JIPipeDocumentation(name = "Physical size (Z)", description = "The physical size / scale in the Z-direction. The unit needs to be the same as in the X/Y direction.")
    @JIPipeParameter("scale-z")
    public Quantity getScaleZ() {
        return scaleZ;
    }

    @JIPipeParameter("scale-z")
    public void setScaleZ(Quantity scaleZ) {
        this.scaleZ = scaleZ;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROI3DListData outputData = new ROI3DListData();
        outputData.setCalibration(scaleXY.getValue(), scaleZ.getValue(), scaleXY.getUnit());
        outputData.createRandomPopulation(objectCount, minDistance);
        dataBatch.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }
}
