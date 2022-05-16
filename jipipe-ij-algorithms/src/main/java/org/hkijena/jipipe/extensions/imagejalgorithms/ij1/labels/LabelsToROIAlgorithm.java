package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.labels;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeCitation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;

@JIPipeDocumentation(name = "Labels to ROI", description = "Converts a label image into a set of ROI. Labels must have a value larger than zero to be detected." +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice. The Z/C/T coordinates of the source slices are saved inside the ROI.")
@JIPipeCitation("Based on 'LabelsToROIs'; Waisman, A., Norris, A. ., Elías Costa , . et al. " +
        "Automatic and unbiased segmentation and quantification of myofibers in skeletal muscle. Sci Rep 11, 11793 (2021). doi: https://doi.org/10.1038/s41598-021-91191-6.")
@JIPipeNode(menuPath = "Labels", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Labels", description = "The labels image", autoCreate = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "ROI", description = "The generated ROI", autoCreate = true)
public class LabelsToROIAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    public LabelsToROIAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public LabelsToROIAlgorithm(LabelsToROIAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus labelsImage = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();
        ROIListData rois = new ROIListData();
        ImageJUtils.forEachIndexedZCTSlice(labelsImage, (ip, index) -> {
            ImageProcessor copy = (ImageProcessor) ip.clone();
            ImagePlus wrapper = new ImagePlus("slice", copy);
            for (int y = 0; y < copy.getHeight(); y++) {
                for (int x = 0; x < copy.getWidth(); x++) {
                    float value = copy.getf(x, y);
                    if(value > 0) {
                        IJ.doWand(wrapper, x, y, 0, "Legacy smooth");
                        Roi roi = wrapper.getRoi();
                        rois.add(roi);
                        copy.setColor(0);
                        copy.fill(roi);
                        wrapper.setRoi((Roi) null);
                        roi.setPosition(index.getC() + 1, index.getZ() + 1, index.getT() + 1);
                        roi.setName("Label-" + value);
                    }
                }
            }
        }, progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(), rois, progressInfo);
    }
}
