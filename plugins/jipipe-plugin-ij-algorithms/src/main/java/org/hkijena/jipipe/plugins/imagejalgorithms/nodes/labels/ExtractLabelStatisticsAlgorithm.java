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
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.ImageJAlgorithmUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

@SetJIPipeDocumentation(name = "Extract label statistics 2D", description = "Extracts statistics for all labels in the image. Statistics are extracted over an image (optional). If no image is supplied, the label itself will be used as the image. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(menuPath = "Labels\nMeasure", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Labels", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Image", create = true, optional = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Output", create = true)
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

    @SetJIPipeDocumentation(name = "Measurements", description = "The measurements that should be extracted from the labels. " +
            "Please note that due to technical limitations, some measurements will not work and instead yield measurements over the whole image." + "<br/><br/>" + ImageStatisticsSetParameter.ALL_DESCRIPTIONS)
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

        ImageJIterationUtils.forEachIndexedZCTSlice(reference, (referenceProcessor, index) -> {
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
