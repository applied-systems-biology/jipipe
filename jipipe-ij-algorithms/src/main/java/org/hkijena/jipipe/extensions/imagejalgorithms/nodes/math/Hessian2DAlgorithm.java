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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.math;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.EDM;
import imagescience.feature.Hessian;
import imagescience.image.Aspects;
import imagescience.image.FloatImage;
import imagescience.image.Image;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.parameters.EigenvalueSelection2D;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

import java.util.Vector;

/**
 * Wrapper around {@link EDM}
 */
@SetJIPipeDocumentation(name = "Hessian 2D", description = "Computes Hessian eigenimages of images." +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(menuPath = "Math", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nFeatureJ", aliasName = "FeatureJ Hessian")
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
        super(info);
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus img = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();
        ImageStack stack = new ImageStack(img.getWidth(), img.getHeight(), img.getProcessor().getColorModel());

        ImageJUtils.forEachIndexedSlice(img, (imp, index) -> {
            ImagePlus slice = new ImagePlus("slice", imp.duplicate());
            ImagePlus processedSlice = applyHessian(slice);
            stack.addSlice("slice" + index, processedSlice.getProcessor());
        }, progressInfo);
        ImagePlus result = new ImagePlus("Segmented Image", stack);
        result.setDimensions(img.getNChannels(), img.getNSlices(), img.getNFrames());
        result.copyScale(img);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(result), progressInfo);
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

    @SetJIPipeDocumentation(name = "Eigenvalue", description = "Allows you to choose whether the largest or smallest Eigenvalues are chosen")
    @JIPipeParameter("eigenvalue-selection")
    public EigenvalueSelection2D getEigenvalueSelection2D() {
        return eigenvalueSelection;
    }

    @JIPipeParameter("eigenvalue-selection")
    public void setEigenvalueSelection2D(EigenvalueSelection2D eigenvalueSelection2D) {
        this.eigenvalueSelection = eigenvalueSelection2D;

    }

    @JIPipeParameter("smoothing")
    @SetJIPipeDocumentation(name = "Smoothing", description = "The smoothing scale at which the required image derivatives are computed. " +
            "The scale is equal to the standard deviation of the Gaussian kernel used for differentiation and must be larger than zero. " +
            "In order to enforce physical isotropy, for each dimension, the scale is divided by the size of the image elements (aspect ratio) in that dimension.")
    public double getSmoothing() {
        return smoothing;
    }

    @JIPipeParameter("smoothing")
    public void setSmoothing(double smoothing) {
        this.smoothing = smoothing;

    }

    @SetJIPipeDocumentation(name = "Compare absolute", description = "Determines whether eigenvalues are compared in absolute sense")
    @JIPipeParameter("compare-absolute")
    public boolean isCompareAbsolute() {
        return compareAbsolute;
    }

    @JIPipeParameter("compare-absolute")
    public void setCompareAbsolute(boolean compareAbsolute) {
        this.compareAbsolute = compareAbsolute;

    }
}
