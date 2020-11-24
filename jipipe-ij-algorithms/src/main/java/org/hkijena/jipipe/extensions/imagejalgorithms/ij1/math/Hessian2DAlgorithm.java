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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.math;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.EDM;
import imagescience.feature.Hessian;
import imagescience.image.Aspects;
import imagescience.image.FloatImage;
import imagescience.image.Image;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.EigenvalueSelection2D;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;

import java.util.Vector;

/**
 * Wrapper around {@link EDM}
 */
@JIPipeDocumentation(name = "Hessian 2D", description = "Computes Hessian eigenimages of images." +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeOrganization(menuPath = "Math", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Output")
public class Hessian2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private EigenvalueSelection2D eigenvalueSelection = EigenvalueSelection2D.Largest;
    private double smoothing = 1.0;
    private boolean compareAbsolute = true;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public Hessian2DAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusGreyscaleData.class)
                .addOutputSlot("Output", ImagePlusGreyscale32FData.class, null)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public Hessian2DAlgorithm(Hessian2DAlgorithm other) {
        super(other);
        this.smoothing = other.smoothing;
        this.eigenvalueSelection = other.eigenvalueSelection;
        this.compareAbsolute = other.compareAbsolute;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus img = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();
        ImageStack stack = new ImageStack(img.getWidth(), img.getHeight(), img.getProcessor().getColorModel());

        ImageJUtils.forEachIndexedSlice(img, (imp, index) -> {
            progressInfo.log("Slice " + index + "/" + img.getStackSize());
            ImagePlus slice = new ImagePlus("slice", imp.duplicate());
            ImagePlus processedSlice = applyHessian(slice);
            stack.addSlice("slice" + index, processedSlice.getProcessor());
        });
        ImagePlus result = new ImagePlus("Segmented Image", stack);
        result.setDimensions(img.getNChannels(), img.getNSlices(), img.getNFrames());

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(result), progressInfo);
    }

    private ImagePlus applyHessian(ImagePlus input) {
        final Image image = Image.wrap(input);
        image.aspects(new Aspects());
        final Hessian hessian = new Hessian();
        final Vector<Image> eigenimages = hessian.run(new FloatImage(image), smoothing, compareAbsolute);
        if (eigenvalueSelection == EigenvalueSelection2D.Largest)
            return eigenimages.get(0).imageplus();
        else
            return eigenimages.get(1).imageplus();
    }

    @JIPipeDocumentation(name = "Eigenvalue", description = "Allows you to choose whether the largest or smallest Eigenvalues are chosen")
    @JIPipeParameter("eigenvalue-selection")
    public EigenvalueSelection2D getEigenvalueSelection2D() {
        return eigenvalueSelection;
    }

    @JIPipeParameter("eigenvalue-selection")
    public void setEigenvalueSelection2D(EigenvalueSelection2D eigenvalueSelection2D) {
        this.eigenvalueSelection = eigenvalueSelection2D;

    }

    @JIPipeParameter("smoothing")
    @JIPipeDocumentation(name = "Smoothing", description = "The smoothing scale at which the required image derivatives are computed. " +
            "The scale is equal to the standard deviation of the Gaussian kernel used for differentiation and must be larger than zero. " +
            "In order to enforce physical isotropy, for each dimension, the scale is divided by the size of the image elements (aspect ratio) in that dimension.")
    public double getSmoothing() {
        return smoothing;
    }

    @JIPipeParameter("smoothing")
    public void setSmoothing(double smoothing) {
        this.smoothing = smoothing;

    }

    @JIPipeDocumentation(name = "Compare absolute", description = "Determines whether eigenvalues are compared in absolute sense")
    @JIPipeParameter("compare-absolute")
    public boolean isCompareAbsolute() {
        return compareAbsolute;
    }

    @JIPipeParameter("compare-absolute")
    public void setCompareAbsolute(boolean compareAbsolute) {
        this.compareAbsolute = compareAbsolute;

    }
}
