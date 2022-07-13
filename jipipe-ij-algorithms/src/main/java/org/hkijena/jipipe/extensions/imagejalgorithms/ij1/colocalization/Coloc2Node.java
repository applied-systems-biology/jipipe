package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.colocalization;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import org.hkijena.jipipe.api.JIPipeCitation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.IJLogToJIPipeProgressInfoPump;
import org.hkijena.jipipe.utils.ReflectionUtils;
import sc.fiji.coloc.Coloc_2;
import sc.fiji.coloc.algorithms.Algorithm;
import sc.fiji.coloc.algorithms.AutoThresholdRegression;
import sc.fiji.coloc.algorithms.InputCheck;
import sc.fiji.coloc.algorithms.MissingPreconditionException;
import sc.fiji.coloc.gadgets.DataContainer;
import sc.fiji.coloc.results.*;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@JIPipeDocumentation(name = "Coloc 2", description = "Colocalization analysis via the pixel intensity correlation over space methods of Pearson, Manders, Costes, Li and more, " +
        "for scatterplots, analysis, automatic thresholding and statistical significance testing. Coloc 2 does NOT perform object based colocalization measurements, where objects are first segmented from the image, then their spatial relationships like overlap etc. are measured. " +
        "This complementary approach is implemented in many ways elsewhere.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Colocalization")
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Analyze\nColocalization")
@JIPipeCitation("See https://imagej.net/plugins/coloc-2")
@JIPipeCitation("See https://imagej.net/imaging/colocalization-analysis for more information abot colocalization")
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Channel 1", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Channel 2", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Mask", autoCreate = true, optional = true, description = "Optional mask")
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Results", autoCreate = true)

public class Coloc2Node extends JIPipeIteratingAlgorithm {

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

    private String channel1Name = "Ch1";

    private String channel2Name = "Ch2";

    public Coloc2Node(JIPipeNodeInfo info) {
        super(info);
    }

    public Coloc2Node(Coloc2Node other) {
        super(other);
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
        this.channel1Name = other.channel1Name;
        this.channel2Name = other.channel2Name;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus channel1Img = dataBatch.getInputData("Channel 1", ImagePlusGreyscaleData.class, progressInfo).getDuplicateImage();
        ImagePlus channel2Img = dataBatch.getInputData("Channel 2", ImagePlusGreyscaleData.class, progressInfo).getDuplicateImage();
        channel2Img = ImageJUtils.convertToSameTypeIfNeeded(channel2Img, channel1Img, true);
        ImagePlus maskImg = null;
        {
            ImagePlusGreyscaleMaskData data = dataBatch.getInputData("Mask", ImagePlusGreyscaleMaskData.class, progressInfo);
            if(data != null) {
                maskImg = data.getDuplicateImage();
            }
        }

        // Setup coloc2
        CustomColoc2 coloc2 = new CustomColoc2(progressInfo);
        coloc2.initializeSettings(channel1Img, channel2Img, maskImg == null ? 0 : 4, Arrays.asList(AutoThresholdRegression.Implementation.values()).indexOf(thresholdRegression),
                false, false, false, liHistogramChannel1, liHistogramChannel2, liICQ, spearmanRankCorrelation, mandersCorrelation,
                kendallTauRankCorrelation, intensityHistogram2D, costesSignificanceTest, psf, costesRandomizations);
        coloc2.setChannelNames(channel1Name, channel2Name);
        progressInfo.log("Running Coloc 2 ... please wait");
        List<AnalysisResults> list;
        try (IJLogToJIPipeProgressInfoPump pump = new IJLogToJIPipeProgressInfoPump(progressInfo)) {
            list = coloc2.runColocalization();
        }
        progressInfo.log("Coloc 2 finished.");

        // Print warnings
        for (AnalysisResults analysisResults : list) {
            for (Object obj : analysisResults.warnings()) {
                Warning warning = (Warning) obj;
                progressInfo.log("Warning by Coloc2: " + warning.getShortMessage());
                progressInfo.log("Warning by Coloc2: " + warning.getLongMessage());
            }

        }

        // Collect results table
        ResultsTableData resultsTableData = new ResultsTableData();
        for (AnalysisResults analysisResults : list) {
            int row = resultsTableData.addRow();
            for (Object value : analysisResults.values()) {
                ValueResult valueResult = (ValueResult) value;
                resultsTableData.setValueAt(valueResult.isNumber ? valueResult.number : valueResult.value, row, valueResult.name);
            }
        }
        dataBatch.addOutputData("Results",resultsTableData,progressInfo);
    }

