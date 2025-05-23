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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.math;

import ij.ImagePlus;
import ij.ImageStack;
import imagescience.feature.Hessian;
import imagescience.image.Aspects;
import imagescience.image.FloatImage;
import imagescience.image.Image;
import org.hkijena.jipipe.api.*;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejalgorithms.parameters.EigenvalueSelection2D;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;

import java.util.Vector;

@SetJIPipeDocumentation(name = "Hessian 2D (FeatureJ, old)", description = "Deprecated: Replace with node with the same name. " +
        "Computes Hessian the eigenvalues of the Hessian, which can be used for example to discriminate locally between plate-like, line-like, and blob-like image structures." +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(menuPath = "Math", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, name = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nFeatureJ", aliasName = "FeatureJ Hessian")
@AddJIPipeCitation("Y. Sato, S. Nakajima, N. Shiraga, H. Atsumi, S. Yoshida, T. Koller, G. Gerig, R. Kikinis Three-Dimensional Multi-Scale Line Filter for Segmentation and Visualization of Curvilinear Structures in Medical Images Medical Image Analysis, vol. 2, no. 2, June 1998, pp. 143-168")
@AddJIPipeCitation("A. F. Frangi, W. J. Niessen, R. M. Hoogeveen, T. van Walsum, M. A. Viergever Model-Based Quantitation of 3D Magnetic Resonance Angiographic Images IEEE Transactions on Medical Imaging, vol. 18, no. 10, October 1999, pp. 946-956")
@AddJIPipeCitation("K. Rohr Landmark-Based Image Analysis using Geometric and Intensity Models Kluwer Academic Publishers, 2001")
@AddJIPipeCitation("see https://imagescience.org/meijering/software/featurej/hessian/")
@LabelAsJIPipeHidden
@Deprecated
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

        ImageJIterationUtils.forEachIndexedSlice(img, (imp, index) -> {
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
