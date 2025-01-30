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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.fft;

import ij.ImagePlus;
import ij.plugin.ContrastEnhancer;
import ij.process.FHT;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

@SetJIPipeDocumentation(name = "FFT Bandpass filter", description = "Applies a Bandpass filter")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "FFT")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Process\nFFT", aliasName = "Bandpass filter...")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Output", create = true)
//@AddJIPipeOutputSlot(value = ImagePlusFFTData.class, name = "Filter", create = true, description = "The filter that was used")
public class FFTBandPassFilter extends JIPipeSimpleIteratingAlgorithm {

    private double filterLargeDiameter = 40.0;
    private double filterSmallDiameter = 3.0;
    private SuppressStripesMode suppressStripesMode = SuppressStripesMode.None;
    private double toleranceOfDirectionPercentage = 5.0;
    private boolean doScaling = true;
    private boolean saturate = true;

    public FFTBandPassFilter(JIPipeNodeInfo nodeInfo) {
        super(nodeInfo);
    }

    public FFTBandPassFilter(FFTBandPassFilter other) {
        super(other);
        this.filterLargeDiameter = other.filterLargeDiameter;
        this.filterSmallDiameter = other.filterSmallDiameter;
        this.suppressStripesMode = other.suppressStripesMode;
        this.toleranceOfDirectionPercentage = other.toleranceOfDirectionPercentage;
        this.doScaling = other.doScaling;
        this.saturate = other.saturate;
    }

    public static ImageProcessor tileMirror(ImageProcessor ip, int width, int height, int x, int y) {
        if (x < 0 || x > (width - 1) || y < 0 || y > (height - 1)) {
            throw new RuntimeException("Image to be tiled is out of bounds.");
        }

        ImageProcessor ipout = ip.createProcessor(width, height);

        ImageProcessor ip2 = ip.crop();
        int w2 = ip2.getWidth();
        int h2 = ip2.getHeight();

        //how many times does ip2 fit into ipout?
        int i1 = (int) Math.ceil(x / (double) w2);
        int i2 = (int) Math.ceil((width - x) / (double) w2);
        int j1 = (int) Math.ceil(y / (double) h2);
        int j2 = (int) Math.ceil((height - y) / (double) h2);

        //tile
        if ((i1 % 2) > 0.5)
            ip2.flipHorizontal();
        if ((j1 % 2) > 0.5)
            ip2.flipVertical();

        for (int i = -i1; i < i2; i += 2) {
            for (int j = -j1; j < j2; j += 2) {
                ipout.insert(ip2, x - i * w2, y - j * h2);
            }
        }

        ip2.flipHorizontal();
        for (int i = -i1 + 1; i < i2; i += 2) {
            for (int j = -j1; j < j2; j += 2) {
                ipout.insert(ip2, x - i * w2, y - j * h2);
            }
        }

        ip2.flipVertical();
        for (int i = -i1 + 1; i < i2; i += 2) {
            for (int j = -j1 + 1; j < j2; j += 2) {
                ipout.insert(ip2, x - i * w2, y - j * h2);
            }
        }

        ip2.flipHorizontal();
        for (int i = -i1; i < i2; i += 2) {
            for (int j = -j1 + 1; j < j2; j += 2) {
                ipout.insert(ip2, x - i * w2, y - j * h2);
            }
        }

        return ipout;
    }

