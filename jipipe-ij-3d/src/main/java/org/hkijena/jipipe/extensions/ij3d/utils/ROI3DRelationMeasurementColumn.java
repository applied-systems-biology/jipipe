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

package org.hkijena.jipipe.extensions.ij3d.utils;

/**
 * Measurements defined by {@link ij.measure.Measurements}.
 * This only includes the measurements that actually generate columns - not measurement settings.
 * This contains individually addressed columns
 */
public enum ROI3DRelationMeasurementColumn {
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

    Roi1Index(65536, "Roi1.Index", "Roi1: Index", "The numeric ROI index"),
    Roi1Name(65536, "Roi1.Name", "Roi1: Name", "The name of the ROI"),
    Roi1Comment(65536, "Roi1.Comment", "Roi1: Comment", "The comment string of the ROI"),
    Roi1Area(65536, "Roi1.Area", "Roi1: Area", "The area of the surface"),
    Roi1Volume(65536, "Roi1.Volume", "Roi1: Volume", "The volume of the ROI"),
    Roi1CenterX(65536, "Roi1.CenterX", "Roi1: Center (X)", "The X location of the ROI's center"),
    Roi1CenterY(65536, "Roi1.CenterY", "Roi1: Center (Y)", "The Y location of the ROI's center"),
    Roi1CenterZ(65536, "Roi1.CenterZ", "Roi1: Center (Z)", "The Z location of the ROI's center"),
    Roi1CenterPixelValue(65536, "Roi1.CenterPixelValue", "Roi1: Center (value)", "The pixel value of the ROI's center (or NaN)"),
    Roi1ShapeMeasurementCompactness(65536, "Roi1.Compactness", "Roi1: Compactness", "The compactness the ROI"),
    Roi1ShapeMeasurementSphericity(65536, "Roi1.Sphericity", "Roi1: Sphericity", "The sphericity the ROI"),
    Roi1ShapeMeasurementFeret(65536, "Roi1.Feret", "Roi1: Feret", "The feret diameter of the object (unit)"),
    Roi1ShapeMeasurementMainElongation(65536, "Roi1.MainElongation", "Roi1: Main elongation", "The main elongation the ROI"),
    Roi1ShapeMeasurementMedianElongation(65536, "Roi1.MedianElongation", "Roi1: Median elongation", "The flatness of the object"),
    Roi1ShapeMeasurementRatioBox(65536, "Roi1.RatioBox", "Roi1: Ratio to box", "The ration between volume and volume of the bounding box (in units)"),
    Roi1ShapeMeasurementRatioEllipsoid(65536, "Roi1.RatioEllipsoid", "Roi1: Ratio to ellipsoid", "The ration between volume and volume of the ellipsoid (in units)"),
    Roi1DistCenterMax(65536, "Roi1.DistCenterMax", "Roi1: Distance to center (max)", "The maximum distance between center and contours"),
    Roi1DistCenterMean(65536, "Roi1.DistCenterMean", "Roi1: Distance to center (mean)", "The average distance between center and contours"),
    Roi1DistCenterSigma(65536, "Roi1.DistCenterSigma", "Roi1: Distance to center (sigma)", "The sigma value for distances between center and contours"),
    Roi1PixelValueMax(65536, "Roi1.PixelValueMax", "Roi1: Pixel value (max)", "The maximum pixel value inside the object (or NaN)"),
    Roi1PixelValueMin(65536, "Roi1.PixelValueMin", "Roi1: Pixel value (min)", "The minimum pixel value inside the object (or NaN)"),
    Roi1PixelValueMean(65536, "Roi1.PixelValueMean", "Roi1: Pixel value (mean)", "The average pixel value inside the object (or NaN)"),
    Roi1PixelValueMedian(65536, "Roi1.PixelValueMedian", "Roi1: Pixel value (median)", "The median pixel value inside the object (or NaN)"),
    Roi1PixelValueStdDev(65536, "Roi1.PixelValueStdDev", "Roi1: Pixel value (std dev)", "The standard deviation of the pixel values inside the object (or NaN)"),
    Roi1PixelValueMode(65536, "Roi1.PixelValueMode", "Roi1: Pixel value (mode)", "The mode of the pixel values inside the object (or NaN)"),
    Roi1PixelValueModeNonZero(65536, "Roi1.PixelValueModeNonZero", "Roi1: Pixel value (mode, non-zero)", "The mode of the pixel values (excluding zero values) inside the object (or NaN)"),
    Roi1PixelValueIntDen(65536, "Roi1.PixelValueIntDen", "Roi1: Pixel value (integrated density)", "The integrated density of the pixel values inside the object (or NaN)"),

