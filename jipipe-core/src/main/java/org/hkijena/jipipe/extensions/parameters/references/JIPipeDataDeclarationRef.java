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
import org.hkijena.jipipe.api.data.JIPipeDataDeclaration;

import java.io.IOException;
import java.util.Objects;

/**
 * Helper to allow easy serialization of {@link JIPipeDataDeclaration} references
 */
@JsonSerialize(using = JIPipeDataDeclarationRef.Serializer.class)
@JsonDeserialize(using = JIPipeDataDeclarationRef.Deserializer.class)
public class JIPipeDataDeclarationRef implements JIPipeValidatable {

    private JIPipeDataDeclaration declaration;

    /**
     * Initializes from data ID
     *
     * @param id data id
     */
    public JIPipeDataDeclarationRef(String id) {
        this(JIPipeDataDeclaration.getInstance(id));
    }

    /**
     * @param declaration The referenced declaration
     */
    public JIPipeDataDeclarationRef(JIPipeDataDeclaration declaration) {
        this.declaration = declaration;
    }

    /**
     * New instance
     */
    public JIPipeDataDeclarationRef() {

    }

    /**
     * Creates a copy
     *
     * @param other the other
     */
    public JIPipeDataDeclarationRef(JIPipeDataDeclarationRef other) {
        if (other != null) {
            this.declaration = other.declaration;
        }
    }

    public JIPipeDataDeclaration getDeclaration() {
        return declaration;
    }

    public void setDeclaration(JIPipeDataDeclaration declaration) {
        this.declaration = declaration;
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        if (declaration == null)
            report.reportIsInvalid("No data type is selected!",
                    "You have to select an data type.",
                    "Please select an data type.",
                    this);
    }

    @Override
    public String toString() {
        if (declaration != null)
            return declaration.getId();
        else
            return "<Null>";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JIPipeDataDeclarationRef that = (JIPipeDataDeclarationRef) o;
        return Objects.equals(declaration, that.declaration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(declaration);
    }

    /**
     * Serializes the reference as ID
     */
    public static class Serializer extends JsonSerializer<JIPipeDataDeclarationRef> {

        @Override
        public void serialize(JIPipeDataDeclarationRef ref, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeString(ref.getDeclaration() != null ? ref.getDeclaration().getId() : null);
        }

    }

    /**
     * Deserializes the reference from a string
     */
    public static class Deserializer extends JsonDeserializer<JIPipeDataDeclarationRef> {

        @Override
        public JIPipeDataDeclarationRef deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode node = jsonParser.readValueAsTree();
            JIPipeDataDeclarationRef result = new JIPipeDataDeclarationRef();
            if (!node.isNull()) {
                result.setDeclaration(JIPipeDataDeclaration.getInstance(node.textValue()));
            }
            return result;
        }
    }
}
