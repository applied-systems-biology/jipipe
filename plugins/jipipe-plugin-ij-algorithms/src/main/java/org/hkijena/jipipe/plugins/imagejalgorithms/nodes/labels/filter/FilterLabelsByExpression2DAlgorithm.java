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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.labels.filter;

import com.google.common.primitives.Ints;
import gnu.trove.list.array.TIntArrayList;
import ij.ImagePlus;
import inra.ijpb.label.LabelImages;
import org.hkijena.jipipe.api.*;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.*;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.ImageJAlgorithmUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

import java.util.HashSet;
import java.util.Set;

@SetJIPipeDocumentation(name = "Filter labels by expression 2D", description = "Filters labels with a filter expression. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(menuPath = "Labels\nFilter", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Output", create = true)
@AddJIPipeCitation("Legland, D.; Arganda-Carreras, I. & Andrey, P. (2016), \"MorphoLibJ: integrated library and plugins for mathematical morphology with ImageJ\", " +
        "Bioinformatics (Oxford Univ Press) 32(22): 3532-3534, PMID 27412086, doi:10.1093/bioinformatics/btw413")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nMorphoLibJ\nLabel Images")
public class FilterLabelsByExpression2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeExpressionParameter expression = new JIPipeExpressionParameter("id > 10 AND num_pixels > 50");

    public FilterLabelsByExpression2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public FilterLabelsByExpression2DAlgorithm(FilterLabelsByExpression2DAlgorithm other) {
        super(other);
        this.expression = new JIPipeExpressionParameter(other.expression);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus outputImage = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getDuplicateImage();

        ImageJUtils.forEachIndexedZCTSlice(outputImage, (ip, index) -> {
            int[] allLabels = LabelImages.findAllLabels(ip);
            int[] numPixels = LabelImages.pixelCount(ip, allLabels);
            TIntArrayList keptLabels = new TIntArrayList();
            JIPipeExpressionVariablesMap parameters = new JIPipeExpressionVariablesMap(iterationStep);
            parameters.set("all.id", Ints.asList(allLabels));
            parameters.set("all.num_pixels", Ints.asList(numPixels));
            for (int i = 0; i < allLabels.length; i++) {
                parameters.set("id", allLabels[i]);
                parameters.set("num_pixels", numPixels[i]);
                if (expression.test(parameters)) {
                    keptLabels.add(allLabels[i]);
                }
            }
            ImageJAlgorithmUtils.removeLabelsExcept(ip, keptLabels.toArray());
        }, progressInfo);

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(outputImage), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Keep label if ...", description = "This filter expression determines which labels are kept. Annotations are available as variables.")
    @JIPipeExpressionParameterSettings(variableSource = VariablesInfo.class, hint = "per label")
    @JIPipeParameter("expression")
    public JIPipeExpressionParameter getExpression() {
        return expression;
    }

    @JIPipeParameter("expression")
    public void setExpression(JIPipeExpressionParameter expression) {
        this.expression = expression;
    }

    public static class VariablesInfo implements JIPipeExpressionVariablesInfo {
        @Override
        public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
            Set<JIPipeExpressionParameterVariableInfo> result = new HashSet<>();
            result.add(new JIPipeExpressionParameterVariableInfo("id", "Label ID", "The ID of the label (number larger than zero)"));
            result.add(new JIPipeExpressionParameterVariableInfo("all.id", "All Label IDs", "All label IDs as list"));
            result.add(new JIPipeExpressionParameterVariableInfo("num_pixels", "Label size", "The number of pixels associated to this label"));
            result.add(new JIPipeExpressionParameterVariableInfo("all.num_pixels", "All label sizes", "All number of pixels as list"));
            return result;
        }
    }
}
