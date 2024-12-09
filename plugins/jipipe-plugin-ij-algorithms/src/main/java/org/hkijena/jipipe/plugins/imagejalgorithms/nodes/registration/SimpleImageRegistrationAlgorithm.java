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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.registration;

import ij.ImagePlus;
import mpicbg.trakem2.transform.CoordinateTransform;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.strings.XMLData;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Port of {@link register_virtual_stack.Register_Virtual_Stack_MT}
 */
@SetJIPipeDocumentation(name = "Simple image registration", description = "All-in-one node that can apply the following image registration techniques: " +
        "Translation, Rigid (translation + rotation), Similarity (translation + rotation + isotropic scaling), Affine, Elastic (BUnwarpJ with cubic B-splines), Moving least squares. " +
        "Users must provide one reference image and any amount of target images.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Registration")
@AddJIPipeCitation("Based on Register Virtual Stack Slices by Albert Cardona, Ignacio Arganda-Carreras and Stephan Saalfeld")
@AddJIPipeCitation("https://imagej.net/plugins/register-virtual-stack-slices")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Reference", description = "The reference images. Please ensure that only once reference image per iteration step is present.", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Target", description = "The target images", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Registered", description = "The registered images", create = true)
@AddJIPipeOutputSlot(value = XMLData.class, name = "Transform", description = "The transform function in TrakEM format", create = true)
public class SimpleImageRegistrationAlgorithm extends JIPipeMergingAlgorithm {

    private SimpleImageRegistrationModel imageRegistrationModel = SimpleImageRegistrationModel.Rigid;
    private SimpleImageRegistrationFeatureModel imageRegistrationFeatureModel = SimpleImageRegistrationFeatureModel.Rigid;
    private final SIFTParameters siftParameters;
    private final SimpleBUnwarpJParameters bUnwarpJParameters;
    private float rod = 0.92f;
    private float maxEpsilon = 25.0f;
    private float minInlierRatio = 0.05f;
    private boolean interpolate = true;

