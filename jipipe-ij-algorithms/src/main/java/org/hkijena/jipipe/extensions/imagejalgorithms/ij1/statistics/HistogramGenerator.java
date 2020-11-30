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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.statistics;

import gnu.trove.map.TDoubleDoubleMap;
import gnu.trove.map.hash.TDoubleDoubleHashMap;
import ij.measure.ResultsTable;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.Arrays;
import java.util.Collections;

/**
 * Algorithm that generates {@link ResultsTableData} as histogram
 */
@JIPipeDocumentation(name = "Histogram", description = "Generates a histogram of the input image. " +
        "It generates following output columns: <pre>value</pre>, <pre>count</pre>.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Statistics")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class HistogramGenerator extends JIPipeSimpleIteratingAlgorithm {

    private boolean applyPerSlice = false;
    private String sliceAnnotation = "Image index";
    private boolean normalize = false;
    private MultiChannelMode multiChannelMode = MultiChannelMode.AverageIntensity;

    /**
     * Creates a new instance
     *
     * @param info the algorithm info
     */
    public HistogramGenerator(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public HistogramGenerator(HistogramGenerator other) {
        super(other);
        this.applyPerSlice = other.applyPerSlice;
        this.normalize = other.normalize;
        this.multiChannelMode = other.multiChannelMode;
        this.sliceAnnotation = other.sliceAnnotation;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        if (applyPerSlice) {
            ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
            ImageJUtils.forEachIndexedSlice(inputData.getImage(), (imp, index) -> {
                TDoubleDoubleMap histogram;
                if (imp instanceof ColorProcessor) {
                    histogram = getColorHistogram((ColorProcessor) imp);
                } else {
                    histogram = getGreyscaleHistogram(imp);
                }
                if (normalize) {
                    histogram = normalizeHistogram(histogram);
                }
                ResultsTableData resultsTable = toResultsTable(histogram);
                if (!StringUtils.isNullOrEmpty(sliceAnnotation)) {
                    dataBatch.addOutputData(getFirstOutputSlot(), resultsTable,
                            Collections.singletonList(new JIPipeAnnotation(sliceAnnotation, "slice=" + index)), JIPipeAnnotationMergeStrategy.Merge, progressInfo);
                } else {
                    dataBatch.addOutputData(getFirstOutputSlot(), resultsTable, progressInfo);
                }
            }, progressInfo);
        } else {
            final TDoubleDoubleMap histogram = new TDoubleDoubleHashMap();
            ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
            ImageJUtils.forEachSlice(inputData.getImage(), imp -> {
                TDoubleDoubleMap sliceHistogram;
                if (imp instanceof ColorProcessor) {
                    sliceHistogram = getColorHistogram((ColorProcessor) imp);
                } else {
                    sliceHistogram = getGreyscaleHistogram(imp);
                }
                mergeHistograms(histogram, sliceHistogram);
            }, progressInfo);
            if (normalize) {
                TDoubleDoubleMap normalizedHistogram = normalizeHistogram(histogram);
                ResultsTableData resultsTable = toResultsTable(normalizedHistogram);
                dataBatch.addOutputData(getFirstOutputSlot(), resultsTable, progressInfo);
            } else {
                ResultsTableData resultsTable = toResultsTable(histogram);
                dataBatch.addOutputData(getFirstOutputSlot(), resultsTable, progressInfo);
            }
        }
    }

    private void mergeHistograms(TDoubleDoubleMap target, TDoubleDoubleMap source) {
        for (double key : source.keys()) {
            double value = source.get(key);
            target.adjustOrPutValue(key, value, value);
        }
    }

    private TDoubleDoubleMap normalizeHistogram(TDoubleDoubleMap histogram) {
        double max = 0;
        for (double value : histogram.values()) {
            max = Math.max(max, value);
        }
        if (max > 0) {
            TDoubleDoubleMap result = new TDoubleDoubleHashMap();
            for (double key : histogram.keys()) {
                result.put(key, histogram.get(key) / max);
            }
            return result;
        } else {
            return histogram;
        }
    }

    private ResultsTableData toResultsTable(TDoubleDoubleMap histogram) {
        ResultsTable resultsTable = new ResultsTable(histogram.size());
        double[] keys = histogram.keys();
        Arrays.sort(keys);
        int row = 0;
        for (double key : keys) {
            resultsTable.setValue("value", row, key);
            resultsTable.setValue("count", row, histogram.get(key));
            ++row;
        }
        return new ResultsTableData(resultsTable);
    }

    @JIPipeDocumentation(name = "Apply per slice", description = "If higher dimensional data is provided, generate a histogram for each slice. If disabled, " +
            "the histogram is generated for the whole image.")
    @JIPipeParameter("apply-per-slice")
    public boolean isApplyPerSlice() {
        return applyPerSlice;
    }

    @JIPipeParameter("apply-per-slice")
    public void setApplyPerSlice(boolean applyPerSlice) {
        this.applyPerSlice = applyPerSlice;
    }

    @JIPipeDocumentation(name = "Multi-channel mode", description = "Determines how values are calculated if a multi-channel image is provided. " +
            "Multi-channel images are converted to RGB automatically.")
    @JIPipeParameter("multi-channel-mode")
    public MultiChannelMode getMultiChannelMode() {
        return multiChannelMode;
    }

    @JIPipeParameter("multi-channel-mode")
    public void setMultiChannelMode(MultiChannelMode multiChannelMode) {
        this.multiChannelMode = multiChannelMode;
    }

    @JIPipeDocumentation(name = "Normalize", description = "If true, the values are divided by the maximum value")
    @JIPipeParameter("normalize")
    public boolean isNormalize() {
        return normalize;
    }

    @JIPipeParameter("normalize")
    public void setNormalize(boolean normalize) {
        this.normalize = normalize;
    }

    private TDoubleDoubleMap getGreyscaleHistogram(ImageProcessor processor) {
        TDoubleDoubleMap result = new TDoubleDoubleHashMap();
        if (processor instanceof FloatProcessor) {
            for (int i = 0; i < processor.getPixelCount(); ++i) {
                result.adjustOrPutValue(processor.getf(i), 1, 1);
            }
        } else {
            for (int i = 0; i < processor.getPixelCount(); ++i) {
                result.adjustOrPutValue(processor.get(i), 1, 1);
            }
        }
        return result;
    }

    @JIPipeDocumentation(name = "Apply per slice annotation", description = "Optional annotation type that generated for each slice output. " +
            "It contains the string 'slice=[Number]'.")
    @JIPipeParameter("slice-annotation")
    public String getSliceAnnotation() {
        return sliceAnnotation;
    }

    @JIPipeParameter("slice-annotation")
    public void setSliceAnnotation(String sliceAnnotation) {
        this.sliceAnnotation = sliceAnnotation;
    }

    private TDoubleDoubleMap getColorHistogram(ColorProcessor processor) {
        TDoubleDoubleMap result = new TDoubleDoubleHashMap();
        byte[] red = new byte[processor.getWidth() * processor.getHeight()];
        byte[] green = new byte[processor.getWidth() * processor.getHeight()];
        byte[] blue = new byte[processor.getWidth() * processor.getHeight()];
        processor.getRGB(red, green, blue);
        switch (multiChannelMode) {
            case RedChannel: {
                for (byte b : red) {
                    result.adjustOrPutValue(b, 1, 1);
                }
            }
            break;
            case GreenChannel: {
                for (byte b : green) {
                    result.adjustOrPutValue(b, 1, 1);
                }
            }
            break;
            case BlueChannel: {
                for (byte b : blue) {
                    result.adjustOrPutValue(b, 1, 1);
                }
            }
            break;
            case Sum: {
                for (int i = 0; i < red.length; ++i) {
                    int sum = red[i] + green[i] + blue[i];
                    result.adjustOrPutValue(sum, 1, 1);
                }
            }
            break;
            case AverageIntensity: {
                for (int i = 0; i < red.length; ++i) {
                    int avg = (red[i] + green[i] + blue[i]) / 3;
                    result.adjustOrPutValue(avg, 1, 1);
                }
            }
            break;
            case WeightedIntensity: {
                for (int i = 0; i < red.length; ++i) {
                    int w = (int) (0.3 * red[i] + 0.59 * green[i] + 0.11 * blue[i]);
                    result.adjustOrPutValue(w, 1, 1);
                }
            }
            break;
        }
        return result;
    }

    /**
     * Available modes that determine which greyscale value is generated if a multi-channel image is provided
     */
    public enum MultiChannelMode {
        AverageIntensity,
        WeightedIntensity,
        Sum,
        RedChannel,
        GreenChannel,
        BlueChannel
    }
}
