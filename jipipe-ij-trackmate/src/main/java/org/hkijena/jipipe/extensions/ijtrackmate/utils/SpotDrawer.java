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

package org.hkijena.jipipe.extensions.ijtrackmate.utils;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.visualization.hyperstack.SpotOverlay;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import org.apache.commons.lang3.Range;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.SpotsCollectionData;
import org.hkijena.jipipe.extensions.ijtrackmate.parameters.SpotFeature;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.RoiDrawer;
import org.hkijena.jipipe.extensions.parameters.library.primitives.BooleanParameterSettings;

import java.awt.*;
import java.util.Collection;

public class SpotDrawer extends AbstractJIPipeParameterCollection {
    private int strokeWidth = 1;

    private Color strokeColor = Color.RED;

    private boolean uniformStrokeColor = true;

    private SpotFeature strokeColorFeature = new SpotFeature(Spot.QUALITY);

    private LabelSettings labelSettings = new LabelSettings();

    private boolean fillSpots = false;

    public SpotDrawer() {
    }

    public SpotDrawer(SpotDrawer other) {
        copyFrom(other);
    }

    public void copyFrom(SpotDrawer other) {
        this.strokeWidth = other.strokeWidth;
        this.strokeColor = other.strokeColor;
        this.uniformStrokeColor = other.uniformStrokeColor;
        this.strokeColorFeature = new SpotFeature(other.strokeColorFeature);
        this.labelSettings = new LabelSettings(other.labelSettings);
        this.fillSpots = other.fillSpots;
    }

    public DisplaySettings createDisplaySettings(SpotsCollectionData spotsCollectionData) {
        DisplaySettings displaySettings = new DisplaySettings();
        displaySettings.setLineThickness(strokeWidth);
        displaySettings.setSpotFilled(fillSpots);
        if (uniformStrokeColor) {
            displaySettings.setSpotUniformColor(strokeColor);
        } else {
            Range<Double> range = spotsCollectionData.getSpotFeatureRange(strokeColorFeature.getValue());
            displaySettings.setSpotColorBy(DisplaySettings.TrackMateObject.SPOTS, strokeColorFeature.getValue());
            displaySettings.setSpotMinMax(range.getMinimum(), range.getMaximum());
        }
        return displaySettings;
    }

    public void drawOnGraphics(SpotsCollectionData spotsCollectionData, Graphics2D graphics2D, Rectangle renderArea, ImageSliceIndex sliceIndex, Collection<Spot> selected) {
        // Setup the canvas
        ImagePlus imagePlus = spotsCollectionData.getImage();
        double magnification = 1.0 * renderArea.width / imagePlus.getWidth();
        ImageCanvas dummyCanvas = ImageJUtils.createZoomedDummyCanvas(imagePlus, renderArea);

        // Setup the display settings
        DisplaySettings displaySettings = createDisplaySettings(spotsCollectionData);

        // Create overlay
        SpotOverlay spotOverlay = new SpotOverlay(spotsCollectionData.getModel(), spotsCollectionData.getImage(), displaySettings);
        ImageJUtils.setRoiCanvas(spotOverlay, imagePlus, dummyCanvas);
        spotOverlay.setSpotSelection(selected);

        // Draw
        graphics2D.translate(renderArea.x, renderArea.y);
        int oldC = imagePlus.getC();
        int oldZ = imagePlus.getZ();
        int oldT = imagePlus.getT();
        imagePlus.setPosition(sliceIndex.getC() + 1, sliceIndex.getZ() + 1, sliceIndex.getT() + 1);
        spotOverlay.drawOverlay(graphics2D);
        imagePlus.setPosition(oldC, oldZ, oldT);

        // Draw label
        if (labelSettings.isDrawLabels()) {
            Font labelFont = new Font(Font.DIALOG, Font.PLAIN, labelSettings.getLabelSize());
            for (Spot spot : spotsCollectionData.getSpots().iterable(true)) {
                int z = (int) (spot.getDoublePosition(2) / imagePlus.getCalibration().pixelDepth);
                int frame = spot.getFeature(Spot.FRAME).intValue();
                if (z == sliceIndex.getZ() && frame == sliceIndex.getT()) {
                    String label;
                    if (labelSettings.isDrawName()) {
                        label = spot.getName();
                    } else {
                        label = TrackMateUtils.FEATURE_DECIMAL_FORMAT.format(spotsCollectionData.getSpotFeature(spot, labelSettings.drawnFeature.getValue(), 0));
                    }
                    RoiDrawer.drawLabelOnGraphics(label,
                            graphics2D,
                            spot.getDoublePosition(0) / imagePlus.getCalibration().pixelWidth,
                            spot.getDoublePosition(1) / imagePlus.getCalibration().pixelHeight,
                            magnification,
                            labelSettings.getLabelForeground(),
                            labelSettings.getLabelBackground().getContent(),
                            labelFont,
                            labelSettings.getLabelBackground().isEnabled());
                }
            }
        }
        graphics2D.translate(-renderArea.x, -renderArea.y);
    }

