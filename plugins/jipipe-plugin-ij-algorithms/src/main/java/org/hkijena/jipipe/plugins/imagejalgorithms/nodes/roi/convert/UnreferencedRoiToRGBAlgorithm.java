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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.convert;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.filter.Filler;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.measure.RoiStatisticsAlgorithm;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ROIElementDrawingMode;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.Measurement;
import org.hkijena.jipipe.plugins.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalDoubleParameter;
import org.hkijena.jipipe.plugins.parameters.library.roi.Margin;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@SetJIPipeDocumentation(name = "Convert only 2D ROI to RGB", description = "Converts ROI lists to color images. The line and fill color is stored within the ROI themselves. " +
        "This algorithm does not need a reference image that determines the output size.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Convert")
@AddJIPipeInputSlot(value = ROI2DListData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusColorRGBData.class, name = "Output", create = true)
public class UnreferencedRoiToRGBAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private Margin imageArea = new Margin();
    private ROIElementDrawingMode drawOutlineMode = ROIElementDrawingMode.Always;
    private ROIElementDrawingMode drawFilledOutlineMode = ROIElementDrawingMode.IfAvailable;
    private boolean drawLabel = false;
    private OptionalColorParameter overrideFillColor = new OptionalColorParameter();
    private OptionalColorParameter overrideLineColor = new OptionalColorParameter();
    private OptionalDoubleParameter overrideLineWidth = new OptionalDoubleParameter();
    private boolean preferAssociatedImage = true;
    private boolean drawOver = true;
    private boolean ignoreZ = false;
    private boolean ignoreC = false;
    private boolean ignoreT = false;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public UnreferencedRoiToRGBAlgorithm(JIPipeNodeInfo info) {
        super(info);
        imageArea.getWidth().ensureExactValue(false);
        imageArea.getHeight().ensureExactValue(false);
        overrideLineWidth.setContent(1.0);
        overrideFillColor.setContent(Color.RED);
        overrideLineColor.setContent(Color.YELLOW);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public UnreferencedRoiToRGBAlgorithm(UnreferencedRoiToRGBAlgorithm other) {
        super(other);
        this.imageArea = new Margin(other.imageArea);
        this.drawOutlineMode = other.drawOutlineMode;
        this.drawFilledOutlineMode = other.drawFilledOutlineMode;
        this.drawLabel = other.drawLabel;
        this.overrideFillColor = new OptionalColorParameter(other.overrideFillColor);
        this.overrideLineColor = new OptionalColorParameter(other.overrideLineColor);
        this.overrideLineWidth = new OptionalDoubleParameter(other.overrideLineWidth);
        this.preferAssociatedImage = other.preferAssociatedImage;
        this.drawOver = other.drawOver;
        this.ignoreC = other.ignoreC;
        this.ignoreZ = other.ignoreZ;
        this.ignoreT = other.ignoreT;
    }

    @SetJIPipeDocumentation(name = "Prefer ROI-associated images", description =
            "ROI can carry a reference to an image (e.g. the thresholding input). With this option enabled, this image is preferred to generating " +
                    "an output based on the pure ROIs.")
    @JIPipeParameter("prefer-associated-image")
    public boolean isPreferAssociatedImage() {
        return preferAssociatedImage;
    }

    @JIPipeParameter("prefer-associated-image")
    public void setPreferAssociatedImage(boolean preferAssociatedImage) {
        this.preferAssociatedImage = preferAssociatedImage;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        if (preferAssociatedImage) {
            for (Map.Entry<Optional<ImagePlus>, ROI2DListData> referenceEntry : iterationStep.getInputData(getFirstInputSlot(), ROI2DListData.class, progressInfo).groupByReferenceImage().entrySet()) {
                ROI2DListData inputData = (ROI2DListData) referenceEntry.getValue().duplicate(progressInfo);
                processROIList(iterationStep, inputData, referenceEntry.getKey().orElse(null), runContext, progressInfo);
            }
        } else {
            ROI2DListData inputData = (ROI2DListData) iterationStep.getInputData(getFirstInputSlot(), ROI2DListData.class, progressInfo).duplicate(progressInfo);
            processROIList(iterationStep, inputData, null, runContext, progressInfo);
        }

    }

    private void processROIList(JIPipeSingleIterationStep iterationStep, ROI2DListData inputData, ImagePlus reference, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        // Find the bounds and future stack position
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap(iterationStep);

        Rectangle bounds = imageArea.getInsideArea(inputData.getBounds(), variables);
        int sx = bounds.width + bounds.x;
        int sy = bounds.height + bounds.y;
        int sz = 1;
        int sc = 1;
        int st = 1;
        for (Roi roi : inputData) {
            int z = roi.getZPosition();
            int c = roi.getCPosition();
            int t = roi.getTPosition();
            sz = Math.max(sz, z);
            sc = Math.max(sc, c);
            st = Math.max(st, t);
        }

        // ROI statistics needed for labels
        Map<Roi, Point> roiCentroids = new HashMap<>();
        Map<Roi, Integer> roiIndices = new HashMap<>();
        Filler roiFiller = new Filler();
        if (drawLabel) {
            RoiStatisticsAlgorithm statisticsAlgorithm =
                    JIPipe.createNode("ij1-roi-statistics");
            statisticsAlgorithm.getMeasurements().setNativeValue(Measurement.Centroid.getNativeValue());
            statisticsAlgorithm.getInputSlot("ROI").addData(inputData, progressInfo);
            statisticsAlgorithm.run(runContext, progressInfo);
            ResultsTableData centroids = statisticsAlgorithm.getFirstOutputSlot().getData(0, ResultsTableData.class, progressInfo);
            for (int row = 0; row < centroids.getRowCount(); row++) {
                Point centroid = new Point((int) centroids.getValueAsDouble(row, "X"),
                        (int) centroids.getValueAsDouble(row, "Y"));
                roiCentroids.put(inputData.get(row), centroid);
                roiIndices.put(inputData.get(row), row);
            }
        }

        ImagePlus result;
        if (reference == null)
            result = IJ.createImage("ROIs", "RGB", sx, sy, sc, sz, st);
        else if (drawOver) {
            result = new ImagePlusColorRGBData(reference).getDuplicateImage();
            result.setTitle("ROIs+Reference");
        } else {
            result = IJ.createImage("ROIs",
                    "RGB",
                    reference.getWidth(),
                    reference.getHeight(),
                    reference.getNChannels(),
                    reference.getNSlices(),
                    reference.getNFrames());
        }


        // Draw ROI
        for (int z = 0; z < sz; z++) {
            for (int c = 0; c < sc; c++) {
                for (int t = 0; t < st; t++) {
                    int stackIndex = result.getStackIndex(c + 1, z + 1, t + 1);
                    ImageProcessor processor = result.getStack().getProcessor(stackIndex);
                    for (Roi roi : inputData) {
                        int rz = ignoreZ ? 0 : roi.getZPosition();
                        int rc = ignoreC ? 0 : roi.getCPosition();
                        int rt = ignoreT ? 0 : roi.getTPosition();
                        if (rz != 0 && rz != (z + 1))
                            continue;
                        if (rc != 0 && rc != (c + 1))
                            continue;
                        if (rt != 0 && rt != (t + 1))
                            continue;
                        if (drawFilledOutlineMode.shouldDraw(roi.getFillColor(), overrideFillColor.getContent())) {
                            Color color = (overrideFillColor.isEnabled() || roi.getFillColor() == null) ? overrideFillColor.getContent() : roi.getFillColor();
                            processor.setColor(color);
                            processor.fill(roi);
                        }
                        if (drawOutlineMode.shouldDraw(roi.getStrokeColor(), overrideLineColor.getContent())) {
                            Color color = (overrideLineColor.isEnabled() || roi.getStrokeColor() == null) ? overrideLineColor.getContent() : roi.getStrokeColor();
                            int width = (overrideLineWidth.isEnabled() || roi.getStrokeWidth() <= 0) ? (int) (double) (overrideLineWidth.getContent()) : (int) roi.getStrokeWidth();
                            processor.setLineWidth(width);
                            processor.setColor(color);
                            roi.drawPixels(processor);
                        }
                        if (drawLabel) {
                            Point centroid = roiCentroids.get(roi);
                            roiFiller.drawLabel(result, processor, roiIndices.get(roi), new Rectangle(centroid.x, centroid.y, 0, 0));
                        }
                    }
                }
            }
        }

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Image area", description = "Allows modification of the output image width and height.")
    @JIPipeParameter("image-area")
    public Margin getImageArea() {
        return imageArea;
    }

    @JIPipeParameter("image-area")
    public void setImageArea(Margin imageArea) {
        this.imageArea = imageArea;
    }

    @SetJIPipeDocumentation(name = "Draw outline", description = "If enabled, draw a white outline of the ROI")
    @JIPipeParameter("draw-outline-mode")
    public ROIElementDrawingMode getDrawOutlineMode() {
        return drawOutlineMode;
    }

    @JIPipeParameter("draw-outline-mode")
    public void setDrawOutlineMode(ROIElementDrawingMode drawOutlineMode) {
        this.drawOutlineMode = drawOutlineMode;
    }

    @SetJIPipeDocumentation(name = "Draw filled outline", description = "If enabled, fill the ROI areas")
    @JIPipeParameter("fill-outline-mode")
    public ROIElementDrawingMode getDrawFilledOutlineMode() {
        return drawFilledOutlineMode;
    }

    @JIPipeParameter("fill-outline-mode")
    public void setDrawFilledOutlineMode(ROIElementDrawingMode drawFilledOutlineMode) {
        this.drawFilledOutlineMode = drawFilledOutlineMode;
    }

    @SetJIPipeDocumentation(name = "Override fill color", description = "If enabled, the fill color will be overridden by this value. " +
            "If a ROI has no fill color, it will always fall back to this color.")
    @JIPipeParameter("override-fill-color")
    public OptionalColorParameter getOverrideFillColor() {
        return overrideFillColor;
    }

    @JIPipeParameter("override-fill-color")
    public void setOverrideFillColor(OptionalColorParameter overrideFillColor) {
        this.overrideFillColor = overrideFillColor;
    }

    @SetJIPipeDocumentation(name = "Override line color", description = "If enabled, the line color will be overridden by this value. " +
            "If a ROI has no line color, it will always fall back to this color.")
    @JIPipeParameter("override-line-color")
    public OptionalColorParameter getOverrideLineColor() {
        return overrideLineColor;
    }

    @JIPipeParameter("override-line-color")
    public void setOverrideLineColor(OptionalColorParameter overrideLineColor) {
        this.overrideLineColor = overrideLineColor;
    }

    @SetJIPipeDocumentation(name = "Override line width", description = "If enabled, the line width will be overridden by this value. " +
            "If a ROI has a line width equal or less than zero, it will fall back to this value.")
    @JIPipeParameter("override-line-width")
    public OptionalDoubleParameter getOverrideLineWidth() {
        return overrideLineWidth;
    }

    @JIPipeParameter("override-line-width")
    public void setOverrideLineWidth(OptionalDoubleParameter overrideLineWidth) {
        this.overrideLineWidth = overrideLineWidth;
    }

    @SetJIPipeDocumentation(name = "Draw labels", description = "If enabled, draw the ROI labels")
    @JIPipeParameter("draw-label")
    public boolean isDrawLabel() {
        return drawLabel;
    }

    @JIPipeParameter("draw-label")
    public void setDrawLabel(boolean drawLabel) {
        this.drawLabel = drawLabel;
    }

    @SetJIPipeDocumentation(name = "Draw over reference", description = "If enabled, draw the ROI over the reference image.")
    @JIPipeParameter("draw-over")
    public boolean isDrawOver() {
        return drawOver;
    }

    @JIPipeParameter("draw-over")
    public void setDrawOver(boolean drawOver) {
        this.drawOver = drawOver;
    }

    @SetJIPipeDocumentation(name = "Ignore Z", description = "If enabled, ROI will show outside their Z layer")
    @JIPipeParameter("ignore-z")
    public boolean isIgnoreZ() {
        return ignoreZ;
    }

    @JIPipeParameter("ignore-z")
    public void setIgnoreZ(boolean ignoreZ) {
        this.ignoreZ = ignoreZ;
    }

    @SetJIPipeDocumentation(name = "Ignore channel", description = "If enabled, ROI will show outside their channel (C) layer")
    @JIPipeParameter("ignore-c")
    public boolean isIgnoreC() {
        return ignoreC;
    }

    @JIPipeParameter("ignore-c")
    public void setIgnoreC(boolean ignoreC) {
        this.ignoreC = ignoreC;
    }

    @SetJIPipeDocumentation(name = "Ignore frame", description = "If enabled, ROI will show outside their frame (T) layer")
    @JIPipeParameter("ignore-t")
    public boolean isIgnoreT() {
        return ignoreT;
    }

    @JIPipeParameter("ignore-t")
    public void setIgnoreT(boolean ignoreT) {
        this.ignoreT = ignoreT;
    }
}