    @JIPipeDocumentation(name = "Channel 1 name", description = "The name of the first channel in the results")
    @JIPipeParameter("channel1-name")
    public String getChannel1Name() {
        return channel1Name;
    }

    @JIPipeParameter("channel1-name")
    public void setChannel1Name(String channel1Name) {
        this.channel1Name = channel1Name;
    }

    @JIPipeDocumentation(name = "Channel 2 name", description = "The name of the second channel in the results")
    @JIPipeParameter("channel2-name")
    public String getChannel2Name() {
        return channel2Name;
    }

    @JIPipeParameter("channel2-name")
    public void setChannel2Name(String channel2Name) {
        this.channel2Name = channel2Name;
    }

    @JIPipeDocumentation(name = "Threshold regression")
    @JIPipeParameter("threshold-regression")
    public AutoThresholdRegression.Implementation getThresholdRegression() {
        return thresholdRegression;
    }

    @JIPipeParameter("threshold-regression")
    public void setThresholdRegression(AutoThresholdRegression.Implementation thresholdRegression) {
        this.thresholdRegression = thresholdRegression;
    }

    @JIPipeDocumentation(name = "Li Histogram Channel 1")
    @JIPipeParameter(value = "li-histogram-channel-1", uiOrder = -100)
    public boolean isLiHistogramChannel1() {
        return liHistogramChannel1;
    }

    @JIPipeParameter("li-histogram-channel-1")
    public void setLiHistogramChannel1(boolean liHistogramChannel1) {
        this.liHistogramChannel1 = liHistogramChannel1;
    }

    @JIPipeDocumentation(name = "Li Histogram Channel 2")
    @JIPipeParameter(value = "li-histogram-channel-2", uiOrder = -90)
    public boolean isLiHistogramChannel2() {
        return liHistogramChannel2;
    }

    @JIPipeParameter("li-histogram-channel-2")
    public void setLiHistogramChannel2(boolean liHistogramChannel2) {
        this.liHistogramChannel2 = liHistogramChannel2;
    }

    @JIPipeDocumentation(name = "Li ICQ")
    @JIPipeParameter(value = "li-icq", uiOrder = -80)
    public boolean isLiICQ() {
        return liICQ;
    }

    @JIPipeParameter("li-icq")
    public void setLiICQ(boolean liICQ) {
        this.liICQ = liICQ;
    }

    @JIPipeDocumentation(name = "Spearman's Rank Correlation")
    @JIPipeParameter(value = "spearman-rank-correlation", uiOrder = -70)
    public boolean isSpearmanRankCorrelation() {
        return spearmanRankCorrelation;
    }

    @JIPipeParameter("spearman-rank-correlation")
    public void setSpearmanRankCorrelation(boolean spearmanRankCorrelation) {
        this.spearmanRankCorrelation = spearmanRankCorrelation;
    }

    @JIPipeDocumentation(name = "Manders' Correlation")
    @JIPipeParameter(value = "manders-correlation", uiOrder = -60)
    public boolean isMandersCorrelation() {
        return mandersCorrelation;
    }

    @JIPipeParameter("manders-correlation")
    public void setMandersCorrelation(boolean mandersCorrelation) {
        this.mandersCorrelation = mandersCorrelation;
    }

    @JIPipeDocumentation(name = "Kendall's Tau Rank Correlation")
    @JIPipeParameter(value = "kendall-tau-rank-correlation", uiOrder = -50)
    public boolean isKendallTauRankCorrelation() {
        return kendallTauRankCorrelation;
    }

    @JIPipeParameter("kendall-tau-rank-correlation")
    public void setKendallTauRankCorrelation(boolean kendallTauRankCorrelation) {
        this.kendallTauRankCorrelation = kendallTauRankCorrelation;
    }

    @JIPipeDocumentation(name = "2D Intensity Histogram")
    @JIPipeParameter(value = "2d-intensity-histogram", uiOrder = -40)
    public boolean isIntensityHistogram2D() {
        return intensityHistogram2D;
    }

    @JIPipeParameter("2d-intensity-histogram")
    public void setIntensityHistogram2D(boolean intensityHistogram2D) {
        this.intensityHistogram2D = intensityHistogram2D;
    }

