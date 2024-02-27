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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi;

/**
 * Measurements defined by {@link ij.measure.Measurements}.
 * This only includes the measurements that actually generate columns - not measurement settings.
 * This contains individually addressed columns
 */
public enum ROI2DRelationMeasurementColumn {
    Colocalization(1, "Colocalization", "Colocalization", "The number of intersecting voxels"),
    PercentageColocalization(2, "PercentageColocalization", "Percentage Colocalization", "Percentage of colocalization between two objects relative to this object"),
    OverlapsBox(4, "OverlapsBox", "Overlaps box", "If the bounding boxes intersect (0 = no, 1 = yes)"),
    Includes(8, "Includes", "Includes", "If the first object includes the second object (0 = no, 1 = yes)"),
    IncludesBox(16, "IncludesBox", "Includes Bounding Box", "If the first object's bounding box includes the second object's bounding box"),
    RadiusCenter(32, "RadiusCenter", "Radius Center", "The radius of the object towards another object"),
    RadiusCenterOpposite(64, "RadiusCenterOpposite", "Radius Center (Opposite)", "The radius of the object towards another object (opposite direction)"),
    DistanceCenter2D(128, "DistanceCenter2D", "Distance between centers (2D)", "Distance between the centers in the X/Y plane"),
    DistanceCenter(256, "DistanceCenter", "Distance between centers (3D)", "Distance between the centers in 3D"),
    DistanceHausdorff(512, "DistanceHausdorff", "Distance (Hausdorff)", "Hausdorff distance"),
    DistanceBorder(1024, "DistanceBorder", "Minimum distance between borders", "The minimum distance between two objects computed on the contours pixels"),
    DistanceCenterBorder(2048, "DistanceCenterBorder", "Minimum distance between center and border", "The minimum distance between the center of the first object and the border of the second object"),
    EdgeContactColocalization(4096, "EdgeContactColocalization", "Edge contact (colocalization)", "Number of contour voxels that co-localize (are shared between the two objects)"),
    EdgeContactSide(8192, "EdgeContactSide", "Edge contact (include side pixels)", "Number of contour voxels that are less than 1 distance squared away from each other"),
    EdgeContactDiagonal(16384, "EdgeContactDiagonal", "Edge contact (include side and diagonal pixels)", "Number of contour voxels that are less than 2 distance squared away from each other"),
    IntersectionArea(32768, "Intersection.Area", "Intersection: Area", "The area of the surface"),
    IntersectionVolume(32768, "Intersection.Volume", "Intersection: Volume", "The volume of the ROI"),
    IntersectionCenterX(32768, "Intersection.CenterX", "Intersection: Center (X)", "The X location of the ROI's center"),
    IntersectionCenterY(32768, "Intersection.CenterY", "Intersection: Center (Y)", "The Y location of the ROI's center"),
    IntersectionCenterZ(32768, "Intersection.CenterZ", "Intersection: Center (Z)", "The Z location of the ROI's center"),
    IntersectionCenterPixelValue(32768, "Intersection.CenterPixelValue", "Intersection: Center (value)", "The pixel value of the ROI's center (or NaN)"),
    IntersectionShapeMeasurementCompactness(32768, "Intersection.Compactness", "Intersection: Compactness", "The compactness the ROI"),
    IntersectionShapeMeasurementSphericity(32768, "Intersection.Sphericity", "Intersection: Sphericity", "The sphericity the ROI"),
    IntersectionShapeMeasurementFeret(32768, "Intersection.Feret", "Intersection: Feret", "The feret diameter of the object (unit)"),
    IntersectionShapeMeasurementMainElongation(32768, "Intersection.MainElongation", "Intersection: Main elongation", "The main elongation the ROI"),
    IntersectionShapeMeasurementMedianElongation(32768, "Intersection.MedianElongation", "Intersection: Median elongation", "The flatness of the object"),
    IntersectionShapeMeasurementRatioBox(32768, "Intersection.RatioBox", "Intersection: Ratio to box", "The ration between volume and volume of the bounding box (in units)"),
    IntersectionShapeMeasurementRatioEllipsoid(32768, "Intersection.RatioEllipsoid", "Intersection: Ratio to ellipsoid", "The ration between volume and volume of the ellipsoid (in units)"),
    IntersectionDistCenterMax(32768, "Intersection.DistCenterMax", "Intersection: Distance to center (max)", "The maximum distance between center and contours"),
    IntersectionDistCenterMean(32768, "Intersection.DistCenterMean", "Intersection: Distance to center (mean)", "The average distance between center and contours"),
    IntersectionDistCenterSigma(32768, "Intersection.DistCenterSigma", "Intersection: Distance to center (sigma)", "The sigma value for distances between center and contours"),
    IntersectionPixelValueMax(32768, "Intersection.PixelValueMax", "Intersection: Pixel value (max)", "The maximum pixel value inside the object (or NaN)"),
    IntersectionPixelValueMin(32768, "Intersection.PixelValueMin", "Intersection: Pixel value (min)", "The minimum pixel value inside the object (or NaN)"),
    IntersectionPixelValueMean(32768, "Intersection.PixelValueMean", "Intersection: Pixel value (mean)", "The average pixel value inside the object (or NaN)"),
    IntersectionPixelValueMedian(32768, "Intersection.PixelValueMedian", "Intersection: Pixel value (median)", "The median pixel value inside the object (or NaN)"),
    IntersectionPixelValueStdDev(32768, "Intersection.PixelValueStdDev", "Intersection: Pixel value (std dev)", "The standard deviation of the pixel values inside the object (or NaN)"),
    IntersectionPixelValueMode(32768, "Intersection.PixelValueMode", "Intersection: Pixel value (mode)", "The mode of the pixel values inside the object (or NaN)"),
    IntersectionPixelValueModeNonZero(32768, "Intersection.PixelValueModeNonZero", "Intersection: Pixel value (mode, non-zero)", "The mode of the pixel values (excluding zero values) inside the object (or NaN)"),
    IntersectionPixelValueIntDen(32768, "Intersection.PixelValueIntDen", "Intersection: Pixel value (integrated density)", "The integrated density of the pixel values inside the object (or NaN)"),