    @SetJIPipeDocumentation(name = "Fill spots", description = "If enabled, the spots are drawn filled")
    @JIPipeParameter("fill-spots")
    public boolean isFillSpots() {
        return fillSpots;
    }

    @JIPipeParameter("fill-spots")
    public void setFillSpots(boolean fillSpots) {
        this.fillSpots = fillSpots;
    }

    @SetJIPipeDocumentation(name = "Stroke width", description = "Width of the spot stroke")
    @JIPipeParameter("stroke-width")
    public int getStrokeWidth() {
        return strokeWidth;
    }

    @JIPipeParameter("stroke-width")
    public void setStrokeWidth(int strokeWidth) {
        this.strokeWidth = strokeWidth;
    }

    @SetJIPipeDocumentation(name = "Stroke uniform color", description = "The default color of a spot. Has no effect if the color is calculated from a feature.")
    @JIPipeParameter("stroke-color")
    public Color getStrokeColor() {
        return strokeColor;
    }

    @JIPipeParameter("stroke-color")
    public void setStrokeColor(Color strokeColor) {
        this.strokeColor = strokeColor;
    }

    @SetJIPipeDocumentation(name = "Stroke color", description = "Determines how the stroke is colored")
    @JIPipeParameter("uniform-stroke-color")
    @BooleanParameterSettings(comboBoxStyle = true, trueLabel = "Uniform color", falseLabel = "Feature")
    public boolean isUniformStrokeColor() {
        return uniformStrokeColor;
    }

    @JIPipeParameter("uniform-stroke-color")
    public void setUniformStrokeColor(boolean uniformStrokeColor) {
        this.uniformStrokeColor = uniformStrokeColor;
    }

    @SetJIPipeDocumentation(name = "Stroke color feature", description = "Determines the feature that is utilized for coloring the stroke. Only takes effect if 'Stroke color' is set to 'Feature'")
    @JIPipeParameter("stroke-color-feature")
    public SpotFeature getStrokeColorFeature() {
        return strokeColorFeature;
    }

    @JIPipeParameter("stroke-color-feature")
    public void setStrokeColorFeature(SpotFeature strokeColorFeature) {
        this.strokeColorFeature = strokeColorFeature;
    }

    @SetJIPipeDocumentation(name = "Draw label", description = "Please use the following settings to modify how labels are drawn")
    @JIPipeParameter("label-settings")
    public LabelSettings getLabelSettings() {
        return labelSettings;
    }

    public static class LabelSettings extends DrawerLabelSettings {

        private boolean drawName = true;
        private SpotFeature drawnFeature = new SpotFeature(Spot.QUALITY);

        public LabelSettings() {
        }

        public LabelSettings(LabelSettings other) {
            super(other);
            this.drawName = other.drawName;
            this.drawnFeature = new SpotFeature(other.drawnFeature);
        }

        @SetJIPipeDocumentation(name = "Label content", description = "Determines the content of the label")
        @JIPipeParameter("draw-name")
        @BooleanParameterSettings(comboBoxStyle = true, trueLabel = "Spot name", falseLabel = "Spot feature")
        public boolean isDrawName() {
            return drawName;
        }

        @JIPipeParameter("draw-name")
        public void setDrawName(boolean drawName) {
            this.drawName = drawName;
        }

        @SetJIPipeDocumentation(name = "Displayed feature", description = "If 'Label content' is set to 'Spot feature', determines the feature to be displayed in the label")
        @JIPipeParameter("drawn-feature")
        public SpotFeature getDrawnFeature() {
            return drawnFeature;
        }

        @JIPipeParameter("drawn-feature")
        public void setDrawnFeature(SpotFeature drawnFeature) {
            this.drawnFeature = drawnFeature;
        }
    }
}
