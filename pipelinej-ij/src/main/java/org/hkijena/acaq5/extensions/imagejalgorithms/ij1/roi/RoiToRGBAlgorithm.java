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

package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.roi;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.filter.Filler;
import ij.process.ImageProcessor;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQDefaultMutableSlotConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.measure.Measurement;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.acaq5.extensions.tables.ResultsTableData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.acaq5.extensions.parameters.colors.OptionalColorParameter;
import org.hkijena.acaq5.extensions.parameters.primitives.OptionalDoubleParameter;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.acaq5.api.algorithm.ACAQIteratingAlgorithm.ITERATING_ALGORITHM_DESCRIPTION;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@ACAQDocumentation(name = "Convert ROI to RGB", description = "Converts ROI lists to masks. The line and fill color is stored within the ROI themselves. " +
        "This algorithm needs a reference image that provides the output sizes. If you do not have a reference image, you can use the unreferenced variant."
        + "\n\n" + ITERATING_ALGORITHM_DESCRIPTION)
@ACAQOrganization(menuPath = "ROI", algorithmCategory = ACAQAlgorithmCategory.Converter)
@AlgorithmInputSlot(value = ROIListData.class, slotName = "ROI")
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Image")
@AlgorithmOutputSlot(value = ImagePlusColorRGBData.class, slotName = "Output")
public class RoiToRGBAlgorithm extends ACAQIteratingAlgorithm {

