package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.util;

import com.google.common.eventbus.EventBus;
import ij.measure.Measurements;
import ij.plugin.filter.Analyzer;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;

/**
 * Encapsulates parameters that are related to {@link ij.plugin.filter.Analyzer}, as
 * this should not be outside the control of ACAQ5 to prevent issues.
 * Those measurements are also used by {@link ij.plugin.frame.RoiManager}.
 */
public class ImageStatisticsParameters implements ACAQParameterCollection {
    private EventBus eventBus = new EventBus();

    private boolean measureArea = true;
    private boolean measurePixelValueStandardDeviation = false;
    private boolean measurePixelValueMinMax = true;
    private boolean measureCenterOfMass = false;
    private boolean measureBoundingRectangle = false;
    private boolean measureShapeDescriptors = false;
    private boolean measureIntegratedDensity = false;
    private boolean measurePixelValueSkewness = false;
    private boolean measureAreaFraction = false;
    private boolean measurePixelValueMean = false;
    private boolean measurePixelValueModal = false;
    private boolean measureCentroid = true;
    private boolean measurePerimeter = false;
    private boolean fitEllipse = false;
    private boolean measureFeretDiameter = false;
    private boolean measurePixelValueMedian = false;
    private boolean measurePixelValueKurtosis = false;
    private boolean outputStackPosition = false;

    /**
     * Creates a new instance
     */
    public ImageStatisticsParameters() {
    }

    /**
     * Creates a copy
     * @param other the original
     */
    public ImageStatisticsParameters(ImageStatisticsParameters other) {
        this.eventBus = other.eventBus;
        this.measureArea = other.measureArea;
        this.measurePixelValueStandardDeviation = other.measurePixelValueStandardDeviation;
        this.measurePixelValueMinMax = other.measurePixelValueMinMax;
        this.measureCenterOfMass = other.measureCenterOfMass;
        this.measureBoundingRectangle = other.measureBoundingRectangle;
        this.measureShapeDescriptors = other.measureShapeDescriptors;
        this.measureIntegratedDensity = other.measureIntegratedDensity;
        this.measurePixelValueSkewness = other.measurePixelValueSkewness;
        this.measureAreaFraction = other.measureAreaFraction;
        this.measurePixelValueMean = other.measurePixelValueMean;
        this.measurePixelValueModal = other.measurePixelValueModal;
        this.measureCentroid = other.measureCentroid;
        this.measurePerimeter = other.measurePerimeter;
        this.fitEllipse = other.fitEllipse;
        this.measureFeretDiameter = other.measureFeretDiameter;
        this.measurePixelValueMedian = other.measurePixelValueMedian;
        this.measurePixelValueKurtosis = other.measurePixelValueKurtosis;
        this.outputStackPosition = other.outputStackPosition;
    }

    /**
     * Sends the settings to {@link ij.plugin.filter.Analyzer}
     */
    public void apply() {
        Analyzer.setMeasurements(getNativeValue());
    }