    public static FHT filterLargeSmall(ImageProcessor ip, double filterLarge, double filterSmall, int stripesHorVert, double scaleStripes) {

        int maxN = ip.getWidth();

        float[] fht = (float[]) ip.getPixels();
        float[] filter = new float[maxN * maxN];
        for (int i = 0; i < maxN * maxN; i++)
            filter[i] = 1f;

        int row;
        int backrow;
        float rowFactLarge;
        float rowFactSmall;

        int col;
        int backcol;
        float factor;
        float colFactLarge;
        float colFactSmall;

        float factStripes;

        // calculate factor in exponent of Gaussian from filterLarge / filterSmall

        double scaleLarge = filterLarge * filterLarge;
        double scaleSmall = filterSmall * filterSmall;
        scaleStripes = scaleStripes * scaleStripes;
        //float FactStripes;

        // loop over rows
        for (int j = 1; j < maxN / 2; j++) {
            row = j * maxN;
            backrow = (maxN - j) * maxN;
            rowFactLarge = (float) Math.exp(-(j * j) * scaleLarge);
            rowFactSmall = (float) Math.exp(-(j * j) * scaleSmall);


            // loop over columns
            for (col = 1; col < maxN / 2; col++) {
                backcol = maxN - col;
                colFactLarge = (float) Math.exp(-(col * col) * scaleLarge);
                colFactSmall = (float) Math.exp(-(col * col) * scaleSmall);
                factor = (1 - rowFactLarge * colFactLarge) * rowFactSmall * colFactSmall;
                switch (stripesHorVert) {
                    case 1:
                        factor *= (1 - (float) Math.exp(-(col * col) * scaleStripes));
                        break;// hor stripes
                    case 2:
                        factor *= (1 - (float) Math.exp(-(j * j) * scaleStripes)); // vert stripes
                }

                fht[col + row] *= factor;
                fht[col + backrow] *= factor;
                fht[backcol + row] *= factor;
                fht[backcol + backrow] *= factor;
                filter[col + row] *= factor;
                filter[col + backrow] *= factor;
                filter[backcol + row] *= factor;
                filter[backcol + backrow] *= factor;
            }
        }

        //process meeting points (maxN/2,0) , (0,maxN/2), and (maxN/2,maxN/2)
        int rowmid = maxN * (maxN / 2);
        rowFactLarge = (float) Math.exp(-(maxN / 2) * (maxN / 2) * scaleLarge);
        rowFactSmall = (float) Math.exp(-(maxN / 2) * (maxN / 2) * scaleSmall);
        factStripes = (float) Math.exp(-(maxN / 2) * (maxN / 2) * scaleStripes);

        fht[maxN / 2] *= (1 - rowFactLarge) * rowFactSmall; // (maxN/2,0)
        fht[rowmid] *= (1 - rowFactLarge) * rowFactSmall; // (0,maxN/2)
        fht[maxN / 2 + rowmid] *= (1 - rowFactLarge * rowFactLarge) * rowFactSmall * rowFactSmall; // (maxN/2,maxN/2)
        filter[maxN / 2] *= (1 - rowFactLarge) * rowFactSmall; // (maxN/2,0)
        filter[rowmid] *= (1 - rowFactLarge) * rowFactSmall; // (0,maxN/2)
        filter[maxN / 2 + rowmid] *= (1 - rowFactLarge * rowFactLarge) * rowFactSmall * rowFactSmall; // (maxN/2,maxN/2)

        switch (stripesHorVert) {
            case 1:
                fht[maxN / 2] *= (1 - factStripes);
                fht[rowmid] = 0;
                fht[maxN / 2 + rowmid] *= (1 - factStripes);
                filter[maxN / 2] *= (1 - factStripes);
                filter[rowmid] = 0;
                filter[maxN / 2 + rowmid] *= (1 - factStripes);
                break; // hor stripes
            case 2:
                fht[maxN / 2] = 0;
                fht[rowmid] *= (1 - factStripes);
                fht[maxN / 2 + rowmid] *= (1 - factStripes);
                filter[maxN / 2] = 0;
                filter[rowmid] *= (1 - factStripes);
                filter[maxN / 2 + rowmid] *= (1 - factStripes);
                break; // vert stripes
        }

        //loop along row 0 and maxN/2
        rowFactLarge = (float) Math.exp(-(maxN / 2) * (maxN / 2) * scaleLarge);
        rowFactSmall = (float) Math.exp(-(maxN / 2) * (maxN / 2) * scaleSmall);
        for (col = 1; col < maxN / 2; col++) {
            backcol = maxN - col;
            colFactLarge = (float) Math.exp(-(col * col) * scaleLarge);
            colFactSmall = (float) Math.exp(-(col * col) * scaleSmall);

            switch (stripesHorVert) {
                case 0:
                    fht[col] *= (1 - colFactLarge) * colFactSmall;
                    fht[backcol] *= (1 - colFactLarge) * colFactSmall;
                    fht[col + rowmid] *= (1 - colFactLarge * rowFactLarge) * colFactSmall * rowFactSmall;
                    fht[backcol + rowmid] *= (1 - colFactLarge * rowFactLarge) * colFactSmall * rowFactSmall;
                    filter[col] *= (1 - colFactLarge) * colFactSmall;
                    filter[backcol] *= (1 - colFactLarge) * colFactSmall;
                    filter[col + rowmid] *= (1 - colFactLarge * rowFactLarge) * colFactSmall * rowFactSmall;
                    filter[backcol + rowmid] *= (1 - colFactLarge * rowFactLarge) * colFactSmall * rowFactSmall;
                    break;
                case 1:
                    factStripes = (float) Math.exp(-(col * col) * scaleStripes);
                    fht[col] *= (1 - colFactLarge) * colFactSmall * (1 - factStripes);
                    fht[backcol] *= (1 - colFactLarge) * colFactSmall * (1 - factStripes);
                    fht[col + rowmid] *= (1 - colFactLarge * rowFactLarge) * colFactSmall * rowFactSmall * (1 - factStripes);
                    fht[backcol + rowmid] *= (1 - colFactLarge * rowFactLarge) * colFactSmall * rowFactSmall * (1 - factStripes);
                    filter[col] *= (1 - colFactLarge) * colFactSmall * (1 - factStripes);
                    filter[backcol] *= (1 - colFactLarge) * colFactSmall * (1 - factStripes);
                    filter[col + rowmid] *= (1 - colFactLarge * rowFactLarge) * colFactSmall * rowFactSmall * (1 - factStripes);
                    filter[backcol + rowmid] *= (1 - colFactLarge * rowFactLarge) * colFactSmall * rowFactSmall * (1 - factStripes);
                    break;
                case 2:
                    factStripes = (float) Math.exp(-(maxN / 2) * (maxN / 2) * scaleStripes);
                    fht[col] = 0;
                    fht[backcol] = 0;
                    fht[col + rowmid] *= (1 - colFactLarge * rowFactLarge) * colFactSmall * rowFactSmall * (1 - factStripes);
                    fht[backcol + rowmid] *= (1 - colFactLarge * rowFactLarge) * colFactSmall * rowFactSmall * (1 - factStripes);
                    filter[col] = 0;
                    filter[backcol] = 0;
                    filter[col + rowmid] *= (1 - colFactLarge * rowFactLarge) * colFactSmall * rowFactSmall * (1 - factStripes);
                    filter[backcol + rowmid] *= (1 - colFactLarge * rowFactLarge) * colFactSmall * rowFactSmall * (1 - factStripes);
            }
        }

        // loop along column 0 and maxN/2
        colFactLarge = (float) Math.exp(-(maxN / 2) * (maxN / 2) * scaleLarge);
        colFactSmall = (float) Math.exp(-(maxN / 2) * (maxN / 2) * scaleSmall);
        for (int j = 1; j < maxN / 2; j++) {
            row = j * maxN;
            backrow = (maxN - j) * maxN;
            rowFactLarge = (float) Math.exp(-(j * j) * scaleLarge);
            rowFactSmall = (float) Math.exp(-(j * j) * scaleSmall);

            switch (stripesHorVert) {
                case 0:
                    fht[row] *= (1 - rowFactLarge) * rowFactSmall;
                    fht[backrow] *= (1 - rowFactLarge) * rowFactSmall;
                    fht[row + maxN / 2] *= (1 - rowFactLarge * colFactLarge) * rowFactSmall * colFactSmall;
                    fht[backrow + maxN / 2] *= (1 - rowFactLarge * colFactLarge) * rowFactSmall * colFactSmall;
                    filter[row] *= (1 - rowFactLarge) * rowFactSmall;
                    filter[backrow] *= (1 - rowFactLarge) * rowFactSmall;
                    filter[row + maxN / 2] *= (1 - rowFactLarge * colFactLarge) * rowFactSmall * colFactSmall;
                    filter[backrow + maxN / 2] *= (1 - rowFactLarge * colFactLarge) * rowFactSmall * colFactSmall;
                    break;
                case 1:
                    factStripes = (float) Math.exp(-(maxN / 2) * (maxN / 2) * scaleStripes);
                    fht[row] = 0;
                    fht[backrow] = 0;
                    fht[row + maxN / 2] *= (1 - rowFactLarge * colFactLarge) * rowFactSmall * colFactSmall * (1 - factStripes);
                    fht[backrow + maxN / 2] *= (1 - rowFactLarge * colFactLarge) * rowFactSmall * colFactSmall * (1 - factStripes);
                    filter[row] = 0;
                    filter[backrow] = 0;
                    filter[row + maxN / 2] *= (1 - rowFactLarge * colFactLarge) * rowFactSmall * colFactSmall * (1 - factStripes);
                    filter[backrow + maxN / 2] *= (1 - rowFactLarge * colFactLarge) * rowFactSmall * colFactSmall * (1 - factStripes);
                    break;
                case 2:
                    factStripes = (float) Math.exp(-(j * j) * scaleStripes);
                    fht[row] *= (1 - rowFactLarge) * rowFactSmall * (1 - factStripes);
                    fht[backrow] *= (1 - rowFactLarge) * rowFactSmall * (1 - factStripes);
                    fht[row + maxN / 2] *= (1 - rowFactLarge * colFactLarge) * rowFactSmall * colFactSmall * (1 - factStripes);
                    fht[backrow + maxN / 2] *= (1 - rowFactLarge * colFactLarge) * rowFactSmall * colFactSmall * (1 - factStripes);
                    filter[row] *= (1 - rowFactLarge) * rowFactSmall * (1 - factStripes);
                    filter[backrow] *= (1 - rowFactLarge) * rowFactSmall * (1 - factStripes);
                    filter[row + maxN / 2] *= (1 - rowFactLarge * colFactLarge) * rowFactSmall * colFactSmall * (1 - factStripes);
                    filter[backrow + maxN / 2] *= (1 - rowFactLarge * colFactLarge) * rowFactSmall * colFactSmall * (1 - factStripes);
            }
        }


        // Return filter
        FHT f = new FHT(new FloatProcessor(maxN, maxN, filter, null));
        f.swapQuadrants();
        return f;
    }

