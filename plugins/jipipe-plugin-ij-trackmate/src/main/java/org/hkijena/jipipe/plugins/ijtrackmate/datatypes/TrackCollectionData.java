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

package org.hkijena.jipipe.plugins.ijtrackmate.datatypes;

import fiji.plugin.trackmate.*;
import fiji.plugin.trackmate.features.EdgeFeatureCalculator;
import fiji.plugin.trackmate.features.TrackFeatureCalculator;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.visualization.hyperstack.TrackOverlay;
import ij.ImagePlus;
import ij.gui.EllipseRoi;
import ij.gui.ImageCanvas;
import ij.gui.Roi;
import org.apache.commons.lang3.Range;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeImageThumbnailData;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeThumbnailData;
import org.hkijena.jipipe.plugins.ijtrackmate.utils.JIPipeLogger;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@SetJIPipeDocumentation(name = "TrackMate tracks", description = "Tracks detected by TrackMate")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains an *.xml file that stores the TrackMate model and a *.tif image file that contains the image that is the basis of the model.", jsonSchemaURL = "https://jipipe.org/schemas/datatypes/trackmate-model-data.schema.json")
public class TrackCollectionData extends SpotsCollectionData {
    private final Map<String, Range<Double>> edgeFeatureRanges = new HashMap<>();
    private final Map<String, Range<Double>> trackFeatureRanges = new HashMap<>();

    public TrackCollectionData(Model model, Settings settings, ImagePlus image) {
        super(model, settings, image);
    }

    public TrackCollectionData(ModelData other) {
        super(other);
    }

