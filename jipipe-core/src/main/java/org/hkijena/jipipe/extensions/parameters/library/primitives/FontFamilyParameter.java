package org.hkijena.jipipe.extensions.parameters.library.primitives;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.jipipe.extensions.parameters.api.enums.EnumParameterSettings;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@EnumParameterSettings(searchable = true)
@JsonSerialize(using = FontFamilyParameter.Serializer.class)
@JsonDeserialize(using = FontFamilyParameter.Deserializer.class)
public class FontFamilyParameter extends DynamicStringEnumParameter {
    public static List<String> AVAILABLE_FONTS = new ArrayList<>();

    public FontFamilyParameter() {
        initializeAvailableValues();
        setValue("Dialog");
    }

    public FontFamilyParameter(DynamicStringEnumParameter other) {
        super(other);
        initializeAvailableValues();
    }

    public FontFamilyParameter(String value) {
        super(value);
        initializeAvailableValues();
    }

    private void initializeAvailableValues() {
        if (AVAILABLE_FONTS.isEmpty()) {
            String[] familyNames = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            AVAILABLE_FONTS.addAll(Arrays.asList(familyNames));
        }
        setAllowedValues(AVAILABLE_FONTS);
    }

    @Override
    public String renderLabel(String value) {
        return "<html><span style=\"font-family: " + value + ";\">" + value + "</span></html>";
    }

    @Override
    public Icon renderIcon(String value) {
        return UIUtils.getIconFromResources("actions/dialog-text-and-font.png");
    }

    public Font toFont(int style, int size) {
        String v = AVAILABLE_FONTS.contains(getValue()) ? getValue() : "Dialog";
        return new Font(v, style, size);
    }

    /**
     * Serializes {@link FontFamilyParameter}
     */
    public static class Serializer extends JsonSerializer<FontFamilyParameter> {
        @Override
        public void serialize(FontFamilyParameter value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
            gen.writeString("" + value.getValue());
        }
    }

    /**
     * Deserializes {@link FontFamilyParameter}
     */
    public static class Deserializer extends JsonDeserializer<FontFamilyParameter> {
        @Override
        public FontFamilyParameter deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            return new FontFamilyParameter(((JsonNode) p.readValueAsTree()).textValue());
        }
    }
}
