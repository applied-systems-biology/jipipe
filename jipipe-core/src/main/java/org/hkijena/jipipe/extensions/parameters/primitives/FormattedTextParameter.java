package org.hkijena.jipipe.extensions.parameters.primitives;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;

@JsonDeserialize(using = FormattedTextParameter.Deserializer.class)
public class FormattedTextParameter {
    private String unformattedText;
    private String formattedText;

    public FormattedTextParameter() {
    }

    public FormattedTextParameter(String unformattedText, String formattedText) {
        this.unformattedText = unformattedText;
        this.formattedText = formattedText;
    }

    public FormattedTextParameter(FormattedTextParameter other) {
        this.unformattedText = other.unformattedText;
    }

    @JsonGetter("unformatted-text")
    public String getUnformattedText() {
        return unformattedText;
    }

    @JsonSetter("formatted-text")
    public void setUnformattedText(String unformattedText) {
        this.unformattedText = unformattedText;
    }

    @JsonGetter("formatted-text")
    public String getFormattedText() {
        return formattedText;
    }

    @JsonSetter("formatted-text")
    public void setFormattedText(String formattedText) {
        this.formattedText = formattedText;
    }

    public static class Deserializer extends JsonDeserializer<FormattedTextParameter> {
        @Override
        public FormattedTextParameter deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonNode node = p.readValueAsTree();
            if(node.isTextual())
                return new FormattedTextParameter(node.asText(), node.asText());
            else {
                return new FormattedTextParameter(node.get("unformatted-text").asText(), node.get("formatted-text").asText());
            }
        }
    }
}
