package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.labels;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJAlgorithmUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

@JIPipeDocumentation(name = "Extract label statistics 2D", description = "Extracts statistics for all labels in the image. Statistics are extracted over an image (optional). If no image is supplied, the label itself will be used as the image. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeNode(menuPath = "Labels\nMeasure", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Labels", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Image", autoCreate = true, optional = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class ExtractLabelStatisticsAlgorithm extends JIPipeIteratingAlgorithm {

    private ImageStatisticsSetParameter measurements = new ImageStatisticsSetParameter();

    public ExtractLabelStatisticsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ExtractLabelStatisticsAlgorithm(ExtractLabelStatisticsAlgorithm other) {
        super(other);
        this.measurements = new ImageStatisticsSetParameter(other.measurements);
    }

    @JIPipeDocumentation(name = "Measurements", description = "The measurements that should be extracted from the labels. Please note that due to technical limitations, some measurements will not work and instead yield measurements over the whole image.")
    @JIPipeParameter("measurements")
    public ImageStatisticsSetParameter getMeasurements() {
        return measurements;
    }

    @JIPipeParameter("measurements")
    public void setMeasurements(ImageStatisticsSetParameter measurements) {
        this.measurements = measurements;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus labels = dataBatch.getInputData("Labels", ImagePlusGreyscaleData.class, progressInfo).getImage();
        ImagePlus reference;
        if (dataBatch.getInputRow("Image") >= 0) {
            reference = dataBatch.getInputData("Image", ImagePlusGreyscaleData.class, progressInfo).getImage();
        } else {
            reference = labels;
        }

        ResultsTableData result = new ResultsTableData();

        ImageJUtils.forEachIndexedZCTSlice(reference, (referenceProcessor, index) -> {
            int z = Math.min(index.getZ(), labels.getNSlices() - 1);
            int c = Math.min(index.getC(), labels.getNChannels() - 1);
            int t = Math.min(index.getT(), labels.getNFrames() - 1);
            ImageProcessor labelProcessor = ImageJUtils.getSliceZero(labels, c, z, t);
            ResultsTableData forRoi = ImageJAlgorithmUtils.measureLabels(labelProcessor, referenceProcessor, this.measurements, index, progressInfo);
            result.addRows(forRoi);
        }, progressInfo);

        dataBatch.addOutputData(getFirstOutputSlot(), result, progressInfo);
    }
}