    IntersectionContourPixelValueMean(32768, "Intersection.ContourPixelValueMean", "Intersection: Pixel value (contour, mean)", "The average pixel value on the contour of the object (or NaN)"),
    IntersectionBoundingBoxMinX(32768, "Intersection.BoundingBoxMinX", "Intersection: Bounding box (min X)", "The minimum X of the bounding box around the object"),
    IntersectionBoundingBoxMinY(32768, "Intersection.BoundingBoxMinY", "Intersection: Bounding box (min Y)", "The minimum Y of the bounding box around the object"),
    IntersectionBoundingBoxMinZ(32768, "Intersection.BoundingBoxMinZ", "Intersection: Bounding box (min Z)", "The minimum Z of the bounding box around the object"),

    IntersectionBoundingBoxMaxX(32768, "Intersection.BoundingBoxMaxX", "Intersection: Bounding box (max X)", "The maximum X of the bounding box around the object"),
    IntersectionBoundingBoxMaxY(32768, "Intersection.BoundingBoxMaxY", "Intersection: Bounding box (max Y)", "The maximum Y of the bounding box around the object"),
    IntersectionBoundingBoxMaxZ(32768, "Intersection.BoundingBoxMaxZ", "Intersection: Bounding box (max Z)", "The maximum Z of the bounding box around the object"),
    IntersectionMassCenterX(32768, "Intersection.MassCenterX", "Intersection: Center of mass (X)", "The X coordinate of the center of Mass (or NaN)"),
    IntersectionMassCenterY(32768, "Intersection.MassCenterY", "Intersection: Center of mass (Y)", "The Y coordinate of the center of Mass (or NaN)"),
    IntersectionMassCenterZ(32768, "Intersection.MassCenterZ", "Intersection: Center of mass (Z)", "The Z coordinate of the center of Mass (or NaN)"),

