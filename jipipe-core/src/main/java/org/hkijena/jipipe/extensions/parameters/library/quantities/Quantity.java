package org.hkijena.jipipe.extensions.parameters.library.quantities;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A double with a unit
 */
public class Quantity {

    /**
     * Contains factors to the standard unit
     * Length: m
     * Weight: g
     * Time: s
     */
    public static final Map<String, Double> UNITS_FACTORS = new HashMap<>();
    public static final Pattern PARSE_QUANTITY_PATTERN = Pattern.compile("([+-]?\\d+[,.]?\\d*)(.*)");
    public static final String UNIT_NO_UNIT = "";

    public static final String UNIT_PIXELS = "pixels";
    public static final String[] KNOWN_UNITS_IMAGE_DIMENSIONS = new String[]{
            "pixel", "nm", "µm", "microns", "mm", "cm", "dm", "m", "km",
            "inch", "in", "foot", "ft", "yard", "yd"
    };
    public static final String[] KNOWN_UNITS = new String[]{
            "pixel",
            "nm", "µm", "microns", "mm", "cm", "dm", "m", "km",
            "inch", "in", "foot", "ft", "yard", "yd",
            "ng", "µg", "mg", "g", "kg", "t",
            "Da",
            "oz", "lb",
            "ns", "µs", "ms", "s", "min", "h", "d"
    };

    static {
        // Length
        UNITS_FACTORS.put("nm", 1e-9);
        UNITS_FACTORS.put("µm", 1e-6);
        UNITS_FACTORS.put("micron", 1e-6);
        UNITS_FACTORS.put("microns", 1e-6);
        UNITS_FACTORS.put("um", 1e-6);
        UNITS_FACTORS.put("mm", 0.001);
        UNITS_FACTORS.put("cm", 0.01);
        UNITS_FACTORS.put("dm", 0.1);
        UNITS_FACTORS.put("m", 1.0);
        UNITS_FACTORS.put("km", 1000.0);

        UNITS_FACTORS.put("inch", 0.0254);
        UNITS_FACTORS.put("in", 0.0254);
        UNITS_FACTORS.put("foot", 0.3048);
        UNITS_FACTORS.put("ft", 0.3048);
        UNITS_FACTORS.put("yard", 0.9144);
        UNITS_FACTORS.put("yd", 0.9144);
        UNITS_FACTORS.put("mile", 1609.34);
        UNITS_FACTORS.put("mi", 1609.34);

        // Weight
        UNITS_FACTORS.put("ng", 1e-9);
        UNITS_FACTORS.put("µg", 1e-6);
        UNITS_FACTORS.put("ug", 1e-6);
        UNITS_FACTORS.put("mg", 0.001);
        UNITS_FACTORS.put("g", 1.0);
        UNITS_FACTORS.put("kg", 1000.0);
        UNITS_FACTORS.put("t", 1000000.0);

        UNITS_FACTORS.put("Da", 1.66054e-24);

        UNITS_FACTORS.put("oz", 28.3495);
        UNITS_FACTORS.put("lb", 453.592);

        // Time
        UNITS_FACTORS.put("ns", 1e-9);
        UNITS_FACTORS.put("µs", 1e-6);
        UNITS_FACTORS.put("us", 1e-6);
        UNITS_FACTORS.put("ms", 0.001);
        UNITS_FACTORS.put("s", 1.0);
        UNITS_FACTORS.put("min", 60.0);
        UNITS_FACTORS.put("h", 3600.0);
        UNITS_FACTORS.put("d", 86400.0);
        UNITS_FACTORS.put("a", 3.154e+7);
    }

    private double value;
    private String unit;

    public Quantity() {
    }

    public Quantity(double value, String unit) {
        this.value = value;
        this.unit = unit;
    }

    public Quantity(Quantity other) {
        this.value = other.value;
        this.unit = other.unit;
    }

    /**
     * Parses a quantity from a string
     *
     * @param string the parsed string
     */
    public static Quantity parse(String string) {
        Matcher matcher = PARSE_QUANTITY_PATTERN.matcher(string);
        if (matcher.find()) {
            String valueString = matcher.group(1);
            String unitString = matcher.groupCount() >= 2 ? matcher.group(2) : "";
            valueString = valueString.replace(',', '.').replace(" ", "");
            unitString = unitString.replace(" ", "");
            return new Quantity(NumberUtils.createDouble(valueString), unitString);
        } else {
            return null;
        }
    }

    public static boolean isPixelsUnit(String unit) {
        return "px".equals(unit) || "pixels".equals(unit) || "pixel".equals(unit);
    }

    @Override
    public String toString() {
        return (value + " " + unit).trim();
    }

    @JsonGetter("value")
    public double getValue() {
        return value;
    }

    @JsonSetter("value")
    public void setValue(double value) {
        this.value = value;
    }

    @JsonGetter("unit")
    public String getUnit() {
        return unit;
    }

    @JsonSetter("unit")
    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Quantity convertTo(String targetUnit) {
        if (Objects.equals(targetUnit, unit)) {
            return this;
        }
        // Pixel special case
        if (unitIsPixels()) {
            if ("px".equals(targetUnit) || "pixels".equals(targetUnit) || "pixel".equals(targetUnit)) {
                return this;
            }
        }

        Double sourceFactor = UNITS_FACTORS.getOrDefault(getUnit(), null);
        Double targetFactor = UNITS_FACTORS.getOrDefault(targetUnit, null);

        if (sourceFactor == null) {
            throw new RuntimeException("Unsupported unit " + unit + ". Supported are: " + String.join(", ", UNITS_FACTORS.keySet()));
        }
        if (targetFactor == null) {
            throw new RuntimeException("Unsupported unit " + targetUnit + ". Supported are: " + String.join(", ", UNITS_FACTORS.keySet()));
        }

        return new Quantity((getValue() * sourceFactor) / targetFactor, targetUnit);
    }

    public boolean unitIsPixels() {
        return isPixelsUnit(unit);
    }

    public Quantity convertToPixels(Quantity pixelSize) {
        if (unitIsPixels()) {
            // Already pixels
            return this;
        } else if (unit.equalsIgnoreCase(pixelSize.unit)) {
            // Same unit
            return new Quantity(value / pixelSize.value, "pixels");
        } else {
            // Different unit -> convert to same unit first
            Quantity converted = convertTo(pixelSize.getUnit());
            return converted.convertToPixels(pixelSize);
        }
    }
}
