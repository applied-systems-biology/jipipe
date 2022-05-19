package org.hkijena.jipipe.extensions.ijweka.nodes;

import ij.ImagePlus;
import ij.ImageStack;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijweka.datatypes.WekaModelData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import trainableSegmentation.WekaSegmentation;

@JIPipeDocumentation(name = "Weka classifier 2D", description = "Classifies an image with a Weka model. If higher-dimensional data is provided, the classification is applied per slice. To obtain ROI from the generated labels, utilize the 'Labels to ROI' node.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Weka")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Image", description = "Image on which the classification should be applied", autoCreate = true)
@JIPipeInputSlot(value = WekaModelData.class, slotName = "Model", description = "The model", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Classified image", description = "The classified image", autoCreate = true)
public class WekaClassification2DAlgorithm extends JIPipeIteratingAlgorithm {
    public WekaClassification2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public WekaClassification2DAlgorithm(WekaClassification2DAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData image = dataBatch.getInputData("Image", ImagePlusData.class, progressInfo);
        WekaModelData modelData = dataBatch.getInputData("Model", WekaModelData.class, progressInfo);
        WekaSegmentation segmentation = modelData.getSegmentation();

        ImageStack stack = new ImageStack(image.getWidth(), image.getHeight(), image.getNSlices() * image.getNChannels() * image.getNChannels());
        ImageJUtils.forEachIndexedZCTSlice(image.getImage(), (ip, index) -> {
            ImagePlus slice = new ImagePlus(image.getImage().getTitle() + " " + index, ip);
            ImagePlus classified = segmentation.applyClassifier(slice);
            stack.setProcessor(classified.getProcessor(), index.zeroSliceIndexToOneStackIndex(image.getImage()));
        }, progressInfo);

        dataBatch.addOutputData("Classified image", new ImagePlusData(new ImagePlus("Classified", stack)), progressInfo);
    }
}