    CurrentIndex(65536, "Current.Index", "Current: Index", "The numeric ROI index"),
    CurrentName(65536, "Current.Name", "Current: Name", "The name of the ROI"),
    CurrentComment(65536, "Current.Comment", "Current: Comment", "The comment string of the ROI"),
    CurrentArea(65536, "Current.Area", "Current: Area", "The area of the surface"),
    CurrentVolume(65536, "Current.Volume", "Current: Volume", "The volume of the ROI"),
    CurrentCenterX(65536, "Current.CenterX", "Current: Center (X)", "The X location of the ROI's center"),
    CurrentCenterY(65536, "Current.CenterY", "Current: Center (Y)", "The Y location of the ROI's center"),
    CurrentCenterZ(65536, "Current.CenterZ", "Current: Center (Z)", "The Z location of the ROI's center"),
    CurrentCenterPixelValue(65536, "Current.CenterPixelValue", "Current: Center (value)", "The pixel value of the ROI's center (or NaN)"),
    CurrentShapeMeasurementCompactness(65536, "Current.Compactness", "Current: Compactness", "The compactness the ROI"),
    CurrentShapeMeasurementSphericity(65536, "Current.Sphericity", "Current: Sphericity", "The sphericity the ROI"),
    CurrentShapeMeasurementFeret(65536, "Current.Feret", "Current: Feret", "The feret diameter of the object (unit)"),
    CurrentShapeMeasurementMainElongation(65536, "Current.MainElongation", "Current: Main elongation", "The main elongation the ROI"),
    CurrentShapeMeasurementMedianElongation(65536, "Current.MedianElongation", "Current: Median elongation", "The flatness of the object"),
    CurrentShapeMeasurementRatioBox(65536, "Current.RatioBox", "Current: Ratio to box", "The ration between volume and volume of the bounding box (in units)"),
    CurrentShapeMeasurementRatioEllipsoid(65536, "Current.RatioEllipsoid", "Current: Ratio to ellipsoid", "The ration between volume and volume of the ellipsoid (in units)"),
    CurrentDistCenterMax(65536, "Current.DistCenterMax", "Current: Distance to center (max)", "The maximum distance between center and contours"),
    CurrentDistCenterMean(65536, "Current.DistCenterMean", "Current: Distance to center (mean)", "The average distance between center and contours"),
    CurrentDistCenterSigma(65536, "Current.DistCenterSigma", "Current: Distance to center (sigma)", "The sigma value for distances between center and contours"),
    CurrentPixelValueMax(65536, "Current.PixelValueMax", "Current: Pixel value (max)", "The maximum pixel value inside the object (or NaN)"),
    CurrentPixelValueMin(65536, "Current.PixelValueMin", "Current: Pixel value (min)", "The minimum pixel value inside the object (or NaN)"),
    CurrentPixelValueMean(65536, "Current.PixelValueMean", "Current: Pixel value (mean)", "The average pixel value inside the object (or NaN)"),
    CurrentPixelValueMedian(65536, "Current.PixelValueMedian", "Current: Pixel value (median)", "The median pixel value inside the object (or NaN)"),
    CurrentPixelValueStdDev(65536, "Current.PixelValueStdDev", "Current: Pixel value (std dev)", "The standard deviation of the pixel values inside the object (or NaN)"),
    CurrentPixelValueMode(65536, "Current.PixelValueMode", "Current: Pixel value (mode)", "The mode of the pixel values inside the object (or NaN)"),
    CurrentPixelValueModeNonZero(65536, "Current.PixelValueModeNonZero", "Current: Pixel value (mode, non-zero)", "The mode of the pixel values (excluding zero values) inside the object (or NaN)"),
    CurrentPixelValueIntDen(65536, "Current.PixelValueIntDen", "Current: Pixel value (integrated density)", "The integrated density of the pixel values inside the object (or NaN)"),

    CurrentContourPixelValueMean(65536, "Current.ContourPixelValueMean", "Current: Pixel value (contour, mean)", "The average pixel value on the contour of the object (or NaN)"),
    CurrentBoundingBoxMinX(65536, "Current.BoundingBoxMinX", "Current: Bounding box (min X)", "The minimum X of the bounding box around the object"),
    CurrentBoundingBoxMinY(65536, "Current.BoundingBoxMinY", "Current: Bounding box (min Y)", "The minimum Y of the bounding box around the object"),
    CurrentBoundingBoxMinZ(65536, "Current.BoundingBoxMinZ", "Current: Bounding box (min Z)", "The minimum Z of the bounding box around the object"),

