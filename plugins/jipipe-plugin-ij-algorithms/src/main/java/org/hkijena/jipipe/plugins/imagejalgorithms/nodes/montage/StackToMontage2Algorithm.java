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

import com.fasterxml.jackson.databind.JsonNode;
import ij.ImagePlus;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.taxonomy.frameworks.ImageJ1Framework;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.MergingSplittingFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ResamplingFunction;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.Image5DSliceIndexExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.MontageCreator;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.parameters.library.primitives.JIPipeFontFamilySizeStyleColorParameter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension.SPATIAL_2D;
import static org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension.SPATIAL_3D;

/**
 * Implementation of {@link ij.plugin.MontageMaker}
 */
@SetJIPipeDocumentation(name = "Stack to montage", description = "Converts an image stack into a montage. ")
@ConfigureJIPipeNode(function = {MergingSplittingFunction.class, ResamplingFunction.class}, framework = ImageJ1Framework.class, spatialDimensions = {SPATIAL_2D, SPATIAL_3D})
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlus2DData.class, name = "Output", create = true)
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
        ImageJIterationUtils.forEachIndexedZCTSlice(stack, (ip, index) -> {
            JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap(iterationStep);
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
                new JIPipeExpressionVariablesMap(iterationStep),
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
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = Image5DSliceIndexExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getSliceFilter() {
        return sliceFilter;
    }

    @JIPipeParameter("slice-filter")
    public void setSliceFilter(JIPipeExpressionParameter sliceFilter) {
        this.sliceFilter = sliceFilter;
    }

    /**
     * #1339: migrate the legacy split label-font parameters (label-font-family,
     * label-font-size) and the label-foreground color into the single
     * {@code label-font} parameter of the montage's label settings when a
     * pre-migration project is loaded. The nested collection is loaded via flat
     * slash-keys, so the keys are prefixed with
     * {@code montage-parameters/label-parameters/}. Idempotent.
     */
    @Override
    protected void onDeserialized(JsonNode node, JIPipeValidationReport issues, JIPipeNotificationInbox notifications) {
        String p = "montage-parameters/label-parameters/";
        if (node.has(p + "label-font-family") || node.has(p + "label-font-size") || node.has(p + "label-foreground")) {
            montageCreator.getLabelParameters().setLabelFont(JIPipeFontFamilySizeStyleColorParameter.fromLegacyKeys(node,
                    p + "label-font-family", p + "label-font-size", null, p + "label-foreground", 12));
        }
    }
}
