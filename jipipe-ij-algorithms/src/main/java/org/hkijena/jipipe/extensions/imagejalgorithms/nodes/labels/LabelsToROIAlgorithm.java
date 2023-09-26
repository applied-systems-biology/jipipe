package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.labels;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import inra.ijpb.binary.BinaryImages;
import inra.ijpb.label.LabelImages;
import org.hkijena.jipipe.api.*;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.imagejalgorithms.parameters.Neighborhood2D;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJAlgorithmUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;

import java.util.HashSet;
import java.util.Set;

@JIPipeDocumentation(name = "Labels to ROI", description = "Converts a label image into a set of ROI. Labels must have a value larger than zero to be detected." +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice. The Z/C/T coordinates of the source slices are saved inside the ROI.")
@JIPipeCitation("Based on 'LabelsToROIs'; Waisman, A., Norris, A. ., Elías Costa , . et al. " +
        "Automatic and unbiased segmentation and quantification of myofibers in skeletal muscle. Sci Rep 11, 11793 (2021). doi: https://doi.org/10.1038/s41598-021-91191-6.")
@JIPipeNode(menuPath = "Labels", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Labels", description = "The labels image", autoCreate = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "ROI", description = "The generated ROI", autoCreate = true)
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nMorphoLibJ\nLabel Images", aliasName = "Labels to ROI")
public class LabelsToROIAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private DefaultExpressionParameter labelNameExpression = new DefaultExpressionParameter("\"label-\" + TO_INTEGER(index)");
    private Method method = Method.Floodfill;

    private Neighborhood2D connectivity = Neighborhood2D.FourConnected;

    public LabelsToROIAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public LabelsToROIAlgorithm(LabelsToROIAlgorithm other) {
        super(other);
        this.method = other.method;
        this.connectivity = other.connectivity;
        this.labelNameExpression = new DefaultExpressionParameter(other.labelNameExpression);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus labelsImage = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getDuplicateImage();
        ROIListData rois = new ROIListData();

        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());

        ImageJUtils.forEachIndexedZCTSlice(labelsImage, (ip, index) -> {
            if (method == Method.ProtectedFloodfill)
                executeProtectedFloodfill(rois, variables, ip, index, progressInfo);
            else
                executeFloodfill(rois, variables, ip, index);
        }, progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(), rois, progressInfo);
    }

    private void executeProtectedFloodfill(ROIListData outputList, ExpressionVariables variables, ImageProcessor ip, ImageSliceIndex index, JIPipeProgressInfo progressInfo) {
        for (int targetLabel : LabelImages.findAllLabels(ip)) {
            if (progressInfo.isCancelled())
                return;

            ImageProcessor copy = (ImageProcessor) ip.clone();
            ByteProcessor mask = ImageJAlgorithmUtils.getLabelMask(copy, targetLabel);
            ImageProcessor components = BinaryImages.componentsLabeling(mask, connectivity.getNativeValue(), 32);

            // Continue with floodfill, but with a custom deleter
            ImagePlus wrapper = new ImagePlus("slice", components);
            for (int y = 0; y < components.getHeight(); y++) {
                for (int x = 0; x < components.getWidth(); x++) {
                    float value = components.getf(x, y);
                    if (value > 0) {
                        IJ.doWand(wrapper, x, y, 0, connectivity.getNativeValue() + " smooth");
                        Roi roi = wrapper.getRoi();
                        outputList.add(roi);

                        // Delete the label
                        LabelImages.replaceLabels(components, new int[]{(int) value}, 0);

                        wrapper.setRoi((Roi) null);
                        roi.setPosition(index.getC() + 1, index.getZ() + 1, index.getT() + 1);

                        variables.set("index", targetLabel);
                        variables.set("x", roi.getBounds().x);
                        variables.set("y", roi.getBounds().y);
                        variables.set("width", roi.getBounds().width);
                        variables.set("height", roi.getBounds().height);

                        String name = labelNameExpression.evaluateToString(variables);
                        roi.setName(name);
                    }
                }
            }
        }
    }

    private void executeFloodfill(ROIListData outputList, ExpressionVariables variables, ImageProcessor ip, ImageSliceIndex index) {
        ImageProcessor copy = (ImageProcessor) ip.clone();
        ImagePlus wrapper = new ImagePlus("slice", copy);
        for (int y = 0; y < copy.getHeight(); y++) {
            for (int x = 0; x < copy.getWidth(); x++) {
                float value = copy.getf(x, y);
                if (value > 0) {
                    IJ.doWand(wrapper, x, y, 0, "Legacy smooth");
                    Roi roi = wrapper.getRoi();
                    outputList.add(roi);
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

    @JIPipeDocumentation(name = "Method", description = "The algorithm responsible for converting labels into ROI")
    @JIPipeParameter(value = "method", important = true)
    public Method getMethod() {
        return method;
    }

    @JIPipeParameter("method")
    public void setMethod(Method method) {
        this.method = method;
        emitParameterUIChangedEvent();
    }

    @JIPipeDocumentation(name = "Connectivity", description = "The connectivity for the connected components algorithm")
    @JIPipeParameter("connectivity")
    public Neighborhood2D getConnectivity() {
        return connectivity;
    }

    @JIPipeParameter("connectivity")
    public void setConnectivity(Neighborhood2D connectivity) {
        this.connectivity = connectivity;
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if ("connectivity".equals(access.getKey()) && method != Method.ProtectedFloodfill)
            return false;
        return super.isParameterUIVisible(tree, access);
    }

    @JIPipeDocumentationDescription(description = "<ul>" +
            "<li>Floodfill: Original implementation by Waisman et al. that applies flood filling for each pixel and removes all labels within the detected area. This is a fast algorithm " +
            "suitable to detect labels that are not nested.</li>" +
            "<li>Protected floodfill: Modified algorithm that utilizes an additional connected components operation to prevent accidental removals. These can happen if one label is encased within another (e.g., structures within a tissue). " +
            "Please note that this algorithm is considerably slower than the 'Floodfill' method, especially for images with many different labels.</li>" +
            "</ul>")
    public enum Method {
        Floodfill,
        ProtectedFloodfill;


        @Override
        public String toString() {
            if (this == ProtectedFloodfill)
                return "Protected floodfill";
            return super.toString();
        }
    }

    public static class VariableSource implements ExpressionParameterVariableSource {

        @Override
        public Set<ExpressionParameterVariable> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
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