    CurrentBoundingBoxMaxX(65536, "Current.BoundingBoxMaxX", "Current: Bounding box (max X)", "The maximum X of the bounding box around the object"),
    CurrentBoundingBoxMaxY(65536, "Current.BoundingBoxMaxY", "Current: Bounding box (max Y)", "The maximum Y of the bounding box around the object"),
    CurrentBoundingBoxMaxZ(65536, "Current.BoundingBoxMaxZ", "Current: Bounding box (max Z)", "The maximum Z of the bounding box around the object"),
    CurrentResolutionXY(65536, "Current.ResolutionXY", "Current: Physical voxel size (X/Y)", "The physical size of a voxel in the X and Y axes"),
    CurrentResolutionZ(65536, "Current.ResolutionZ", "Current: Physical voxel size (Z)", "The physical size of a voxel in the Z axis"),
    CurrentResolutionUnit(65536, "Current.ResolutionUnit", "Current: Physical voxel size (unit)", "The unit of the physical size"),
    CurrentMassCenterX(65536, "Current.MassCenterX", "Current: Center of mass (X)", "The X coordinate of the center of Mass (or NaN)"),
    CurrentMassCenterY(65536, "Current.MassCenterY", "Current: Center of mass (Y)", "The Y coordinate of the center of Mass (or NaN)"),
    CurrentMassCenterZ(65536, "Current.MassCenterZ", "Current: Center of mass (Z)", "The Z coordinate of the center of Mass (or NaN)"),

    CurrentLocationChannel(65536, "Current.Channel", "Current: Channel", "The channel of the ROI (0 = all channels)"),
    CurrentLocationFrame(65536, "Current.Frame", "Current: Frame", "The frame of the ROI (0 = all frames)"),
    CurrentFillColor(65536, "Current.FillColor", "Current: Fill color", "The fill color (HEX string)"),
    OtherIndex(131072, "Other.Index", "Other: Index", "The numeric ROI index"),
    OtherName(131072, "Other.Name", "Other: Name", "The name of the ROI"),
    OtherComment(131072, "Other.Comment", "Other: Comment", "The comment string of the ROI"),
    OtherArea(131072, "Other.Area", "Other: Area", "The area of the surface"),
    OtherVolume(131072, "Other.Volume", "Other: Volume", "The volume of the ROI"),
    OtherCenterX(131072, "Other.CenterX", "Other: Center (X)", "The X location of the ROI's center"),
    OtherCenterY(131072, "Other.CenterY", "Other: Center (Y)", "The Y location of the ROI's center"),
    OtherCenterZ(131072, "Other.CenterZ", "Other: Center (Z)", "The Z location of the ROI's center"),
    OtherCenterPixelValue(131072, "Other.CenterPixelValue", "Other: Center (value)", "The pixel value of the ROI's center (or NaN)"),
    OtherShapeMeasurementCompactness(131072, "Other.Compactness", "Other: Compactness", "The compactness the ROI"),
    OtherShapeMeasurementSphericity(131072, "Other.Sphericity", "Other: Sphericity", "The sphericity the ROI"),
    OtherShapeMeasurementFeret(131072, "Other.Feret", "Other: Feret", "The feret diameter of the object (unit)"),
    OtherShapeMeasurementMainElongation(131072, "Other.MainElongation", "Other: Main elongation", "The main elongation the ROI"),
    OtherShapeMeasurementMedianElongation(131072, "Other.MedianElongation", "Other: Median elongation", "The flatness of the object"),
    OtherShapeMeasurementRatioBox(131072, "Other.RatioBox", "Other: Ratio to box", "The ration between volume and volume of the bounding box (in units)"),
    OtherShapeMeasurementRatioEllipsoid(131072, "Other.RatioEllipsoid", "Other: Ratio to ellipsoid", "The ration between volume and volume of the ellipsoid (in units)"),
    OtherDistCenterMax(131072, "Other.DistCenterMax", "Other: Distance to center (max)", "The maximum distance between center and contours"),
    OtherDistCenterMean(131072, "Other.DistCenterMean", "Other: Distance to center (mean)", "The average distance between center and contours"),
    OtherDistCenterSigma(131072, "Other.DistCenterSigma", "Other: Distance to center (sigma)", "The sigma value for distances between center and contours"),
    OtherPixelValueMax(131072, "Other.PixelValueMax", "Other: Pixel value (max)", "The maximum pixel value inside the object (or NaN)"),
    OtherPixelValueMin(131072, "Other.PixelValueMin", "Other: Pixel value (min)", "The minimum pixel value inside the object (or NaN)"),
    OtherPixelValueMean(131072, "Other.PixelValueMean", "Other: Pixel value (mean)", "The average pixel value inside the object (or NaN)"),
    OtherPixelValueMedian(131072, "Other.PixelValueMedian", "Other: Pixel value (median)", "The median pixel value inside the object (or NaN)"),
    OtherPixelValueStdDev(131072, "Other.PixelValueStdDev", "Other: Pixel value (std dev)", "The standard deviation of the pixel values inside the object (or NaN)"),
    OtherPixelValueMode(131072, "Other.PixelValueMode", "Other: Pixel value (mode)", "The mode of the pixel values inside the object (or NaN)"),
    OtherPixelValueModeNonZero(131072, "Other.PixelValueModeNonZero", "Other: Pixel value (mode, non-zero)", "The mode of the pixel values (excluding zero values) inside the object (or NaN)"),
    OtherPixelValueIntDen(131072, "Other.PixelValueIntDen", "Other: Pixel value (integrated density)", "The integrated density of the pixel values inside the object (or NaN)"),

