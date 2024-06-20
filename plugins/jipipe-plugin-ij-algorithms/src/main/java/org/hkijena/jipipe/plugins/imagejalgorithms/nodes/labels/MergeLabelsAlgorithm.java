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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.labels;

import ij.ImagePlus;
import inra.ijpb.label.LabelImages;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

@SetJIPipeDocumentation(name = "Merge label inside ROI", description = "Sets all labels inside the specified ROI to the same value.")
@ConfigureJIPipeNode(menuPath = "Labels", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Labels", create = true)
@AddJIPipeInputSlot(value = ROIListData.class, name = "ROI", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Labels", create = true)
@AddJIPipeCitation("Legland, D.; Arganda-Carreras, I. & Andrey, P. (2016), \"MorphoLibJ: integrated library and plugins for mathematical morphology with ImageJ\", " +
        "Bioinformatics (Oxford Univ Press) 32(22): 3532-3534, PMID 27412086, doi:10.1093/bioinformatics/btw413")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nMorphoLibJ\nLabel Images")
public class MergeLabelsAlgorithm extends JIPipeIteratingAlgorithm {

    public MergeLabelsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public MergeLabelsAlgorithm(MergeLabelsAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = iterationStep.getInputData("Labels", ImagePlusGreyscaleData.class, progressInfo).getImage();
        ROIListData rois = iterationStep.getInputData("ROI", ROIListData.class, progressInfo);
        if (rois.isEmpty()) {
            iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(inputImage), progressInfo);
            return;
        }
        if (rois.size() > 1) {
            rois = new ROIListData(rois);
            rois.logicalOr();
        }
        ImagePlus outputImage = ImageJUtils.duplicate(inputImage);
        outputImage.setTitle(inputImage.getTitle());
        LabelImages.mergeLabels(outputImage, rois.get(0), false);
        outputImage.setDimensions(inputImage.getNChannels(), inputImage.getNSlices(), inputImage.getNFrames());
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(outputImage), progressInfo);
    }
}
