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

package org.hkijena.jipipe.plugins.ij3d.nodes.overlay;

import ij.ImagePlus;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.ij3d.datatypes.Ij3dSuiteRoiListData;
import org.hkijena.jipipe.plugins.ij3d.utils.Roi3dDrawer;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.RoiDrawer;

/**
 * Wrapper around {@link RoiDrawer}
 */
@SetJIPipeDocumentation(name = "IJ3D Render 3D overlay", description = "Renders the 3D overlay to RGB")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "ROI")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusColorRGBData.class, name = "Output", create = true)
public class RenderOverlay3DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final Roi3dDrawer drawer;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public RenderOverlay3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.drawer = new Roi3dDrawer();
        registerSubParameter(drawer);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public RenderOverlay3DAlgorithm(RenderOverlay3DAlgorithm other) {
        super(other);
        this.drawer = new Roi3dDrawer(other.drawer);
        registerSubParameter(drawer);
    }

    @Override
    public boolean canPassThrough() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData image = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        Ij3dSuiteRoiListData rois = new Ij3dSuiteRoiListData();
        for (Ij3dSuiteRoiListData data : image.extractOverlaysOfType(Ij3dSuiteRoiListData.class)) {
            rois.addAll(data);
        }
        ImagePlus outputImage = drawer.draw(rois, image.getImage(), progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusColorRGBData(outputImage), progressInfo);

    }

    @SetJIPipeDocumentation(name = "ROI rendering settings", description = "The following settings determine how the 3D ROI are rendered")
    @JIPipeParameter("drawer-settings")
    public Roi3dDrawer getDrawer() {
        return drawer;
    }
}
