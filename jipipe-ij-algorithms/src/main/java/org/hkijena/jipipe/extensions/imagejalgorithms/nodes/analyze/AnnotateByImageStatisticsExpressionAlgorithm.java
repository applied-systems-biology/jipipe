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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.analyze;

import com.google.common.primitives.Floats;
import com.google.common.primitives.Longs;
import ij.ImagePlus;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.utils.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.imagejalgorithms.parameters.ImageROITargetArea;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJAlgorithmUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageStatistics5DExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.tables.parameters.collections.ExpressionTableColumnGeneratorProcessorParameterList;
import org.hkijena.jipipe.extensions.tables.parameters.processors.ExpressionTableColumnGeneratorProcessor;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Annotate by image statistics (Expression)", description = "Annotates the incoming images by their statistics. The statistics are created via annotations.")
@JIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "For images")
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Image", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Image", autoCreate = true)
public class AnnotateByImageStatisticsExpressionAlgorithm extends JIPipeIteratingAlgorithm {

    private ImageROITargetArea targetArea = ImageROITargetArea.WholeImage;
    private ExpressionTableColumnGeneratorProcessorParameterList annotations = new ExpressionTableColumnGeneratorProcessorParameterList();

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public AnnotateByImageStatisticsExpressionAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.annotations.setCustomInstanceGenerator(() -> new ExpressionTableColumnGeneratorProcessor("", ""));
        ImageJAlgorithmUtils.updateROIOrMaskSlot(targetArea, getSlotConfiguration());
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public AnnotateByImageStatisticsExpressionAlgorithm(AnnotateByImageStatisticsExpressionAlgorithm other) {
        super(other);
        this.targetArea = other.targetArea;
        this.annotations = new ExpressionTableColumnGeneratorProcessorParameterList(other.annotations);
        this.annotations.setCustomInstanceGenerator(() -> new ExpressionTableColumnGeneratorProcessor("", ""));
        ImageJAlgorithmUtils.updateROIOrMaskSlot(targetArea, getSlotConfiguration());
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus img = dataBatch.getInputData("Image", ImagePlusGreyscaleData.class, progressInfo).getImage();

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
            copy.setC(-1);
            copy.setT(-1);
            copy.setZ(-1);
            return copy;
        }));

        List<Float> pixelsList = new ArrayList<>();
        List<JIPipeTextAnnotation> outputAnnotations = new ArrayList<>();

        int currentIndexBatch = 0;
        ExpressionVariables parameters = new ExpressionVariables();
        parameters.set("width", img.getWidth());
        parameters.set("height", img.getHeight());
        parameters.set("num_z", img.getNSlices());
        parameters.set("num_c", img.getNChannels());
        parameters.set("num_t", img.getNFrames());

        for (JIPipeTextAnnotation annotation : dataBatch.getMergedTextAnnotations().values()) {
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
                ImageProcessor mask = ImageJAlgorithmUtils.getMaskProcessorFromMaskOrROI(targetArea, dataBatch, index, indexProgress);
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

            for (ExpressionTableColumnGeneratorProcessor columnGenerator : annotations) {
                Object expressionResult = columnGenerator.getKey().evaluate(parameters);
                outputAnnotations.add(new JIPipeTextAnnotation(columnGenerator.getValue(), StringUtils.nullToEmpty(expressionResult)));
            }

            ++currentIndexBatch;
        }

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), outputAnnotations, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
    }

    @JIPipeDocumentation(name = "Get statistics from ...", description = "Determines where the algorithm is applied to.")
    @JIPipeParameter("roi:target-area")
    public ImageROITargetArea getTargetArea() {
        return targetArea;
    }

    @JIPipeParameter("roi:target-area")
    public void setTargetArea(ImageROITargetArea targetArea) {
        this.targetArea = targetArea;
        ImageJAlgorithmUtils.updateROIOrMaskSlot(targetArea, getSlotConfiguration());
    }

    @JIPipeDocumentation(name = "Generated annotations", description = "Use these expressions to generate the annotations. The expressions contain statistics, as well as incoming annotations of the current image.")
    @JIPipeParameter(value = "annotations", uiOrder = -30)
    @ExpressionParameterSettings(variableSource = ImageStatistics5DExpressionParameterVariableSource.class)
    public ExpressionTableColumnGeneratorProcessorParameterList getAnnotations() {
        return annotations;
    }

    @JIPipeParameter("annotations")
    public void setAnnotations(ExpressionTableColumnGeneratorProcessorParameterList annotations) {
        this.annotations = annotations;
        this.annotations.setCustomInstanceGenerator(() -> new ExpressionTableColumnGeneratorProcessor("", ""));
    }
}
