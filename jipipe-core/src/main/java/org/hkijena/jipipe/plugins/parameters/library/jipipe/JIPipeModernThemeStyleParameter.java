package org.hkijena.jipipe.plugins.parameters.library.jipipe;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.jipipe.plugins.parameters.api.enums.DynamicEnumParameter;
import org.hkijena.jipipe.utils.ThemeUtils;

import java.io.IOException;
import java.util.ArrayList;

@JsonSerialize(using = JIPipeModernThemeStyleParameter.Serializer.class)
@JsonDeserialize(using = JIPipeModernThemeStyleParameter.Deserializer.class)
public class JIPipeModernThemeStyleParameter extends DynamicEnumParameter<String> {

    /**
     * Creates a new instance with null value
     */
    public JIPipeModernThemeStyleParameter() {
        initializeAllowedValues();
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public JIPipeModernThemeStyleParameter(JIPipeModernThemeStyleParameter other) {
        setValue(other.getValue());
        setAllowedValues(new ArrayList<>(other.getAllowedValues()));
    }

    /**
     * Creates a new instance
     *
     * @param value initial value
     */
    public JIPipeModernThemeStyleParameter(String value) {
        super(value);
        initializeAllowedValues();
    }

    /**
     * Serializes {@link JIPipeModernThemeStyleParameter}
     */
    public static class Serializer extends JsonSerializer<JIPipeModernThemeStyleParameter> {
        @Override
        public void serialize(JIPipeModernThemeStyleParameter value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
            gen.writeString("" + value.getValue());
        }
    }

    /**
     * Deserializes {@link JIPipeModernThemeStyleParameter}
     */
    public static class Deserializer extends JsonDeserializer<JIPipeModernThemeStyleParameter> {
        @Override
        public JIPipeModernThemeStyleParameter deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            return new JIPipeModernThemeStyleParameter(((JsonNode) p.readValueAsTree()).textValue());
        }
    }

    private void initializeAllowedValues() {
        setAllowedValues(ThemeUtils.getAvailableStyleIds());
    }
}