    @SetJIPipeDocumentation(name = "Large diameter (px)", description = "Larger structures are filtered down to the specified diameter")
    @JIPipeParameter("filter-large-diameter")
    public double getFilterLargeDiameter() {
        return filterLargeDiameter;
    }

    @JIPipeParameter("filter-large-diameter")
    public void setFilterLargeDiameter(double filterLargeDiameter) {
        this.filterLargeDiameter = filterLargeDiameter;
    }

    @SetJIPipeDocumentation(name = "Small diameter (px)", description = "Smaller structures are filtered up to the specified diameter")
    @JIPipeParameter("filter-small-diameter")
    public double getFilterSmallDiameter() {
        return filterSmallDiameter;
    }

    @JIPipeParameter("filter-small-diameter")
    public void setFilterSmallDiameter(double filterSmallDiameter) {
        this.filterSmallDiameter = filterSmallDiameter;
    }

    @SetJIPipeDocumentation(name = "Suppress stripes", description = "If enabled, suppress stripes in the specified direction")
    @JIPipeParameter("suppress-stripes")
    public SuppressStripesMode getSuppressStripesMode() {
        return suppressStripesMode;
    }

    @JIPipeParameter("suppress-stripes")
    public void setSuppressStripesMode(SuppressStripesMode suppressStripesMode) {
        this.suppressStripesMode = suppressStripesMode;
    }

