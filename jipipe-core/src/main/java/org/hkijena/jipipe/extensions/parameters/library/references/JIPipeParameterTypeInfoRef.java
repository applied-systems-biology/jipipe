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

package org.hkijena.jipipe.extensions.parameters.library.references;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeValidatable;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;

import java.io.IOException;
import java.util.Objects;

/**
 * A parameter that holds a reference to an {@link org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo}
 */
@JsonSerialize(using = JIPipeParameterTypeInfoRef.Serializer.class)
@JsonDeserialize(using = JIPipeParameterTypeInfoRef.Deserializer.class)
public class JIPipeParameterTypeInfoRef implements JIPipeValidatable {
    private JIPipeParameterTypeInfo info;

    /**
     * @param fieldClass The field class of the parameter
     */
    public JIPipeParameterTypeInfoRef(Class<?> fieldClass) {
        this.info = Objects.requireNonNull(JIPipe.getParameterTypes().getInfoByFieldClass(fieldClass));
    }

    /**
     * @param info The referenced info
     */
    public JIPipeParameterTypeInfoRef(JIPipeParameterTypeInfo info) {
        this.info = info;
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public JIPipeParameterTypeInfoRef(JIPipeParameterTypeInfoRef other) {
        this.info = other.info;
    }

    /**
     * New instance
     */
    public JIPipeParameterTypeInfoRef() {

    }

    public JIPipeParameterTypeInfo getInfo() {
        return info;
    }

    public void setInfo(JIPipeParameterTypeInfo info) {
        this.info = info;
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        if (info == null)
            report.reportIsInvalid("No parameter type type is selected!",
                    "You have to select a parameter type.",
                    "Please select a parameter type.",
                    this);
    }

    @Override
    public String toString() {
        if (info != null)
            return info.getId();
        else
            return "<Null>";
    }

    /**
     * Serializes the reference as ID
     */
    public static class Serializer extends JsonSerializer<JIPipeParameterTypeInfoRef> {

        @Override
        public void serialize(JIPipeParameterTypeInfoRef ref, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeString(ref.getInfo() != null ? ref.getInfo().getId() : null);
        }

    }

    /**
     * Deserializes the reference from a string
     */
    public static class Deserializer extends JsonDeserializer<JIPipeParameterTypeInfoRef> {

        @Override
        public JIPipeParameterTypeInfoRef deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode node = jsonParser.readValueAsTree();
            JIPipeParameterTypeInfoRef result = new JIPipeParameterTypeInfoRef();
            if (!node.isNull()) {
                result.setInfo(JIPipe.getParameterTypes().getInfoById(node.textValue()));
            }
            return result;
        }
    }
}