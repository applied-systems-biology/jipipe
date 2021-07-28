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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.features;

import ij.ImagePlus;
import ij.plugin.filter.Convolver;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

/**
 * Based on code by Dimiter Prodanov
 * https://imagej.nih.gov/ij/plugins/mexican-hat/Mexican_Hat_Filter.java
 */
@JIPipeDocumentation(name = "Laplacian of Gaussian 2D", description = "Applies a Laplacian of Gaussian filter. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeNode(menuPath = "Blur", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Output", autoCreate = true)
public class LaplacianOfGaussian2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private int radius = 2;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public LaplacianOfGaussian2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public LaplacianOfGaussian2DAlgorithm(LaplacianOfGaussian2DAlgorithm other) {
        super(other);
        this.radius = other.radius;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscale32FData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
        int sz = 2 * radius + 1;
        float[] kernel2 = computeKernel2D();
        ImageJUtils.forEachSlice(img, ip -> {
            Convolver con = new Convolver();
            con.convolveFloat(ip, kernel2, sz, sz);
            double sigma2 = (sz - 1) / 6.0;
            sigma2 *= sigma2;
            ip.multiply(sigma2);
        }, progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), progressInfo);
    }

    private float[] computeKernel2D() {
        int sz = 2 * radius + 1;
        final double sigma2 = 2 * ((double) radius / 3.0 + 1 / 6.0) * ((double) radius / 3.0 + 1 / 6.0);
        float[] kernel = new float[sz * sz];
        final double PIs = 4 / Math.sqrt(Math.PI * sigma2) / sigma2 / sigma2;
        float sum = 0;
        for (int u = -radius; u <= radius; u++) {
            for (int w = -radius; w <= radius; w++) {
                final double x2 = u * u + w * w;
                final int idx = u + radius + sz * (w + radius);
                kernel[idx] = (float) ((x2 - sigma2) * Math.exp(-x2 / sigma2) * PIs);
                ///System.out.print(kernel[c] +" ");
                sum += kernel[idx];

            }
        }
        sum = Math.abs(sum);
        if (sum < 1e-5) sum = 1;
        if (sum != 1) {
            for (int i = 0; i < kernel.length; i++) {
                kernel[i] /= sum;
                //System.out.print(kernel[i] +" ");
            }
        }
        return kernel;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @JIPipeDocumentation(name = "Radius", description = "Radius of the filter in pixels")
    @JIPipeParameter("radius")
    public int getRadius() {
        return radius;
    }

    @JIPipeParameter("radius")
    public void setRadius(int radius) {
        this.radius = radius;
    }
}