    @SetJIPipeDocumentation(name = "Tolerance of direction (%)", description = "Tolerance of direction. Use to calculate the sharpness of the filter.")
    @JIPipeParameter("tolerance-of-direction-perc")
    public double getToleranceOfDirectionPercentage() {
        return toleranceOfDirectionPercentage;
    }

    @JIPipeParameter("tolerance-of-direction-perc")
    public void setToleranceOfDirectionPercentage(double toleranceOfDirectionPercentage) {
        this.toleranceOfDirectionPercentage = toleranceOfDirectionPercentage;
    }

    @SetJIPipeDocumentation(name = "Auto-scale after filtering", description = "After filtering, apply automated scaling of values")
    @JIPipeParameter("do-scaling")
    public boolean isDoScaling() {
        return doScaling;
    }

    @JIPipeParameter("do-scaling")
    public void setDoScaling(boolean doScaling) {
        this.doScaling = doScaling;
    }

    @SetJIPipeDocumentation(name = "Auto-scale saturates values", description = "During auto-scaling, saturate pixel values")
    @JIPipeParameter("saturate")
    public boolean isSaturate() {
        return saturate;
    }

    @JIPipeParameter("saturate")
    public void setSaturate(boolean saturate) {
        this.saturate = saturate;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus imp = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();
        Map<ImageSliceIndex, ImageProcessor> outputMap = new HashMap<>();
        Map<ImageSliceIndex, ImageProcessor> filterMap = new HashMap<>();
        ImageJUtils.forEachIndexedZCTSlice(imp, (ip, index) -> {
            doFilter(ip, index, outputMap, filterMap, progressInfo, imp.getBitDepth());
        }, progressInfo);

        ImagePlus outputImage = ImageJUtils.mergeMappedSlices(outputMap);
        ImagePlus outputFilter = ImageJUtils.mergeMappedSlices(filterMap, false);

        outputImage.copyScale(imp);
        iterationStep.addOutputData("Output", new ImagePlusGreyscaleData(outputImage), progressInfo);
//        iterationStep.addOutputData("Filter", new ImagePlusFFTData(outputFilter), progressInfo);
    }

