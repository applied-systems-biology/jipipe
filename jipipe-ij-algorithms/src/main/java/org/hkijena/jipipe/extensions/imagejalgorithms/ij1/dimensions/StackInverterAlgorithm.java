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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.dimensions;

import ij.ImagePlus;
import ij.plugin.StackReverser;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.ImagePlus3DData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ImageProcessor}
 */
@JIPipeDocumentation(name = "Invert 3D stack Z-order", description = "Inverts the order of a Z-stack.")
@JIPipeOrganization(menuPath = "Dimensions", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlus3DData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlus3DData.class, slotName = "Output")
public class StackInverterAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public StackInverterAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlus3DData.class)
                .addOutputSlot("Output", ImagePlus3DData.class, "Input")
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public StackInverterAlgorithm(StackInverterAlgorithm other) {
        super(other);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlus img = dataBatch.getInputData(getFirstInputSlot(), ImagePlus3DData.class).getDuplicateImage();
        StackReverser reverser = new StackReverser();
        reverser.flipStack(img);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlus3DData(img));
    }
}
