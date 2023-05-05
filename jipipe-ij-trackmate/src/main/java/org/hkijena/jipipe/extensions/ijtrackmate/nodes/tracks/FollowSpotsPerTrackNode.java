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

package org.hkijena.jipipe.extensions.ijtrackmate.nodes.tracks;

import fiji.plugin.trackmate.Spot;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.TrackCollectionData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@JIPipeDocumentation(name = "Visualize tracked spots", description = "For each track, generates an image that follows the spot over time")
@JIPipeNode(menuPath = "Tracking\nVisualize", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Image", autoCreate = true)
@JIPipeInputSlot(value = TrackCollectionData.class, slotName = "Tracks", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", inheritedSlot = "Image", autoCreate = true)
public class FollowSpotsPerTrackNode extends JIPipeIteratingAlgorithm {

    private OptionalAnnotationNameParameter trackIDAnnotation = new OptionalAnnotationNameParameter("Track ID", true);
    private int minWidth = 0;
    private int minHeight = 0;

    private boolean cropXY = true;

    private boolean cropZ = true;

    private boolean cropT = true;

    public FollowSpotsPerTrackNode(JIPipeNodeInfo info) {
        super(info);
    }

    public FollowSpotsPerTrackNode(FollowSpotsPerTrackNode other) {
        super(other);
        this.trackIDAnnotation = new OptionalAnnotationNameParameter(other.trackIDAnnotation);
        this.minWidth = other.minWidth;
        this.minHeight = other.minHeight;
        this.cropXY = other.cropXY;
        this.cropZ = other.cropZ;
        this.cropT = other.cropT;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus sourceImage = dataBatch.getInputData("Image", ImagePlusData.class, progressInfo).getImage();
        TrackCollectionData data = dataBatch.getInputData("Tracks", TrackCollectionData.class, progressInfo);
        Calibration calibration = data.getImage().getCalibration();

        final Rectangle imageArea = new Rectangle(0, 0, sourceImage.getWidth(), sourceImage.getHeight());

        for (Integer trackID : data.getTrackModel().trackIDs(true)) {
            List<JIPipeTextAnnotation> annotationList = new ArrayList<>();
            trackIDAnnotation.addAnnotationIfEnabled(annotationList, trackID + "");

            Set<Spot> spots = data.getTrackModel().trackSpots(trackID);
            if (spots.isEmpty())
                continue;

            int spotWidth = minWidth;
            int spotHeight = minHeight;

            // Z
            int minZ = Integer.MAX_VALUE;
            int maxZ = Integer.MIN_VALUE;

            // T
            int minT = Integer.MAX_VALUE;
            int maxT = Integer.MIN_VALUE;

            // XY
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;

            // Find the extents
            for (Spot spot : spots) {
                double radius = Optional.ofNullable(spot.getFeature(Spot.RADIUS)).orElse(1d) / calibration.pixelWidth;
                int x = (int) (spot.getDoublePosition(0) / calibration.pixelWidth);
                int y = (int) (spot.getDoublePosition(1) / calibration.pixelHeight);
                int z = (int) (spot.getFloatPosition(2) / calibration.pixelDepth);
                spotWidth = (int) Math.max(spotWidth, 2 * radius);
                spotHeight = (int) Math.max(spotHeight, 2 * radius);
                int t = Optional.ofNullable(spot.getFeature(Spot.POSITION_T)).orElse(0d).intValue();
                maxZ = Math.max(z, maxZ);
                minZ = Math.min(z, minZ);
                minT = Math.min(t, minT);
                maxT = Math.max(t, maxT);
                minX = Math.min(x, minX);
                maxX = Math.max(x, maxX);
                minY = Math.min(y, minY);
                maxY = Math.max(y, maxY);
            }

            // Move the X and Y towards the left corner
            minX -= spotWidth / 2;
            minY -= spotHeight / 2;
            maxX += spotWidth / 2;
            maxY += spotHeight / 2;

            // If we do not want cropping, set it to the image dimensions
            if (!cropXY) {
                minX = 0;
                maxX = sourceImage.getWidth() - 1;
                minY = 0;
                maxY = sourceImage.getHeight() - 1;
            }
            if (!cropT) {
                minT = 0;
                maxT = sourceImage.getNFrames() - 1;
            }
            if (!cropZ) {
                minZ = 0;
                maxZ = sourceImage.getNSlices() - 1;
            }

            // Create target image
            ImagePlus targetImage = IJ.createHyperStack("Track_" + trackID,
                    maxX - minX + 1,
                    maxY - minY + 1,
                    sourceImage.getNChannels(),
                    maxZ - minZ + 1,
                    maxT - minT + 1,
                    sourceImage.getBitDepth());

            for (Spot spot : spots) {
                int x = (int) (spot.getDoublePosition(0) / calibration.pixelWidth - spotWidth / 2);
                int y = (int) (spot.getDoublePosition(1) / calibration.pixelHeight - spotHeight / 2);
                int z = (int) (spot.getFloatPosition(2) / calibration.pixelDepth);
                int t = Optional.ofNullable(spot.getFeature(Spot.POSITION_T)).orElse(0d).intValue();

                int targetX = x - minX;
                int targetY = y - minY;
                int targetZ = z - minZ;
                int targetT = t - minT;

                Rectangle sourceRect = new Rectangle(x, y, spotWidth, spotHeight);
                Rectangle actualSourceRect = sourceRect.intersection(imageArea);

                targetX += actualSourceRect.x - sourceRect.x;
                targetY += actualSourceRect.y - sourceRect.y;

                for (int c = 0; c < sourceImage.getNChannels(); c++) {
                    ImageProcessor sourceProcessor = ImageJUtils.getSliceZero(sourceImage, c, z, t);
                    ImageProcessor targetProcessor = ImageJUtils.getSliceZero(targetImage, c, targetZ, targetT);

                    sourceProcessor.setRoi(actualSourceRect);
                    ImageProcessor crop = sourceProcessor.crop();
                    sourceProcessor.setRoi((Roi) null);

                    targetProcessor.insert(crop, targetX, targetY);
                }

            }

            dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(targetImage), annotationList, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        }
    }

    @JIPipeDocumentation(name = "Annotate with track ID", description = "If enabled, the track ID is annotated to each ROI")
    @JIPipeParameter("track-id-annotation")
    public OptionalAnnotationNameParameter getTrackIDAnnotation() {
        return trackIDAnnotation;
    }

    @JIPipeParameter("track-id-annotation")
    public void setTrackIDAnnotation(OptionalAnnotationNameParameter trackIDAnnotation) {
        this.trackIDAnnotation = trackIDAnnotation;
    }

    @JIPipeDocumentation(name = "Minimum spot width", description = "The minimum width of the area around the spot")
    @JIPipeParameter("min-spot-width")
    public int getMinWidth() {
        return minWidth;
    }

    @JIPipeParameter("min-spot-width")
    public void setMinWidth(int minWidth) {
        this.minWidth = minWidth;
    }

    @JIPipeDocumentation(name = "Minimum spot height", description = "The minimum height of the area around the spot")
    @JIPipeParameter("min-spot-height")
    public int getMinHeight() {
        return minHeight;
    }

    @JIPipeParameter("min-spot-height")
    public void setMinHeight(int minHeight) {
        this.minHeight = minHeight;
    }

    @JIPipeDocumentation(name = "Crop XY", description = "If enabled, crop the X and Y planes to the areas where the spots are located")
    @JIPipeParameter("crop-xy")
    public boolean isCropXY() {
        return cropXY;
    }

    @JIPipeParameter("crop-xy")
    public void setCropXY(boolean cropXY) {
        this.cropXY = cropXY;
    }

    @JIPipeDocumentation(name = "Crop Z", description = "If enabled, crop the depth to the areas where the spots are located")
    @JIPipeParameter("crop-z")
    public boolean isCropZ() {
        return cropZ;
    }

    @JIPipeParameter("crop-z")
    public void setCropZ(boolean cropZ) {
        this.cropZ = cropZ;
    }

    @JIPipeDocumentation(name = "Crop frames", description = "If enabled, crop the frames to the areas where the spots are located")
    @JIPipeParameter("crop-t")
    public boolean isCropT() {
        return cropT;
    }

    @JIPipeParameter("crop-t")
    public void setCropT(boolean cropT) {
        this.cropT = cropT;
    }
}