    OtherContourPixelValueMean(131072, "Other.ContourPixelValueMean", "Other: Pixel value (contour, mean)", "The average pixel value on the contour of the object (or NaN)"),
    OtherBoundingBoxMinX(131072, "Other.BoundingBoxMinX", "Other: Bounding box (min X)", "The minimum X of the bounding box around the object"),
    OtherBoundingBoxMinY(131072, "Other.BoundingBoxMinY", "Other: Bounding box (min Y)", "The minimum Y of the bounding box around the object"),
    OtherBoundingBoxMinZ(131072, "Other.BoundingBoxMinZ", "Other: Bounding box (min Z)", "The minimum Z of the bounding box around the object"),

    OtherBoundingBoxMaxX(131072, "Other.BoundingBoxMaxX", "Other: Bounding box (max X)", "The maximum X of the bounding box around the object"),
    OtherBoundingBoxMaxY(131072, "Other.BoundingBoxMaxY", "Other: Bounding box (max Y)", "The maximum Y of the bounding box around the object"),
    OtherBoundingBoxMaxZ(131072, "Other.BoundingBoxMaxZ", "Other: Bounding box (max Z)", "The maximum Z of the bounding box around the object"),
    OtherResolutionXY(131072, "Other.ResolutionXY", "Other: Physical voxel size (X/Y)", "The physical size of a voxel in the X and Y axes"),
    OtherResolutionZ(131072, "Other.ResolutionZ", "Other: Physical voxel size (Z)", "The physical size of a voxel in the Z axis"),
    OtherResolutionUnit(131072, "Other.ResolutionUnit", "Other: Physical voxel size (unit)", "The unit of the physical size"),
    OtherMassCenterX(131072, "Other.MassCenterX", "Other: Center of mass (X)", "The X coordinate of the center of Mass (or NaN)"),
    OtherMassCenterY(131072, "Other.MassCenterY", "Other: Center of mass (Y)", "The Y coordinate of the center of Mass (or NaN)"),
    OtherMassCenterZ(131072, "Other.MassCenterZ", "Other: Center of mass (Z)", "The Z coordinate of the center of Mass (or NaN)"),

    OtherLocationChannel(131072, "Other.Channel", "Other: Channel", "The channel of the ROI (0 = all channels)"),
    OtherLocationFrame(131072, "Other.Frame", "Other: Frame", "The frame of the ROI (0 = all frames)"),
    OtherFillColor(131072, "Other.FillColor", "Other: Fill color", "The fill color (HEX string)");

    private final int nativeValue;
    private final String columnName;
    private final String name;
    private final String description;

    ROI2DRelationMeasurementColumn(int nativeValue, String columnName, String name, String description) {
        this.nativeValue = nativeValue;
        this.columnName = columnName;
        this.name = name;
        this.description = description;
    }

    public int getNativeValue() {
        return nativeValue;
    }

    public String getColumnName() {
        return columnName;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
