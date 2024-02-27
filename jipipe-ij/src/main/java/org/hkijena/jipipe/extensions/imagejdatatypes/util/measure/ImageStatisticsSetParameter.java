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

package org.hkijena.jipipe.extensions.imagejdatatypes.util.measure;

import ij.plugin.filter.Analyzer;
import org.hkijena.jipipe.extensions.parameters.api.enums.DynamicSetParameter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@link DynamicSetParameter} that contains image statistics measurements.
 * Contains {@link Measurement} items
 */
public class ImageStatisticsSetParameter extends DynamicSetParameter<Measurement> {

    public static final String ALL_DESCRIPTIONS = "<p><strong>Area</strong><br/><br/>Measures ROI areas. Area of selection in square pixels. Area is in calibrated units if available. They are stored as 'Area'.</p>" +
            "<br/><p><strong>Area fraction</strong><br/><br/>The percentage of non-zero pixels. Values are stored in '%Area'</p>" +
            "<br/><p><strong>Bounding rectangle</strong><br/><br/>The smallest rectangle enclosing the selection. Uses the headings BX, BY, Width and Height, where BX and BY are the coordinates of the upper left corner of the rectangle.</p>" +
            "<br/><p><strong>Center of mass</strong><br/><br/>Measures the brightness-weighted average of the x and y coordinates all pixels in the image or selection. They are stored into 'XM' and 'YM' respectively. These coordinates are the first order spatial moments.</p>" +
            "<br/><p><strong>Centroid</strong><br/><br/>The center point of the selection. This is the average of the x and y coordinates of all of the pixels in the image or selection. Values are stored in 'X' and 'Y' respectively.</p>" +
            "<br/><p><strong>Feret's diameter</strong><br/><br/>The longest distance between any two points along the selection boundary, also known as maximum caliper. Uses the Feret heading. FeretAngle (0-180 degrees) is the angle between the Feret's diameter and a line parallel to the x-axis of the image. " +
            "MinFeret is the minimum caliper diameter. The starting coordinates of the Feret's diameter (FeretX and FeretY) are also generated.</p><br/><p><strong>Fit ellipse</strong><br/><br/>Fit an ellipse to the selection. Uses the headings Major, Minor and Angle. " +
            "Major and Minor are the primary and seconday axis of the best fitting ellipse. Angle (0-180 degrees) is the angle between the primary axis and a line parallel to the x-axis of the image. The coordinates of the center of the ellipse are displayed as X and Y if Centroid is checked. " +
            "Note that ImageJ cannot calculate the major and minor axis lengths if Pixel Aspect Ratio is not 1.0.</p><br/><p><strong>Integrated density</strong><br/><br/>Calculates and displays two values: 'IntDen' (the product of Area and Mean Gray Value) and 'RawIntDen' (the sum of the values of the pixels in the image or selection).</p>" +
            "<br/><p><strong>Min & max pixel values</strong><br/><br/>Measures the minimum and maximum greyscale pixel values. They are stored into 'Min' and 'Max' respectively.</p><br/><p><strong>Perimeter</strong><br/><br/>The length of the outside boundary of the selection. Values are stored in 'Perim.'</p>" +
            "<br/><p><strong>Pixel values kurtosis</strong><br/><br/>The fourth order moment about the greyscale pixel value mean. Values are stored in 'Kurt'</p><br/><p><strong>Pixel values mean</strong><br/><br/>Mean of grayscale pixel values. Values are stored in 'Mean'.</p>" +
            "<br/><p><strong>Pixel values median</strong><br/><br/>The median value of the pixels in the image or selection. Values are stored in 'Median'</p><br/><p><strong>Pixel values modal</strong><br/><br/>Most frequently occurring gray value within the selection. Values are stored in 'Mode'.</p>" +
            "<br/><p><strong>Pixel values skewness</strong><br/><br/>The third order moment about the mean of grayscale pixel values. Values are stored in 'Skew'.</p><br/><p><strong>Pixel values standard deviation</strong><br/><br/>Measures the standard deviation of greyscale pixel values. They are stored as 'StdDev'.</p>" +
            "<br/><p><strong>Shape descriptors</strong><br/><br/>Measures the following shape descriptors:<br/>" +
            "<ul><li>Circ. (circularity): 4π*area/perimeter^2. A value of 1.0 indicates a perfect circle. As the value approaches 0.0, it indicates an increasingly elongated shape. Values may not be valid for very small particles.</li>" +
            "<li>AR (aspect ratio): major_axis/minor_axis. To show major and minor axis enable 'Fit ellipse'</li>" +
            "<li>Round (roundness): 4*area/(π*major_axis^2), or the inverse of the aspect ratio.</li>" +
            "<li>Solidity: area/convex area.</li></ul></p>" +
            "<br/><p><strong>Stack position</strong><br/><br/>The current position (channel, slice and frame) in the stack or hyperstack. " +
            "Uses the headings 'Ch', 'Slice' and 'Frame'. Please note that JIPipe algorithms sometimes handle stacks differently compared to their ImageJ equivalents." +
            " We recommend to not rely on this measurement.</p><br/>";

    public ImageStatisticsSetParameter() {
        super(new HashSet<>(Arrays.asList(Measurement.values())));
        setCollapsed(true);
        initialize();
    }

    public ImageStatisticsSetParameter(ImageStatisticsSetParameter other) {
        super(other);
    }

    public ImageStatisticsSetParameter(Set<Measurement> values) {
        super(values);
        initialize();
    }

    private void initialize() {
        getAllowedValues().addAll(Arrays.asList(Measurement.values()));
    }

    public int getNativeValue() {
        int result = 0;
        for (Measurement value : getValues()) {
            result |= value.getNativeValue();
        }
        return result;
    }

    /**
     * Sets the values from native values
     *
     * @param nativeValue multiple native values
     */
    public void setNativeValue(int nativeValue) {
        getValues().clear();
        for (Measurement value : getAllowedValues()) {
            if ((value.getNativeValue() & nativeValue) == value.getNativeValue()) {
                getValues().add(value);
            }
        }
    }

    @Override
    public String renderLabel(Measurement value) {
        return value != null ? value.getLabel() : "<null>";
    }

    /**
     * Sends the settings to {@link ij.plugin.filter.Analyzer}
     */
    public void updateAnalyzer() {
        Analyzer.setMeasurements(getNativeValue());
    }
}
