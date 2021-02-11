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

package org.hkijena.jipipe.extensions.parameters.references;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.jipipe.api.JIPipeValidatable;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;

import java.io.IOException;
import java.util.Objects;

/**
 * Helper to allow easy serialization of {@link JIPipeDataInfo} references
 */
@JsonSerialize(using = JIPipeDataInfoRef.Serializer.class)
@JsonDeserialize(using = JIPipeDataInfoRef.Deserializer.class)
public class JIPipeDataInfoRef implements JIPipeValidatable {

    private JIPipeDataInfo info;

    /**
     * Initializes from data ID
     *
     * @param id data id
     */
    public JIPipeDataInfoRef(String id) {
        this(JIPipeDataInfo.getInstance(id));
    }

    /**
     * @param info The referenced info
     */
    public JIPipeDataInfoRef(JIPipeDataInfo info) {
        this.info = info;
    }

    /**
     * New instance
     */
    public JIPipeDataInfoRef() {

    }

    /**
     * Creates a copy
     *
     * @param other the other
     */
    public JIPipeDataInfoRef(JIPipeDataInfoRef other) {
        if (other != null) {
            this.info = other.info;
        }
    }

    public JIPipeDataInfo getInfo() {
        return info;
    }

    public void setInfo(JIPipeDataInfo info) {
        this.info = info;
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        if (info == null)
            report.reportIsInvalid("No data type is selected!",
                    "You have to select an data type.",
                    "Please select an data type.",
                    this);
    }

    @Override
    public String toString() {
        if (info != null)
            return info.getId();
        else
            return "<Null>";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JIPipeDataInfoRef that = (JIPipeDataInfoRef) o;
        return Objects.equals(info, that.info);
    }

    @Override
    public int hashCode() {
        return Objects.hash(info);
    }

    /**
     * Serializes the reference as ID
     */
    public static class Serializer extends JsonSerializer<JIPipeDataInfoRef> {

        @Override
        public void serialize(JIPipeDataInfoRef ref, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeString(ref.getInfo() != null ? ref.getInfo().getId() : null);
        }

    }

    /**
     * Deserializes the reference from a string
     */
    public static class Deserializer extends JsonDeserializer<JIPipeDataInfoRef> {

        @Override
        public JIPipeDataInfoRef deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode node = jsonParser.readValueAsTree();
            JIPipeDataInfoRef result = new JIPipeDataInfoRef();
            if (!node.isNull()) {
                result.setInfo(JIPipeDataInfo.getInstance(node.textValue()));
            }
            return result;
        }
    }
}
