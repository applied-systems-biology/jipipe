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

package org.hkijena.jipipe.extensions.imagejdatatypes.algorithms;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeDataParameterSettings;

/**
 * Converts ImageJ data type into each other
 */
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", create = true)
@SetJIPipeDocumentation(name = "Convert ImageJ image", description = "Converts an ImageJ image into another ImageJ image data type")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class)
public class ImageTypeConverter extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeDataInfoRef outputType = new JIPipeDataInfoRef(ImagePlusData.class);

    /**
     * Creates a new instance
     *
     * @param info Algorithm info
     */
    public ImageTypeConverter(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public ImageTypeConverter(ImageTypeConverter other) {
        super(other);
        this.outputType = other.outputType;
    }

    @SetJIPipeDocumentation(name = "Output image type", description = "Determines the output image type")
    @JIPipeParameter(value = "output-type", important = true)
    @JIPipeDataParameterSettings(dataBaseClass = ImagePlusData.class)
    public JIPipeDataInfoRef getOutputType() {
        return outputType;
    }

    @JIPipeParameter("output-type")
    public void setOutputType(JIPipeDataInfoRef outputType) {
        if (outputType == null || outputType.getInfo() == null) {
            this.outputType = new JIPipeDataInfoRef(ImagePlusData.class);
        }
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData data = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        JIPipeData converted = JIPipe.createData(outputType.getInfo().getDataClass(), data.getImage());
        iterationStep.addOutputData(getFirstOutputSlot(), converted, progressInfo);
    }

}
