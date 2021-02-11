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

package org.hkijena.jipipe.extensions.imagejdatatypes.util.measure;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Measurements defined by {@link ij.measure.Measurements}.
 * This only includes the measurements that actually generate columns - not measurement settings
 */
public enum Measurement {
    Area(1),
    PixelValueMean(2),
    PixelValueStandardDeviation(4),
    PixelValueModal(8),
    PixelValueMinMax(16),
    Centroid(32),
    CenterOfMass(64),
    Perimeter(128),
    BoundingRectangle(512),
    FitEllipse(2048),
    ShapeDescriptors(8192),
    FeretDiameter(16384),
    IntegratedDensity(0x8000),
    PixelValueMedian(0x10000),
    PixelValueSkewness(0x20000),
    PixelValueKurtosis(0x40000),
    AreaFraction(0x80000),
    StackPosition(0x100000);

    private final int nativeValue;

    Measurement(int nativeValue) {
        this.nativeValue = nativeValue;
    }

    public static String getAllDescriptions() {
        StringBuilder builder = new StringBuilder();
        List<Measurement> measurements = Arrays.asList(values());
        measurements.sort(Comparator.comparing(Measurement::getLabel));
        for (Measurement measurement : measurements) {
            builder.append("<p><strong>").append(measurement.getLabel()).append("</strong><br/><br/>");
            builder.append(measurement.getDescription());
            builder.append("</p><br/>");
        }
        return builder.toString();
    }

    public int getNativeValue() {
        return nativeValue;
    }

    public String getLabel() {
        switch (this) {
            case Area:
                return "Area";
            case PixelValueStandardDeviation:
                return "Pixel values standard deviation";
            case PixelValueMinMax:
                return "Min & max pixel values";
            case CenterOfMass:
                return "Center of mass";
            case BoundingRectangle:
                return "Bounding rectangle";
            case ShapeDescriptors:
                return "Shape descriptors";
            case IntegratedDensity:
                return "Integrated density";
            case PixelValueSkewness:
                return "Pixel values skewness";
            case AreaFraction:
                return "Area fraction";
            case PixelValueMean:
                return "Pixel values mean";
            case PixelValueModal:
                return "Pixel values modal";
            case Centroid:
                return "Centroid";
            case Perimeter:
                return "Perimeter";
            case FitEllipse:
                return "Fit ellipse";
            case FeretDiameter:
                return "Feret's diameter";
            case PixelValueMedian:
                return "Pixel values median";
            case PixelValueKurtosis:
                return "Pixel values kurtosis";
            case StackPosition:
                return "Stack position";
            default:
                return name();
        }
    }

    public String getDescription() {
        switch (this) {
            case Area:
                return "Measures ROI areas. Area of selection in square pixels. Area is in calibrated units if available. They are stored as 'Area'.";
            case PixelValueStandardDeviation:
                return "Measures the standard deviation of greyscale pixel values. They are stored as 'StdDev'.";
            case PixelValueMinMax:
                return "Measures the minimum and maximum greyscale pixel values. They are stored into 'Min' and 'Max' respectively.";
            case CenterOfMass:
                return "Measures the brightness-weighted average of the x and y coordinates all pixels in the image or selection. " +
                        "They are stored into 'XM' and 'YM' respectively. These coordinates are the first order spatial moments.";
            case BoundingRectangle:
                return "The smallest rectangle enclosing the selection. " +
                        "Uses the headings BX, BY, Width and Height, where BX and BY are the coordinates of the upper left corner of the rectangle.";
            case ShapeDescriptors:
                return "Measures the following shape descriptors:<br/>" +
                        "<ul><li>Circ. (circularity): 4π*area/perimeter^2. A value of 1.0 indicates a perfect circle. As the value approaches 0.0, it indicates an increasingly elongated shape. Values may not be valid for very small particles.</li>" +
                        "<li>AR (aspect ratio): major_axis/minor_axis. To show major and minor axis enable 'Fit ellipse'</li>" +
                        "<li>Round (roundness): 4*area/(π*major_axis^2), or the inverse of the aspect ratio.</li>" +
                        "<li>Solidity: area/convex area.</li></ul>";
            case IntegratedDensity:
                return "Calculates and displays two values: \"IntDen\" (the product of Area and Mean Gray Value) and \"RawIntDen\" (the sum of the values of the pixels in the image or selection).";
            case PixelValueSkewness:
                return "The third order moment about the mean of grayscale pixel values. Values are stored in 'Skew'.";
            case AreaFraction:
                return "The percentage of non-zero pixels. Values are stored in '%Area'";
            case PixelValueMean:
                return "Mean of grayscale pixel values. Values are stored in 'Mean'.";
            case PixelValueModal:
                return "Most frequently occurring gray value within the selection. Values are stored in 'Mode'.";
            case Centroid:
                return "The center point of the selection. This is the average of the x and y coordinates of all of the pixels in the image or selection. " +
                        "Values are stored in 'X' and 'Y' respectively.";
            case Perimeter:
                return "The length of the outside boundary of the selection. " +
                        "Values are stored in 'Perim.'";
            case FitEllipse:
                return "Fit an ellipse to the selection. Uses the headings Major, Minor and Angle. Major and Minor are the primary" +
                        " and seconday axis of the best fitting ellipse. Angle (0-180 degrees) is the" +
                        " angle between the primary axis and a line parallel to the x-axis of the image. " +
                        "The coordinates of the center of the ellipse are displayed as X and Y if Centroid is checked. " +
                        "Note that ImageJ cannot calculate the major and minor axis lengths if Pixel Aspect Ratio is not 1.0.";
            case FeretDiameter:
                return "The longest distance between any two points along the selection boundary, also known as maximum caliper. " +
                        "Uses the Feret heading. FeretAngle (0-180 degrees) is the angle between the Feret's diameter and a line parallel to the x-axis of the image. " +
                        "MinFeret is the minimum caliper diameter. " +
                        "The starting coordinates of the Feret's diameter (FeretX and FeretY) are also generated.";
            case PixelValueMedian:
                return "The median value of the pixels in the image or selection. " +
                        "Values are stored in 'Median'";
            case PixelValueKurtosis:
                return "The fourth order moment about the greyscale pixel value mean. " +
                        "Values are stored in 'Kurt'";
            case StackPosition:
                return "The current position (channel, slice and frame) in the stack or hyperstack. Uses the headings \"Ch\", \"Slice\" and \"Frame\"." +
                        " Please note that JIPipe algorithms sometimes handle stacks differently compared to their ImageJ equivalents. We recommend to " +
                        "not rely on this measurement.";
            default:
                return "";
        }
    }
}
