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

package org.hkijena.jipipe.plugins.parameters.library.auth;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Charsets;

import java.io.IOException;
import java.util.Base64;
import java.util.Objects;

/**
 * A parameter that stores a password
 */
@JsonSerialize(using = PasswordParameter.Serializer.class)
@JsonDeserialize(using = PasswordParameter.Deserializer.class)
public class PasswordParameter {

    private String password = "";

    public PasswordParameter() {
    }

    public PasswordParameter(String password) {
        this.password = password;
    }

    public PasswordParameter(PasswordParameter other) {
        this.password = other.password;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PasswordParameter that = (PasswordParameter) o;
        return Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(password);
    }

    public static class Serializer extends JsonSerializer<PasswordParameter> {
        @Override
        public void serialize(PasswordParameter passwordParameter, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeString(Base64.getEncoder().encodeToString(passwordParameter.password.getBytes(Charsets.UTF_8)));
        }
    }

    public static class Deserializer extends JsonDeserializer<PasswordParameter> {
        @Override
        public PasswordParameter deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode node = jsonParser.readValueAsTree();
            if (node.isNull())
                return new PasswordParameter();
            else {
                String encoded = node.asText();
                byte[] decodedBytes = Base64.getDecoder().decode(encoded);
                String decodedString = new String(decodedBytes);
                return new PasswordParameter(decodedString);
            }
        }
    }
}
