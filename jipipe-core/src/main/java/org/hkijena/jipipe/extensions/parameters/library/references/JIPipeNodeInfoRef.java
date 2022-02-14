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
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;

import java.io.IOException;

/**
 * A parameter that holds a reference to an {@link JIPipeNodeInfo}
 */
@JsonSerialize(using = JIPipeNodeInfoRef.Serializer.class)
@JsonDeserialize(using = JIPipeNodeInfoRef.Deserializer.class)
public class JIPipeNodeInfoRef implements JIPipeValidatable {
    private JIPipeNodeInfo info;

    /**
     * @param info The referenced info
     */
    public JIPipeNodeInfoRef(JIPipeNodeInfo info) {
        this.info = info;
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public JIPipeNodeInfoRef(JIPipeNodeInfoRef other) {
        this.info = other.info;
    }

    /**
     * New instance
     */
    public JIPipeNodeInfoRef() {

    }

    public JIPipeNodeInfo getInfo() {
        return info;
    }

    public void setInfo(JIPipeNodeInfo info) {
        this.info = info;
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        if (info == null)
            report.reportIsInvalid("No algorithm type is selected!",
                    "You have to select an algorithm type.",
                    "Please select an algorithm type.",
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
    public static class Serializer extends JsonSerializer<JIPipeNodeInfoRef> {

        @Override
        public void serialize(JIPipeNodeInfoRef ref, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeString(ref.getInfo() != null ? ref.getInfo().getId() : null);
        }

    }

    /**
     * Deserializes the reference from a string
     */
    public static class Deserializer extends JsonDeserializer<JIPipeNodeInfoRef> {

        @Override
        public JIPipeNodeInfoRef deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode node = jsonParser.readValueAsTree();
            JIPipeNodeInfoRef result = new JIPipeNodeInfoRef();
            if (!node.isNull()) {
                result.setInfo(JIPipe.getNodes().getInfoById(node.textValue()));
            }
            return result;
        }
    }
}
