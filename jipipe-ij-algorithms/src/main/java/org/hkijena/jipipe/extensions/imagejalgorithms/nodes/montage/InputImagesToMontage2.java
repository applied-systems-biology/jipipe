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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.montage;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeAlias;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeMultiDataBatch;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.MontageCreator;

import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Create montage", description = "Creates a montage of all input images. Supports 2D and 3D images.")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Montage")
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nStacks", aliasName = "Make Montage...")
public class InputImagesToMontage2 extends JIPipeMergingAlgorithm {

    private final MontageCreator montageCreator;

    public InputImagesToMontage2(JIPipeNodeInfo info) {
        super(info);
        this.montageCreator = new MontageCreator();
        registerSubParameters(montageCreator);
    }

    public InputImagesToMontage2(InputImagesToMontage2 other) {
        super(other);
        this.montageCreator = new MontageCreator(other.montageCreator);
        registerSubParameters(this);
    }


    @Override
    protected void runIteration(JIPipeMultiDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        List<MontageCreator.InputEntry> inputEntries = new ArrayList<>();
        for (Integer row : dataBatch.getInputRows(getFirstInputSlot())) {
            ImagePlus img = getFirstInputSlot().getData(row, ImagePlusData.class, progressInfo).getImage();
            List<JIPipeTextAnnotation> textAnnotations = getFirstInputSlot().getTextAnnotations(row);
            inputEntries.add(new MontageCreator.InputEntry(img, textAnnotations, new ExpressionVariables()));
        }
        ImagePlus montage = montageCreator.createMontage(inputEntries, new ExpressionVariables(), progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(montage), progressInfo);
    }

    @JIPipeDocumentation(name = "Montage", description = "General montage settings")
    @JIPipeParameter(value = "montage-parameters", uiOrder = -100)
    public MontageCreator getMontageCreator() {
        return montageCreator;
    }
}
