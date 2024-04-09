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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.analyze;

import com.google.common.primitives.Floats;
import com.google.common.primitives.Longs;
import ij.ImagePlus;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.imagejalgorithms.parameters.ImageROITargetArea;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.ImageJAlgorithmUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageStatistics5DExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.plugins.tables.parameters.collections.ExpressionTableColumnGeneratorProcessorParameterList;
import org.hkijena.jipipe.plugins.tables.parameters.processors.ExpressionTableColumnGeneratorProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@SetJIPipeDocumentation(name = "Extract image statistics (Expression)", description = "Extracts statistics of the whole image or a masked part. The table columns are created via expressions.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Analyze")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Image", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Measurements", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Analyze", aliasName = "Measure (custom)")
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
        this.columns.setCustomInstanceGenerator(() -> new ExpressionTableColumnGeneratorProcessor("", ""));
        ImageJAlgorithmUtils.updateROIOrMaskSlot(targetArea, getSlotConfiguration());
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
        this.columns.setCustomInstanceGenerator(() -> new ExpressionTableColumnGeneratorProcessor("", ""));
        ImageJAlgorithmUtils.updateROIOrMaskSlot(targetArea, getSlotConfiguration());
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus img = iterationStep.getInputData("Image", ImagePlusGreyscaleData.class, progressInfo).getImage();

        // Get all indices and group them
        List<ImageSliceIndex> allIndices = new ArrayList<>();
        for (int z = 0; z < img.getNSlices(); z++) {
            for (int c = 0; c < img.getNChannels(); c++) {
                for (int t = 0; t < img.getNFrames(); t++) {
                    ImageSliceIndex index = new ImageSliceIndex(c, z, t);
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
        JIPipeExpressionVariablesMap parameters = new JIPipeExpressionVariablesMap();
        parameters.set("width", img.getWidth());
        parameters.set("height", img.getHeight());
        parameters.set("num_z", img.getNSlices());
        parameters.set("num_c", img.getNChannels());
        parameters.set("num_t", img.getNFrames());

        for (JIPipeTextAnnotation annotation : iterationStep.getMergedTextAnnotations().values()) {
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
                ImageProcessor ip = ImageJUtils.getSliceZero(img, index);
                ImageProcessor mask = ImageJAlgorithmUtils.getMaskProcessorFromMaskOrROI(targetArea, iterationStep, index, indexProgress);
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
            for (ExpressionTableColumnGeneratorProcessor columnGenerator : columns) {
                Object expressionResult = columnGenerator.getKey().evaluate(parameters);
                resultsTableData.setLastValue(expressionResult, columnGenerator.getValue());
            }

            ++currentIndexBatch;
        }

        iterationStep.addOutputData(getFirstOutputSlot(), resultsTableData, progressInfo);

    }

    @SetJIPipeDocumentation(name = "Apply per slice", description = "If true, the operation is applied for each Z-slice separately. If false, all Z-slices are put together.")
    @JIPipeParameter("apply-per-slice")
    public boolean isApplyPerSlice() {
        return applyPerSlice;
    }

    @JIPipeParameter("apply-per-slice")
    public void setApplyPerSlice(boolean applyPerSlice) {
        this.applyPerSlice = applyPerSlice;
    }

    @SetJIPipeDocumentation(name = "Apply per channel", description = "If true, the operation is applied for each channel-slice separately. If false, all channel-slices are put together. " +
            "Please note that 'Channel' does not refer to a pixel channel like Red in RGB.")
    @JIPipeParameter("apply-per-channel")
    public boolean isApplyPerChannel() {
        return applyPerChannel;
    }

    @JIPipeParameter("apply-per-channel")
    public void setApplyPerChannel(boolean applyPerChannel) {
        this.applyPerChannel = applyPerChannel;
    }

    @SetJIPipeDocumentation(name = "Apply per frame", description = "If true, the operation is applied for each frame separately. If false, all frames are put together.")
    @JIPipeParameter("apply-per-frame")
    public boolean isApplyPerFrame() {
        return applyPerFrame;
    }

    @JIPipeParameter("apply-per-frame")
    public void setApplyPerFrame(boolean applyPerFrame) {
        this.applyPerFrame = applyPerFrame;
    }

    @SetJIPipeDocumentation(name = "Get statistics from ...", description = "Determines where the algorithm is applied to.")
    @JIPipeParameter("roi:target-area")
    public ImageROITargetArea getTargetArea() {
        return targetArea;
    }

    @JIPipeParameter("roi:target-area")
    public void setTargetArea(ImageROITargetArea targetArea) {
        if (this.targetArea != targetArea) {
            this.targetArea = targetArea;
            ImageJAlgorithmUtils.updateROIOrMaskSlot(targetArea, getSlotConfiguration());
        }
    }

    @SetJIPipeDocumentation(name = "Generated columns", description = "Use these expressions to generate the table columns. The expressions contain statistics, as well as incoming annotations of the current image.")
    @JIPipeParameter(value = "columns", uiOrder = -30)
    @JIPipeExpressionParameterSettings(variableSource = ImageStatistics5DExpressionParameterVariablesInfo.class)
    public ExpressionTableColumnGeneratorProcessorParameterList getColumns() {
        return columns;
    }

    @JIPipeParameter("columns")
    public void setColumns(ExpressionTableColumnGeneratorProcessorParameterList columns) {
        this.columns = columns;
        this.columns.setCustomInstanceGenerator(() -> new ExpressionTableColumnGeneratorProcessor("", ""));
    }
}
