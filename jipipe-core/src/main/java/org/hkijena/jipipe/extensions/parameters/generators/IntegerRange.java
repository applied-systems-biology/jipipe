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

package org.hkijena.jipipe.extensions.parameters.generators;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import org.hkijena.jipipe.utils.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parameter that contains an integer range as string.
 * The format is the following: [range];[range];... with range being an integer or [from]-[to]
 * where from and to are inclusive. Returns the list of integers defined by the string. Empty ranges are ignored.
 * Spaces are ignored. Negative values must be enclosed in brackets
 */
public class IntegerRange {

    public static final String DOCUMENTATION_DESCRIPTION = "The format is the following: [range];[range];... with [range] either being a single integer or a range [from]-[to] (both inclusive). Negative values must be enclosed in parentheses. Example: 0-5;1;(-1)-10";

    private String value;

    /**
     * Creates a new instance with a null value
     */
    public IntegerRange() {
    }

    /**
     * Creates a new instance and initializes it
     *
     * @param value the value
     */
    public IntegerRange(String value) {
        this.value = value;
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public IntegerRange(IntegerRange other) {
        this.value = other.value;
    }

    @JsonGetter("value")
    public String getValue() {
        return value;
    }

    @JsonSetter("value")
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Generates the list of integers based on the value. Throws no exceptions.
     *
     * @return null if the format is wrong
     */
    public List<Integer> tryGetIntegers() {
        try {
            return getIntegers();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Generates the list of integers based on the value
     *
     * @return the generated integers
     * @throws NumberFormatException if the format is wrong
     */
    public List<Integer> getIntegers() throws NumberFormatException {
        String string = StringUtils.orElse(value, "").replace(" ", "");
        List<Integer> integers = new ArrayList<>();
        string = string.replace(',', ';');
        for (String range : string.split(";")) {
            if (StringUtils.isNullOrEmpty(range))
                continue;
            if (range.contains("-")) {
                StringBuilder fromBuilder = new StringBuilder();
                StringBuilder toBuilder = new StringBuilder();
                boolean negative = false;
                boolean writeToFrom = true;
                for (int i = 0; i < range.length(); i++) {
                    char c = range.charAt(i);
                    if (c == '(') {
                        if (negative)
                            throw new NumberFormatException("Cannot nest brackets!");
                        negative = true;
                    } else if (c == ')') {
                        if (!negative)
                            throw new NumberFormatException("Cannot end missing start bracket!");
                        negative = false;
                    } else if (c == '-') {
                        if (negative) {
                            if (writeToFrom)
                                fromBuilder.append(c);
                            else
                                toBuilder.append(c);
                        } else {
                            if (!writeToFrom)
                                throw new RuntimeException("Additional hyphen detected!");
                            writeToFrom = false;
                        }
                    } else {
                        if (writeToFrom)
                            fromBuilder.append(c);
                        else
                            toBuilder.append(c);
                    }
                }

                // Parse borders
                int from = Integer.parseInt(fromBuilder.toString());
                int to = Integer.parseInt(toBuilder.toString());

                if (from <= to) {
                    for (int i = from; i <= to; ++i) {
                        integers.add(i);
                    }
                } else {
                    for (int i = to; i >= to; --i) {
                        integers.add(i);
                    }
                }
            } else {
                integers.add(Integer.parseInt(range));
            }
        }
        return integers;
    }

    @Override
    public String toString() {
        return StringUtils.orElse(value, "[Empty]");
    }

    /**
     * Serializes the reference as ID
     */
    public static class Serializer extends JsonSerializer<IntegerRange> {

        @Override
        public void serialize(IntegerRange ref, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeString(ref.value);
        }

    }

    /**
     * Deserializes the reference from a string
     */
    public static class Deserializer extends JsonDeserializer<IntegerRange> {

        @Override
        public IntegerRange deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode node = jsonParser.readValueAsTree();
            IntegerRange result = new IntegerRange();
            if (!node.isNull()) {
                result.setValue(node.textValue());
            }
            return result;
        }
    }
}