    /**
     * Converts the settings into the equivalent used by {@link ij.plugin.filter.Analyzer}
     * See {@link ij.measure.Measurements} for the list of measurements.
     * @return settings used by {@link ij.plugin.filter.Analyzer}
     */
    public int getNativeValue() {
        int result = 0;
        if(measureArea)
            result |= Measurements.AREA;
        if(measurePixelValueMean)
            result |= Measurements.MEAN;
        if(measurePixelValueStandardDeviation)
            result |= Measurements.STD_DEV;
        if(measurePixelValueModal)
            result |= Measurements.MODE;
        if(measurePixelValueMinMax)
            result |= Measurements.MIN_MAX;
        if(measureCentroid)
            result |= Measurements.CENTROID;
        if(measureCenterOfMass)
            result |= Measurements.CENTER_OF_MASS;
        if(measurePerimeter)
            result |= Measurements.PERIMETER;
        if(measureBoundingRectangle)
            result |= Measurements.RECT;
        if(fitEllipse)
            result |= Measurements.ELLIPSE;
        if(measureShapeDescriptors)
            result |= Measurements.SHAPE_DESCRIPTORS;
        if(measureFeretDiameter)
            result |= Measurements.FERET;
        if(measureIntegratedDensity)
            result |= Measurements.INTEGRATED_DENSITY;
        if(measurePixelValueMedian)
            result |= Measurements.MEDIAN;
        if(measurePixelValueSkewness)
            result |= Measurements.SKEWNESS;
        if(measurePixelValueKurtosis)
            result |= Measurements.KURTOSIS;
        if(measureAreaFraction)
            result |= Measurements.AREA_FRACTION;
        if(outputStackPosition)
            result |= Measurements.STACK_POSITION;
        return result;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @ACAQDocumentation(name = "Measure area",
            description = "Measures ROI areas. Area of selection in square pixels. Area is in calibrated units if available. They are stored as 'Area'.")
    @ACAQParameter("measure-area")
    public boolean isMeasureArea() {
        return measureArea;
    }

    @ACAQParameter("measure-area")
    public void setMeasureArea(boolean measureArea) {
        this.measureArea = measureArea;
    }

    @ACAQDocumentation(name = "Measure pixel values standard deviation",
            description = "Measures the standard deviation of greyscale pixel values. They are stored as 'StdDev'.")
    @ACAQParameter("measure-pixel-stdev")
    public boolean isMeasurePixelValueStandardDeviation() {
        return measurePixelValueStandardDeviation;
    }

    @ACAQParameter("measure-pixel-stdev")
    public void setMeasurePixelValueStandardDeviation(boolean measurePixelValueStandardDeviation) {
        this.measurePixelValueStandardDeviation = measurePixelValueStandardDeviation;
    }

    @ACAQDocumentation(name = "Measure min & max pixel values",
            description = "Measures the minimum and maximum greyscale pixel values. They are stored into 'Min' and 'Max' respectively.")
    @ACAQParameter("measure-pixel-minmax")
    public boolean isMeasurePixelValueMinMax() {
        return measurePixelValueMinMax;
    }

    @ACAQParameter("measure-pixel-minmax")
    public void setMeasurePixelValueMinMax(boolean measurePixelValueMinMax) {
        this.measurePixelValueMinMax = measurePixelValueMinMax;
    }

    @ACAQDocumentation(name = "Measure center of mass",
            description = "Measures the brightness-weighted average of the x and y coordinates all pixels in the image or selection. " +
                    "They are stored into 'XM' and 'YM' respectively. These coordinates are the first order spatial moments.")
    @ACAQParameter("measure-center-of-mass")
    public boolean isMeasureCenterOfMass() {
        return measureCenterOfMass;
    }

    @ACAQParameter("measure-center-of-mass")
    public void setMeasureCenterOfMass(boolean measureCenterOfMass) {
        this.measureCenterOfMass = measureCenterOfMass;
    }

    @ACAQDocumentation(name = "Measure bounding rectangle",
            description = "The smallest rectangle enclosing the selection. " +
                    "Uses the headings BX, BY, Width and Height, where BX and BY are the coordinates of the upper left corner of the rectangle.")
    @ACAQParameter("measure-bounding-rectangle")
    public boolean isMeasureBoundingRectangle() {
        return measureBoundingRectangle;
    }

    @ACAQParameter("measure-bounding-rectangle")
    public void setMeasureBoundingRectangle(boolean measureBoundingRectangle) {
        this.measureBoundingRectangle = measureBoundingRectangle;
    }
    @ACAQDocumentation(name = "Measure shape descriptors",
            description = "Measures the following shape descriptors:<br/>" +
                    "<ul><li>Circ. (circularity): 4π*area/perimeter^2. A value of 1.0 indicates a perfect circle. As the value approaches 0.0, it indicates an increasingly elongated shape. Values may not be valid for very small particles.</li>" +
                    "<li>AR (aspect ratio): major_axis/minor_axis. To show major and minor axis enable 'Fit ellipse'</li>" +
                    "<li>Round (roundness): 4*area/(π*major_axis^2), or the inverse of the aspect ratio.</li>" +
                    "<li>Solidity: area/convex area.</li></ul>")
    @ACAQParameter("measure-shape-descriptors")
    public boolean isMeasureShapeDescriptors() {
        return measureShapeDescriptors;
    }

    @ACAQParameter("measure-shape-descriptors")
    public void setMeasureShapeDescriptors(boolean measureShapeDescriptors) {
        this.measureShapeDescriptors = measureShapeDescriptors;
    }

    @ACAQDocumentation(name = "Measure integrated density",
            description = "Calculates and displays two values: \"IntDen\" (the product of Area and Mean Gray Value) and \"RawIntDen\" (the sum of the values of the pixels in the image or selection).")
    @ACAQParameter("measure-integrated-density")
    public boolean isMeasureIntegratedDensity() {
        return measureIntegratedDensity;
    }

    @ACAQParameter("measure-integrated-density")
    public void setMeasureIntegratedDensity(boolean measureIntegratedDensity) {
        this.measureIntegratedDensity = measureIntegratedDensity;
    }

    @ACAQDocumentation(name = "Measure pixel values skewness",
            description = "The third order moment about the mean of grayscale pixel values. Values are stored in 'Skew'.")
    @ACAQParameter("measure-pixel-skewness")
    public boolean isMeasurePixelValueSkewness() {
        return measurePixelValueSkewness;
    }

    @ACAQParameter("measure-pixel-skewness")
    public void setMeasurePixelValueSkewness(boolean measurePixelValueSkewness) {
        this.measurePixelValueSkewness = measurePixelValueSkewness;
    }

    @ACAQDocumentation(name = "Measure area fraction",
            description = "The percentage of non-zero pixels. Values are stored in '%Area'")
    @ACAQParameter("measure-area-fraction")
    public boolean isMeasureAreaFraction() {
        return measureAreaFraction;
    }

    @ACAQParameter("measure-area-fraction")
    public void setMeasureAreaFraction(boolean measureAreaFraction) {
        this.measureAreaFraction = measureAreaFraction;
    }

    @ACAQDocumentation(name = "Measure pixel values mean",
            description = "Mean of grayscale pixel values. Values are stored in 'Mean'.")
    @ACAQParameter("measure-pixel-mean")
    public boolean isMeasurePixelValueMean() {
        return measurePixelValueMean;
    }

    @ACAQParameter("measure-pixel-mean")
    public void setMeasurePixelValueMean(boolean measurePixelValueMean) {
        this.measurePixelValueMean = measurePixelValueMean;
    }

    @ACAQDocumentation(name = "Measure pixel values modal",
            description = "Most frequently occurring gray value within the selection. Values are stored in 'Mode'.")
    @ACAQParameter("measure-pixel-modal")
    public boolean isMeasurePixelValueModal() {
        return measurePixelValueModal;
    }

    @ACAQParameter("measure-pixel-modal")
    public void setMeasurePixelValueModal(boolean measurePixelValueModal) {
        this.measurePixelValueModal = measurePixelValueModal;
    }

    @ACAQDocumentation(name = "Measure centroid",
            description = "The center point of the selection. This is the average of the x and y coordinates of all of the pixels in the image or selection. " +
                    "Values are stored in 'X' and 'Y' respectively.")
    @ACAQParameter("measure-centroid")
    public boolean isMeasureCentroid() {
        return measureCentroid;
    }

    @ACAQParameter("measure-centroid")
    public void setMeasureCentroid(boolean measureCentroid) {
        this.measureCentroid = measureCentroid;
    }

    @ACAQDocumentation(name = "Measure perimeter",
            description = "The length of the outside boundary of the selection. " +
                    "Values are stored in 'Perim.'")
    @ACAQParameter("measure-perimeter")
    public boolean isMeasurePerimeter() {
        return measurePerimeter;
    }

    @ACAQParameter("measure-perimeter")
    public void setMeasurePerimeter(boolean measurePerimeter) {
        this.measurePerimeter = measurePerimeter;
    }

    @ACAQDocumentation(name = "Fit ellipse",
            description = "Fit an ellipse to the selection. Uses the headings Major, Minor and Angle. Major and Minor are the primary" +
                    " and seconday axis of the best fitting ellipse. Angle (0-180 degrees) is the" +
                    " angle between the primary axis and a line parallel to the x-axis of the image. " +
                    "The coordinates of the center of the ellipse are displayed as X and Y if Centroid is checked. " +
                    "Note that ImageJ cannot calculate the major and minor axis lengths if Pixel Aspect Ratio is not 1.0.")
    @ACAQParameter("fit-ellipse")
    public boolean isFitEllipse() {
        return fitEllipse;
    }

    @ACAQParameter("fit-ellipse")
    public void setFitEllipse(boolean fitEllipse) {
        this.fitEllipse = fitEllipse;
    }

    @ACAQDocumentation(name = "Measure Feret's diameter",
            description = "The longest distance between any two points along the selection boundary, also known as maximum caliper. " +
                    "Uses the Feret heading. FeretAngle (0-180 degrees) is the angle between the Feret's diameter and a line parallel to the x-axis of the image. " +
                    "MinFeret is the minimum caliper diameter. " +
                    "The starting coordinates of the Feret's diameter (FeretX and FeretY) are also generated.")
    @ACAQParameter("measure-feret")
    public boolean isMeasureFeretDiameter() {
        return measureFeretDiameter;
    }

    @ACAQParameter("measure-feret")
    public void setMeasureFeretDiameter(boolean measureFeretDiameter) {
        this.measureFeretDiameter = measureFeretDiameter;
    }

    @ACAQDocumentation(name = "Measure pixel value median",
            description = "The median value of the pixels in the image or selection. " +
                    "Values are stored in 'Median'")
    @ACAQParameter("measure-pixel-median")
    public boolean isMeasurePixelValueMedian() {
        return measurePixelValueMedian;
    }

    @ACAQParameter("measure-pixel-median")
    public void setMeasurePixelValueMedian(boolean measurePixelValueMedian) {
        this.measurePixelValueMedian = measurePixelValueMedian;
    }

    @ACAQDocumentation(name = "Measure pixel value kurtosis",
            description = "The fourth order moment about the greyscale pixel value mean. " +
                    "Values are stored in 'Kurt'")
    @ACAQParameter("measure-pixel-kurtosis")
    public boolean isMeasurePixelValueKurtosis() {
        return measurePixelValueKurtosis;
    }

    @ACAQParameter("measure-pixel-kurtosis")
    public void setMeasurePixelValueKurtosis(boolean measurePixelValueKurtosis) {
        this.measurePixelValueKurtosis = measurePixelValueKurtosis;
    }

    @ACAQDocumentation(name = "Output stack position",
            description = "The current position (channel, slice and frame) in the stack or hyperstack. Uses the headings \"Ch\", \"Slice\" and \"Frame\"." +
                    " Please note that ACAQ5 algorithms sometimes handle stacks differently compared to their ImageJ equivalents. We recommend to " +
                    "not rely on this measurement.")
    @ACAQParameter("output-stack-position")
    public boolean isOutputStackPosition() {
        return outputStackPosition;
    }

    @ACAQParameter("output-stack-position")
    public void setOutputStackPosition(boolean outputStackPosition) {
        this.outputStackPosition = outputStackPosition;
    }
}