    Roi1ContourPixelValueMean(65536, "Roi1.ContourPixelValueMean", "Roi1: Pixel value (contour, mean)", "The average pixel value on the contour of the object (or NaN)"),
    Roi1BoundingBoxMinX(65536, "Roi1.BoundingBoxMinX", "Roi1: Bounding box (min X)", "The minimum X of the bounding box around the object"),
    Roi1BoundingBoxMinY(65536, "Roi1.BoundingBoxMinY", "Roi1: Bounding box (min Y)", "The minimum Y of the bounding box around the object"),
    Roi1BoundingBoxMinZ(65536, "Roi1.BoundingBoxMinZ", "Roi1: Bounding box (min Z)", "The minimum Z of the bounding box around the object"),

    Roi1BoundingBoxMaxX(65536, "Roi1.BoundingBoxMaxX", "Roi1: Bounding box (max X)", "The maximum X of the bounding box around the object"),
    Roi1BoundingBoxMaxY(65536, "Roi1.BoundingBoxMaxY", "Roi1: Bounding box (max Y)", "The maximum Y of the bounding box around the object"),
    Roi1BoundingBoxMaxZ(65536, "Roi1.BoundingBoxMaxZ", "Roi1: Bounding box (max Z)", "The maximum Z of the bounding box around the object"),
    Roi1ResolutionXY(65536, "Roi1.ResolutionXY", "Roi1: Physical voxel size (X/Y)", "The physical size of a voxel in the X and Y axes"),
    Roi1ResolutionZ(65536, "Roi1.ResolutionZ", "Roi1: Physical voxel size (Z)", "The physical size of a voxel in the Z axis"),
    Roi1ResolutionUnit(65536, "Roi1.ResolutionUnit", "Roi1: Physical voxel size (unit)", "The unit of the physical size"),
    Roi1MassCenterX(65536, "Roi1.MassCenterX", "Roi1: Center of mass (X)", "The X coordinate of the center of Mass (or NaN)"),
    Roi1MassCenterY(65536, "Roi1.MassCenterY", "Roi1: Center of mass (Y)", "The Y coordinate of the center of Mass (or NaN)"),
    Roi1MassCenterZ(65536, "Roi1.MassCenterZ", "Roi1: Center of mass (Z)", "The Z coordinate of the center of Mass (or NaN)"),

    Roi1LocationChannel(65536, "Roi1.Channel", "Roi1: Channel", "The channel of the ROI (0 = all channels)"),
    Roi1LocationFrame(65536, "Roi1.Frame", "Roi1: Frame", "The frame of the ROI (0 = all frames)"),
    Roi1FillColor(65536, "Roi1.FillColor", "Roi1: Fill color", "The fill color (HEX string)"),
    Roi2Index(131072, "Roi2.Index", "Roi2: Index", "The numeric ROI index"),
    Roi2Name(131072, "Roi2.Name", "Roi2: Name", "The name of the ROI"),
    Roi2Comment(131072, "Roi2.Comment", "Roi2: Comment", "The comment string of the ROI"),
    Roi2Area(131072, "Roi2.Area", "Roi2: Area", "The area of the surface"),
    Roi2Volume(131072, "Roi2.Volume", "Roi2: Volume", "The volume of the ROI"),
    Roi2CenterX(131072, "Roi2.CenterX", "Roi2: Center (X)", "The X location of the ROI's center"),
    Roi2CenterY(131072, "Roi2.CenterY", "Roi2: Center (Y)", "The Y location of the ROI's center"),
    Roi2CenterZ(131072, "Roi2.CenterZ", "Roi2: Center (Z)", "The Z location of the ROI's center"),
    Roi2CenterPixelValue(131072, "Roi2.CenterPixelValue", "Roi2: Center (value)", "The pixel value of the ROI's center (or NaN)"),
    Roi2ShapeMeasurementCompactness(131072, "Roi2.Compactness", "Roi2: Compactness", "The compactness the ROI"),
    Roi2ShapeMeasurementSphericity(131072, "Roi2.Sphericity", "Roi2: Sphericity", "The sphericity the ROI"),
    Roi2ShapeMeasurementFeret(131072, "Roi2.Feret", "Roi2: Feret", "The feret diameter of the object (unit)"),
    Roi2ShapeMeasurementMainElongation(131072, "Roi2.MainElongation", "Roi2: Main elongation", "The main elongation the ROI"),
    Roi2ShapeMeasurementMedianElongation(131072, "Roi2.MedianElongation", "Roi2: Median elongation", "The flatness of the object"),
    Roi2ShapeMeasurementRatioBox(131072, "Roi2.RatioBox", "Roi2: Ratio to box", "The ration between volume and volume of the bounding box (in units)"),
    Roi2ShapeMeasurementRatioEllipsoid(131072, "Roi2.RatioEllipsoid", "Roi2: Ratio to ellipsoid", "The ration between volume and volume of the ellipsoid (in units)"),
    Roi2DistCenterMax(131072, "Roi2.DistCenterMax", "Roi2: Distance to center (max)", "The maximum distance between center and contours"),
    Roi2DistCenterMean(131072, "Roi2.DistCenterMean", "Roi2: Distance to center (mean)", "The average distance between center and contours"),
    Roi2DistCenterSigma(131072, "Roi2.DistCenterSigma", "Roi2: Distance to center (sigma)", "The sigma value for distances between center and contours"),
    Roi2PixelValueMax(131072, "Roi2.PixelValueMax", "Roi2: Pixel value (max)", "The maximum pixel value inside the object (or NaN)"),
    Roi2PixelValueMin(131072, "Roi2.PixelValueMin", "Roi2: Pixel value (min)", "The minimum pixel value inside the object (or NaN)"),
    Roi2PixelValueMean(131072, "Roi2.PixelValueMean", "Roi2: Pixel value (mean)", "The average pixel value inside the object (or NaN)"),
    Roi2PixelValueMedian(131072, "Roi2.PixelValueMedian", "Roi2: Pixel value (median)", "The median pixel value inside the object (or NaN)"),
    Roi2PixelValueStdDev(131072, "Roi2.PixelValueStdDev", "Roi2: Pixel value (std dev)", "The standard deviation of the pixel values inside the object (or NaN)"),
    Roi2PixelValueMode(131072, "Roi2.PixelValueMode", "Roi2: Pixel value (mode)", "The mode of the pixel values inside the object (or NaN)"),
    Roi2PixelValueModeNonZero(131072, "Roi2.PixelValueModeNonZero", "Roi2: Pixel value (mode, non-zero)", "The mode of the pixel values (excluding zero values) inside the object (or NaN)"),
    Roi2PixelValueIntDen(131072, "Roi2.PixelValueIntDen", "Roi2: Pixel value (integrated density)", "The integrated density of the pixel values inside the object (or NaN)"),