    @JIPipeDocumentation(name = "Costes' Significance Test")
    @JIPipeParameter(value = "costes-significance-test", uiOrder = -30)
    public boolean isCostesSignificanceTest() {
        return costesSignificanceTest;
    }

    @JIPipeParameter("costes-significance-test")
    public void setCostesSignificanceTest(boolean costesSignificanceTest) {
        this.costesSignificanceTest = costesSignificanceTest;
    }

    @JIPipeDocumentation(name = "PSF")
    @JIPipeParameter("psf")
    public int getPsf() {
        return psf;
    }

    @JIPipeParameter("psf")
    public void setPsf(int psf) {
        this.psf = psf;
    }

    @JIPipeDocumentation(name = "Costes randomizations")
    @JIPipeParameter("costes-randomizations")
    public int getCostesRandomizations() {
        return costesRandomizations;
    }

    @JIPipeParameter("costes-randomizations")
    public void setCostesRandomizations(int costesRandomizations) {
        this.costesRandomizations = costesRandomizations;
    }

    public static class CustomColoc2<T extends RealType<T> & NativeType<T>> extends Coloc_2<T> {

        private final JIPipeProgressInfo progressInfo;

        public CustomColoc2(JIPipeProgressInfo progressInfo) {
            this.progressInfo = progressInfo;
        }

        public ArrayList<MaskInfo> getMaskInfos() {
            return masks;
        }

        public void setChannelNames(String ch1Name, String ch2Name) {
            this.Ch1Name = ch1Name;
            this.Ch2Name = ch2Name;
        }

        public List<AnalysisResults<T>> runColocalization() {
            List<AnalysisResults<T>> resultsList = new ArrayList<>();
            try {
                for (final Coloc_2.MaskInfo mi : masks) {
                    Object roi = ReflectionUtils.getDeclaredFieldValue("roi", mi); // Why is this internal?
                    resultsList.add(customColocalise(img1, img2, (Coloc_2<T>.BoundingBox) roi, mi.mask, null));
                }
            }
            catch (final MissingPreconditionException e) {
                throw new RuntimeException(e);
            }
            return resultsList;
        }

        private RandomAccessibleInterval<T> project(
                final RandomAccessibleInterval<T> image)
        {
            if (image.numDimensions() < 2) {
                throw new IllegalArgumentException("Dimensionality too small: " + //
                        image.numDimensions());
            }

            final IterableInterval<T> input = Views.iterable(image);
            final T type = input.firstElement(); // e.g. unsigned 8-bit
            final long xLen = image.dimension(0);
            final long yLen = image.dimension(1);

            // initialize output image with minimum value of the pixel type
            final long[] outputDims = { xLen, yLen };
            final Img<T> output = new ArrayImgFactory<T>().create(outputDims, type);
            for (final T sample : output) {
                sample.setReal(type.getMinValue());
            }

            // loop over the input image, performing the max projection
            final Cursor<T> inPos = input.localizingCursor();
            final RandomAccess<T> outPos = output.randomAccess();
            while (inPos.hasNext()) {
                final T inPix = inPos.next();
                final long xPos = inPos.getLongPosition(0);
                final long yPos = inPos.getLongPosition(1);
                outPos.setPosition(xPos, 0);
                outPos.setPosition(yPos, 1);
                final T outPix = outPos.get();
                if (outPix.compareTo(inPix) < 0) {
                    outPix.set(inPix);
                }
            }
            return output;
        }

