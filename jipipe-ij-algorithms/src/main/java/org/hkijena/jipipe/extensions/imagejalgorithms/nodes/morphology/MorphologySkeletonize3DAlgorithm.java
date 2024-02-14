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
 *
 */

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.morphology;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeCitation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleMaskData;
import sc.fiji.skeletonize3D.Skeletonize3D_;

@JIPipeDocumentation(name = "Morphological skeletonize 3D", description = "Performs the skeletonization of 2D and 3D binary images (8-bit images)")
@JIPipeNode(menuPath = "Morphology", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlus3DGreyscaleMaskData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlus3DGreyscaleMaskData.class, slotName = "Output", autoCreate = true)
@JIPipeCitation("Implementation by Ignacio Arganda-Carreras https://imagej.net/plugins/skeletonize3d")
@JIPipeCitation("Lee et al. “Building skeleton models via 3-D medial surface/axis thinning algorithms. Computer Vision, Graphics, and Image Processing, 56(6):462–478, 1994.”")
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nSkeletonize", aliasName = "Skeletonize (3D)")
public class MorphologySkeletonize3DAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public MorphologySkeletonize3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public MorphologySkeletonize3DAlgorithm(MorphologySkeletonize3DAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus image = iterationStep.getInputData(getFirstInputSlot(), ImagePlus3DGreyscaleMaskData.class, progressInfo).getDuplicateImage();
        Skeletonize3D_ skeletonize3D = new Skeletonize3D_();
        skeletonize3D.setup("", image);
        skeletonize3D.run(image.getProcessor());
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlus3DGreyscaleMaskData(image), progressInfo);
    }
}
