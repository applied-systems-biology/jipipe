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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.colocalization;

import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.Overlay;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.LongType;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.colors.ColorMap;
import org.hkijena.jipipe.plugins.parameters.library.primitives.BooleanParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.plugins.strings.StringData;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.IJLogToJIPipeProgressInfoPump;
import sc.fiji.coloc.algorithms.AutoThresholdRegression;
import sc.fiji.coloc.algorithms.Histogram2D;
import sc.fiji.coloc.gadgets.DataContainer;
import sc.fiji.coloc.results.AnalysisResults;
import sc.fiji.coloc.results.NamedContainer;
import sc.fiji.coloc.results.ValueResult;
import sc.fiji.coloc.results.Warning;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SetJIPipeDocumentation(name = "Coloc 2", description = "Colocalization analysis via the pixel intensity correlation over space methods of Pearson, Manders, Costes, Li and more, " +
        "for scatterplots, analysis, automatic thresholding and statistical significance testing. Coloc 2 does NOT perform object based colocalization measurements, where objects are first segmented from the image, then their spatial relationships like overlap etc. are measured. " +
        "This complementary approach is implemented in many ways elsewhere.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Colocalization")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Analyze\nColocalization")
@AddJIPipeCitation("See https://imagej.net/plugins/coloc-2")
@AddJIPipeCitation("See https://imagej.net/imaging/colocalization-analysis for more information abot colocalization")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Channel 1", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Channel 2", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, name = "Mask", optional = true, description = "Optional mask")
@AddJIPipeInputSlot(value = ROI2DListData.class, name = "ROI", optional = true, description = "Optional ROI")
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Results", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Plots", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Histograms", create = true)

public class Coloc2Node extends JIPipeIteratingAlgorithm {

    public static final JIPipeDataSlotInfo SLOT_INPUT_MASK = new JIPipeDataSlotInfo(ImagePlusGreyscaleMaskData.class, JIPipeSlotType.Input, "Mask", "Optional mask", true);
    public static final JIPipeDataSlotInfo SLOT_INPUT_ROI = new JIPipeDataSlotInfo(ROI2DListData.class, JIPipeSlotType.Input, "ROI", "Optional ROI", true);
    public static final JIPipeDataSlotInfo SLOT_OUTPUT_WARNINGS = new JIPipeDataSlotInfo(StringData.class, JIPipeSlotType.Output, "Warnings", "Warnings generated by Coloc 2");
    private final Settings settings;
    private boolean inputMasks;
    private boolean outputWarnings;
    private String channel1Name = "Ch1";
    private String channel2Name = "Ch2";
    private OptionalTextAnnotationNameParameter plotNameAnnotation = new OptionalTextAnnotationNameParameter("Name", true);

    private OptionalTextAnnotationNameParameter histogramNameAnnotation = new OptionalTextAnnotationNameParameter("Name", true);

    public Coloc2Node(JIPipeNodeInfo info) {
        super(info);
        this.settings = new Settings();
        setInputMasks(false);
        setOutputWarnings(false);
        registerSubParameter(settings);
    }

