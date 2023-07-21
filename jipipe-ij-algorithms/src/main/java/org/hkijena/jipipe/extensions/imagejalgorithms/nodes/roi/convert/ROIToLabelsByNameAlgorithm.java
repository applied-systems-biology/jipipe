package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi.convert;

import com.google.common.primitives.Ints;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@JIPipeDocumentation(name = "ROI to Labels (by name)", description = "Converts ROI and an optional reference image into a label image. " +
        "The label value is provided by mapping the name to a value. If no name mapping is provided for a ROI, a unique label is generated. If no reference image is provided, " +
        "the dimensions are estimated from the ROI. The background color (where no ROI is located) is zero.")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Convert")
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", description = "Optional reference image used to calculate the size of the output", autoCreate = true, optional = true)
@JIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Labels", description = "Output label image", autoCreate = true)
public class ROIToLabelsByNameAlgorithm extends JIPipeIteratingAlgorithm {

    private ParameterCollectionList labelAssignment = ParameterCollectionList.containingCollection(ROINameToLabelEntry.class);
    private boolean drawOutline = false;
    private boolean fillOutline = true;

    public ROIToLabelsByNameAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ROIToLabelsByNameAlgorithm(ROIToLabelsByNameAlgorithm other) {
        super(other);
        this.drawOutline = other.drawOutline;
        this.fillOutline = other.fillOutline;
        this.labelAssignment = new ParameterCollectionList(other.labelAssignment);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ExpressionVariables parameters = new ExpressionVariables();
        parameters.putAnnotations(dataBatch.getMergedTextAnnotations());

        ROIListData rois = dataBatch.getInputData("ROI", ROIListData.class, progressInfo);

        // Create result image
        ImagePlus result;
        if (dataBatch.getInputRow("Reference") >= 0) {
            ImagePlus reference = dataBatch.getInputData("Reference", ImagePlusData.class, progressInfo).getImage();
            result = IJ.createHyperStack("Labels",
                    reference.getWidth(),
                    reference.getHeight(),
                    reference.getNChannels(),
                    reference.getNSlices(),
                    reference.getNFrames(),
                    32);
        } else {
            Rectangle bounds = rois.getBounds();
            int sx = bounds.width + bounds.x;
            int sy = bounds.height + bounds.y;
            int sz = 1;
            int sc = 1;
            int st = 1;
            for (Roi roi : rois) {
                int z = roi.getZPosition();
                int c = roi.getCPosition();
                int t = roi.getTPosition();
                sz = Math.max(sz, z);
                sc = Math.max(sc, c);
                st = Math.max(st, t);
            }
            result = IJ.createHyperStack("Labels",
                    sx,
                    sy,
                    sc,
                    sz,
                    st,
                    32);
        }

        // Find naming
        Map<String, Integer> nameToLabelMapping = new HashMap<>();
        for (ROINameToLabelEntry entry : labelAssignment.mapToCollection(ROINameToLabelEntry.class)) {
            progressInfo.log("Found mapping from '" + entry.getName() + "' to label " + entry.getLabel());
            nameToLabelMapping.put(entry.getName(), entry.getLabel());
        }

        // Calculate labels
        int[] labelMap = new int[rois.size()];
        boolean[] labelMapSuccesses = new boolean[rois.size()];
        for (int i = 0; i < rois.size(); i++) {
            Roi roi = rois.get(i);
            Integer label = nameToLabelMapping.getOrDefault(StringUtils.nullToEmpty(roi.getName()), null);
            if (label != null) {
                labelMap[i] = label;
                labelMapSuccesses[i] = true;
            }
        }
        if (labelMap.length > 0) {
            int label = Ints.max(labelMap) + 1;
            for (int i = 0; i < rois.size(); i++) {
                if (!labelMapSuccesses[i]) {
                    labelMap[i] = label;
                    ++label;
                }
            }
        }

        // Apply labels
        for (int z = 0; z < result.getNSlices(); z++) {
            for (int c = 0; c < result.getNChannels(); c++) {
                for (int t = 0; t < result.getNFrames(); t++) {
                    int stackIndex = result.getStackIndex(c + 1, z + 1, t + 1);
                    ImageProcessor processor = result.getStack().getProcessor(stackIndex);

                    for (int i = 0; i < rois.size(); i++) {
                        Roi roi = rois.get(i);
                        int rz = roi.getZPosition();
                        int rc = roi.getCPosition();
                        int rt = roi.getTPosition();
                        if (rz != 0 && rz != (z + 1))
                            continue;
                        if (rc != 0 && rc != (c + 1))
                            continue;
                        if (rt != 0 && rt != (t + 1))
                            continue;

                        processor.setColor(labelMap[i]);
                        if (fillOutline)
                            processor.fill(roi);
                        if (drawOutline)
                            roi.drawPixels(processor);
                    }
                }
            }
        }

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscale32FData(result), progressInfo);
    }

    @JIPipeDocumentation(name = "Draw outline", description = "If enabled, the label value is drawn as outline")
    @JIPipeParameter("draw-outline")
    public boolean isDrawOutline() {
        return drawOutline;
    }

    @JIPipeParameter("draw-outline")
    public void setDrawOutline(boolean drawOutline) {
        this.drawOutline = drawOutline;
    }

    @JIPipeDocumentation(name = "Fill outline", description = "If enabled, the ROI is filled with the label value")
    @JIPipeParameter("fill-outline")
    public boolean isFillOutline() {
        return fillOutline;
    }

    @JIPipeParameter("fill-outline")
    public void setFillOutline(boolean fillOutline) {
        this.fillOutline = fillOutline;
    }

    @JIPipeDocumentation(name = "Label assignment", description = "Add items into the list to assign ROI names to labels.")
    @JIPipeParameter("label-assignment")
    public ParameterCollectionList getLabelAssignment() {
        return labelAssignment;
    }

    @JIPipeParameter("label-assignment")
    public void setLabelAssignment(ParameterCollectionList labelAssignment) {
        this.labelAssignment = labelAssignment;
    }

    public static class VariableSource implements ExpressionParameterVariableSource {

        public static final Set<ExpressionParameterVariable> VARIABLES;

        static {
            VARIABLES = new HashSet<>();
            VARIABLES.add(ExpressionParameterVariable.ANNOTATIONS_VARIABLE);
            VARIABLES.add(new ExpressionParameterVariable("Index", "Automatically assigned index of the roi (min 1)", "index"));
            VARIABLES.add(new ExpressionParameterVariable("Name", "Name of the ROI (can be empty)", "name"));
            VARIABLES.add(new ExpressionParameterVariable("Bounding box X", "Top-left X coordinate of the bounding box around the ROI", "x"));
            VARIABLES.add(new ExpressionParameterVariable("Bounding box Y", "Top-left Y coordinate of the bounding box around around the ROI", "y"));
            VARIABLES.add(new ExpressionParameterVariable("Bounding box width", "Width of the bounding box around around the ROI", "width"));
            VARIABLES.add(new ExpressionParameterVariable("Bounding box height", "Height of the bounding box around around the ROI", "height"));
        }

        @Override
        public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
            return VARIABLES;
        }
    }

    public static class ROINameToLabelEntry extends AbstractJIPipeParameterCollection {
        private String name;
        private int label;

        public ROINameToLabelEntry() {
        }

        public ROINameToLabelEntry(ROINameToLabelEntry other) {
            this.name = other.name;
            this.label = other.label;
        }

        @JIPipeDocumentation(name = "ROI name")
        @JIPipeParameter(value = "name", uiOrder = -100)
        public String getName() {
            return name;
        }

        @JIPipeParameter("name")
        public void setName(String name) {
            this.name = name;
        }

        @JIPipeDocumentation(name = "Assign label")
        @JIPipeParameter(value = "label", uiOrder = -50)
        public int getLabel() {
            return label;
        }

        @JIPipeParameter("label")
        public void setLabel(int label) {
            this.label = label;
        }
    }
}
