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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.montage;

import ij.ImagePlus;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.Image5DSliceIndexExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.MontageCreator;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of {@link ij.plugin.MontageMaker}
 */
@SetJIPipeDocumentation(name = "Stack to montage", description = "Converts an image stack into a montage. ")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Montage")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlus2DData.class, name = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nStacks", aliasName = "Make Montage... (of stacks)")
public class StackToMontage2Algorithm extends JIPipeIteratingAlgorithm {

    private final MontageCreator montageCreator;
    private boolean addZCTAnnotations = true;
    private JIPipeExpressionParameter sliceFilter = new JIPipeExpressionParameter();

    public StackToMontage2Algorithm(JIPipeNodeInfo info) {
        super(info);
        this.montageCreator = new MontageCreator();
        this.montageCreator.setLabelExpression(new JIPipeExpressionParameter("z + \", \" + c + \", \" + t"));
        registerSubParameters(montageCreator);
    }

    public StackToMontage2Algorithm(StackToMontage2Algorithm other) {
        super(other);
        this.montageCreator = new MontageCreator(other.montageCreator);
        this.addZCTAnnotations = other.addZCTAnnotations;
        this.sliceFilter = new JIPipeExpressionParameter(other.sliceFilter);
        registerSubParameters(this);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        List<MontageCreator.InputEntry> inputEntries = new ArrayList<>();
        ImagePlus stack = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();
        ImageJUtils.forEachIndexedZCTSlice(stack, (ip, index) -> {
            JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
            variables.putAnnotations(iterationStep.getMergedTextAnnotations());
            Image5DSliceIndexExpressionParameterVariablesInfo.apply(variables, stack, index);

            if (sliceFilter.test(variables)) {
                ImagePlus imp = new ImagePlus("Slice", ip);
                inputEntries.add(new MontageCreator.InputEntry(imp, Collections.emptyList(), variables));
            } else {
                progressInfo.log("Skipped slice " + index + " (filter)");
            }
        }, progressInfo);

        ImagePlus montage = montageCreator.createMontage(inputEntries,
                new ArrayList<>(iterationStep.getMergedTextAnnotations().values()),
                new JIPipeExpressionVariablesMap(),
                progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(montage), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Montage", description = "General montage settings")
    @JIPipeParameter(value = "montage-parameters", uiOrder = -100)
    public MontageCreator getMontageCreator() {
        return montageCreator;
    }

    @SetJIPipeDocumentation(name = "Limit to slices", description = "Allows to limit the montage to specific slices")
    @JIPipeParameter("slice-filter")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = Image5DSliceIndexExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getSliceFilter() {
        return sliceFilter;
    }

    @JIPipeParameter("slice-filter")
    public void setSliceFilter(JIPipeExpressionParameter sliceFilter) {
        this.sliceFilter = sliceFilter;
    }
}