    public Coloc2Node(Coloc2Node other) {
        super(other);
        this.channel1Name = other.channel1Name;
        this.channel2Name = other.channel2Name;
        this.setInputMasks(other.inputMasks);
        this.settings = new Settings(other.settings);
        this.plotNameAnnotation = new OptionalTextAnnotationNameParameter(other.plotNameAnnotation);
        this.histogramNameAnnotation = new OptionalTextAnnotationNameParameter(other.histogramNameAnnotation);
        registerSubParameter(settings);
        this.setOutputWarnings(other.outputWarnings);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus channel1Img = iterationStep.getInputData("Channel 1", ImagePlusGreyscaleData.class, progressInfo).getDuplicateImage();
        ImagePlus channel2Img = iterationStep.getInputData("Channel 2", ImagePlusGreyscaleData.class, progressInfo).getDuplicateImage();
        channel2Img = ImageJUtils.convertToSameTypeIfNeeded(channel2Img, channel1Img, true);

        if (!ImageJUtils.imagesHaveSameSize(channel1Img, channel2Img)) {
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new GraphNodeValidationReportContext(this),
                    "Input images do not have the same size!",
                    "All input images in the same batch should have the same width, height, number of slices, number of frames, and number of channels."));
        }

        ImagePlus maskImg = null;
        if (inputMasks) {
            ImagePlusGreyscaleMaskData data = iterationStep.getInputData("Mask", ImagePlusGreyscaleMaskData.class, progressInfo);
            if (data != null) {
                maskImg = data.getDuplicateImage();
            }
        } else {
            ROI2DListData data = iterationStep.getInputData("ROI", ROI2DListData.class, progressInfo);
            if (data != null) {
                data = new ROI2DListData(data);
                data.flatten();
                maskImg = data.toMask(channel1Img, true, false, 1);
            }
        }

        if (maskImg != null) {
            if (!ImageJUtils.imagesHaveSameSize(channel1Img, maskImg)) {
                throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                        new GraphNodeValidationReportContext(this),
                        "Input images do not have the same size!",
                        "All input images in the same batch should have the same width, height, number of slices, number of frames, and number of channels."));
            }
        }

        // Setup coloc2
        CustomColoc2<?> coloc2 = new CustomColoc2(progressInfo);
        coloc2.initializeSettings(channel1Img, channel2Img, maskImg, settings.thresholdRegression,
                false, settings.liHistogramChannel1, settings.liHistogramChannel2, settings.liICQ, settings.spearmanRankCorrelation, settings.mandersCorrelation,
                settings.kendallTauRankCorrelation, settings.intensityHistogram2D, settings.costesSignificanceTest, settings.psf, settings.costesRandomizations);
        coloc2.setChannelNames(channel1Name, channel2Name);
        progressInfo.log("Running Coloc 2 ... please wait");
        List<? extends CustomColoc2.ColocResult<?>> list;
        try (IJLogToJIPipeProgressInfoPump pump = new IJLogToJIPipeProgressInfoPump(progressInfo)) {
            list = coloc2.runColocalization();
        }
        progressInfo.log("Coloc 2 finished.");

        // Print warnings
        StringBuilder warnings = new StringBuilder();
        for (CustomColoc2.ColocResult result : list) {
            AnalysisResults analysisResults = result.getAnalysisResults();
            for (Object obj : analysisResults.warnings()) {
                Warning warning = (Warning) obj;
                progressInfo.log("Warning by Coloc2: " + warning.getShortMessage());
                progressInfo.log("Warning by Coloc2: " + warning.getLongMessage());
                warnings.append(warning.getShortMessage()).append("\n").append(warning.getLongMessage()).append("\n\n");
            }
        }
        if (outputWarnings) {
            iterationStep.addOutputData("Warnings", new StringData(warnings.toString()), progressInfo);
        }

        // Collect results table
        ResultsTableData resultsTableData = new ResultsTableData();
        for (CustomColoc2.ColocResult result : list) {
            AnalysisResults analysisResults = result.getAnalysisResults();
            int row = resultsTableData.addRow();
            for (Object value : analysisResults.values()) {
                ValueResult valueResult = (ValueResult) value;
                resultsTableData.setValueAt(valueResult.isNumber ? valueResult.number : valueResult.value, row, valueResult.name);
            }
        }
        iterationStep.addOutputData("Results", resultsTableData, progressInfo);

        for (CustomColoc2.ColocResult result : list) {

            AnalysisResults analysisResults = result.getAnalysisResults();
            Map<String, Histogram2D> histograms = new HashMap<>();

            // Write histograms
            for (Object o : analysisResults.histograms().entrySet()) {
                Map.Entry entry = (Map.Entry) o;
                RandomAccessibleInterval<LongType> plotImage = (RandomAccessibleInterval<LongType>) entry.getKey();
                Histogram2D histogram2D = (Histogram2D) entry.getValue();

                // We need to search for the name via the plot image because reasons
                String name = "unnamed";
                for (Object obj : analysisResults.images()) {
                    NamedContainer<RandomAccessibleInterval<? extends RealType<?>>> container = (NamedContainer<RandomAccessibleInterval<? extends RealType<?>>>) obj;
                    if (container.getObject() == plotImage) {
                        name = container.toString();
                        break;
                    }
                }

                // Save for later
                histograms.put(name, histogram2D);

                // Histogram to table
                {
                    List<JIPipeTextAnnotation> annotations = new ArrayList<>();
                    histogramNameAnnotation.addAnnotationIfEnabled(annotations, name);
                    ResultsTableData tableData = histogramToTable(histogram2D);
                    iterationStep.addOutputData("Histograms", tableData, annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
                }
            }


            // Write output images that were not handled yet
            for (Object obj : analysisResults.images()) {
                NamedContainer<RandomAccessibleInterval<? extends RealType<?>>> container = (NamedContainer<RandomAccessibleInterval<? extends RealType<?>>>) obj;
                String name = container.toString();
                RandomAccessibleInterval img = container.getObject();
                ImagePlus wrapped = ImageJFunctions.wrap(img, name);

                Histogram2D histogram2D = histograms.get(name);
                if (histogram2D != null) {
                    renderHistogramPlot(histogram2D, img, wrapped, result.getDataContainer());
                }

                List<JIPipeTextAnnotation> annotations = new ArrayList<>();
                plotNameAnnotation.addAnnotationIfEnabled(annotations, name);
                iterationStep.addOutputData("Plots", new ImagePlusData(wrapped), annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
            }
        }
    }

    private void renderHistogramPlot(Histogram2D histogram, RandomAccessibleInterval img, ImagePlus wrapped, DataContainer dataContainer) {
        AutoThresholdRegression autoThreshold = dataContainer.getAutoThreshold();
        if (histogram != null && autoThreshold != null) {
            Overlay overlay = new Overlay();
            drawHistogramLine(overlay, histogram, img, autoThreshold.getAutoThresholdSlope(),
                    autoThreshold.getAutoThresholdIntercept());
            wrapped.setOverlay(overlay);
            ImageJUtils.setLutFromColorMap(wrapped, ColorMap.fire, null);
        }
    }

    protected void drawHistogramLine(Overlay overlay, Histogram2D histogram, RandomAccessibleInterval<? extends RealType<?>> img, double slope,
                                     double intercept) {
        double startX, startY, endX, endY;
        long imgWidth = img.dimension(0);
        long imgHeight = img.dimension(1);
        /*
         * since we want to draw the line over the whole image we can directly
         * use screen coordinates for x values.
         */
        startX = 0.0;
        endX = imgWidth;

        // check if we can get some exta information for drawing
        // get calibrated start y coordinates
        double calibratedStartY = slope * histogram.getXMin() + intercept;
        double calibratedEndY = slope * histogram.getXMax() + intercept;
        // convert calibrated coordinates to screen coordinates
        startY = calibratedStartY * histogram.getYBinWidth();
        endY = calibratedEndY * histogram.getYBinWidth();

        /*
         * since the screen origin is in the top left of the image, we need to
         * x-mirror our line
         */
        startY = (imgHeight - 1) - startY;
        endY = (imgHeight - 1) - endY;
        // create the line ROI and add it to the overlay
        Line lineROI = new Line(startX, startY, endX, endY);
        /*
         * Set drawing width of line to one, in case it has been changed
         * globally.
         */
        lineROI.setStrokeWidth(1.0f);
        lineROI.setStrokeColor(Color.WHITE);
        overlay.add(lineROI);
    }

    private ResultsTableData histogramToTable(Histogram2D histogram2D) {
        double xBinWidth = 1.0 / histogram2D.getXBinWidth();
        double yBinWidth = 1.0 / histogram2D.getYBinWidth();
        double xMin = histogram2D.getXMin();
        double yMin = histogram2D.getYMin();
        // check if we have bins of size one or other ones
        boolean xBinWidthIsOne = Math.abs(xBinWidth - 1.0) < 0.00001;
        boolean yBinWidthIsOne = Math.abs(yBinWidth - 1.0) < 0.00001;
        // configure decimal places accordingly
        int xDecimalPlaces = xBinWidthIsOne ? 0 : 3;
        int yDecimalPlaces = yBinWidthIsOne ? 0 : 3;
        // create a cursor to access the histogram data
        RandomAccessibleInterval plotImage = histogram2D.getPlotImage();
        RandomAccess<LongType> cursor = plotImage.randomAccess();
        // loop over 2D histogram

        String vHeadingX = xBinWidthIsOne ? "X value" : "X bin start";
        String vHeadingY = yBinWidthIsOne ? "Y value" : "Y bin start";

        ResultsTableData resultsTableData = new ResultsTableData();
        int columnHeadingX = resultsTableData.addColumn(vHeadingX, false);
        int columnHeadingY = resultsTableData.addColumn(vHeadingY, false);
        int columnCount = resultsTableData.addColumn("count", false);


        for (int i = 0; i < plotImage.dimension(0); ++i) {
            for (int j = 0; j < plotImage.dimension(1); ++j) {
                cursor.setPosition(i, 0);
                cursor.setPosition(j, 1);
                int row = resultsTableData.addRow();
                resultsTableData.setValueAt(xMin + (i * xBinWidth), row, columnHeadingX);
                resultsTableData.setValueAt(yMin + (j * yBinWidth), row, columnHeadingY);
                resultsTableData.setValueAt(cursor.get().getRealDouble(), row, columnCount);
            }
        }

        return resultsTableData;
    }

    @SetJIPipeDocumentation(name = "Annotate histograms with name", description = "If enabled, generated histograms are annotated by their name.")
    @JIPipeParameter("histogram-name-annotation")
    public OptionalTextAnnotationNameParameter getHistogramNameAnnotation() {
        return histogramNameAnnotation;
    }

    @JIPipeParameter("histogram-name-annotation")
    public void setHistogramNameAnnotation(OptionalTextAnnotationNameParameter histogramNameAnnotation) {
        this.histogramNameAnnotation = histogramNameAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate plots with name", description = "If enabled, generated plots are annotated by their name.")
    @JIPipeParameter("plot-name-annotation")
    public OptionalTextAnnotationNameParameter getPlotNameAnnotation() {
        return plotNameAnnotation;
    }

    @JIPipeParameter("plot-name-annotation")
    public void setPlotNameAnnotation(OptionalTextAnnotationNameParameter plotNameAnnotation) {
        this.plotNameAnnotation = plotNameAnnotation;
    }

    @SetJIPipeDocumentation(name = "Output warnings", description = "If enabled, Coloc 2 warnings will be generated as text data")
    @JIPipeParameter("output-warnings")
    public boolean isOutputWarnings() {
        return outputWarnings;
    }

    @JIPipeParameter("output-warnings")
    public void setOutputWarnings(boolean outputWarnings) {
        this.outputWarnings = outputWarnings;
        toggleSlot(SLOT_OUTPUT_WARNINGS, outputWarnings);
    }

    @SetJIPipeDocumentation(name = "Colocalization settings")
    @JIPipeParameter("coloc2-settings")
    public Settings getSettings() {
        return settings;
    }

    @SetJIPipeDocumentation(name = "Restrict to 2D ROI/mask", description = "Allows to change whether to restrict the calculations to a 2D ROI or to a mask")
    @JIPipeParameter("input-masks")
    @BooleanParameterSettings(comboBoxStyle = true, trueLabel = "Mask", falseLabel = "ROI")
    public boolean isInputMasks() {
        return inputMasks;
    }

    @JIPipeParameter("input-masks")
    public void setInputMasks(boolean inputMasks) {
        this.inputMasks = inputMasks;
        toggleSlot(SLOT_INPUT_MASK, inputMasks);
        toggleSlot(SLOT_INPUT_ROI, !inputMasks);
    }

    @SetJIPipeDocumentation(name = "Channel 1 name", description = "The name of the first channel in the results")
    @JIPipeParameter("channel1-name")
    public String getChannel1Name() {
        return channel1Name;
    }

    @JIPipeParameter("channel1-name")
    public void setChannel1Name(String channel1Name) {
        this.channel1Name = channel1Name;
    }

    @SetJIPipeDocumentation(name = "Channel 2 name", description = "The name of the second channel in the results")
    @JIPipeParameter("channel2-name")
    public String getChannel2Name() {
        return channel2Name;
    }

    @JIPipeParameter("channel2-name")
    public void setChannel2Name(String channel2Name) {
        this.channel2Name = channel2Name;
    }

    public static class Settings extends AbstractJIPipeParameterCollection {

        private AutoThresholdRegression.Implementation thresholdRegression = AutoThresholdRegression.Implementation.Costes;
        private boolean liHistogramChannel1 = true;
        private boolean liHistogramChannel2 = true;
        private boolean liICQ = true;
        private boolean spearmanRankCorrelation = true;
        private boolean mandersCorrelation = true;
        private boolean kendallTauRankCorrelation = true;
        private boolean intensityHistogram2D = true;
        private boolean costesSignificanceTest = true;
        private int psf = 3;
        private int costesRandomizations = 10;

        public Settings() {
        }

        public Settings(Settings other) {
            this.thresholdRegression = other.thresholdRegression;
            this.liHistogramChannel1 = other.liHistogramChannel1;
            this.liHistogramChannel2 = other.liHistogramChannel2;
            this.liICQ = other.liICQ;
            this.spearmanRankCorrelation = other.spearmanRankCorrelation;
            this.mandersCorrelation = other.mandersCorrelation;
            this.kendallTauRankCorrelation = other.kendallTauRankCorrelation;
            this.intensityHistogram2D = other.intensityHistogram2D;
            this.costesSignificanceTest = other.costesSignificanceTest;
            this.psf = other.psf;
            this.costesRandomizations = other.costesRandomizations;
        }

        @SetJIPipeDocumentation(name = "Threshold regression")
        @JIPipeParameter("threshold-regression")
        public AutoThresholdRegression.Implementation getThresholdRegression() {
            return thresholdRegression;
        }

        @JIPipeParameter("threshold-regression")
        public void setThresholdRegression(AutoThresholdRegression.Implementation thresholdRegression) {
            this.thresholdRegression = thresholdRegression;
        }

        @SetJIPipeDocumentation(name = "Li Histogram Channel 1")
        @JIPipeParameter(value = "li-histogram-channel-1", uiOrder = -100)
        public boolean isLiHistogramChannel1() {
            return liHistogramChannel1;
        }

        @JIPipeParameter("li-histogram-channel-1")
        public void setLiHistogramChannel1(boolean liHistogramChannel1) {
            this.liHistogramChannel1 = liHistogramChannel1;
        }

        @SetJIPipeDocumentation(name = "Li Histogram Channel 2")
        @JIPipeParameter(value = "li-histogram-channel-2", uiOrder = -90)
        public boolean isLiHistogramChannel2() {
            return liHistogramChannel2;
        }

        @JIPipeParameter("li-histogram-channel-2")
        public void setLiHistogramChannel2(boolean liHistogramChannel2) {
            this.liHistogramChannel2 = liHistogramChannel2;
        }

        @SetJIPipeDocumentation(name = "Li ICQ")
        @JIPipeParameter(value = "li-icq", uiOrder = -80)
        public boolean isLiICQ() {
            return liICQ;
        }

        @JIPipeParameter("li-icq")
        public void setLiICQ(boolean liICQ) {
            this.liICQ = liICQ;
        }

        @SetJIPipeDocumentation(name = "Spearman's Rank Correlation")
        @JIPipeParameter(value = "spearman-rank-correlation", uiOrder = -70)
        public boolean isSpearmanRankCorrelation() {
            return spearmanRankCorrelation;
        }

        @JIPipeParameter("spearman-rank-correlation")
        public void setSpearmanRankCorrelation(boolean spearmanRankCorrelation) {
            this.spearmanRankCorrelation = spearmanRankCorrelation;
        }

        @SetJIPipeDocumentation(name = "Manders' Correlation")
        @JIPipeParameter(value = "manders-correlation", uiOrder = -60)
        public boolean isMandersCorrelation() {
            return mandersCorrelation;
        }

        @JIPipeParameter("manders-correlation")
        public void setMandersCorrelation(boolean mandersCorrelation) {
            this.mandersCorrelation = mandersCorrelation;
        }

        @SetJIPipeDocumentation(name = "Kendall's Tau Rank Correlation")
        @JIPipeParameter(value = "kendall-tau-rank-correlation", uiOrder = -50)
        public boolean isKendallTauRankCorrelation() {
            return kendallTauRankCorrelation;
        }

        @JIPipeParameter("kendall-tau-rank-correlation")
        public void setKendallTauRankCorrelation(boolean kendallTauRankCorrelation) {
            this.kendallTauRankCorrelation = kendallTauRankCorrelation;
        }

        @SetJIPipeDocumentation(name = "2D Intensity Histogram")
        @JIPipeParameter(value = "2d-intensity-histogram", uiOrder = -40)
        public boolean isIntensityHistogram2D() {
            return intensityHistogram2D;
        }

        @JIPipeParameter("2d-intensity-histogram")
        public void setIntensityHistogram2D(boolean intensityHistogram2D) {
            this.intensityHistogram2D = intensityHistogram2D;
        }

        @SetJIPipeDocumentation(name = "Costes' Significance Test")
        @JIPipeParameter(value = "costes-significance-test", uiOrder = -30)
        public boolean isCostesSignificanceTest() {
            return costesSignificanceTest;
        }

        @JIPipeParameter("costes-significance-test")
        public void setCostesSignificanceTest(boolean costesSignificanceTest) {
            this.costesSignificanceTest = costesSignificanceTest;
        }

        @SetJIPipeDocumentation(name = "PSF")
        @JIPipeParameter("psf")
        public int getPsf() {
            return psf;
        }

        @JIPipeParameter("psf")
        public void setPsf(int psf) {
            this.psf = psf;
        }

        @SetJIPipeDocumentation(name = "Costes randomizations")
        @JIPipeParameter("costes-randomizations")
        public int getCostesRandomizations() {
            return costesRandomizations;
        }

        @JIPipeParameter("costes-randomizations")
        public void setCostesRandomizations(int costesRandomizations) {
            this.costesRandomizations = costesRandomizations;
        }
    }

}
