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

import bunwarpj.Param;
import ij.ImagePlus;
import mpicbg.trakem2.transform.AffineModel2D;
import mpicbg.trakem2.transform.CoordinateTransform;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationStep;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.RegistrationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.strings.XMLData;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Port of {@link register_virtual_stack.Register_Virtual_Stack_MT}
 */
@SetJIPipeDocumentation(name = "Simple image registration (pair-wise)", description = "All-in-one node that can apply the following image registration techniques: " +
        "Translation, Rigid (translation + rotation), Similarity (translation + rotation + isotropic scaling), Affine, Elastic (BUnwarpJ with cubic B-splines), Moving least squares. " +
        "This node applies a pair-wise registration where the target image is registered to the reference image.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Registration")
@AddJIPipeCitation("Based on Register Virtual Stack Slices by Albert Cardona, Ignacio Arganda-Carreras and Stephan Saalfeld")
@AddJIPipeCitation("https://imagej.net/plugins/register-virtual-stack-slices")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Reference", description = "The reference image", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Target", description = "The target image that will be registered to the reference", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Reference", description = "The reference image (registered if the registration is two-way)", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Target", description = "The registered image", create = true)
@AddJIPipeOutputSlot(value = XMLData.class, name = "Transform", description = "The transform function in TrakEM format", create = true)
public class SimpleImageRegistrationAlgorithm extends JIPipeIteratingAlgorithm {

    private final SIFTParameters siftParameters;
    private final SimpleBUnwarpJParameters bUnwarpJParameters;
    private final SimpleImageRegistrationParameters parameters;

    public SimpleImageRegistrationAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.siftParameters = new SIFTParameters();
        this.bUnwarpJParameters = new SimpleBUnwarpJParameters();
        this.parameters = new SimpleImageRegistrationParameters();
        registerSubParameters(siftParameters, bUnwarpJParameters, parameters);
    }

    public SimpleImageRegistrationAlgorithm(SimpleImageRegistrationAlgorithm other) {
        super(other);
        this.siftParameters = new SIFTParameters(other.siftParameters);
        this.bUnwarpJParameters  = new SimpleBUnwarpJParameters(other.bUnwarpJParameters);
        this.parameters = new SimpleImageRegistrationParameters(other.parameters);
        registerSubParameters(siftParameters, bUnwarpJParameters, parameters);
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

    @SetJIPipeDocumentation(name = "Registration", description = "Registration parameters")
    @JIPipeParameter("parameters")
    public SimpleImageRegistrationParameters getParameters() {
        return parameters;
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterCollection subParameter) {
        if(subParameter == bUnwarpJParameters) {
            return parameters.getImageRegistrationModel() == SimpleImageRegistrationModel.Elastic;
        }
        return super.isParameterUIVisible(tree, subParameter);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus reference = iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo).getDuplicateImage();
        ImagePlus target = iterationStep.getInputData("Target", ImagePlusData.class, progressInfo).getDuplicateImage();

        // Generate BUnwarpJ parameters
        Param unwarpJParametersParam = bUnwarpJParameters.toParam();

        // TODO: handle hyperstack
        ImagePlus imp1 = new ImagePlus("imp1", reference.getImage());
        ImagePlus imp2 = new ImagePlus("imp2", target.getImage());
        ImagePlus imp1mask = new ImagePlus();
        ImagePlus imp2mask = new ImagePlus();
        CoordinateTransform coordinateTransform = parameters.getImageRegistrationModel().toCoordinateTransform();
        Rectangle commonBounds = new Rectangle(0,0, imp1.getWidth(), imp1.getHeight());
        List<Rectangle> bounds = new ArrayList<>();
        bounds.add(new Rectangle(0,0, imp1.getWidth(), imp1.getHeight()));

        RegistrationUtils.register(imp1, imp2, imp1mask, imp2mask, coordinateTransform, commonBounds, bounds, parameters, siftParameters, unwarpJParametersParam, progressInfo);

        iterationStep.addOutputData("Reference", new ImagePlusData(imp1), progressInfo);
        iterationStep.addOutputData("Target", new ImagePlusData(imp2), progressInfo);

        System.out.println();

    }
}
