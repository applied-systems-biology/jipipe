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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.morphology;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeCitation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleMaskData;
import sc.fiji.skeletonize3D.Skeletonize3D_;

@JIPipeDocumentation(name = "Morphological skeletonize 3D", description = "Performs the skeletonization of 2D and 3D binary images (8-bit images)")
@JIPipeNode(menuPath = "Morphology", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlus3DGreyscaleMaskData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlus3DGreyscaleMaskData.class, slotName = "Output", autoCreate = true)
@JIPipeCitation("Implementation by Ignacio Arganda-Carreras https://imagej.net/plugins/skeletonize3d")
@JIPipeCitation("Lee et al. “Building skeleton models via 3-D medial surface/axis thinning algorithms. Computer Vision, Graphics, and Image Processing, 56(6):462–478, 1994.”")
public class MorphologySkeletonize3DAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public MorphologySkeletonize3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public MorphologySkeletonize3DAlgorithm(MorphologySkeletonize3DAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus image = dataBatch.getInputData(getFirstInputSlot(), ImagePlus3DGreyscaleMaskData.class, progressInfo).getDuplicateImage();
        Skeletonize3D_ skeletonize3D = new Skeletonize3D_();
        skeletonize3D.setup("", image);
        skeletonize3D.run(image.getProcessor());
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlus3DGreyscaleMaskData(image), progressInfo);
    }
}
