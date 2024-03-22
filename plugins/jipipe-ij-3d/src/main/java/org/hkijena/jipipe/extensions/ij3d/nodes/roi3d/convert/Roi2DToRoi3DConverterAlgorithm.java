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

package org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.convert;

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
import org.hkijena.jipipe.extensions.ij3d.IJ3DUtils;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.imagejalgorithms.parameters.Neighborhood3D;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;

@SetJIPipeDocumentation(name = "Convert 2D ROI to 3D ROI", description = "Converts a 2D ROI list into a 3D ROI list. Please note that you need to enable the fast mode to merge 2D ROI in the Z-axis.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Convert")
@AddJIPipeInputSlot(value = ROIListData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ROI3DListData.class, slotName = "Output", create = true)
public class Roi2DToRoi3DConverterAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private boolean fast = false;
    private boolean force2D = true;
    private Neighborhood3D neighborhood = Neighborhood3D.TwentySixConnected;

    public Roi2DToRoi3DConverterAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public Roi2DToRoi3DConverterAlgorithm(Roi2DToRoi3DConverterAlgorithm other) {
        super(other);
        this.fast = other.fast;
        this.force2D = other.force2D;
        this.neighborhood = other.neighborhood;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROIListData inputData = iterationStep.getInputData(getFirstInputSlot(), ROIListData.class, progressInfo);
        ROI3DListData outputData = IJ3DUtils.roi2DtoRoi3D(inputData, force2D, fast, neighborhood, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Fast mode / Enable 3D", description = "If enabled, multiple ROIs at once will be converted to a mask and passed to the 2D-to-3D converter. This will work only as expected if you do not have overlapping ROIs. " +
            "Must be enabled if you want 3D ROI.")
    @JIPipeParameter("fast")
    public boolean isFast() {
        return fast;
    }

    @JIPipeParameter("fast")
    public void setFast(boolean fast) {
        this.fast = fast;
    }

    @SetJIPipeDocumentation(name = "Force 2D", description = "If enabled, the generated 3D ROI will be 2D. 2D objects will not be connected into 3D objects.")
    @JIPipeParameter("force-2d")
    public boolean isForce2D() {
        return force2D;
    }

    @JIPipeParameter("force-2d")
    public void setForce2D(boolean force2D) {
        this.force2D = force2D;
    }

    @SetJIPipeDocumentation(name = "Neighborhood", description = "Determines which neighborhood is used to find connected components.")
    @JIPipeParameter("neighborhood")
    public Neighborhood3D getNeighborhood() {
        return neighborhood;
    }

    @JIPipeParameter("neighborhood")
    public void setNeighborhood(Neighborhood3D neighborhood) {
        this.neighborhood = neighborhood;
    }
}