    public SimpleImageRegistrationAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.siftParameters = new SIFTParameters();
        this.bUnwarpJParameters = new SimpleBUnwarpJParameters();
        registerSubParameters(siftParameters, bUnwarpJParameters);
    }

    public SimpleImageRegistrationAlgorithm(SimpleImageRegistrationAlgorithm other) {
        super(other);
        this.siftParameters = new SIFTParameters(other.siftParameters);
        this.bUnwarpJParameters  = new SimpleBUnwarpJParameters(other.bUnwarpJParameters);
        registerSubParameters(siftParameters, bUnwarpJParameters);
    }

    @SetJIPipeDocumentation(name = "Image registration model", description = "The image registration method." +
            "<ul>" +
            "<li>Translation: no deformation</li>" +
            "<li>Rigid: translate + rotate</li>" +
            "<li>Similarity: translate + rotate + isotropic scale</li>" +
            "<li>Affine: free affine transform</li>" +
            "<li>Elastic: BUnwarpJ splines</li>" +
            "<li>Moving least squares: maximal warping</li>" +
            "</ul>")
    @JIPipeParameter(value = "image-registration-model", important = true)
    public SimpleImageRegistrationModel getImageRegistrationModel() {
        return imageRegistrationModel;
    }

    @JIPipeParameter("image-registration-model")
    public void setImageRegistrationModel(SimpleImageRegistrationModel imageRegistrationModel) {
        this.imageRegistrationModel = imageRegistrationModel;
        emitParameterUIChangedEvent();
    }

    @SetJIPipeDocumentation(name = "Image feature extraction model", description = " The expected transformation model finding inliers" +
            " (i.e. correspondences or landmarks between images) in the feature extraction.")
    @JIPipeParameter(value = "image-registration-feature-model", important = true)
    public SimpleImageRegistrationFeatureModel getImageRegistrationFeatureModel() {
        return imageRegistrationFeatureModel;
    }

    @JIPipeParameter("image-registration-feature-model")
    public void setImageRegistrationFeatureModel(SimpleImageRegistrationFeatureModel imageRegistrationFeatureModel) {
        this.imageRegistrationFeatureModel = imageRegistrationFeatureModel;
    }

    @SetJIPipeDocumentation(name = "SIFT parameters", description = "Parameters for the SIFT feature extraction")
    @JIPipeParameter("sift-parameters")
    public SIFTParameters getSiftParameters() {
        return siftParameters;
    }

    @SetJIPipeDocumentation(name = "BUnwarpJ parameters", description = "Only used if the model is set to 'Elastic'. Settings for BUnwarpJ.")
    @JIPipeParameter("bunwarpj-parameters")
    public SimpleBUnwarpJParameters getbUnwarpJParameters() {
        return bUnwarpJParameters;
    }

    @SetJIPipeDocumentation(name = "Ratio of distances", description = "Closest/ next neighbor distance ratio")
    @JIPipeParameter("rod")
    public float getRod() {
        return rod;
    }

    @JIPipeParameter("rod")
    public void setRod(float rod) {
        this.rod = rod;
    }

    @SetJIPipeDocumentation(name = "Max epsilon", description = "Maximal allowed alignment error in pixels")
    @JIPipeParameter("max-epsilon")
    public float getMaxEpsilon() {
        return maxEpsilon;
    }

    @JIPipeParameter("max-epsilon")
    public void setMaxEpsilon(float maxEpsilon) {
        this.maxEpsilon = maxEpsilon;
    }

    @SetJIPipeDocumentation(name = "Inlier/candidates ratio")
     @JIPipeParameter("min-inlier-ratio")
    public float getMinInlierRatio() {
        return minInlierRatio;
    }

    @JIPipeParameter("min-inlier-ratio")
    public void setMinInlierRatio(float minInlierRatio) {
        this.minInlierRatio = minInlierRatio;
    }

    @SetJIPipeDocumentation(name = "Interpolate")
    @JIPipeParameter("interpolate")
    public boolean isInterpolate() {
        return interpolate;
    }

    @JIPipeParameter("interpolate")
    public void setInterpolate(boolean interpolate) {
        this.interpolate = interpolate;
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterCollection subParameter) {
        if(subParameter == bUnwarpJParameters) {
            return this.imageRegistrationModel == SimpleImageRegistrationModel.Elastic;
        }
        return super.isParameterUIVisible(tree, subParameter);
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        List<ImagePlusData> referenceImages = iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo);

        if(referenceImages.size() != 1) {
            throw new JIPipeValidationRuntimeException(new IllegalArgumentException("Expected exactly one reference image"),
                    "Only one reference image is allowed",
                    "Image registration requires exactly one reference image per iteration step",
                    "Check if the 'Reference' slot only receives exactly one image per iteration step");
        }

        // Collect reference and target images
        ImagePlus referenceImage = referenceImages.get(0).getImage();
        Map<Integer, ImagePlus> targetImages = new HashMap<>();
        List<Integer> sortedInputRows = iterationStep.getInputRows("Target").stream().sorted().collect(Collectors.toList());
        for (int inputRow : iterationStep.getInputRows("Target")) {
            ImagePlus targetImage = getInputSlot("Target").getData(inputRow, ImagePlusData.class, progressInfo).getImage();
            targetImages.put(inputRow, targetImage);
        }

        // Forward registration (from reference image to the end of the sequence)
        ImagePlus currentReferenceImage = referenceImage;
        ImagePlus currentReferenceMask = new ImagePlus();
        Map<Integer, CoordinateTransform> transforms = new HashMap<>();
        for (int i : sortedInputRows) {
            ImagePlus current = targetImages.get(i);
            ImagePlus mask = new ImagePlus();

            CoordinateTransform coordinateTransform = imageRegistrationModel.toCoordinateTransform();
            transforms.put(i, coordinateTransform);

        }

    }
}
