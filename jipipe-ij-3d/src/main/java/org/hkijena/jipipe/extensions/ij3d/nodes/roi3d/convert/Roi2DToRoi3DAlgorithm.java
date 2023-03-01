package org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.convert;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ij3d.IJ3DUtils;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.Neighborhood3D;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;

@JIPipeDocumentation(name = "Convert 2D ROI to 3D ROI", description = "Converts a 2D ROI list into a 3D ROI list. Please note that you need to enable the fast mode to merge 2D ROI in the Z-axis.")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Convert")
@JIPipeInputSlot(value = ROIListData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ROI3DListData.class, slotName = "Output", autoCreate = true)
public class Roi2DToRoi3DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private boolean fast = false;
    private boolean force2D = true;
    private Neighborhood3D neighborhood = Neighborhood3D.TwentySixConnected;

    public Roi2DToRoi3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public Roi2DToRoi3DAlgorithm(Roi2DToRoi3DAlgorithm other) {
        super(other);
        this.fast = other.fast;
        this.force2D = other.force2D;
        this.neighborhood = other.neighborhood;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROIListData inputData = dataBatch.getInputData(getFirstInputSlot(), ROIListData.class, progressInfo);
        ROI3DListData outputData = IJ3DUtils.roi2DtoRoi3D(inputData, force2D, fast, neighborhood, progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @JIPipeDocumentation(name = "Fast mode / Enable 3D", description = "If enabled, multiple ROIs at once will be converted to a mask and passed to the 2D-to-3D converter. This will work only as expected if you do not have overlapping ROIs. " +
            "Must be enabled if you want 3D ROI.")
    @JIPipeParameter("fast")
    public boolean isFast() {
        return fast;
    }

    @JIPipeParameter("fast")
    public void setFast(boolean fast) {
        this.fast = fast;
    }

    @JIPipeDocumentation(name = "Force 2D", description = "If enabled, the generated 3D ROI will be 2D. 2D objects will not be connected into 3D objects.")
    @JIPipeParameter("force-2d")
    public boolean isForce2D() {
        return force2D;
    }

    @JIPipeParameter("force-2d")
    public void setForce2D(boolean force2D) {
        this.force2D = force2D;
    }

    @JIPipeDocumentation(name = "Neighborhood", description = "Determines which neighborhood is used to find connected components.")
    @JIPipeParameter("neighborhood")
    public Neighborhood3D getNeighborhood() {
        return neighborhood;
    }

    @JIPipeParameter("neighborhood")
    public void setNeighborhood(Neighborhood3D neighborhood) {
        this.neighborhood = neighborhood;
    }
}
