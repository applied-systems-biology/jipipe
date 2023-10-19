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

package org.hkijena.jipipe.extensions.ij3d.nodes.overlay;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.ij3d.utils.Roi3DDrawer;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.RoiDrawer;

/**
 * Wrapper around {@link RoiDrawer}
 */
@JIPipeDocumentation(name = "Render 3D overlay", description = "Renders the 3D overlay to RGB")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "ROI")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusColorRGBData.class, slotName = "Output", autoCreate = true)
public class RenderOverlay3DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final Roi3DDrawer drawer;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public RenderOverlay3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.drawer = new Roi3DDrawer();
        registerSubParameter(drawer);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public RenderOverlay3DAlgorithm(RenderOverlay3DAlgorithm other) {
        super(other);
        this.drawer = new Roi3DDrawer(other.drawer);
        registerSubParameter(drawer);
    }

    @Override
    protected boolean canPassThrough() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData image = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        ROI3DListData rois = new ROI3DListData();
        for (ROI3DListData data : image.extractOverlaysOfType(ROI3DListData.class)) {
            rois.addAll(data);
        }
        ImagePlus outputImage = drawer.draw(rois, image.getImage(), progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusColorRGBData(outputImage), progressInfo);

    }

    @JIPipeDocumentation(name = "ROI rendering settings", description = "The following settings determine how the 3D ROI are rendered")
    @JIPipeParameter("drawer-settings")
    public Roi3DDrawer getDrawer() {
        return drawer;
    }
}