    Roi2ContourPixelValueMean(131072, "Roi2.ContourPixelValueMean", "Roi2: Pixel value (contour, mean)", "The average pixel value on the contour of the object (or NaN)"),
    Roi2BoundingBoxMinX(131072, "Roi2.BoundingBoxMinX", "Roi2: Bounding box (min X)", "The minimum X of the bounding box around the object"),
    Roi2BoundingBoxMinY(131072, "Roi2.BoundingBoxMinY", "Roi2: Bounding box (min Y)", "The minimum Y of the bounding box around the object"),
    Roi2BoundingBoxMinZ(131072, "Roi2.BoundingBoxMinZ", "Roi2: Bounding box (min Z)", "The minimum Z of the bounding box around the object"),

    Roi2BoundingBoxMaxX(131072, "Roi2.BoundingBoxMaxX", "Roi2: Bounding box (max X)", "The maximum X of the bounding box around the object"),
    Roi2BoundingBoxMaxY(131072, "Roi2.BoundingBoxMaxY", "Roi2: Bounding box (max Y)", "The maximum Y of the bounding box around the object"),
    Roi2BoundingBoxMaxZ(131072, "Roi2.BoundingBoxMaxZ", "Roi2: Bounding box (max Z)", "The maximum Z of the bounding box around the object"),
    Roi2ResolutionXY(131072, "Roi2.ResolutionXY", "Roi2: Physical voxel size (X/Y)", "The physical size of a voxel in the X and Y axes"),
    Roi2ResolutionZ(131072, "Roi2.ResolutionZ", "Roi2: Physical voxel size (Z)", "The physical size of a voxel in the Z axis"),
    Roi2ResolutionUnit(131072, "Roi2.ResolutionUnit", "Roi2: Physical voxel size (unit)", "The unit of the physical size"),
    Roi2MassCenterX(131072, "Roi2.MassCenterX", "Roi2: Center of mass (X)", "The X coordinate of the center of Mass (or NaN)"),
    Roi2MassCenterY(131072, "Roi2.MassCenterY", "Roi2: Center of mass (Y)", "The Y coordinate of the center of Mass (or NaN)"),
    Roi2MassCenterZ(131072, "Roi2.MassCenterZ", "Roi2: Center of mass (Z)", "The Z coordinate of the center of Mass (or NaN)"),

    Roi2LocationChannel(131072, "Roi2.Channel", "Roi2: Channel", "The channel of the ROI (0 = all channels)"),
    Roi2LocationFrame(131072, "Roi2.Frame", "Roi2: Frame", "The frame of the ROI (0 = all frames)"),
    Roi2FillColor(131072, "Roi2.FillColor", "Roi2: Fill color", "The fill color (HEX string)")
    ;

    private final int nativeValue;
    private final String columnName;
    private final String name;
    private final String description;

    ROI3DRelationMeasurementColumn(int nativeValue, String columnName, String name, String description) {
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
