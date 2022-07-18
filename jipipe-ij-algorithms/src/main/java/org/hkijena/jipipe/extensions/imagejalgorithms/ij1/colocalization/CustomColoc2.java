package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.colocalization;

import ij.IJ;
import ij.ImagePlus;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.utils.ReflectionUtils;
import sc.fiji.coloc.Coloc_2;
import sc.fiji.coloc.algorithms.*;
import sc.fiji.coloc.gadgets.DataContainer;
import sc.fiji.coloc.results.AnalysisResults;
import sc.fiji.coloc.results.ResultHandler;
import sc.fiji.coloc.results.Warning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Customized version of {@link Coloc_2} because many properties are protected
 *
 * @param <T> img type
 */
public class CustomColoc2<T extends RealType<T> & NativeType<T>> extends Coloc_2<T> {

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

    public List<ColocResult<T>> runColocalization() {
        List<ColocResult<T>> resultsList = new ArrayList<>();
        try {
            for (final Coloc_2.MaskInfo mi : masks) {
                Object roi = ReflectionUtils.getDeclaredFieldValue("roi", mi); // Why is this internal?
                resultsList.add(customColocalise(img1, img2, (Coloc_2<T>.BoundingBox) roi, mi.mask, null));
            }
        } catch (final MissingPreconditionException e) {
            throw new RuntimeException(e);
        }
        return resultsList;
    }

    private RandomAccessibleInterval<T> project(
            final RandomAccessibleInterval<T> image) {
        if (image.numDimensions() < 2) {
            throw new IllegalArgumentException("Dimensionality too small: " + //
                    image.numDimensions());
        }

        final IterableInterval<T> input = Views.iterable(image);
        final T type = input.firstElement(); // e.g. unsigned 8-bit
        final long xLen = image.dimension(0);
        final long yLen = image.dimension(1);

        // initialize output image with minimum value of the pixel type
        final long[] outputDims = {xLen, yLen};
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
     * @param image1        First image.
     * @param image2        Second image.
     * @param roi           Region of interest to which analysis is confined.
     * @param mask          Mask to which analysis is confined.
     * @param extraHandlers additional objects to be notified of analysis results.
     * @return Data structure housing the results.
     */
    public ColocResult<T> customColocalise(final Img<T> image1, final Img<T> image2,
                                           final BoundingBox roi, final Img<T> mask,
                                           final List<ResultHandler<T>> extraHandlers)
            throws MissingPreconditionException {
        // create a new container for the selected images and channels
        DataContainer<T> container;
        if (mask != null) {
            container = new DataContainer<>(image1, image2, img1Channel, img2Channel,
                    Ch1Name, Ch2Name, mask, roi.offset, roi.size);
        } else if (roi != null) {
            // we have no mask, but a regular ROI in use
            container = new DataContainer<>(image1, image2, img1Channel, img2Channel,
                    Ch1Name, Ch2Name, roi.offset, roi.size);
        } else {
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
            } catch (final MissingPreconditionException e) {
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

        return new ColocResult<>(container, analysisResults);
    }


    public boolean initializeSettings(ImagePlus imp1, ImagePlus imp2, ImagePlus mask, AutoThresholdRegression.Implementation thresholdRegression,
                                      boolean gdDisplayShuffledCostes, boolean gdUseLiCh1,
                                      boolean gdUseLiCh2, boolean gdUseLiICQ, boolean gdUseSpearmanRank, boolean gdUseManders,
                                      boolean gdUseKendallTau, boolean gdUseScatterplot, boolean gdUseCostes, int gdPsf,
                                      int gdNrCostesRandomisations) {
        // get image names for output
        Ch1Name = imp1.getTitle();
        Ch2Name = imp2.getTitle();

        // make sure neither image is RGB type
        if (imp1.getBitDepth() == 24 || imp2.getBitDepth() == 24) {
            IJ.showMessage("You should not use RGB color images to measure colocalization. Provide each channel as a 8-bit or 16-bit image.");
            return false;
        }

        // make sure both images have the same bit-depth
        if (imp1.getBitDepth() != imp2.getBitDepth()) {
            IJ.showMessage("Both images must have the same bit-depth.");
            return false;
        }

        // get information about the mask/ROI to use
        if (mask == null) {
            indexMask = 0;
            roiConfig = RoiConfiguration.None;
            masks.add(new MaskInfo(null, null));
        } else {
            indexMask = 4;
            roiConfig = RoiConfiguration.Mask;
            final ImagePlus maskImp = mask;
            final Img<T> maskImg = ImagePlusAdapter.<T>wrap(maskImp);
            // get a valid mask info for the image
            final MaskInfo mi = getBoundingBoxOfMask(maskImg);
            masks.add(mi);
        }

        // save the ImgLib wrapped images as members
        img1 = ImagePlusAdapter.wrap(imp1);
        img2 = ImagePlusAdapter.wrap(imp2);

        // get information about the mask/ROI to use
        indexRegr = Arrays.asList(AutoThresholdRegression.Implementation.values()).indexOf(thresholdRegression);

        // read out GUI data
        autoSavePdf = false;
        displayImages = false;

        // Parse algorithm options
        pearsonsCorrelation = new PearsonsCorrelation<>(
                PearsonsCorrelation.Implementation.Fast);

        if (gdUseLiCh1) liHistogramCh1 = new LiHistogram2D<>("Li - Ch1", true);
        if (gdUseLiCh2) liHistogramCh2 = new LiHistogram2D<>("Li - Ch2", false);
        if (gdUseLiICQ) liICQ = new LiICQ<>();
        if (gdUseSpearmanRank) {
            SpearmanRankCorrelation = new SpearmanRankCorrelation<>();
        }
        if (gdUseManders) mandersCorrelation = new MandersColocalization<>();
        if (gdUseKendallTau) kendallTau = new KendallTauRankCorrelation<>();
        if (gdUseScatterplot) histogram2D = new Histogram2D<>(
                "2D intensity histogram");
        if (gdUseCostes) {
            costesSignificance = new CostesSignificanceTest<>(pearsonsCorrelation,
                    gdPsf, gdNrCostesRandomisations, gdDisplayShuffledCostes);
        }

        return true;
    }

    public static class ColocResult<T extends RealType<T> & NativeType<T>> {
        private final DataContainer<T> dataContainer;
        private final AnalysisResults<T> analysisResults;

        public ColocResult(DataContainer<T> dataContainer, AnalysisResults<T> analysisResults) {
            this.dataContainer = dataContainer;
            this.analysisResults = analysisResults;
        }

        public DataContainer<T> getDataContainer() {
            return dataContainer;
        }

        public AnalysisResults<T> getAnalysisResults() {
            return analysisResults;
        }
    }
}
