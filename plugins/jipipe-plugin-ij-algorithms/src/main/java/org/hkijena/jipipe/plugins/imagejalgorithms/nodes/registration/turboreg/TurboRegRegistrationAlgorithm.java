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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.registration.turboreg;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.turboreg.*;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.plugins.strings.JsonData;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Port of {@link register_virtual_stack.Register_Virtual_Stack_MT}
 */
@SetJIPipeDocumentation(name = "TurboReg registration 2D", description = "Aligns the target image to the source image using methods " +
        "implemented by TurboReg and MultiStackReg. ")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Registration")
@AddJIPipeCitation("Based on TurboReg")
@AddJIPipeCitation("Based on MultiStackReg")
@AddJIPipeCitation("https://bigwww.epfl.ch/thevenaz/turboreg/")
@AddJIPipeCitation("https://github.com/miura/MultiStackRegistration/")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Reference", description = "The reference image. Can have fewer slices/channels/frames than the target image", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Input", description = "The target image that will be registered to the reference", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Reference", description = "Copy of the reference image", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Registered", description = "The registered image", create = true)
@AddJIPipeOutputSlot(value = JsonData.class, name = "Transform", description = "The transform serialized in JSON format", create = true)
public class TurboRegRegistrationAlgorithm extends JIPipeIteratingAlgorithm {
    
    private TurboRegTransformationType transformationType = TurboRegTransformationType.RigidBody;
    private final AdvancedTurboRegParameters advancedTurboRegParameters;


    public TurboRegRegistrationAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.advancedTurboRegParameters = new AdvancedTurboRegParameters();
        registerSubParameter(advancedTurboRegParameters);
    }

    public TurboRegRegistrationAlgorithm(TurboRegRegistrationAlgorithm other) {
        super(other);
        this.transformationType = other.transformationType;
        this.advancedTurboRegParameters = new AdvancedTurboRegParameters(other.advancedTurboRegParameters);
        registerSubParameter(advancedTurboRegParameters);
    }

    @SetJIPipeDocumentation(name = "Transformation", description = "The type of transformation to be used." +
            "<ul>" +
            "<li>Translation. Upon translation, a straight line is mapped to a straight line of identical orientation, with conservation of the distance between any pair of points. A single landmark in each image gives a complete description of a translation. The mapping is of the form x = u + Δu. </li>" +
            "<li>Rigid Body. Upon rigid-body transformation, the distance between any pair of points is conserved. A single landmark is necessary to describe the translational component of the rigid-body transformation, while the rotational component is given by an angle. The mapping is of the form x = { {cos θ, −sin θ}, {sin θ, cos θ} } ⋅ u + Δu.<li>" +
            "<li>Scaled rotation. Upon scaled rotation, a straight line is mapped to a straight line; moreover, the angle between any pair of lines is conserved (this is sometimes called a conformal mapping). A pair of landmarks in each image is needed to give a complete description of a scaled rotation. The mapping is of the form x = λ { {cos θ, −sin θ}, {sin θ, cos θ} } ⋅ u + Δu.<li>" +
            "<li>Affine. Upon affine transformation, a straight line is mapped to a straight line, with conservation of flat angles between lines (parallel or coincident lines remain parallel or coincident). In 2D, a simplex—three landmarks—in each image is needed to give a complete description of an affine transformation. The mapping is of the form x = { {a11, a12}, {a21, a22} } ⋅ u + Δu.<li>" +
            "<li>Bilinear. Upon bilinear transformation, a straight line is mapped to a conic section. In 2D, four landmarks in each image are needed to give a complete description of a bilinear transformation. The mapping is of the form x = { {a11, a12}, {a21, a22} } ⋅ u + b u1 u2 + Δu.<li>" +
            "<li>None. Do not apply any transformation.<li>" +
            "</ul>")
    @JIPipeParameter("transformation-type")
    public TurboRegTransformationType getTransformationType() {
        return transformationType;
    }

    @JIPipeParameter("transformation-type")
    public void setTransformationType(TurboRegTransformationType transformationType) {
        this.transformationType = transformationType;
    }

    @SetJIPipeDocumentation(name = "Advanced parameters", description = "Advanced parameters for TurboReg")
    @JIPipeParameter("advanced-parameters")
    public AdvancedTurboRegParameters getAdvancedTurboRegParameters() {
        return advancedTurboRegParameters;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus target = iterationStep.getInputData("Reference", ImagePlusGreyscaleData.class, progressInfo).getDuplicateImage();
        ImagePlus source = iterationStep.getInputData("Input", ImagePlusGreyscaleData.class, progressInfo).getDuplicateImage();

        if(transformationType == TurboRegTransformationType.GenericTransformation) {
            progressInfo.log("Transformation set to 'None'. Skipping.");
            iterationStep.addOutputData("Reference", new ImagePlusData(source), progressInfo);
            iterationStep.addOutputData("Target", new ImagePlusData(target), progressInfo);
            iterationStep.addOutputData("Transform", new JsonData(JsonUtils.toPrettyJsonString(new TurboRegTransformationInfo())), progressInfo);
            return;
        }
        if(source.getWidth() != target.getWidth() || source.getHeight() != target.getHeight()) {
            throw new RuntimeException("Source and target images do not have the same width and height!");
        }

        Map<ImageSliceIndex, ImageProcessor> transformedTargetProcessors = new HashMap<>();
        TurboRegTransformationInfo transformation = new TurboRegTransformationInfo();

        ImageJUtils.forEachIndexedZCTSliceWithProgress(source, (sourceIp, index, sliceProgress) -> {
            // Find the best matching target
            // TODO: smarter selection of the reference slice
            ImageSliceIndex targetIndex = ImageJUtils.toSafeZeroIndex(target, index);
            ImageProcessor targetIp = ImageJUtils.getSliceZero(target, targetIndex);

            TurboRegResult aligned = TurboRegUtils.alignImage2D(new ImagePlus("source", sourceIp),
                    new ImagePlus("target", targetIp),
                    transformationType,
                    advancedTurboRegParameters);

            transformedTargetProcessors.put(index, aligned.getTransformedTargetImage().getProcessor());

            // Modify the transformation
            TurboRegTransformationInfo.Entry transformationEntry = aligned.getTransformation().getEntries().get(0);
            transformationEntry.setSourceImageIndex(index);
            transformationEntry.setTargetImageIndex(targetIndex);
            transformation.getEntries().add(transformationEntry);
        }, progressInfo);


        ImagePlus result = ImageJUtils.mergeMappedSlices(transformedTargetProcessors);
        result.copyScale(target);
        iterationStep.addOutputData("Reference", new ImagePlusData(target), progressInfo);
        iterationStep.addOutputData("Registered", new ImagePlusData(result), progressInfo);
        iterationStep.addOutputData("Transform", new JsonData(JsonUtils.toPrettyJsonString(transformation)), progressInfo);

    }

}
