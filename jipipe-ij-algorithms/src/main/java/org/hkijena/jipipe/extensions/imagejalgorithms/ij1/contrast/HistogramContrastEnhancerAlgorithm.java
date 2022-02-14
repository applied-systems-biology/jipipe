package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.contrast;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.*;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;

import static ij.measure.Measurements.MIN_MAX;

/**
 * This class is based on {@link ij.plugin.ContrastEnhancer}
 * This class is not very portable, so I adapted the code here
 */
@JIPipeDocumentation(name = "Histogram-based contrast enhancer", description = "Implementation of the ImageJ contrast enhancer feature that uses histograms and normalization methods.")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true, inheritedSlot = "Input")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Contrast")
public class HistogramContrastEnhancerAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private Method method = Method.StretchHistogramAndNormalize;
    private double saturatedPixels = 0.35;
    private boolean useStackHistogram = false;

    public HistogramContrastEnhancerAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public HistogramContrastEnhancerAlgorithm(HistogramContrastEnhancerAlgorithm other) {
        super(other);
        this.method = other.method;
        this.saturatedPixels = other.saturatedPixels;
        this.useStackHistogram = other.useStackHistogram;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus img = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getDuplicateImage();

        switch (method) {
            case StretchHistogram:
                stretchHistogram(img, saturatedPixels, false);
                break;
            case StretchHistogramAndNormalize:
                stretchHistogram(img, saturatedPixels, img.getType() != ImagePlus.COLOR_RGB);
                break;
            case EqualizeHistogram:
                if (img.getType() != ImagePlus.GRAY32) {
                    equalize(img, false);
                }
                break;
            case EqualizeHistogramClassic:
                if (img.getType() != ImagePlus.GRAY32) {
                    equalize(img, true);
                }
                break;
        }

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), progressInfo);
    }


    @JIPipeDocumentation(name = "Saturated pixels (%)", description = "Only used if histogram stretching is used.\n" +
            "Determines the number of pixels in the image that are allowed to become saturated. " +
            "Increasing this value increases contrast. " +
            "This value should be greater than zero to prevent a few outlying pixel from causing the histogram stretch to not work as intended. ")
    @JIPipeParameter("saturated-pixels")
    public double getSaturatedPixels() {
        return saturatedPixels;
    }

    @JIPipeParameter("saturated-pixels")
    public boolean setSaturatedPixels(double saturatedPixels) {
        if (saturatedPixels < 0 || saturatedPixels > 100)
            return false;
        this.saturatedPixels = saturatedPixels;
        return true;
    }

    @JIPipeDocumentation(name = "Method", description = "The method that should be used to enhance the contrast.<br/>" +
            "<ul><li>Stretch histogram sets the min/max display range to the min/max of the histogram.</li>" +
            "<li>Stretch histogram + normalize additionally will recalculate the pixel values of the image so the range" +
            " is equal to the maximum range for the data type, or 0 - 1.0 for float images. " +
            "The maximum range is 0-255 for 8-bit images and 0-65535 for 16-bit images. Normalization is silently skipped for RGB images.</li>" +
            "<li>Equalize histogram uses an histogram equalization algorithm. 32bit images are not supported and will be silently skipped.</li>" +
            "<li>Equalize histogram (Classic) also uses an histogram equalization algorithm. Histogram values are used directly instead of their SQRT." +
            " 32bit images are not supported and will be silently skipped.</li>" +
            "</ul>")
    @JIPipeParameter("method")
    public Method getMethod() {
        return method;
    }

    @JIPipeParameter("method")
    public void setMethod(Method method) {
        this.method = method;
    }

    @JIPipeDocumentation(name = "Use stack histogram", description = "If enabled, the histogram of all slices is used instead of the per-slice histograms.")
    @JIPipeParameter("use-stack-histogram")
    public boolean isUseStackHistogram() {
        return useStackHistogram;
    }

    @JIPipeParameter("use-stack-histogram")
    public void setUseStackHistogram(boolean useStackHistogram) {
        this.useStackHistogram = useStackHistogram;
    }

    public void stretchHistogram(ImagePlus imp, double saturated, boolean normalize) {
        ImageStatistics stats = null;
        if (useStackHistogram)
            stats = new StackStatistics(imp);
        int stackSize = imp.getStackSize();
        ImageStack stack = imp.getStack();
        for (int i = 1; i <= stackSize; i++) {
            IJ.showProgress(i, stackSize);
            ImageProcessor ip = stack.getProcessor(i);
            ip.setRoi(imp.getRoi());
            if (!useStackHistogram)
                stats = ImageStatistics.getStatistics(ip, MIN_MAX, null);
            stretchHistogram(ip, saturated, stats, normalize);
        }
    }

    public void stretchHistogram(ImageProcessor ip, double saturated, boolean normalize) {
        useStackHistogram = false;
        stretchHistogram(new ImagePlus("", ip), saturated, normalize);
    }

    public void stretchHistogram(ImageProcessor ip, double saturated, ImageStatistics stats, boolean normalize) {
        int[] a = getMinAndMax(ip, saturated, stats);
        int hmin = a[0], hmax = a[1];
        if (hmax > hmin) {
            double min = stats.histMin + hmin * stats.binSize;
            double max = stats.histMin + hmax * stats.binSize;
            if (stats.histogram16 != null && ip instanceof ShortProcessor) {
                min = hmin;
                max = hmax;
            }
            ip.resetRoi();
            if (normalize)
                normalize(ip, min, max);
            else {
                ip.setMinAndMax(min, max);
            }
        }
    }

    void stretchCompositeImageHistogram(CompositeImage imp, double saturated, ImageStatistics stats) {
        ImageProcessor ip = imp.getProcessor();
        int[] a = getMinAndMax(ip, saturated, stats);
        int hmin = a[0], hmax = a[1];
        if (hmax > hmin) {
            double min = stats.histMin + hmin * stats.binSize;
            double max = stats.histMin + hmax * stats.binSize;
            if (stats.histogram16 != null && imp.getBitDepth() == 16) {
                min = hmin;
                max = hmax;
            }
            imp.setDisplayRange(min, max);
        }
		/*
		int channels = imp.getNChannels();b
		int channel = imp.getChannel();
		int slice = imp.getSlice();
		int frame = imp.getFrame();
		for (int c=1; c<=channels; c++) {
			imp.setPosition(c, slice, frame);
			ImageProcessor ip = imp.getProcessor();
			int[] a = getMinAndMax(ip, saturated, stats);
			int hmin=a[0], hmax=a[1];
			if (hmax>hmin) {
				double min = stats.histMin+hmin*stats.binSize;
				double max = stats.histMin+hmax*stats.binSize;
				imp.setDisplayRange(min, max);
			}
		}
		imp.setPosition(channel, slice, frame);
		*/
    }

    int[] getMinAndMax(ImageProcessor ip, double saturated, ImageStatistics stats) {
        int hmin, hmax;
        int threshold;
        int[] histogram = stats.histogram;
        if (stats.histogram16 != null && ip instanceof ShortProcessor)
            histogram = stats.histogram16;
        int hsize = histogram.length;
        if (saturated > 0.0)
            threshold = (int) (stats.pixelCount * saturated / 200.0);
        else
            threshold = 0;
        int i = -1;
        boolean found = false;
        int count = 0;
        int maxindex = hsize - 1;
        do {
            i++;
            count += histogram[i];
            found = count > threshold;
        } while (!found && i < maxindex);
        hmin = i;

        i = hsize;
        count = 0;
        do {
            i--;
            count += histogram[i];
            found = count > threshold;
        } while (!found && i > 0);
        hmax = i;
        int[] a = new int[2];
        a[0] = hmin;
        a[1] = hmax;
        return a;
    }

    void normalize(ImageProcessor ip, double min, double max) {
        int min2 = 0;
        int max2 = 255;
        int range = 256;
        if (ip instanceof ShortProcessor) {
            max2 = 65535;
            range = 65536;
        } else if (ip instanceof FloatProcessor)
            normalizeFloat(ip, min, max);
        int[] lut = new int[range];
        for (int i = 0; i < range; i++) {
            if (i <= min)
                lut[i] = 0;
            else if (i >= max)
                lut[i] = max2;
            else
                lut[i] = (int) (((double) (i - min) / (max - min)) * max2);
        }
        applyTable(ip, lut);
    }

    void applyTable(ImageProcessor ip, int[] lut) {
        if (false) {
            ImageProcessor mask = ip.getMask();
            if (mask != null) ip.snapshot();
            ip.applyTable(lut);
            if (mask != null) ip.reset(mask);
        } else
            ip.applyTable(lut);
    }

    void normalizeFloat(ImageProcessor ip, double min, double max) {
        double scale = max > min ? 1.0 / (max - min) : 1.0;
        int size = ip.getWidth() * ip.getHeight();
        float[] pixels = (float[]) ip.getPixels();
        double v;
        for (int i = 0; i < size; i++) {
            v = pixels[i] - min;
            if (v < 0.0) v = 0.0;
            v *= scale;
            if (v > 1.0) v = 1.0;
            pixels[i] = (float) v;
        }
    }

    public void equalize(ImagePlus imp, boolean classicEqualization) {
        if (imp.getBitDepth() == 32) {
            IJ.showMessage("Contrast Enhancer", "Equalization of 32-bit images not supported.");
            return;
        }
        int[] histogram = null;
        if (useStackHistogram) {
            ImageStatistics stats = new StackStatistics(imp);
            histogram = stats.histogram;
            if (stats.histogram16 != null && imp.getBitDepth() == 16)
                histogram = stats.histogram16;
        }
        {
            int stackSize = imp.getStackSize();
            //int[] mask = imp.getMask();
            //Rectangle rect = imp.get
            ImageStack stack = imp.getStack();
            for (int i = 1; i <= stackSize; i++) {
                IJ.showProgress(i, stackSize);
                ImageProcessor ip = stack.getProcessor(i);
                if (histogram == null)
                    histogram = ip.getHistogram();
                equalize(ip, histogram, classicEqualization);
            }
        }
        if (imp.getBitDepth() == 16 && imp.getStackSize() > 1) {
            ImageStack stack = imp.getStack();
            ImageProcessor ip = stack.getProcessor(stack.size() / 2);
            ImageStatistics stats = ip.getStats();
            imp.getProcessor().setMinAndMax(stats.min, stats.max);
        } else
            imp.getProcessor().resetMinAndMax();
    }

    /**
     * Changes the tone curves of images.
     * It should bring up the detail in the flat regions of your image.
     * Histogram Equalization can enhance meaningless detail and hide
     * important but small high-contrast features. This method uses a
     * similar algorithm, but uses the square root of the histogram
     * values, so its effects are less extreme. Hold the alt key down
     * to use the standard histogram equalization algorithm.
     * This code was contributed by Richard Kirk (rak@cre.canon.co.uk).
     */
    public void equalize(ImageProcessor ip, boolean classicEqualization) {
        equalize(ip, ip.getHistogram(), classicEqualization);
    }

    private void equalize(ImageProcessor ip, int[] histogram, boolean classicEqualization) {
        ip.resetRoi();
        int max;
        int range;
        if (ip instanceof ShortProcessor) {    // Short
            max = 65535;
            range = 65535;
        } else { //bytes
            max = 255;
            range = 255;
        }
        double sum;
        sum = getWeightedValue(histogram, 0, classicEqualization);
        for (int i = 1; i < max; i++)
            sum += 2 * getWeightedValue(histogram, i, classicEqualization);
        sum += getWeightedValue(histogram, max, classicEqualization);
        double scale = range / sum;
        int[] lut = new int[range + 1];
        lut[0] = 0;
        sum = getWeightedValue(histogram, 0, classicEqualization);
        for (int i = 1; i < max; i++) {
            double delta = getWeightedValue(histogram, i, classicEqualization);
            sum += delta;
            lut[i] = (int) Math.round(sum * scale);
            sum += delta;
        }
        lut[max] = max;
        applyTable(ip, lut);
    }

    private double getWeightedValue(int[] histogram, int i, boolean classicEqualization) {
        int h = histogram[i];
        if (h < 2 || classicEqualization) return h;
        return Math.sqrt(h);
    }

    public enum Method {
        StretchHistogram,
        StretchHistogramAndNormalize,
        EqualizeHistogram,
        EqualizeHistogramClassic;


        @Override
        public String toString() {
            switch (this) {
                case StretchHistogram:
                    return "Stretch histogram";
                case StretchHistogramAndNormalize:
                    return "Stretch histogram + normalize";
                case EqualizeHistogram:
                    return "Equalize histogram";
                case EqualizeHistogramClassic:
                    return "Equalize histogram (Classic)";
            }
            throw new UnsupportedOperationException();
        }
    }
}
