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

package org.hkijena.jipipe.plugins.ijtrackmate.utils;

import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.visualization.hyperstack.TrackOverlay;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import org.apache.commons.lang3.Range;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.ijtrackmate.datatypes.TrackCollectionData;
import org.hkijena.jipipe.plugins.ijtrackmate.parameters.EdgeFeature;
import org.hkijena.jipipe.plugins.ijtrackmate.parameters.TrackFeature;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.awt.*;
import java.util.Collection;

public class TrackDrawer extends AbstractJIPipeParameterCollection {
    private int strokeWidth = 1;

    private Color strokeColor = Color.YELLOW;

    private StrokeColorMode strokeColorMode = StrokeColorMode.PerTrack;

    private int fadeTrackRange = 30;

    private EdgeFeature strokeColorEdgeFeature = new EdgeFeature("DISPLACEMENT");

    private TrackFeature strokeColorTrackFeature = new TrackFeature("TRACK_ID");

//    private LabelSettings labelSettings = new LabelSettings();

    private DisplaySettings.TrackDisplayMode trackDisplayMode = DisplaySettings.TrackDisplayMode.FULL;

    public TrackDrawer() {
    }

    public TrackDrawer(TrackDrawer other) {
        copyFrom(other);
    }

    public void copyFrom(TrackDrawer other) {
        this.strokeWidth = other.strokeWidth;
        this.strokeColor = other.strokeColor;
        this.strokeColorMode = other.strokeColorMode;
        this.strokeColorEdgeFeature = new EdgeFeature(other.strokeColorEdgeFeature);
//        this.labelSettings = new LabelSettings(other.labelSettings);
        this.trackDisplayMode = other.trackDisplayMode;
        this.fadeTrackRange = other.fadeTrackRange;
        this.strokeColorTrackFeature = new TrackFeature(other.strokeColorTrackFeature);
    }

    public DisplaySettings createDisplaySettings(TrackCollectionData trackCollectionData) {
        DisplaySettings displaySettings = new DisplaySettings();
        displaySettings.setLineThickness(strokeWidth);
        displaySettings.setTrackDisplayMode(trackDisplayMode);
        displaySettings.setFadeTrackRange(fadeTrackRange);
        switch (strokeColorMode) {
            case Uniform: {
                displaySettings.setTrackUniformColor(strokeColor);
            }
            break;
            case PerEdge: {
                Range<Double> range = trackCollectionData.getEdgeFeatureRange(strokeColorEdgeFeature.getValue());
                displaySettings.setTrackColorBy(DisplaySettings.TrackMateObject.EDGES, strokeColorEdgeFeature.getValue());
                displaySettings.setTrackMinMax(range.getMinimum(), range.getMaximum());
            }
            break;
            case PerTrack: {
                Range<Double> range = trackCollectionData.getTrackFeatureRange(strokeColorTrackFeature.getValue());
                displaySettings.setTrackColorBy(DisplaySettings.TrackMateObject.TRACKS, strokeColorTrackFeature.getValue());
                displaySettings.setTrackMinMax(range.getMinimum(), range.getMaximum());
            }
        }

        return displaySettings;
    }

    public void drawOnGraphics(TrackCollectionData trackCollectionData, Graphics2D graphics2D, Rectangle renderArea, ImageSliceIndex sliceIndex, Collection<DefaultWeightedEdge> selected) {
        // Setup the canvas
        ImagePlus imagePlus = trackCollectionData.getImage();
        double magnification = 1.0 * renderArea.width / imagePlus.getWidth();
        ImageCanvas dummyCanvas = ImageJUtils.createZoomedDummyCanvas(imagePlus, renderArea);

        // Setup the display settings
        DisplaySettings displaySettings = createDisplaySettings(trackCollectionData);

        // Create overlay
        TrackOverlay trackOverlay = new TrackOverlay(trackCollectionData.getModel(), trackCollectionData.getImage(), displaySettings);
        ImageJUtils.setRoiCanvas(trackOverlay, imagePlus, dummyCanvas);
        trackOverlay.setHighlight(selected);

        // Draw
        graphics2D.translate(renderArea.x, renderArea.y);
        int oldC = imagePlus.getC();
        int oldZ = imagePlus.getZ();
        int oldT = imagePlus.getT();
        imagePlus.setPosition(sliceIndex.getC() + 1, sliceIndex.getZ() + 1, sliceIndex.getT() + 1);
        trackOverlay.drawOverlay(graphics2D);
        imagePlus.setPosition(oldC, oldZ, oldT);

        // Draw label
//        if(labelSettings.isDrawLabels()) {
//            Font labelFont = new Font(Font.DIALOG, Font.PLAIN, labelSettings.getLabelSize());
//            for (Spot spot : trackCollectionData.getSpots().iterable(true)) {
//                int z = (int) spot.getDoublePosition(2);
//                int frame = spot.getFeature(Spot.FRAME).intValue();
//                if(z == sliceIndex.getZ() && frame == sliceIndex.getT()) {
//                    String label;
//                    if(labelSettings.isDrawName()) {
//                        label = spot.getName();
//                    }
//                    else {
//                        label = TrackMateUtils.FEATURE_DECIMAL_FORMAT.format(trackCollectionData.getSpotFeature(spot, labelSettings.drawnFeature.getValue(), 0));
//                    }
//                    RoiDrawer.drawLabelOnGraphics(label,
//                            graphics2D,
//                            spot.getDoublePosition(0),
//                            spot.getDoublePosition(1),
//                            magnification,
//                            labelSettings.getLabelForeground(),
//                            labelSettings.getLabelBackground().getContent(),
//                            labelFont,
//                            labelSettings.getLabelBackground().isEnabled());
//                }
//            }
//        }
        graphics2D.translate(-renderArea.x, -renderArea.y);
    }