        /**
         * Call this method to run a whole colocalisation configuration, all selected
         * algorithms get run on the supplied images. You can specify the data further
         * by supplying appropriate information in the mask structure.
         *
         * @param image1 First image.
         * @param image2 Second image.
         * @param roi Region of interest to which analysis is confined.
         * @param mask Mask to which analysis is confined.
         * @param extraHandlers additional objects to be notified of analysis results.
         * @return Data structure housing the results.
         */
        public AnalysisResults<T> customColocalise(final Img<T> image1, final Img<T> image2,
                                             final BoundingBox roi, final Img<T> mask,
                                             final List<ResultHandler<T>> extraHandlers)
                throws MissingPreconditionException
        {
            // create a new container for the selected images and channels
            DataContainer<T> container;
            if (mask != null) {
                container = new DataContainer<>(image1, image2, img1Channel, img2Channel,
                        Ch1Name, Ch2Name, mask, roi.offset, roi.size);
            }
            else if (roi != null) {
                // we have no mask, but a regular ROI in use
                container = new DataContainer<>(image1, image2, img1Channel, img2Channel,
                        Ch1Name, Ch2Name, roi.offset, roi.size);
            }
            else {
                // no mask and no ROI is present
                container = new DataContainer<>(image1, image2, img1Channel, img2Channel,
                        Ch1Name, Ch2Name);
            }

            // create a results handler
            final List<ResultHandler<T>> listOfResultHandlers =
                    new ArrayList<>();
            final AnalysisResults<T> analysisResults = new AnalysisResults<>();
            listOfResultHandlers.add(analysisResults);
            if (extraHandlers != null) listOfResultHandlers.addAll(extraHandlers);
            // ResultHandler<T> resultHandler = new EasyDisplay<T>(container);

            // this contains the algorithms that will be run when the user clicks ok
            final List<Algorithm<T>> userSelectedJobs = new ArrayList<>();

            // add some pre-processing jobs:
            userSelectedJobs.add(container.setInputCheck(new InputCheck<T>()));
            userSelectedJobs.add(container.setAutoThreshold(
                    new AutoThresholdRegression<>(pearsonsCorrelation,
                            AutoThresholdRegression.Implementation.values()[indexRegr])));

            // add user selected algorithms
            addIfValid(pearsonsCorrelation, userSelectedJobs);
            addIfValid(liHistogramCh1, userSelectedJobs);
            addIfValid(liHistogramCh2, userSelectedJobs);
            addIfValid(liICQ, userSelectedJobs);
            addIfValid(SpearmanRankCorrelation, userSelectedJobs);
            addIfValid(mandersCorrelation, userSelectedJobs);
            addIfValid(kendallTau, userSelectedJobs);
            addIfValid(histogram2D, userSelectedJobs);
            addIfValid(costesSignificance, userSelectedJobs);

            // execute all algorithms
            int count = 0;
            final int jobs = userSelectedJobs.size();
            for (final Algorithm<T> a : userSelectedJobs) {
                try {
                    count++;
                    progressInfo.log(count + "/" + jobs + ": Running " + a.getName());
                    a.execute(container);
                }
                catch (final MissingPreconditionException e) {
                    for (final ResultHandler<T> r : listOfResultHandlers) {
                        r.handleWarning(new Warning("Probem with input data", a.getName() +
                                ": " + e.getMessage()));
                    }
                }
            }

            // let the algorithms feed their results to the handler
            for (final Algorithm<T> a : userSelectedJobs) {
                for (final ResultHandler<T> r : listOfResultHandlers)
                    a.processResults(r);
            }
            // if we have ROIs/masks, add them to results
//            if (displayImages) {
//                swDisplay.displayOriginalImages = true;
//                RandomAccessibleInterval<T> channel1, channel2;
//                if (mask != null || roi != null) {
//                    final long[] offset = container.getMaskBBOffset();
//                    final long[] size = container.getMaskBBSize();
//                    channel1 = createMaskImage(container.getSourceImage1(), //
//                            container.getMask(), offset, size);
//                    channel2 = createMaskImage(container.getSourceImage2(), //
//                            container.getMask(), offset, size);
//                }
//                else {
//                    channel1 = container.getSourceImage1();
//                    channel2 = container.getSourceImage2();
//                }
//                channel1 = project(channel1);
//                channel2 = project(channel2);
//
//                for (final ResultHandler<T> r : listOfResultHandlers) {
//                    r.handleImage(channel1, "Channel 1 (Max Projection)");
//                    r.handleImage(channel2, "Channel 2 (Max Projection)");
//                }
//            }
//            if (swDisplay != null) {
//                // do the actual results processing
//                swDisplay.process();
//                // add window to the IJ window manager
//                swDisplay.addWindowListener(new WindowAdapter() {
//
//                    @Override
//                    public void windowClosing(final WindowEvent e) {
//                        WindowManager.removeWindow(swDisplay);
//                        swDisplay.dispose();
//                        // NB: For some reason, garbage collection of this bundle of objects
//                        // does not occur when this window listener reference remains in
//                        // place. As such, we explicitly unregister ourself here.
//                        swDisplay.removeWindowListener(this);
//                    }
//                });
//                WindowManager.addWindow(swDisplay);
//            }
//
//            // show PDF saving dialog if requested
//            if (autoSavePdf) pdfWriter.process();

            return analysisResults;
        }
    }
}
