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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.labels;

import gnu.trove.list.array.TIntArrayList;
import ij.ImagePlus;
import inra.ijpb.label.LabelImages;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJAlgorithmUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleData;

import java.util.HashSet;
import java.util.Set;

@JIPipeDocumentation(name = "Filter labels by expression 3D", description = "Filters labels with a filter expression. ")
@JIPipeNode(menuPath = "Labels\nFilter", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlus3DGreyscaleData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlus3DGreyscaleData.class, slotName = "Output", autoCreate = true)
public class FilterLabelsByExpression3DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private DefaultExpressionParameter expression = new DefaultExpressionParameter("id > 10 AND num_pixels > 50");

    public FilterLabelsByExpression3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public FilterLabelsByExpression3DAlgorithm(FilterLabelsByExpression3DAlgorithm other) {
        super(other);
        this.expression = new DefaultExpressionParameter(other.expression);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus image = dataBatch.getInputData(getFirstInputSlot(), ImagePlus3DGreyscaleData.class, progressInfo).getDuplicateImage();

        int[] allLabels = LabelImages.findAllLabels(image.getStack());
        int[] numPixels = LabelImages.pixelCount(image, allLabels);
        TIntArrayList keptLabels = new TIntArrayList();
        ExpressionVariables parameters = new ExpressionVariables();
        for (JIPipeTextAnnotation annotation : dataBatch.getMergedTextAnnotations().values()) {
            parameters.set(annotation.getName(), annotation.getValue());
        }
        for (int i = 0; i < allLabels.length; i++) {
            parameters.set("id", allLabels[i]);
            parameters.set("num_pixels", numPixels[i]);
            if (expression.test(parameters)) {
                keptLabels.add(allLabels[i]);
            }
        }

        org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils.forEachIndexedZCTSlice(image, (ip, index) -> ImageJAlgorithmUtils.removeLabelsExcept(ip, keptLabels.toArray()), progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlus3DGreyscaleData(image), progressInfo);
    }

    @JIPipeDocumentation(name = "Filter expression", description = "This filter expression determines which labels are kept. Annotations are available as variables.")
    @ExpressionParameterSettings(variableSource = FilterLabelsByExpression2DAlgorithm.VariableSource.class)
    @JIPipeParameter("expression")
    public DefaultExpressionParameter getExpression() {
        return expression;
    }

    @JIPipeParameter("expression")
    public void setExpression(DefaultExpressionParameter expression) {
        this.expression = expression;
    }

    public static class VariableSource implements ExpressionParameterVariableSource {
        @Override
        public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
            Set<ExpressionParameterVariable> result = new HashSet<>();
            result.add(new ExpressionParameterVariable("Label ID", "The ID of the label (number larger than zero)", "id"));
            result.add(new ExpressionParameterVariable("Label size", "The number of pixels associated to this label", "num_pixels"));
            return result;
        }
    }
}