    @SetJIPipeDocumentation(name = "Fade track range", description = "If the display mode is set to local track surroundings, sets the fading range")
    @JIPipeParameter("fade-track-range")
    public int getFadeTrackRange() {
        return fadeTrackRange;
    }

    @JIPipeParameter("fade-track-range")
    public void setFadeTrackRange(int fadeTrackRange) {
        this.fadeTrackRange = fadeTrackRange;
    }

    @SetJIPipeDocumentation(name = "Display mode", description = "Determines how tracks are displayed")
    @JIPipeParameter("track-display-mode")
    public DisplaySettings.TrackDisplayMode getTrackDisplayMode() {
        return trackDisplayMode;
    }

    @JIPipeParameter("track-display-mode")
    public void setTrackDisplayMode(DisplaySettings.TrackDisplayMode trackDisplayMode) {
        this.trackDisplayMode = trackDisplayMode;
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

    @SetJIPipeDocumentation(name = "Stroke color mode", description = "Determines how the stroke is colored")
    @JIPipeParameter("stroke-color-mode")
    public StrokeColorMode getStrokeColorMode() {
        return strokeColorMode;
    }

    @JIPipeParameter("stroke-color-mode")
    public void setStrokeColorMode(StrokeColorMode strokeColorMode) {
        this.strokeColorMode = strokeColorMode;
    }

    @SetJIPipeDocumentation(name = "Stroke color edge feature", description = "Determines the feature that is utilized for coloring the stroke. Only takes effect if 'Stroke color mode' is set to 'Color per edge'")
    @JIPipeParameter("stroke-color-edge-feature")
    public EdgeFeature getStrokeColorEdgeFeature() {
        return strokeColorEdgeFeature;
    }

    @JIPipeParameter("stroke-color-edge-feature")
    public void setStrokeColorEdgeFeature(EdgeFeature strokeColorEdgeFeature) {
        this.strokeColorEdgeFeature = strokeColorEdgeFeature;
    }

    @SetJIPipeDocumentation(name = "Stroke color track feature", description = "Determines the feature that is utilized for coloring the stroke. Only takes effect if 'Stroke color mode' is set to 'Color per track'")
    @JIPipeParameter("stroke-color-track-feature")
    public TrackFeature getStrokeColorTrackFeature() {
        return strokeColorTrackFeature;
    }

    @JIPipeParameter("stroke-color-track-feature")
    public void setStrokeColorTrackFeature(TrackFeature strokeColorTrackFeature) {
        this.strokeColorTrackFeature = strokeColorTrackFeature;
    }

//        @JIPipeDocumentation(name = "Draw label", description = "Please use the following settings to modify how labels are drawn")
//    @JIPipeParameter("label-settings")
//    public LabelSettings getLabelSettings() {
//        return labelSettings;
//    }

//    public static class LabelSettings extends DrawerLabelSettings {
//
//        private boolean drawName = true;
//        private SpotFeature drawnFeature = new SpotFeature(Spot.QUALITY);
//
//        public LabelSettings() {
//        }
//
//        public LabelSettings(LabelSettings other) {
//            super(other);
//            this.drawName = other.drawName;
//            this.drawnFeature = new SpotFeature(other.drawnFeature);
//        }
//
//        @JIPipeDocumentation(name = "Label content", description = "Determines the content of the label")
//        @JIPipeParameter("draw-name")
//        @BooleanParameterSettings(comboBoxStyle = true, trueLabel = "Spot name", falseLabel = "Spot feature")
//        public boolean isDrawName() {
//            return drawName;
//        }
//
//        @JIPipeParameter("draw-name")
//        public void setDrawName(boolean drawName) {
//            this.drawName = drawName;
//        }
//
//        @JIPipeDocumentation(name = "Displayed feature", description = "If 'Label content' is set to 'Spot feature', determines the feature to be displayed in the label")
//        @JIPipeParameter("drawn-feature")
//        public SpotFeature getDrawnFeature() {
//            return drawnFeature;
//        }
//
//        @JIPipeParameter("drawn-feature")
//        public void setDrawnFeature(SpotFeature drawnFeature) {
//            this.drawnFeature = drawnFeature;
//        }
//    }

    public enum StrokeColorMode {
        Uniform,
        PerEdge,
        PerTrack;


        @Override
        public String toString() {
            switch (this) {
                case PerEdge:
                    return "Color per edge";
                case PerTrack:
                    return "Color per track";
                default:
                    return super.toString();
            }
        }
    }
}
