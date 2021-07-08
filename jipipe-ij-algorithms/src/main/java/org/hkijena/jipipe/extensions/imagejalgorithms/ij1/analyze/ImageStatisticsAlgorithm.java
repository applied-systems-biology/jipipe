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
 */

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.analyze;

import gnu.trove.list.array.TByteArrayList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TShortArrayList;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.ShortProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJUtils2;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageROITargetArea;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.Measurement;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.NaturalOrderComparator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Extract image statistics", description = "Extracts statistics of the whole image or a masked part. Please note " +
        "that this node will not be able to extract the shape of masked areas. All shape-description features (Centroid, Perimeter, ...) are calculated on the " +
        "rectangle formed by the image dimensions.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Analyze")
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Image", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Measurements", autoCreate = true)
public class ImageStatisticsAlgorithm extends JIPipeIteratingAlgorithm {

    private ImageStatisticsSetParameter measurements = new ImageStatisticsSetParameter();
    private boolean applyPerSlice = true;
    private boolean applyPerChannel = true;
    private boolean applyPerFrame = true;
    private ImageROITargetArea targetArea = ImageROITargetArea.WholeImage;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ImageStatisticsAlgorithm(JIPipeNodeInfo info) {
        super(info);
        ImageJUtils2.updateROIOrMaskSlot(targetArea, getSlotConfiguration());
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public ImageStatisticsAlgorithm(ImageStatisticsAlgorithm other) {
        super(other);
        this.measurements = new ImageStatisticsSetParameter(other.measurements);
        this.applyPerChannel = other.applyPerChannel;
        this.applyPerFrame = other.applyPerFrame;
        this.applyPerSlice = other.applyPerSlice;
        this.targetArea = other.targetArea;
        ImageJUtils2.updateROIOrMaskSlot(targetArea, getSlotConfiguration());
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus img = dataBatch.getInputData("Image", ImagePlusGreyscaleData.class, progressInfo).getImage();

        // Get all indices and group them
        List<ImageSliceIndex> allIndices = new ArrayList<>();
        for (int z = 0; z < img.getNSlices(); z++) {
            for (int c = 0; c < img.getNChannels(); c++) {
                for (int t = 0; t < img.getNFrames(); t++) {
                    ImageSliceIndex index = new ImageSliceIndex(z, c, t);
                    allIndices.add(index);
                }
            }
        }

        Map<ImageSliceIndex, List<ImageSliceIndex>> groupedIndices = allIndices.stream().collect(Collectors.groupingBy(index -> {
            ImageSliceIndex copy = new ImageSliceIndex(index);
            if (!applyPerChannel)
                copy.setC(-1);
            if (!applyPerFrame)
                copy.setT(-1);
            if (!applyPerSlice)
                copy.setZ(-1);
            return copy;
        }));

        TByteArrayList pixels8u = new TByteArrayList();
        TShortArrayList pixels16u = new TShortArrayList();
        TFloatArrayList pixels32f = new TFloatArrayList();

        ResultsTableData resultsTableData = new ResultsTableData();

        int currentIndexBatch = 0;
        for (List<ImageSliceIndex> indices : groupedIndices.values()) {
            JIPipeProgressInfo batchProgress = progressInfo.resolveAndLog("Batch", currentIndexBatch, groupedIndices.size());

            // Ensure the capacity of the pixel buffers
            int requestedCapacity = img.getWidth() * img.getHeight() * indices.size();

            if (img.getBitDepth() == 8) {
                pixels8u.clear();
                pixels8u.ensureCapacity(requestedCapacity);
            } else if (img.getBitDepth() == 16) {
                pixels16u.clear();
                pixels16u.ensureCapacity(requestedCapacity);
            } else if (img.getBitDepth() == 32) {
                pixels32f.clear();
                pixels32f.ensureCapacity(requestedCapacity);
            }

            // Fetch the pixel buffers
            for (ImageSliceIndex index : indices) {
                JIPipeProgressInfo indexProgress = batchProgress.resolveAndLog("Slice " + index);
                ImageProcessor ip = ImageJUtils.getSliceZero(img, index);
                ImageProcessor mask = ImageJUtils2.getMaskProcessorFromMaskOrROI(targetArea, dataBatch, index, indexProgress);
                if (img.getBitDepth() == 8) {
                    ImageJUtils.getMaskedPixels_8U(ip, mask, pixels8u);
                } else if (img.getBitDepth() == 16) {
                    ImageJUtils.getMaskedPixels_16U(ip, mask, pixels16u);
                } else if (img.getBitDepth() == 32) {
                    ImageJUtils.getMaskedPixels_32F(ip, mask, pixels32f);
                }
            }

            // Generate statistics
            ImageStatistics statistics;
            if (img.getBitDepth() == 8) {
                statistics = (new ByteProcessor(pixels8u.size(), 1, pixels8u.toArray())).getStatistics();
            } else if (img.getBitDepth() == 16) {
                statistics = (new ShortProcessor(pixels16u.size(), 1, pixels16u.toArray(), null)).getStatistics();
            } else if (img.getBitDepth() == 32) {
                statistics = (new FloatProcessor(pixels32f.size(), 1, pixels32f.toArray())).getStatistics();
            } else {
                throw new UnsupportedOperationException();
            }

            addStatisticsRow(resultsTableData, statistics, measurements, indices, requestedCapacity, img.getWidth(), img.getHeight());

            ++currentIndexBatch;
        }

        dataBatch.addOutputData(getFirstOutputSlot(), resultsTableData, progressInfo);

    }

    @JIPipeDocumentation(name = "Extracted measurements", description = "Please select which measurements should be extracted. " +
            "Each measurement will be assigned to one or multiple output table columns.<br/><br/>" + ImageStatisticsSetParameter.ALL_DESCRIPTIONS)
    @JIPipeParameter("measurements")
    public ImageStatisticsSetParameter getMeasurements() {
        return measurements;
    }

    @JIPipeParameter("measurements")
    public void setMeasurements(ImageStatisticsSetParameter measurements) {
        this.measurements = measurements;
    }

    @JIPipeDocumentation(name = "Apply per slice", description = "If true, the operation is applied for each Z-slice separately. If false, all Z-slices are put together.")
    @JIPipeParameter("apply-per-slice")
    public boolean isApplyPerSlice() {
        return applyPerSlice;
    }

    @JIPipeParameter("apply-per-slice")
    public void setApplyPerSlice(boolean applyPerSlice) {
        this.applyPerSlice = applyPerSlice;
    }

    @JIPipeDocumentation(name = "Apply per channel", description = "If true, the operation is applied for each channel-slice separately. If false, all channel-slices are put together. " +
            "Please note that 'Channel' does not refer to a pixel channel like Red in RGB.")
    @JIPipeParameter("apply-per-channel")
    public boolean isApplyPerChannel() {
        return applyPerChannel;
    }

    @JIPipeParameter("apply-per-channel")
    public void setApplyPerChannel(boolean applyPerChannel) {
        this.applyPerChannel = applyPerChannel;
    }

    @JIPipeDocumentation(name = "Apply per frame", description = "If true, the operation is applied for each frame separately. If false, all frames are put together.")
    @JIPipeParameter("apply-per-frame")
    public boolean isApplyPerFrame() {
        return applyPerFrame;
    }

    @JIPipeParameter("apply-per-frame")
    public void setApplyPerFrame(boolean applyPerFrame) {
        this.applyPerFrame = applyPerFrame;
    }

    @JIPipeDocumentation(name = "Get statistics from ...", description = "Determines where the algorithm is applied to.")
    @JIPipeParameter("roi:target-area")
    public ImageROITargetArea getTargetArea() {
        return targetArea;
    }

    @JIPipeParameter("roi:target-area")
    public void setTargetArea(ImageROITargetArea targetArea) {
        this.targetArea = targetArea;
        ImageJUtils2.updateROIOrMaskSlot(targetArea, getSlotConfiguration());
    }

    public static void addStatisticsRow(ResultsTableData resultsTableData, ImageStatistics statistics, ImageStatisticsSetParameter measurements, Collection<ImageSliceIndex> slices, int allPixels, int width, int height) {
        resultsTableData.addRow();

        final double perimeter = 2 * width + 2 * height;
        final double major = Math.max(width / 2.0, height / 2.0);
        final double minor = Math.min(width / 2.0, height / 2.0);
        final double area = statistics.area;

        for (Measurement measurement : measurements.getValues()) {
            switch (measurement) {
                case StackPosition:
                    resultsTableData.setLastValue(slices.stream().map(s -> s.getC() + "").sorted(NaturalOrderComparator.INSTANCE).collect(Collectors.joining(", ")), "Ch");
                    resultsTableData.setLastValue(slices.stream().map(s -> s.getZ() + "").sorted(NaturalOrderComparator.INSTANCE).collect(Collectors.joining(", ")), "Slice");
                    resultsTableData.setLastValue(slices.stream().map(s -> s.getT() + "").sorted(NaturalOrderComparator.INSTANCE).collect(Collectors.joining(", ")), "Frame");
                    break;
                case Area:
                    resultsTableData.setLastValue(area, "Area");
                    break;
                case PixelValueMinMax:
                    resultsTableData.setLastValue(statistics.min, "Min");
                    resultsTableData.setLastValue(statistics.max, "Max");
                    break;
                case PixelValueStandardDeviation:
                    resultsTableData.setLastValue(statistics.stdDev, "StdDev");
                    break;
                case Centroid:
                    resultsTableData.setLastValue(width / 2, "X");
                    resultsTableData.setLastValue(height / 2, "Y");
                    break;
                case CenterOfMass:
                    resultsTableData.setLastValue(width / 2, "XM");
                    resultsTableData.setLastValue(height / 2, "YM");
                    break;
                case BoundingRectangle:
                    resultsTableData.setLastValue(0, "BX");
                    resultsTableData.setLastValue(0, "BY");
                    resultsTableData.setLastValue(width, "Width");
                    resultsTableData.setLastValue(height, "Height");
                    break;
                case ShapeDescriptors:
                    resultsTableData.setLastValue(4.0 * Math.PI * ((width * height) / (perimeter * perimeter)), "Circ.");
                    resultsTableData.setLastValue(major / minor, "AR");
                    resultsTableData.setLastValue(4.0 * (width * height) / (Math.PI * major * major), "Round");
                    resultsTableData.setLastValue(1, "Solidity");
                    break;
                case IntegratedDensity:
                    resultsTableData.setLastValue(area * statistics.mean, "IntDen");
                    resultsTableData.setLastValue(statistics.pixelCount * statistics.umean, "RawIntDen");
                    break;
                case PixelValueSkewness:
                    resultsTableData.setLastValue(statistics.skewness, "Skew");
                    break;
                case AreaFraction:
                    resultsTableData.setLastValue(area / allPixels, "%Area");
                    break;
                case PixelValueMean:
                    resultsTableData.setLastValue(statistics.mean, "Mean");
                    break;
                case PixelValueModal:
                    resultsTableData.setLastValue(statistics.dmode, "Mode");
                    break;
                case PixelValueMedian:
                    resultsTableData.setLastValue(statistics.median, "Median");
                    break;
                case PixelValueKurtosis:
                    resultsTableData.setLastValue(statistics.kurtosis, "Kurt");
                    break;
                case FitEllipse:
                    resultsTableData.setLastValue(major, "Major");
                    resultsTableData.setLastValue(minor, "Minor");
                    resultsTableData.setLastValue(0, "Angle");
                    break;
                case FeretDiameter:
                    double hyp = Math.sqrt(width * width + height * height);
                    resultsTableData.setLastValue(hyp, "Feret");
                    resultsTableData.setLastValue(0, "FeretX");
                    resultsTableData.setLastValue(0, "FeretY");
                    resultsTableData.setLastValue(Math.toDegrees(Math.asin(height / hyp)), "FeretAngle");
                    break;
                case Perimeter:
                    resultsTableData.setLastValue(perimeter, "Perim.");
                    break;
            }
        }
    }
}