    public static TrackCollectionData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        ModelData modelData = ModelData.importData(storage, progressInfo);
        return new TrackCollectionData(modelData.getModel(), modelData.getSettings(), modelData.getImage());
    }

    public TrackModel getTrackModel() {
        return getModel().getTrackModel();
    }

    public void computeTrackFeatures(JIPipeProgressInfo progressInfo) {
        final Logger logger = new JIPipeLogger(progressInfo);
        getModel().setLogger(logger);
        final TrackFeatureCalculator calculator = new TrackFeatureCalculator(getModel(), getSettings(), true);
        calculator.setNumThreads(1);
        if (calculator.checkInput() && calculator.process()) {
            getModel().notifyFeaturesComputed();
        } else {
            throw new RuntimeException("Unable to compute track features: " + calculator.getErrorMessage());
        }
    }

    public void computeEdgeFeatures(JIPipeProgressInfo progressInfo) {
        final Logger logger = new JIPipeLogger(progressInfo);
        getModel().setLogger(logger);
        final EdgeFeatureCalculator calculator = new EdgeFeatureCalculator(getModel(), getSettings(), true);
        calculator.setNumThreads(1);
        if (calculator.checkInput() && calculator.process()) {
            getModel().notifyFeaturesComputed();
        } else {
            throw new RuntimeException("Unable to compute track features: " + calculator.getErrorMessage());
        }
    }

    public ROI2DListData trackToROIList(int trackId) {
        ROI2DListData result = new ROI2DListData();
        for (Spot spot : getTrackModel().trackSpots(trackId)) {
            double x = spot.getDoublePosition(0) / getImage().getCalibration().pixelWidth;
            double y = spot.getDoublePosition(1) / getImage().getCalibration().pixelWidth;
            int z = (int) spot.getFloatPosition(2);
            int t = Optional.ofNullable(spot.getFeature(Spot.POSITION_T)).orElse(-1d).intValue();
            double radius = Optional.ofNullable(spot.getFeature(Spot.RADIUS)).orElse(1d) / getImage().getCalibration().pixelWidth;

            double x1 = x - radius;
            double x2 = x + radius;
            double y1 = y - radius;
            double y2 = y + radius;
            EllipseRoi roi = new EllipseRoi(x1, y1, x2, y2, 1);
            roi.setPosition(0, z + 1, t + 1);
            roi.setName(spot.getName());

            result.add(roi);
        }
        return result;
    }

    public int getNTracks() {
        return getTrackModel().nTracks(true);
    }

    public Set<Spot> getTrackSpots(int trackId) {
        return getTrackModel().trackSpots(trackId);
    }

    /**
     * Returns a copy of this track collection that has only the selected track IDs
     * Please note that all spots are still present
     *
     * @param selectedTrackIds the selected tracks
     * @return collection with only the selected tracks
     */
    public TrackCollectionData filterTracks(Set<Integer> selectedTrackIds) {
        TrackCollectionData result = new TrackCollectionData(this);
        for (Integer trackID : result.getTrackModel().trackIDs(true)) {
            if (!selectedTrackIds.contains(trackID)) {
                result.getModel().setTrackVisibility(trackID, false);
            }
        }
        return result;
    }

    /**
     * Gets a feature from a track.
     * Calculates features if necessary
     *
     * @param trackId      the track ID
     * @param feature      the feature
     * @param defaultValue the default value
     * @return the value.
     */
    public double getTrackFeature(int trackId, String feature, double defaultValue) {
        Double trackFeature = getModel().getFeatureModel().getTrackFeature(trackId, feature);
        if (trackFeature == null) {
            computeTrackFeatures(new JIPipeProgressInfo());
        } else {
            return trackFeature;
        }
        trackFeature = getModel().getFeatureModel().getTrackFeature(trackId, feature);
        if (trackFeature != null) {
            return trackFeature;
        } else {
            return defaultValue;
        }
    }

    /**
     * Gets a feature from an edge.
     * Calculates features if necessary
     *
     * @param edge         the edge
     * @param feature      the feature
     * @param defaultValue the default value
     * @return the value.
     */
    public double getEdgeFeature(DefaultWeightedEdge edge, String feature, double defaultValue) {
        Double trackFeature = getModel().getFeatureModel().getEdgeFeature(edge, feature);
        if (trackFeature == null) {
            computeEdgeFeatures(new JIPipeProgressInfo());
        } else {
            return trackFeature;
        }
        trackFeature = getModel().getFeatureModel().getEdgeFeature(edge, feature);
        if (trackFeature != null) {
            return trackFeature;
        } else {
            return defaultValue;
        }
    }

    public Iterable<Integer> getTrackIds() {
        return getTrackModel().trackIDs(true);
    }

    /**
     * Returns the range of values for a feature. This method makes use of a cache for fast access.
     *
     * @param featureName the feature
     * @return the range. returns an empty range (min = 0 and max = 0) if no feature values are available
     */
    public Range<Double> getEdgeFeatureRange(String featureName) {
        Range<Double> result = edgeFeatureRanges.getOrDefault(featureName, null);
        if (result == null) {
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;

            for (Integer trackId : getTrackIds()) {
                for (DefaultWeightedEdge trackEdge : getTrackModel().trackEdges(trackId)) {
                    double feature = getEdgeFeature(trackEdge, featureName, Double.NaN);
                    if (Double.isNaN(feature))
                        continue;
                    min = Math.min(feature, min);
                    max = Math.max(feature, max);
                }
            }
            if (Double.isFinite(min)) {
                result = Range.between(min, max);
            } else {
                result = Range.is(0d);
            }
            edgeFeatureRanges.put(featureName, result);
        }
        return result;
    }

    public void recalculateEdgeFeatureRange() {
        this.edgeFeatureRanges.clear();
    }

    /**
     * Returns the range of values for a feature. This method makes use of a cache for fast access.
     *
     * @param featureName the feature
     * @return the range. returns an empty range (min = 0 and max = 0) if no feature values are available
     */
    public Range<Double> getTrackFeatureRange(String featureName) {
        Range<Double> result = trackFeatureRanges.getOrDefault(featureName, null);
        if (result == null) {
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;

            for (Integer trackId : getTrackIds()) {
                double feature = getTrackFeature(trackId, featureName, Double.NaN);
                if (Double.isNaN(feature))
                    continue;
                min = Math.min(feature, min);
                max = Math.max(feature, max);
            }
            if (Double.isFinite(min)) {
                result = Range.between(min, max);
            } else {
                result = Range.is(0d);
            }
            trackFeatureRanges.put(featureName, result);
        }
        return result;
    }

    public void recalculateTrackFeatureRanges() {
        this.trackFeatureRanges.clear();
    }

    @Override
    public JIPipeThumbnailData createThumbnail(int width, int height, JIPipeProgressInfo progressInfo) {
        ImagePlus image = getImage();
        double factorX = 1.0 * width / image.getWidth();
        double factorY = 1.0 * height / image.getHeight();
        double factor = Math.max(factorX, factorY);
//        boolean smooth = factor < 0;
        int imageWidth = (int) (image.getWidth() * factor);
        int imageHeight = (int) (image.getHeight() * factor);
        ImagePlus rgbImage = ImageJUtils.channelsToRGB(image);
        rgbImage = ImageJUtils.convertToColorRGBIfNeeded(rgbImage);

        computeTrackFeatures(new JIPipeProgressInfo());

        // ROI rendering
        BufferedImage bufferedImage = rgbImage.getBufferedImage();
        DisplaySettings displaySettings = new DisplaySettings();
        displaySettings.setLineThickness(5);
        displaySettings.setTrackDisplayMode(DisplaySettings.TrackDisplayMode.FULL);
        displaySettings.setTrackColorBy(DisplaySettings.TrackMateObject.TRACKS, "TRACK_ID");
        displaySettings.setTrackMinMax(0, getTrackModel().nTracks(true));
        TrackOverlay overlay = new TrackOverlay(getModel(), rgbImage, displaySettings);
        try {
            Field field = Roi.class.getDeclaredField("ic");
            field.setAccessible(true);
            field.set(overlay, new ImageCanvas(getImage()));
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        Graphics2D graphics = bufferedImage.createGraphics();
        overlay.drawOverlay(graphics);
        graphics.dispose();
        Image scaledInstance = bufferedImage.getScaledInstance(imageWidth, imageHeight, Image.SCALE_DEFAULT);
        return new JIPipeImageThumbnailData(scaledInstance);
    }

    @Override
    public String toString() {
        return getModel().getTrackModel().nTracks(true) + " tracks, " + super.toString();
    }

    @Override
    public String toDetailedString() {
        return toString();
    }
}
