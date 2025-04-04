package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.enhance;

import histogram2.HistogramMatcher;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.measure.CurveFitter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.OptionalJIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageSliceIndex;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.expressions.Image5DExpressionParameterVariablesInfo2;

import java.util.HashMap;
import java.util.Map;

@SetJIPipeDocumentation(name = "Bleach correction", description = "Applies bleach correction for 2D or 3D time series. Applies bleach correction per channel.")
@ConfigureJIPipeNode(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Enhance")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Input", create = true)
@AddJIPipeInputSlot(value = ROI2DListData.class, name = "ROI", create = true, optional = true, description = "The ROI where the correction should be applied. If not provided, the whole image is corrected.")
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Output", create = true)
@AddJIPipeCitation("Also cite 10.12688/f1000research.27171.1")
public class BleachCorrectionAlgorithm extends JIPipeIteratingAlgorithm {

    private double simpleRatioBaseLine = 0;
    private Method method = Method.SimpleRatio;
    private OptionalJIPipeExpressionParameter channelFilter = new OptionalJIPipeExpressionParameter(false, "c IN ARRAY(0, 1, 2)");

    public BleachCorrectionAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public BleachCorrectionAlgorithm(BleachCorrectionAlgorithm other) {
        super(other);
        this.simpleRatioBaseLine = other.simpleRatioBaseLine;
        this.method = other.method;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus originalImage = iterationStep.getInputData("Input", ImagePlusGreyscaleData.class, progressInfo).getImage();
        ROI2DListData inputRoi = iterationStep.getInputData("ROI", ROI2DListData.class, progressInfo);
        Roi curROI = null;
        if (inputRoi != null) {
            ROI2DListData tmp = new ROI2DListData();
            tmp.addAll(inputRoi);
            if (tmp.size() > 1) {
                tmp.logicalOr();
            }
            curROI = tmp.get(0);
        }

        // Convert image
        ImagePlus greyscaleImage;
        if (originalImage.getType() == ImagePlus.GRAY32) {
            progressInfo.log("Converting image from 32-bit to 16-bit ...");
            greyscaleImage = ImageJUtils.convertToGrayscale16UIfNeeded(originalImage);
        } else {
            greyscaleImage = ImageJUtils.duplicate(originalImage);
        }

        JIPipeExpressionVariablesMap variablesMap = new JIPipeExpressionVariablesMap();
        Image5DExpressionParameterVariablesInfo2.writeToVariables(originalImage, variablesMap);

        Map<ImageSliceIndex, ImageProcessor> resultMap = new HashMap<>();
        Roi finalCurROI = curROI;
        ImageJIterationUtils.forEachIndexedCHyperStack(greyscaleImage, (imp, index, channelProgress) -> {
            if (!channelFilter.isEnabled() || channelFilter.getContent().test(variablesMap)) {
                switch (method) {
                    case SimpleRatio:
                        copyChannelToResultsMap(correctBleachSimpleRatio(imp, finalCurROI, simpleRatioBaseLine, channelProgress), index, resultMap);
                        break;
                    case ExponentialFit:
                        copyChannelToResultsMap(correctBleachExponentialFit(imp, finalCurROI, channelProgress), index, resultMap);
                        break;
                    case HistogramMatching:
                        copyChannelToResultsMap(correctBleachHistogramMatching(imp, finalCurROI, channelProgress), index, resultMap);
                        break;
                    default:
                        throw new UnsupportedOperationException("Unknown method " + method);
                }
            } else {
                channelProgress.log("Channel will not be processed.");
                copyChannelToResultsMap(imp, index, resultMap);
            }
        }, progressInfo);

        ImagePlus resultImg = ImageJUtils.mergeMappedSlices(resultMap);
        ImageJUtils.copyAttributes(originalImage, resultImg);
        ImageJUtils.copyLUTs(originalImage, resultImg);

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(resultImg), progressInfo);
    }

    private static void copyChannelToResultsMap(ImagePlus imp, ImageSliceIndex index, Map<ImageSliceIndex, ImageProcessor> resultMap) {
        for (int t = 0; t < imp.getNFrames(); t++) {
            for (int z = 0; z < imp.getNSlices(); z++) {
                resultMap.put(new ImageSliceIndex(index.getC(), z, t), ImageJUtils.getSliceZero(imp, 0, z, t));
            }
        }
    }

    @SetJIPipeDocumentation(name = "Background intensity", description = "The background intensity for the simple ratio correction method")
    @JIPipeParameter("simple-ratio-baseline")
    public double getSimpleRatioBaseLine() {
        return simpleRatioBaseLine;
    }

    @JIPipeParameter("simple-ratio-baseline")
    public void setSimpleRatioBaseLine(double simpleRatioBaseLine) {
        this.simpleRatioBaseLine = simpleRatioBaseLine;
    }

    @SetJIPipeDocumentation(name = "Method", description = "The correction method that should be used. " +
            "<ul>" +
            "<li>SimpleRatio: </li>" +
            "<li>HistogramMatching: Samples the histogram of initial frame, and for the successive frames, histograms are matched to the first frame. This avoids the increase in noise in the latter part of the sequence.</li>" +
            "<li>ExponentialFit: Fit the mean intensity time series of given image in this class. fit equation is 11, parameter from http://rsb.info.nih.gov/ij/developer/api/constant-values.html#ij.measure.CurveFitter.STRAIGHT_LINE</li>" +
            "</ul>")
    @JIPipeParameter("method")
    public Method getMethod() {
        return method;
    }

    @JIPipeParameter("method")
    public void setMethod(Method method) {
        this.method = method;
    }

    @SetJIPipeDocumentation(name = "Filter channels", description = "If enabled, allows to limit the bleach correction to specific channels")
    @JIPipeParameter("channel-filter")
    @AddJIPipeExpressionParameterVariable(fromClass = Image5DExpressionParameterVariablesInfo2.class)
    @AddJIPipeExpressionParameterVariable(key = "channel", name = "Current channel", description = "The currently processed channel")
    @JIPipeExpressionParameterSettings(hint = "per channel")
    public OptionalJIPipeExpressionParameter getChannelFilter() {
        return channelFilter;
    }

    @JIPipeParameter("channel-filter")
    public void setChannelFilter(OptionalJIPipeExpressionParameter channelFilter) {
        this.channelFilter = channelFilter;
    }

    public enum Method {
        SimpleRatio,
        ExponentialFit,
        HistogramMatching
    }

    /**
     * Fit the mean intensity time series of given ImagePlus in this class.
     * fit equation is 11, parameter from
     * <a href="http://rsb.info.nih.gov/ij/developer/api/constant-values.html#ij.measure.CurveFitter.STRAIGHT_LINE">...</a>
     *
     * @return an instance of CurveFitter
     */
    private static CurveFitter dcayFitting(ImagePlus imp, Roi curROI, JIPipeProgressInfo progressInfo) {
        ImageProcessor curip;
        ImageStatistics imgstat;
        double[] xA = new double[imp.getStackSize()];
        double[] yA = new double[imp.getStackSize()];

        if (curROI == null)
            curROI = new Roi(0, 0, imp.getWidth(), imp.getHeight());

        for (int i = 0; i < imp.getStackSize(); i++) {
            curip = imp.getImageStack().getProcessor(i + 1);
            curip.setRoi(curROI);
            imgstat = curip.getStatistics();
            xA[i] = i;
            yA[i] = imgstat.mean;
        }
        CurveFitter cf = new CurveFitter(xA, yA);
        double firstframeint = yA[0];
        double lastframeint = yA[yA.length - 1];
        double guess_a = firstframeint - lastframeint;
        if (guess_a <= 0) {
            IJ.error("This sequence seems to be not decaying");
            return null;
        }
        double guess_c = lastframeint;
        double maxiteration = 2000;
        double NumRestarts = 2;
        double errotTol = 10;
        double[] fitparam = {-1 * guess_a, -0.0001, guess_c, maxiteration, NumRestarts, errotTol};

        cf.setInitialParameters(fitparam);
        cf.doFit(11);

//        if ((!GraphicsEnvironment.isHeadless()) && (!doHeadLess)){
//            Fitter.plot(cf);
//        }
//        progressInfo.log(cf.getResultString());
        progressInfo.log(cf.getResultString());
        return cf;
    }

    /**
     * Curve fitting for 3D time series is done with average intensity value for
     * wach time point (stack intensity mean is used, so the fitted points = time
     * point, not slice number)
     *
     * @param zframes
     * @param tframes
     * @param imp
     * @param curROI
     * @param progressInfo
     * @return
     */
    private static CurveFitter decayFitting3D(int zframes, int tframes, ImagePlus imp, Roi curROI, JIPipeProgressInfo progressInfo) {
        ImageProcessor curip;
        ImageStatistics imgstat;
        double[] xA = new double[tframes];
        double[] yA = new double[tframes];
        double curStackMean = 0.0;
        if (curROI == null)
            curROI = new Roi(0, 0, imp.getWidth(), imp.getHeight());
        for (int i = 0; i < tframes; i++) {
            curStackMean = 0.0;
            for (int j = 0; j < zframes; j++) {
                curip = imp.getImageStack().getProcessor(i * zframes + j + 1);
                curip.setRoi(curROI);
                imgstat = curip.getStatistics();
                curStackMean += imgstat.mean;
            }
            curStackMean /= zframes;
            xA[i] = i;
            yA[i] = curStackMean;
        }
        CurveFitter cf = new CurveFitter(xA, yA);
        double firstframeint = yA[0];
        double lastframeint = yA[yA.length - 1];
        double guess_a = firstframeint - lastframeint;
        if (guess_a <= 0) {
            throw new RuntimeException("This sequence seems to be not decaying");
        }
        double guess_c = lastframeint;
        double maxiteration = 2000;
        double NumRestarts = 2;
        double errotTol = 10;
        double[] fitparam = {-1 * guess_a, -0.0001, guess_c, maxiteration, NumRestarts, errotTol};

        cf.setInitialParameters(fitparam);

        cf.doFit(11); //
//        if ((!GraphicsEnvironment.isHeadless()) || (doHeadLess != true)) {
//            Fitter.plot(cf);
//        }
        progressInfo.log(cf.getResultString());
        return cf;
    }

    /**
     * calculate estimated value from fitted "Exponential with Offset" equation
     *
     * @param a magnitude (difference between max and min of curve)
     * @param b exponent, defines degree of decay
     * @param c offset.
     * @param x timepoints (or time frame number)
     * @return estimate of intensity at x
     */
    private static double calcExponentialOffset(double a, double b, double c, double x) {
        return (a * Math.exp(-b * x) + c);
    }

    public static ImagePlus correctBleachExponentialFit(ImagePlus imp, Roi curROI, JIPipeProgressInfo progressInfo) {
        final boolean verbose = true;
        int[] impdimA = imp.getDimensions();
        progressInfo.log("slices" + Integer.toString(impdimA[3]) + "  -- frames" + Integer.toString(impdimA[4]));
        // progressInfo.log(Integer.toString(imp.getNChannels())+":"+Integer.toString(imp.getNSlices())+":"+
        // Integer.toString(imp.getNFrames()));
        int zframes = impdimA[3];
        int tframes = impdimA[4];
        boolean is3DT = false;
        if (impdimA[3] > 1 && impdimA[4] > 1) { // if slices and frames are both more than 1
            is3DT = true;
            if ((impdimA[3] * impdimA[4]) != imp.getStackSize()) {
                throw new RuntimeException("slice and time frames do not match with the length of the stack. Please correct!");
            }
        }
        CurveFitter cf;
        if (is3DT)
            cf = decayFitting3D(zframes, tframes, imp, curROI, progressInfo);
        else
            cf = dcayFitting(imp, curROI, progressInfo);
        double[] respara = cf.getParams();
        double res_a = respara[0];
        double res_b = respara[1];
        double res_c = respara[2];
        double ratio = 0.0;
        ImageProcessor curip;
        progressInfo.log(res_a + "," + res_b + "," + res_c);
        if (is3DT) {
            for (int i = 0; i < tframes; i++) {
                for (int j = 0; j < zframes; j++) {
                    curip = imp.getImageStack().getProcessor(i * zframes + j + 1);
                    ratio = calcExponentialOffset(res_a, res_b, res_c, 0.0)
                            / calcExponentialOffset(res_a, res_b, res_c, (double) (i));
                    curip.multiply(ratio);
                }
            }
        } else {
            progressInfo.log("Original Int" + "\t" + "Corrected Int" + "\t" + "Ratio");

            for (int i = 0; i < imp.getStackSize(); i++) {
                curip = imp.getImageStack().getProcessor(i + 1);

                double orgint = curip.getStatistics().mean;

                ratio = calcExponentialOffset(res_a, res_b, res_c, 0.0)
                        / calcExponentialOffset(res_a, res_b, res_c, (double) (i));
                curip.multiply(ratio);

                double corint = curip.getStatistics().mean;

                //for testing
                String monitor = orgint + "\t" + corint + "\t" +
                        ratio;
                progressInfo.log(monitor);
            }
        }

        return imp;
    }

    public static ImagePlus correctBleachHistogramMatching(ImagePlus imp, Roi curROI, JIPipeProgressInfo progressInfo) {

        int histbinnum = 0;
        if (imp.getBitDepth() == 8)
            histbinnum = 256;
        else if (imp.getBitDepth() == 16)
            histbinnum = 65536;// 65535;

        boolean is3DT = false;
        int zframes = 1;
        int timeframes = 1;
        int[] impdimA = imp.getDimensions();
        progressInfo.log("slices" + Integer.toString(impdimA[3]) + "  -- frames" + Integer.toString(impdimA[4]));
        // progressInfo.log(Integer.toString(imp.getNChannels())+":"+Integer.toString(imp.getNSlices())+":"+
        // Integer.toString(imp.getNFrames()));
        if (impdimA[3] > 1 && impdimA[4] > 1) { // if slices and frames are both more than 1
            is3DT = true;
            zframes = impdimA[3];
            timeframes = impdimA[4];
            if ((zframes * timeframes) != imp.getStackSize()) {
                throw new RuntimeException("slice and time frames do not match with the length of the stack. Please correct!");
            }
        }

        ImageStack stack = imp.getStack();
        ImageProcessor ipA = null;
        ImageProcessor ipB = null;
        HistogramMatcher m = new HistogramMatcher();
        int[] hA = new int[histbinnum];
        int[] hB = new int[histbinnum];
        int[] F = new int[histbinnum];
        int[] histB = null; // for each slice
        int[] histA = null;
        // progressInfo.log(Integer.toString(stack.getSize()));
        int i = 0;
        int j = 0;
        int k = 0;
        /*
         * in case of 3D, stack histogram of the first time point is measured, and then
         * this stack histogram is used as reference (hB) for the rest of time points.
         */
        if (is3DT) {
            // should implement here,
            for (i = 0; i < timeframes; i++) {
                if (i == 0) {
                    for (j = 0; j < zframes; j++) {
                        ipB = stack.getProcessor(i * zframes + j + 1);
                        histB = ipB.getHistogram();
                        for (k = 0; k < histbinnum; k++)
                            hB[k] += histB[k];
                    }
                } else {
                    for (k = 0; k < histbinnum; k++)
                        hA[k] = 0;
                    for (j = 0; j < zframes; j++) {
                        ipA = stack.getProcessor(i * zframes + j + 1);
                        histA = ipA.getHistogram();
                        for (k = 0; k < histbinnum; k++)
                            hA[k] += histA[k];
                    }
                    F = m.matchHistograms(hA, hB);
                    for (j = 0; j < zframes; j++) {
                        ipA = stack.getProcessor(i * zframes + j + 1);
                        ipA.applyTable(F);
                    }
                    progressInfo.log("corrected time point: " + (i + 1));
                }
            }

        } else { // 2D case.
            for (i = 0; i < stack.getSize(); i++) {
                if (i == 0) {
                    ipB = stack.getProcessor(i + 1);
                    hB = ipB.getHistogram();
                } else {
                    ipA = stack.getProcessor(i + 1);
                    hA = ipA.getHistogram();
                    F = m.matchHistograms(hA, hB);
                    ipA.applyTable(F);
                    progressInfo.log("corrected frame: " + (i + 1));
                }
            }
        }
        // imp.show();

        return imp;
    }

    public static ImagePlus correctBleachSimpleRatio(ImagePlus imp, Roi curROI, double baselineInt, JIPipeProgressInfo progressInfo) {

        double referenceInt = 0;

        boolean is3DT = false;
        int zframes = 1;
        int timeframes = 1;
        int[] impdimA = imp.getDimensions();
        progressInfo.log("slices" + Integer.toString(impdimA[3]) + "  -- frames" + Integer.toString(impdimA[4]));
        // progressInfo.log(Integer.toString(imp.getNChannels())+":"+Integer.toString(imp.getNSlices())+":"+
        // Integer.toString(imp.getNFrames()));
        if (impdimA[3] > 1 && impdimA[4] > 1) { // if slices and frames are both more than 1
            is3DT = true;
            zframes = impdimA[3];
            timeframes = impdimA[4];
            if ((zframes * timeframes) != imp.getStackSize()) {
                throw new RuntimeException("slice and time frames do not match with the length of the stack. Please correct!");
            }
        }

        ImageStatistics imgstat = new ImageStatistics();
        ImageProcessor curip = null;
        double currentInt = 0.0;
        double ratio = 1.0;
        if (curROI == null)
            curROI = new Roi(0, 0, imp.getWidth(), imp.getHeight());
        if (!is3DT) {
            for (int i = 0; i < imp.getStackSize(); i++) {
                curip = imp.getImageStack().getProcessor(i + 1);
                curip.setRoi(0, 0, imp.getWidth(), imp.getHeight());
                curip.add(-1 * baselineInt);

                curip.setRoi(curROI);
                imgstat = curip.getStatistics();

                curip.setRoi(0, 0, imp.getWidth(), imp.getHeight());
                if (i == 0) {
                    referenceInt = imgstat.mean;
                    progressInfo.log("ref intensity=" + imgstat.mean);
                } else {
                    currentInt = imgstat.mean;
                    ratio = referenceInt / currentInt;
                    curip.multiply(ratio);
                    progressInfo.log("frame" + (i + 1) + "mean int=" + currentInt + " ratio=" + ratio);
                }

            }
        } else {
            for (int i = 0; i < timeframes; i++) {
                currentInt = 0.0;
                for (int j = 0; j < zframes; j++) {
                    curip = imp.getImageStack().getProcessor(i * zframes + j + 1);
                    curip.setRoi(0, 0, imp.getWidth(), imp.getHeight());
                    curip.add(-1 * baselineInt);
                    curip.setRoi(curROI);
                    imgstat = curip.getStatistics();
                    currentInt += imgstat.mean;
                    curip.setRoi(0, 0, imp.getWidth(), imp.getHeight());
                }
                currentInt /= zframes;

                if (i == 0) {
                    referenceInt = currentInt;
                    progressInfo.log("ref intensity=" + referenceInt);
                } else {
                    ratio = referenceInt / currentInt;
                    for (int j = 0; j < zframes; j++) {
                        curip = imp.getImageStack().getProcessor(i * zframes + j + 1);
                        curip.setRoi(0, 0, imp.getWidth(), imp.getHeight());
                        curip.multiply(ratio);
                    }
                    progressInfo.log("frame" + (i + 1) + "mean int=" + currentInt + " ratio=" + ratio);
                }
            }
        }
        return imp;
    }
}
