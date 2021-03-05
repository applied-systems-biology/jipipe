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

import com.google.common.primitives.Floats;
import com.google.common.primitives.Longs;
import gnu.trove.list.array.TByteArrayList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TShortArrayList;
import ij.ImagePlus;
import ij.process.*;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageROITargetArea;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageStatistics5DExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.Measurement;
import org.hkijena.jipipe.extensions.parameters.expressions.*;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalStringParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.parameters.collections.ExpressionTableColumnGeneratorProcessorParameterList;
import org.hkijena.jipipe.extensions.tables.parameters.processors.ExpressionTableColumnGeneratorProcessor;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.NaturalOrderComparator;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Extract image statistics (Expression)", description = "Extracts statistics of the whole image or a masked part. The table columns are created via expressions.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Analyze")
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Image", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Measurements", autoCreate = true)
public class ImageStatisticsExpressionAlgorithm extends JIPipeIteratingAlgorithm {

    private boolean applyPerSlice = true;
    private boolean applyPerChannel = true;
    private boolean applyPerFrame = true;
    private ImageROITargetArea targetArea = ImageROITargetArea.WholeImage;
    private ExpressionTableColumnGeneratorProcessorParameterList columns = new ExpressionTableColumnGeneratorProcessorParameterList();

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ImageStatisticsExpressionAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.columns.setCustomInstanceGenerator(() -> new ExpressionTableColumnGeneratorProcessor("",""));
        updateRoiSlot();
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public ImageStatisticsExpressionAlgorithm(ImageStatisticsExpressionAlgorithm other) {
        super(other);
        this.applyPerChannel = other.applyPerChannel;
        this.applyPerFrame = other.applyPerFrame;
        this.applyPerSlice = other.applyPerSlice;
        this.targetArea = other.targetArea;
        this.columns = new ExpressionTableColumnGeneratorProcessorParameterList(other.columns);
        this.columns.setCustomInstanceGenerator(() -> new ExpressionTableColumnGeneratorProcessor("",""));
        updateRoiSlot();
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus img = dataBatch.getInputData("Image", ImagePlusGreyscaleData.class, progressInfo).getImage();

        // Get all indices and group them
        List<ImageSliceIndex> allIndices = new ArrayList<>();
        for (int z = 0; z < img.getNSlices(); z++) {
            for (int c = 0; c < img.getNChannels(); c++) {
                for (int t = 0; t < img.getNFrames(); t++) {
                    ImageSliceIndex index = new ImageSliceIndex(z,c,t);
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

        List<Float> pixelsList = new ArrayList<>();

        ResultsTableData resultsTableData = new ResultsTableData();

        int currentIndexBatch = 0;
        ExpressionParameters parameters = new ExpressionParameters();
        parameters.set("width", img.getWidth());
        parameters.set("height", img.getHeight());
        parameters.set("num_z", img.getNSlices());
        parameters.set("num_c", img.getNChannels());
        parameters.set("num_t", img.getNFrames());

        for (JIPipeAnnotation annotation : dataBatch.getAnnotations().values()) {
            parameters.set(annotation.getName(), annotation.getValue());
        }

        for (List<ImageSliceIndex> indices : groupedIndices.values()) {
            JIPipeProgressInfo batchProgress = progressInfo.resolveAndLog("Batch", currentIndexBatch, groupedIndices.size());

            parameters.set("c", indices.stream().map(ImageSliceIndex::getC).sorted().collect(Collectors.toList()));
            parameters.set("z", indices.stream().map(ImageSliceIndex::getZ).sorted().collect(Collectors.toList()));
            parameters.set("t", indices.stream().map(ImageSliceIndex::getT).sorted().collect(Collectors.toList()));

            pixelsList.clear();

            // Fetch the pixel buffers
            for (ImageSliceIndex index : indices) {
                JIPipeProgressInfo indexProgress = batchProgress.resolveAndLog("Slice " + index);
                ImageProcessor ip = ImageJUtils.getSlice(img, index);
                ImageProcessor mask = getMask(dataBatch, index, indexProgress);
                ImageJUtils.getMaskedPixels_Slow(ip, mask, pixelsList);
            }

            // Generate statistics
            ImageStatistics statistics = (new FloatProcessor(pixelsList.size(), 1, Floats.toArray(pixelsList))).getStatistics();

            // Write statistics to expressions
            parameters.set("stat_histogram", Longs.asList(statistics.getHistogram()));
            parameters.set("stat_area", statistics.area);
            parameters.set("stat_stdev", statistics.stdDev);
            parameters.set("stat_min", statistics.min);
            parameters.set("stat_max", statistics.max);
            parameters.set("stat_mean", statistics.mean);
            parameters.set("stat_mode", statistics.dmode);
            parameters.set("stat_median", statistics.median);
            parameters.set("stat_kurtosis", statistics.kurtosis);
            parameters.set("stat_int_den", statistics.area * statistics.mean);
            parameters.set("stat_raw_int_den", statistics.pixelCount * statistics.umean);
            parameters.set("stat_skewness", statistics.skewness);
            parameters.set("stat_area_fraction", statistics.areaFraction);
            parameters.set("pixels", pixelsList);

            resultsTableData.addRow();
            for (ExpressionTableColumnGeneratorProcessor columnGenerator : columns){
                Object expressionResult = columnGenerator.getKey().evaluate(parameters);
                resultsTableData.setLastValue(expressionResult, columnGenerator.getValue());
            }

            ++currentIndexBatch;
        }

        dataBatch.addOutputData(getFirstOutputSlot(), resultsTableData, progressInfo);

    }

    @JIPipeDocumentation(name = "Load example", description = "Loads example parameters that showcase how to use this algorithm.")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/graduation-cap.png")
    public void setToExample(JIPipeWorkbench parent) {
        if (UIUtils.confirmResetParameters(parent, "Load example")) {
            columns.clear();
            columns.add(new ExpressionTableColumnGeneratorProcessor("HISTOGRAM_THRESHOLD_OTSU(stat_histogram)", "Otsu threshold"));
            columns.add(new ExpressionTableColumnGeneratorProcessor("PERCENTILE(pixels, 30)", "30th percentile"));
            columns.add(new ExpressionTableColumnGeneratorProcessor("(stat_max + stat_min) / 2", "Middle gray"));
            getEventBus().post(new ParameterChangedEvent(this, "columns"));
        }
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
        updateRoiSlot();
    }

    @JIPipeDocumentation(name = "Generated columns", description = "Use these expressions to generate the table columns. The expressions contain statistics, as well as incoming annotations of the current image.")
    @JIPipeParameter(value = "columns", uiOrder = -30)
    @ExpressionParameterSettings(variableSource = ImageStatistics5DExpressionParameterVariableSource.class)
    public ExpressionTableColumnGeneratorProcessorParameterList getColumns() {
        return columns;
    }

    @JIPipeParameter("columns")
    public void setColumns(ExpressionTableColumnGeneratorProcessorParameterList columns) {
        this.columns = columns;
        this.columns.setCustomInstanceGenerator(() -> new ExpressionTableColumnGeneratorProcessor("",""));
    }

    public ImageProcessor getMask(JIPipeDataBatch dataBatch, ImageSliceIndex sliceIndex, JIPipeProgressInfo progressInfo) {
        switch (targetArea) {
            case WholeImage: {
                ImagePlusData img = dataBatch.getInputData("Image", ImagePlusData.class, progressInfo);
                return ImageROITargetArea.createWhiteMask(img.getImage());
            }
            case InsideRoi: {
                ROIListData rois = dataBatch.getInputData("ROI", ROIListData.class, progressInfo);
                ImagePlusData img = dataBatch.getInputData("Image", ImagePlusData.class, progressInfo);
                if (rois.isEmpty()) {
                    return ImageROITargetArea.createWhiteMask(img.getImage());
                } else {
                    return rois.getMaskForSlice(img.getImage().getWidth(), img.getImage().getHeight(),
                            false, true, 0, sliceIndex).getProcessor();
                }
            }
            case OutsideRoi: {
                ROIListData rois = dataBatch.getInputData("ROI", ROIListData.class, progressInfo);
                ImagePlusData img = dataBatch.getInputData("Image", ImagePlusData.class, progressInfo);
                if (rois.isEmpty()) {
                    return ImageROITargetArea.createWhiteMask(img.getImage());
                } else {
                    ImageProcessor processor = rois.getMaskForSlice(img.getImage().getWidth(), img.getImage().getHeight(),
                            false, true, 0, sliceIndex).getProcessor();
                    processor.invert();
                    return processor;
                }
            }
            case InsideMask: {
                ImagePlus mask = dataBatch.getInputData("Mask", ImagePlusData.class, progressInfo).getImage();
                if (mask.getStackSize() > 1) {
                    return mask.getStack().getProcessor(sliceIndex.getStackIndex(mask));
                } else {
                    return mask.getProcessor();
                }
            }
            case OutsideMask: {
                ImagePlus mask = dataBatch.getInputData("Mask", ImagePlusData.class, progressInfo).getImage();
                ImageProcessor processor;
                if (mask.getStackSize() > 1) {
                    processor = mask.getStack().getProcessor(sliceIndex.getStackIndex(mask)).duplicate();
                } else {
                    processor = mask.getProcessor().duplicate();
                }
                processor.invert();
                return processor;
            }
        }
        throw new UnsupportedOperationException();
    }

    private void updateRoiSlot() {
        JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) getSlotConfiguration();
        if (targetArea == ImageROITargetArea.WholeImage) {
            if (slotConfiguration.getInputSlots().containsKey("ROI")) {
                slotConfiguration.removeInputSlot("ROI", false);
            }
            if (slotConfiguration.getInputSlots().containsKey("Mask")) {
                slotConfiguration.removeInputSlot("Mask", false);
            }
        } else if (targetArea == ImageROITargetArea.InsideRoi || targetArea == ImageROITargetArea.OutsideRoi) {
            if (!slotConfiguration.getInputSlots().containsKey("ROI")) {
                slotConfiguration.addSlot("ROI", new JIPipeDataSlotInfo(ROIListData.class, JIPipeSlotType.Input, "ROI"), false);
            }
            if (slotConfiguration.getInputSlots().containsKey("Mask")) {
                slotConfiguration.removeInputSlot("Mask", false);
            }
        } else if (targetArea == ImageROITargetArea.InsideMask || targetArea == ImageROITargetArea.OutsideMask) {
            if (slotConfiguration.getInputSlots().containsKey("ROI")) {
                slotConfiguration.removeInputSlot("ROI", false);
            }
            if (!slotConfiguration.getInputSlots().containsKey("Mask")) {
                slotConfiguration.addSlot("Mask", new JIPipeDataSlotInfo(ImagePlusGreyscaleMaskData.class, JIPipeSlotType.Input, "Mask"), false);
            }
        }
    }
}
