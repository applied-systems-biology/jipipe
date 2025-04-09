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

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import ij.ImagePlus;
import ij.gui.EllipseRoi;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import org.apache.commons.lang3.Range;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeImageThumbnailData;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeThumbnailData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageSliceIndex;
import org.hkijena.jipipe.plugins.parameters.library.colors.ColorMap;

import java.awt.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@SetJIPipeDocumentation(name = "TrackMate spots", description = "Spots detected by TrackMate")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains an *.xml file that stores the TrackMate model and a *.tif image file that contains the image that is the basis of the model.", jsonSchemaURL = "https://jipipe.org/schemas/datatypes/trackmate-model-data.schema.json")
public class SpotsCollectionData extends ModelData {

    private final Map<String, Range<Double>> spotFeatureRanges = new HashMap<>();

    public SpotsCollectionData(Model model, Settings settings, ImagePlus image) {
        super(model, settings, image);
    }

    public SpotsCollectionData(ModelData other) {
        super(other);
    }

    public static SpotsCollectionData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        ModelData modelData = ModelData.importData(storage, progressInfo);
        return new SpotsCollectionData(modelData.getModel(), modelData.getSettings(), modelData.getImage());
    }

    public int getNSpots() {
        return getSpots().getNSpots(true);
    }

    @Override
    public String toString() {
        return getModel().getSpots().getNSpots(true) + " spots";
    }

    @Override
    public String toDetailedString() {
        return getModel().getSpots().toString();
    }

    public SpotCollection getSpots() {
        return getModel().getSpots();
    }

    /**
     * Returns the range of values for a feature. This method makes use of a cache for fast access.
     *
     * @param featureName the feature
     * @return the range. returns an empty range (min = 0 and max = 0) if no feature values are available
     */
    public Range<Double> getSpotFeatureRange(String featureName) {
        Range<Double> result = spotFeatureRanges.getOrDefault(featureName, null);
        if (result == null) {
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            for (Spot spot : getSpots().iterable(true)) {
                double feature = getSpotFeature(spot, featureName, Double.NaN);
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
            spotFeatureRanges.put(featureName, result);
        }
        return result;
    }

    public void recalculateSpotFeatureRange() {
        spotFeatureRanges.clear();
    }

    @Override
    public JIPipeThumbnailData createThumbnail(int width, int height, JIPipeProgressInfo progressInfo) {
        ImagePlus image = getImage();
        double factorX = 1.0 * width / image.getWidth();
        double factorY = 1.0 * height / image.getHeight();
        double factor = Math.max(factorX, factorY);
        boolean smooth = factor < 0;
        int imageWidth = (int) (image.getWidth() * factor);
        int imageHeight = (int) (image.getHeight() * factor);
        ImagePlus rgbImage = ImageJUtils.channelsToRGB(image);
        rgbImage = ImageJUtils.convertToColorRGBIfNeeded(rgbImage);

        // ROI rendering
        ROI2DListData rois = spotsToROIList();
        int dMax = 1;
        for (Roi roi : rois) {
            int d = roi.getZPosition() + roi.getCPosition() + roi.getTPosition();
            dMax = Math.max(d, dMax);
        }
        for (Roi roi : rois) {
            int d = roi.getZPosition() + roi.getCPosition() + roi.getTPosition();
            roi.setStrokeColor(ColorMap.hsv.apply(1.0 * d / dMax));
        }
        rois.draw(rgbImage.getProcessor(),
                new ImageSliceIndex(0, 0, 0),
                true,
                true,
                true,
                true,
                false,
                false,
                1,
                Color.RED,
                Color.YELLOW,
                Collections.emptyList());

        ImageProcessor resized = rgbImage.getProcessor().resize(imageWidth, imageHeight, smooth);
        return new JIPipeImageThumbnailData(resized);
    }

    /**
     * Gets a feature of a spot.
     *
     * @param spot         the spot
     * @param feature      the feature
     * @param defaultValue the default value
     * @return the value
     */
    public double getSpotFeature(Spot spot, String feature, double defaultValue) {
        Double result = spot.getFeature(feature);
        if (result == null)
            return defaultValue;
        else
            return result;
    }

    public ROI2DListData spotsToROIList() {
        ROI2DListData result = new ROI2DListData();
        for (Spot spot : getSpots().iterable(true)) {
            double x = spot.getDoublePosition(0) / getImage().getCalibration().pixelWidth;
            double y = spot.getDoublePosition(1) / getImage().getCalibration().pixelHeight;
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
}
