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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.metadata;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.CustomExpressionVariablesParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.OptionalJIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.Image5DExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.Map;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Change image properties from expressions", description = "Sets properties of the input images to values extracted from expressions.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Modify")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
public class ChangeImageMetadataFromExpressionsAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    private final CustomExpressionVariablesParameter customFilterVariables;
    private OptionalJIPipeExpressionParameter imageTitle = new OptionalJIPipeExpressionParameter(false, "title");

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ChangeImageMetadataFromExpressionsAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.customFilterVariables = new CustomExpressionVariablesParameter(this);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public ChangeImageMetadataFromExpressionsAlgorithm(ChangeImageMetadataFromExpressionsAlgorithm other) {
        super(other);
        this.imageTitle = new OptionalJIPipeExpressionParameter(other.imageTitle);
        this.customFilterVariables = new CustomExpressionVariablesParameter(other.customFilterVariables, this);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        ImagePlus imagePlus = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getDuplicateImage();

        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        customFilterVariables.writeToVariables(variables, true, "custom.", true, "custom");
        variables.set("title", StringUtils.nullToEmpty(imagePlus.getTitle()));
        variables.set("width", imagePlus.getWidth());
        variables.set("height", imagePlus.getHeight());
        variables.set("num_c", imagePlus.getNChannels());
        variables.set("num_z", imagePlus.getNSlices());
        variables.set("num_t", imagePlus.getNFrames());
        variables.set("num_d", imagePlus.getNDimensions());

        // Make metadata accessible
        Map<String, String> roiProperties = ImageJUtils.getImageProperties(imagePlus);
        variables.set("metadata", roiProperties);
        for (Map.Entry<String, String> entry : roiProperties.entrySet()) {
            variables.set("metadata." + entry.getKey(), entry.getValue());
        }

        if (imageTitle.isEnabled()) {
            imagePlus.setTitle(imageTitle.getContent().evaluateToString(variables));
        }

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(imagePlus), progressInfo);
    }

    @JIPipeDocumentation(name = "Image title", description = "Allows to change the image title")
    @JIPipeParameter("image-title")
    @JIPipeExpressionParameterVariable(fromClass = Image5DExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = TextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @JIPipeExpressionParameterVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @JIPipeExpressionParameterVariable(key = "metadata", name = "Image metadata", description = "A map containing the image metadata/properties (string keys, string values)")
    @JIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "Image metadata/properties accessible via their string keys")
    @JIPipeExpressionParameterVariable(key = "name", description = "The current name of the image")
    public OptionalJIPipeExpressionParameter getImageTitle() {
        return imageTitle;
    }

    @JIPipeParameter("image-title")
    public void setImageTitle(OptionalJIPipeExpressionParameter imageTitle) {
        this.imageTitle = imageTitle;
    }
}
