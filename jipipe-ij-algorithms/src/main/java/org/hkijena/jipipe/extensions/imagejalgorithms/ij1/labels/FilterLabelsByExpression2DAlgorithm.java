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

import com.google.common.primitives.Ints;
import gnu.trove.list.array.TIntArrayList;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import inra.ijpb.label.LabelImages;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameters;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

import java.util.HashSet;
import java.util.Set;

@JIPipeDocumentation(name = "Filter labels by expression 2D", description = "Filters labels with a filter expression. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeOrganization(menuPath = "Labels", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", autoCreate = true)
public class FilterLabelsByExpression2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private DefaultExpressionParameter expression = new DefaultExpressionParameter("value > 10 AND num_pixels > 50");

    public FilterLabelsByExpression2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public FilterLabelsByExpression2DAlgorithm(FilterLabelsByExpression2DAlgorithm other) {
        super(other);
        this.expression = new DefaultExpressionParameter(other.expression);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();
        ImageStack stack = new ImageStack(inputImage.getWidth(), inputImage.getHeight(), inputImage.getStackSize());

        ImageJUtils.forEachIndexedZCTSlice(inputImage, (ip, index ) -> {
            int[] allLabels = LabelImages.findAllLabels(ip);
            int[] numPixels = LabelImages.pixelCount(ip, allLabels);
            TIntArrayList keptLabels = new TIntArrayList();
            ExpressionParameters parameters = new ExpressionParameters();
            for (int i = 0; i < allLabels.length; i++) {
                parameters.set("id", allLabels[i]);
                parameters.set("num_pixels", numPixels[i]);
                if(expression.test(parameters)) {
                    keptLabels.add(allLabels[i]);
                }
            }
            ImageProcessor modified = ip.duplicate();
            LabelImages.keepLabels(modified, keptLabels.toArray());
            stack.setProcessor(modified, index.zeroSliceIndexToOneStackIndex(inputImage));
        }, progressInfo);

        ImagePlus outputImage = new ImagePlus("Filtered", stack);

        outputImage.setDimensions(inputImage.getNChannels(), inputImage.getNSlices(), inputImage.getNFrames());
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(outputImage), progressInfo);
    }

    @JIPipeDocumentation(name = "Filter expression", description = "This filter expression determines which labels are kept.")
    @ExpressionParameterSettings(variableSource = VariableSource.class)
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
