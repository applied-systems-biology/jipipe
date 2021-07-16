package org.hkijena.jipipe.extensions.parameters.quantities;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A double with a unit
 */
public class Quantity {
    public static final Pattern PARSE_QUANTITY_PATTERN = Pattern.compile("([+-]?\\d+[,.]?\\d*)(.*)");
    public static final String UNIT_NO_UNIT = "";
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

    public static final String[] KNOWN_UNITS_IMAGE_DIMENSIONS = new String[] {
        "pixel", "nm", "µm", "microns", "mm", "cm", "dm", "m", "km"
    };

    public static final String[] KNOWN_UNITS = new String[] {
        "pixel",
        "nm", "µm", "microns", "mm", "cm", "dm", "m", "km",
        "ng", "µg", "mg", "g", "kg", "t",
        "Da",
        "ns", "µs", "ms", "s", "min", "h", "d"
    };

    /**
     * Parses a quantity from a string
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
        }
        else {
            return null;
        }
    }
}
