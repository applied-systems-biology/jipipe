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
import ij.plugin.filter.FFTFilter;
import ij.process.FHT;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.fft.ImagePlusFFTData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageSliceIndex;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

@SetJIPipeDocumentation(name = "FFT Custom Filter", description = "Applies a custom FFT filter")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "FFT")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Process\nFFT", aliasName = "Custom Filter...")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Input", create = true)
@AddJIPipeInputSlot(value = ImagePlusFFTData.class, name = "Filter", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Output", create = true)
public class FFTCustomFilter extends JIPipeIteratingAlgorithm {

    public FFTCustomFilter(JIPipeNodeInfo info) {
        super(info);
    }

    public FFTCustomFilter(JIPipeIteratingAlgorithm other) {
        super(other);
    }

    public static void doInverseTransform(FHT fht, ImageProcessor ip, Rectangle rect, int originalBitDepth) {
        fht.inverseTransform();
        //if (fht.quadrantSwapNeeded)
        //	fht.swapQuadrants();
        fht.resetMinAndMax();
        ImageProcessor ip2 = fht;
        fht.setRoi(rect.x, rect.y, rect.width, rect.height);
        ip2 = fht.crop();
        int bitDepth = fht.originalBitDepth > 0 ? fht.originalBitDepth : originalBitDepth;
        switch (bitDepth) {
            case 8:
                ip2 = ip2.convertToByte(true);
                break;
            case 16:
                ip2 = ip2.convertToShort(true);
                break;
        }
        ip.insert(ip2, 0, 0);
    }

    public static void customFilter(FHT fht, ImageProcessor filter) {
        int size = fht.getWidth();
        fht.swapQuadrants(filter);
        float[] fhtPixels = (float[]) fht.getPixels();
        boolean isFloat = filter.getBitDepth() == 32;
        for (int i = 0; i < fhtPixels.length; i++) {
            if (isFloat)
                fhtPixels[i] = fhtPixels[i] * filter.getf(i);
            else
                fhtPixels[i] = (float) (fhtPixels[i] * (filter.get(i) / 255.0));
        }
        fht.swapQuadrants(filter);
    }

    public static FHT newFHT(ImageProcessor ip, Rectangle rect, int originalWidth, int originalHeight, int originalBitDepth) {
        FHT fht;
        int width = ip.getWidth();
        int height = ip.getHeight();
        int maxN = Math.max(width, height);
        int size = 2;
        while (size < 1.5 * maxN) size *= 2;
        rect.x = (int) Math.round((size - width) / 2.0);
        rect.y = (int) Math.round((size - height) / 2.0);
        rect.width = width;
        rect.height = height;
        FFTFilter fftFilter = new FFTFilter();
        fht = new FHT(fftFilter.tileMirror(ip, size, size, rect.x, rect.y));
        fht.originalWidth = originalWidth;
        fht.originalHeight = originalHeight;
        fht.originalBitDepth = originalBitDepth;
        return fht;
    }

    public static ImageProcessor resizeFilter(ImageProcessor ip, int maxN, JIPipeProgressInfo progressInfo) {
        int width = ip.getWidth();
        int height = ip.getHeight();
        if (width == maxN && height == maxN)
            return ip;
        progressInfo.log("Scaling filter to " + maxN + "x" + maxN);
        return ip.resize(maxN, maxN);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus impInput = iterationStep.getInputData("Input", ImagePlusGreyscaleData.class, progressInfo).getImage();
        ImagePlus impFilter = iterationStep.getInputData("Filter", ImagePlusFFTData.class, progressInfo).getImage();

        Map<ImageSliceIndex, ImageProcessor> outputSlices = new HashMap<>();
        ImageJIterationUtils.forEachIndexedZCTSliceWithProgress(impInput, (ip2, index, sliceProgress) -> {
            ImageProcessor ip = ip2.duplicate();
            ImageProcessor filter = ImageJUtils.getSliceZeroSafe(impFilter, index).duplicate();

            Rectangle rect = new Rectangle();
            FHT fht = newFHT(ip, rect, ip.getWidth(), ip.getHeight(), ip2.getBitDepth());
            filter = resizeFilter(filter, fht.getWidth(), progressInfo);
            fht.transform();
            customFilter(fht, filter);
            doInverseTransform(fht, ip, rect, ip2.getBitDepth());
            ip.resetMinAndMax();

            outputSlices.put(index, ip);
        }, progressInfo);

        ImagePlus impOutput = ImageJUtils.mergeMappedSlices(outputSlices);
        impOutput.copyScale(impInput);
        new ContrastEnhancer().stretchHistogram(impOutput, 0.0);

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(impOutput), progressInfo);
    }
}
