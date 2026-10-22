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

import static org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension.SPATIAL_3D;
import static org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension.SPATIAL_2D;
import org.hkijena.jipipe.api.nodes.taxonomy.frameworks.IJ3DFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.MergingSplittingFunction;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.plugins.ij3d.datatypes.Ij3dSuiteRoiListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;

@SetJIPipeDocumentation(name = "IJ3D Remove 3D overlay", description = "Remove overlay ROIs")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
@ConfigureJIPipeNode(function = MergingSplittingFunction.class, framework = IJ3DFramework.class, spatialDimensions = { SPATIAL_2D, SPATIAL_3D })
public class RemoveOverlay3DAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public RemoveOverlay3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public RemoveOverlay3DAlgorithm(RemoveOverlay3DAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData img = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).shallowCopy();
        img.removeOverlaysOfType(Ij3dSuiteRoiListData.class);
        iterationStep.addOutputData(getFirstOutputSlot(), img, progressInfo);
    }
}
