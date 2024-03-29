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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.metadata;

import ij.ImagePlus;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.OptionalJIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.extensions.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.Image5DExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.Map;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@SetJIPipeDocumentation(name = "Change image properties from expressions", description = "Sets properties of the input images to values extracted from expressions.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Modify")
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", create = true)
public class ChangeImageMetadataFromExpressionsAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    private OptionalJIPipeExpressionParameter imageTitle = new OptionalJIPipeExpressionParameter(false, "title");

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ChangeImageMetadataFromExpressionsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public ChangeImageMetadataFromExpressionsAlgorithm(ChangeImageMetadataFromExpressionsAlgorithm other) {
        super(other);
        this.imageTitle = new OptionalJIPipeExpressionParameter(other.imageTitle);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus imagePlus = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getDuplicateImage();

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        getDefaultCustomExpressionVariables().writeToVariables(variables);
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

    @Override
    public boolean isEnableDefaultCustomExpressionVariables() {
        return true;
    }

    @SetJIPipeDocumentation(name = "Image title", description = "Allows to change the image title")
    @JIPipeParameter("image-title")
    @JIPipeExpressionParameterVariable(fromClass = Image5DExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
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
