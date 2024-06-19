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

package org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.convert;

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
import org.hkijena.jipipe.plugins.ij3d.utils.Roi3DDrawer;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;

@SetJIPipeDocumentation(name = "Convert 3D ROI to RGB", description = "Converts 3D ROI lists to a label image. Depending on the number of objects, an 8-bit, 16-bit, or 32-bit label image is generated.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Convert")
@AddJIPipeInputSlot(value = ROI3DListData.class, name = "ROI", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Image", create = true, optional = true, description = "Optional image where the objects are drawn on")
@AddJIPipeOutputSlot(value = ImagePlusColorRGBData.class, slotName = "Output", create = true)
public class Roi3DToRGBAlgorithm extends JIPipeIteratingAlgorithm {

    private final Roi3DDrawer drawer;

    public Roi3DToRGBAlgorithm(JIPipeNodeInfo info) {
        super(info);
        drawer = new Roi3DDrawer();
        registerSubParameter(drawer);
    }

    public Roi3DToRGBAlgorithm(Roi3DToRGBAlgorithm other) {
        super(other);
        this.drawer = new Roi3DDrawer(other.drawer);
        registerSubParameter(drawer);
    }

    @SetJIPipeDocumentation(name = "ROI rendering settings", description = "The following settings determine how the 3D ROI are rendered")
    @JIPipeParameter("drawer-settings")
    public Roi3DDrawer getDrawer() {
        return drawer;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROI3DListData roi3DListData = iterationStep.getInputData("ROI", ROI3DListData.class, progressInfo);
        ImagePlusData referenceImage = iterationStep.getInputData("Image", ImagePlusData.class, progressInfo);

        ImagePlus outputImage = drawer.draw(roi3DListData, referenceImage != null ? referenceImage.getImage() : null, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusColorRGBData(outputImage), progressInfo);
    }
}