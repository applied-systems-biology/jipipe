package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.labels;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeCitation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;

import java.util.HashSet;
import java.util.Set;

@JIPipeDocumentation(name = "Labels to ROI", description = "Converts a label image into a set of ROI. Labels must have a value larger than zero to be detected." +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice. The Z/C/T coordinates of the source slices are saved inside the ROI.")
@JIPipeCitation("Based on 'LabelsToROIs'; Waisman, A., Norris, A. ., Elías Costa , . et al. " +
        "Automatic and unbiased segmentation and quantification of myofibers in skeletal muscle. Sci Rep 11, 11793 (2021). doi: https://doi.org/10.1038/s41598-021-91191-6.")
@JIPipeNode(menuPath = "Labels", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Labels", description = "The labels image", autoCreate = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "ROI", description = "The generated ROI", autoCreate = true)
public class LabelsToROIAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private DefaultExpressionParameter labelNameExpression = new DefaultExpressionParameter("\"label-\" + TO_INTEGER(index)");

    public LabelsToROIAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public LabelsToROIAlgorithm(LabelsToROIAlgorithm other) {
        super(other);
        this.labelNameExpression = new DefaultExpressionParameter(other.labelNameExpression);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus labelsImage = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getDuplicateImage();
        ROIListData rois = new ROIListData();

        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());

        ImageJUtils.forEachIndexedZCTSlice(labelsImage, (ip, index) -> {
            ImageProcessor copy = (ImageProcessor) ip.clone();
            ImagePlus wrapper = new ImagePlus("slice", copy);
            for (int y = 0; y < copy.getHeight(); y++) {
                for (int x = 0; x < copy.getWidth(); x++) {
                    float value = copy.getf(x, y);
                    if(value > 0) {
                        IJ.doWand(wrapper, x, y, 0, "Legacy smooth");
                        Roi roi = wrapper.getRoi();
                        rois.add(roi);
                        copy.setColor(0);
                        copy.fill(roi);
                        wrapper.setRoi((Roi) null);
                        roi.setPosition(index.getC() + 1, index.getZ() + 1, index.getT() + 1);

                        variables.set("index", value);
                        variables.set("x", roi.getBounds().x);
                        variables.set("y", roi.getBounds().y);
                        variables.set("width", roi.getBounds().width);
                        variables.set("height", roi.getBounds().height);

                        String name = labelNameExpression.evaluateToString(variables);
                        roi.setName(name);
                    }
                }
            }
        }, progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(), rois, progressInfo);
    }

    @JIPipeDocumentation(name = "Label name", description = "Expression for the generation of the label name")
    @ExpressionParameterSettings(variableSource = VariableSource.class)
    @JIPipeParameter("label-name-expression")
    public DefaultExpressionParameter getLabelNameExpression() {
        return labelNameExpression;
    }

    @JIPipeParameter("label-name-expression")
    public void setLabelNameExpression(DefaultExpressionParameter labelNameExpression) {
        this.labelNameExpression = labelNameExpression;
    }

    public static class VariableSource implements ExpressionParameterVariableSource {

        @Override
        public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
            Set<ExpressionParameterVariable> variables = new HashSet<>();
            variables.add(ExpressionParameterVariable.ANNOTATIONS_VARIABLE);
            variables.add(new ExpressionParameterVariable("Label index", "The index of the label (value > 0)", "index"));
            variables.add(new ExpressionParameterVariable("Bounding box X", "Top-left X coordinate of the bounding box around the ROI", "x"));
            variables.add(new ExpressionParameterVariable("Bounding box Y", "Top-left Y coordinate of the bounding box around around the ROI", "y"));
            variables.add(new ExpressionParameterVariable("Bounding box width", "Width of the bounding box around around the ROI", "width"));
            variables.add(new ExpressionParameterVariable("Bounding box height", "Height of the bounding box around around the ROI", "height"));
            return variables;
        }
    }
}
