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
 *
 */

package org.hkijena.jipipe.extensions.ijtrackmate.utils;

import com.google.common.eventbus.EventBus;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.visualization.hyperstack.SpotOverlay;
import fiji.plugin.trackmate.visualization.hyperstack.TrackOverlay;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import org.apache.commons.lang3.Range;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.TrackCollectionData;
import org.hkijena.jipipe.extensions.ijtrackmate.parameters.EdgeFeature;
import org.hkijena.jipipe.extensions.ijtrackmate.parameters.SpotFeature;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.RoiDrawer;
import org.hkijena.jipipe.extensions.parameters.library.primitives.BooleanParameterSettings;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Collection;

public class TrackDrawer implements JIPipeParameterCollection {
    private final EventBus eventBus = new EventBus();

    private int strokeWidth = 1;

    private Color strokeColor = Color.YELLOW;

    private boolean uniformStrokeColor = true;

    private EdgeFeature strokeColorFeature = new EdgeFeature("DISPLACEMENT");

    private LabelSettings labelSettings = new LabelSettings();

    public TrackDrawer() {
    }

    public TrackDrawer(TrackDrawer other) {
       copyFrom(other);
    }

    public void copyFrom(TrackDrawer other) {
        this.strokeWidth = other.strokeWidth;
        this.strokeColor = other.strokeColor;
        this.uniformStrokeColor = other.uniformStrokeColor;
        this.strokeColorFeature = new EdgeFeature(other.strokeColorFeature);
        this.labelSettings = new LabelSettings(other.labelSettings);
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    public DisplaySettings createDisplaySettings(TrackCollectionData trackCollectionData) {
        DisplaySettings displaySettings = new DisplaySettings();
        displaySettings.setLineThickness(strokeWidth);
        if(uniformStrokeColor) {
            displaySettings.setTrackUniformColor(strokeColor);
        } else {
            Range<Double> range = trackCollectionData.getEdgeFeatureRange(strokeColorFeature.getValue());
            displaySettings.setTrackColorBy(DisplaySettings.TrackMateObject.EDGES, strokeColorFeature.getValue());
            displaySettings.setTrackMinMax(range.getMinimum(), range.getMaximum());
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
        TrackOverlay spotOverlay = new TrackOverlay(trackCollectionData.getModel(), trackCollectionData.getImage(), displaySettings);
        ImageJUtils.setRoiCanvas(spotOverlay, imagePlus, dummyCanvas);
        spotOverlay.setHighlight(selected);

        // Draw
        graphics2D.translate(renderArea.x, renderArea.y);
        spotOverlay.drawOverlay(graphics2D);

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

    @JIPipeDocumentation(name = "Stroke width", description = "Width of the spot stroke")
    @JIPipeParameter("stroke-width")
    public int getStrokeWidth() {
        return strokeWidth;
    }

    @JIPipeParameter("stroke-width")
    public void setStrokeWidth(int strokeWidth) {
        this.strokeWidth = strokeWidth;
    }

    @JIPipeDocumentation(name = "Stroke uniform color", description = "The default color of a spot. Has no effect if the color is calculated from a feature.")
    @JIPipeParameter("stroke-color")
    public Color getStrokeColor() {
        return strokeColor;
    }

    @JIPipeParameter("stroke-color")
    public void setStrokeColor(Color strokeColor) {
        this.strokeColor = strokeColor;
    }

    @JIPipeDocumentation(name = "Stroke color", description = "Determines how the stroke is colored")
    @JIPipeParameter("uniform-stroke-color")
    @BooleanParameterSettings(comboBoxStyle = true, trueLabel = "Uniform color", falseLabel = "Feature")
    public boolean isUniformStrokeColor() {
        return uniformStrokeColor;
    }

    @JIPipeParameter("uniform-stroke-color")
    public void setUniformStrokeColor(boolean uniformStrokeColor) {
        this.uniformStrokeColor = uniformStrokeColor;
    }

    @JIPipeDocumentation(name = "Stroke color feature", description = "Determines the feature that is utilized for coloring the stroke. Only takes effect if 'Stroke color' is set to 'Feature'")
    @JIPipeParameter("stroke-color-feature")
    public EdgeFeature getStrokeColorFeature() {
        return strokeColorFeature;
    }

    @JIPipeParameter("stroke-color-feature")
    public void setStrokeColorFeature(EdgeFeature strokeColorFeature) {
        this.strokeColorFeature = strokeColorFeature;
    }

    @JIPipeDocumentation(name = "Draw label", description = "Please use the following settings to modify how labels are drawn")
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

        @JIPipeDocumentation(name = "Label content", description = "Determines the content of the label")
        @JIPipeParameter("draw-name")
        @BooleanParameterSettings(comboBoxStyle = true, trueLabel = "Spot name", falseLabel = "Spot feature")
        public boolean isDrawName() {
            return drawName;
        }

        @JIPipeParameter("draw-name")
        public void setDrawName(boolean drawName) {
            this.drawName = drawName;
        }

        @JIPipeDocumentation(name = "Displayed feature", description = "If 'Label content' is set to 'Spot feature', determines the feature to be displayed in the label")
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