    private boolean drawOutline = true;
    private boolean drawFilledOutline = false;
    private boolean drawLabel = false;
    private OptionalColorParameter overrideFillColor = new OptionalColorParameter();
    private OptionalColorParameter overrideLineColor = new OptionalColorParameter();
    private OptionalDoubleParameter overrideLineWidth = new OptionalDoubleParameter();
    private boolean drawOver = false;

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public RoiToRGBAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQDefaultMutableSlotConfiguration.builder().addInputSlot("ROI", ROIListData.class)
                .addInputSlot("Image", ImagePlusData.class)
                .addOutputSlot("Output", ImagePlusColorRGBData.class, null)
                .seal()
                .build());
        overrideLineWidth.setContent(1.0);
        overrideFillColor.setContent(Color.RED);
        overrideLineColor.setContent(Color.YELLOW);
    }

    /**
     * Instantiates a new algorithm.
     *
     * @param other the other
     */
    public RoiToRGBAlgorithm(RoiToRGBAlgorithm other) {
        super(other);
        this.drawOutline = other.drawOutline;
        this.drawFilledOutline = other.drawFilledOutline;
        this.drawLabel = other.drawLabel;
        this.overrideFillColor = new OptionalColorParameter(other.overrideFillColor);
        this.overrideLineColor = new OptionalColorParameter(other.overrideLineColor);
        this.overrideLineWidth = new OptionalDoubleParameter(other.overrideLineWidth);
        this.drawOver = other.drawOver;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ROIListData inputData = (ROIListData) dataInterface.getInputData("ROI", ROIListData.class).duplicate();
        ImagePlus reference = dataInterface.getInputData("Image", ImagePlusData.class).getImage();

        // Find the bounds and future stack position
        int sx = reference.getWidth();
        int sy = reference.getHeight();
        int sz = reference.getNSlices();
        int sc = reference.getNChannels();
        int st = reference.getNFrames();

        // ROI statistics needed for labels
        Map<Roi, Point> roiCentroids = new HashMap<>();
        Map<Roi, Integer> roiIndices = new HashMap<>();
        Filler roiFiller = new Filler();
        if (drawLabel) {
            RoiStatisticsAlgorithm statisticsAlgorithm = ACAQAlgorithm.newInstance("ij1-roi-statistics");
            statisticsAlgorithm.setRequireReferenceImage(true);
            statisticsAlgorithm.getMeasurements().setNativeValue(Measurement.Centroid.getNativeValue());
            statisticsAlgorithm.getInputSlot("ROI").addData(inputData);
            statisticsAlgorithm.getInputSlot("Reference").addData(new ImagePlusData(reference));
            statisticsAlgorithm.run(subProgress.resolve("ROI statistics"), algorithmProgress, isCancelled);
            ResultsTableData centroids = statisticsAlgorithm.getFirstOutputSlot().getData(0, ResultsTableData.class);
            for (int row = 0; row < centroids.getRowCount(); row++) {
                Point centroid = new Point((int) centroids.getValueAsDouble(row, "X"),
                        (int) centroids.getValueAsDouble(row, "Y"));
                roiCentroids.put(inputData.get(row), centroid);
                roiIndices.put(inputData.get(row), row);
            }
        }

        ImagePlus result;
        if (drawOver) {
            result = ImagePlusColorRGBData.convertIfNeeded(reference.duplicate());
            result.setTitle("Reference+ROIs");
        } else {
            result = IJ.createImage("ROIs", "RGB", sx, sy, sc, sz, st);
        }

        // Draw ROI
        for (int z = 0; z < sz; z++) {
            for (int c = 0; c < sc; c++) {
                for (int t = 0; t < st; t++) {
                    int stackIndex = result.getStackIndex(c + 1, z + 1, t + 1);
                    ImageProcessor processor = result.getStack().getProcessor(stackIndex);
                    for (Roi roi : inputData) {
                        int rz = roi.getZPosition();
                        int rc = roi.getCPosition();
                        int rt = roi.getTPosition();
                        if (rz != 0 && rz != (z + 1))
                            continue;
                        if (rc != 0 && rc != (c + 1))
                            continue;
                        if (rt != 0 && rt != (t + 1))
                            continue;
                        if (drawFilledOutline) {
                            Color color = (overrideFillColor.isEnabled() || roi.getFillColor() == null) ? overrideFillColor.getContent() : roi.getFillColor();
                            processor.setColor(color);
                            processor.fill(roi);
                        }
                        if (drawOutline) {
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

        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusData(result));
    }


    @Override
    public void reportValidity(ACAQValidityReport report) {
    }

    @ACAQDocumentation(name = "Draw outline", description = "If enabled, draw a white outline of the ROI")
    @ACAQParameter("draw-outline")
    public boolean isDrawOutline() {
        return drawOutline;
    }

    @ACAQParameter("draw-outline")
    public void setDrawOutline(boolean drawOutline) {
        this.drawOutline = drawOutline;
    }

    @ACAQDocumentation(name = "Draw filled outline", description = "If enabled, fill the ROI areas")
    @ACAQParameter("fill-outline")
    public boolean isDrawFilledOutline() {
        return drawFilledOutline;
    }

    @ACAQParameter("fill-outline")
    public void setDrawFilledOutline(boolean drawFilledOutline) {
        this.drawFilledOutline = drawFilledOutline;
    }

    @ACAQDocumentation(name = "Draw labels", description = "If enabled, draw the ROI labels")
    @ACAQParameter("draw-label")
    public boolean isDrawLabel() {
        return drawLabel;
    }

    @ACAQParameter("draw-label")
    public void setDrawLabel(boolean drawLabel) {
        this.drawLabel = drawLabel;
    }

    @ACAQDocumentation(name = "Override fill color", description = "If enabled, the fill color will be overridden by this value. " +
            "If a ROI has no fill color, it will always fall back to this color.")
    @ACAQParameter("override-fill-color")
    public OptionalColorParameter getOverrideFillColor() {
        return overrideFillColor;
    }

    @ACAQParameter("override-fill-color")
    public void setOverrideFillColor(OptionalColorParameter overrideFillColor) {
        this.overrideFillColor = overrideFillColor;
    }

    @ACAQDocumentation(name = "Override line color", description = "If enabled, the line color will be overridden by this value. " +
            "If a ROI has no line color, it will always fall back to this color.")
    @ACAQParameter("override-line-color")
    public OptionalColorParameter getOverrideLineColor() {
        return overrideLineColor;
    }

    @ACAQParameter("override-line-color")
    public void setOverrideLineColor(OptionalColorParameter overrideLineColor) {
        this.overrideLineColor = overrideLineColor;
    }

    @ACAQDocumentation(name = "Override line width", description = "If enabled, the line width will be overridden by this value. " +
            "If a ROI has a line width equal or less than zero, it will fall back to this value.")
    @ACAQParameter("override-line-width")
    public OptionalDoubleParameter getOverrideLineWidth() {
        return overrideLineWidth;
    }

    @ACAQParameter("override-line-width")
    public void setOverrideLineWidth(OptionalDoubleParameter overrideLineWidth) {
        this.overrideLineWidth = overrideLineWidth;
    }

    @ACAQDocumentation(name = "Draw over reference", description = "If enabled, draw the ROI over the reference image.")
    @ACAQParameter("draw-over")
    public boolean isDrawOver() {
        return drawOver;
    }

    @ACAQParameter("draw-over")
    public void setDrawOver(boolean drawOver) {
        this.drawOver = drawOver;
    }
}