    private void doFilter(ImageProcessor ip, ImageSliceIndex index, Map<ImageSliceIndex, ImageProcessor> outputMap, Map<ImageSliceIndex, ImageProcessor> filterMap, JIPipeProgressInfo progressInfo, int bitDepth) {
        Rectangle roiRect = ip.getRoi();
        int maxN = Math.max(roiRect.width, roiRect.height);
        double sharpness = (100.0 - toleranceOfDirectionPercentage) / 100.0;
        boolean doScaling = this.doScaling;
        boolean saturate = this.saturate;

        int i = 2;
        while (i < 1.5 * maxN) {
            i *= 2;
        }

        // Calculate the inverse of the 1/e frequencies for large and small structures.
        double filterLarge = 2.0 * filterLargeDiameter / (double) i;
        double filterSmall = 2.0 * filterSmallDiameter / (double) i;

        // fit image into power of 2 size
        Rectangle fitRect = new Rectangle();
        fitRect.x = (int) Math.round((i - roiRect.width) / 2.0);
        fitRect.y = (int) Math.round((i - roiRect.height) / 2.0);
        fitRect.width = roiRect.width;
        fitRect.height = roiRect.height;

        // put image (ROI) into power 2 size image
        // mirroring to avoid wrap around effects
        progressInfo.log("Pad to " + i + "x" + i);
        ip = tileMirror(ip, i, i, fitRect.x, fitRect.y);

        // transform forward
        progressInfo.log(i + "x" + i + " forward transform");
        FHT fht = new FHT(ip);
        fht.setShowProgress(false);
        fht.transform();

        // filter out large and small structures
        progressInfo.log("Filter in frequency domain");
        FHT filterFht = filterLargeSmall(fht, filterLarge, filterSmall, suppressStripesMode.nativeValue, sharpness);

        // transform backward
        progressInfo.log("Inverse transform");
        fht.inverseTransform();

        // crop to original size and do scaling if selected
        progressInfo.log("Crop and convert to original type");
        fht.setRoi(fitRect);
        ip = fht.crop();
        if (doScaling) {
            ImagePlus imp2 = new ImagePlus("filtered", ip);
            new ContrastEnhancer().stretchHistogram(imp2, saturate ? 1.0 : 0.0);
            ip = imp2.getProcessor();
        }

        // convert back to original data type
        switch (bitDepth) {
            case 8:
                ip = ip.convertToByte(doScaling);
                break;
            case 16:
                ip = ip.convertToShort(doScaling);
                break;
        }

        outputMap.put(index, ip);
        filterMap.put(index, filterFht);
    }

    public enum SuppressStripesMode {
        Horizontal(1),
        Vertical(2),
        None(0);

        private final int nativeValue;

        SuppressStripesMode(int nativeValue) {
            this.nativeValue = nativeValue;
        }

        public int getNativeValue() {
            return nativeValue;
        }
    }
}
