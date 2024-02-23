package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.labels;

import ij.ImagePlus;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJAlgorithmUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

@SetJIPipeDocumentation(name = "Extract label statistics 2D", description = "Extracts statistics for all labels in the image. Statistics are extracted over an image (optional). If no image is supplied, the label itself will be used as the image. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(menuPath = "Labels\nMeasure", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Labels", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Image", create = true, optional = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Analyze", aliasName = "Analyze Particles... (labels)")
public class ExtractLabelStatisticsAlgorithm extends JIPipeIteratingAlgorithm {

    private ImageStatisticsSetParameter measurements = new ImageStatisticsSetParameter();

    private boolean measureInPhysicalUnits = true;

    public ExtractLabelStatisticsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ExtractLabelStatisticsAlgorithm(ExtractLabelStatisticsAlgorithm other) {
        super(other);
        this.measurements = new ImageStatisticsSetParameter(other.measurements);
        this.measureInPhysicalUnits = other.measureInPhysicalUnits;
    }

    @SetJIPipeDocumentation(name = "Measurements", description = "The measurements that should be extracted from the labels. Please note that due to technical limitations, some measurements will not work and instead yield measurements over the whole image.")
    @JIPipeParameter(value = "measurements", important = true)
    public ImageStatisticsSetParameter getMeasurements() {
        return measurements;
    }

    @JIPipeParameter("measurements")
    public void setMeasurements(ImageStatisticsSetParameter measurements) {
        this.measurements = measurements;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus labels = iterationStep.getInputData("Labels", ImagePlusGreyscaleData.class, progressInfo).getImage();
        ImagePlus reference;
        if (iterationStep.getInputRow("Image") >= 0) {
            reference = iterationStep.getInputData("Image", ImagePlusGreyscaleData.class, progressInfo).getImage();
        } else {
            reference = labels;
        }
        Calibration calibration = measureInPhysicalUnits ? (reference.getCalibration() != null ? reference.getCalibration() : labels.getCalibration()) : null;

        ResultsTableData result = new ResultsTableData();

        ImageJUtils.forEachIndexedZCTSlice(reference, (referenceProcessor, index) -> {
            int z = Math.min(index.getZ(), labels.getNSlices() - 1);
            int c = Math.min(index.getC(), labels.getNChannels() - 1);
            int t = Math.min(index.getT(), labels.getNFrames() - 1);
            ImageProcessor labelProcessor = ImageJUtils.getSliceZero(labels, c, z, t);
            ResultsTableData forRoi = ImageJAlgorithmUtils.measureLabels(labelProcessor, referenceProcessor, this.measurements, index, calibration, progressInfo);
            result.addRows(forRoi);
        }, progressInfo);

        iterationStep.addOutputData(getFirstOutputSlot(), result, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Measure in physical units", description = "If true, measurements will be generated in physical units if available. The calibration of the reference image is preferred.")
    @JIPipeParameter("measure-in-physical-units")
    public boolean isMeasureInPhysicalUnits() {
        return measureInPhysicalUnits;
    }

    @JIPipeParameter("measure-in-physical-units")
    public void setMeasureInPhysicalUnits(boolean measureInPhysicalUnits) {
        this.measureInPhysicalUnits = measureInPhysicalUnits;
    }
}
