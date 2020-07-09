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
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmDeclaration;
import org.hkijena.jipipe.api.registries.JIPipeAlgorithmRegistry;

import java.io.IOException;

/**
 * A parameter that holds a reference to an {@link JIPipeAlgorithmDeclaration}
 */
@JsonSerialize(using = JIPipeAlgorithmDeclarationRef.Serializer.class)
@JsonDeserialize(using = JIPipeAlgorithmDeclarationRef.Deserializer.class)
public class JIPipeAlgorithmDeclarationRef implements JIPipeValidatable {
    private JIPipeAlgorithmDeclaration declaration;

    /**
     * @param declaration The referenced declaration
     */
    public JIPipeAlgorithmDeclarationRef(JIPipeAlgorithmDeclaration declaration) {
        this.declaration = declaration;
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public JIPipeAlgorithmDeclarationRef(JIPipeAlgorithmDeclarationRef other) {
        this.declaration = other.declaration;
    }

    /**
     * New instance
     */
    public JIPipeAlgorithmDeclarationRef() {

    }

    public JIPipeAlgorithmDeclaration getDeclaration() {
        return declaration;
    }

    public void setDeclaration(JIPipeAlgorithmDeclaration declaration) {
        this.declaration = declaration;
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        if (declaration == null)
            report.reportIsInvalid("No algorithm type is selected!",
                    "You have to select an algorithm type.",
                    "Please select an algorithm type.",
                    this);
    }

    @Override
    public String toString() {
        if (declaration != null)
            return declaration.getId();
        else
            return "<Null>";
    }

    /**
     * Serializes the reference as ID
     */
    public static class Serializer extends JsonSerializer<JIPipeAlgorithmDeclarationRef> {

        @Override
        public void serialize(JIPipeAlgorithmDeclarationRef ref, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeString(ref.getDeclaration() != null ? ref.getDeclaration().getId() : null);
        }

    }

    /**
     * Deserializes the reference from a string
     */
    public static class Deserializer extends JsonDeserializer<JIPipeAlgorithmDeclarationRef> {

        @Override
        public JIPipeAlgorithmDeclarationRef deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode node = jsonParser.readValueAsTree();
            JIPipeAlgorithmDeclarationRef result = new JIPipeAlgorithmDeclarationRef();
            if (!node.isNull()) {
                result.setDeclaration(JIPipeAlgorithmRegistry.getInstance().getDeclarationById(node.textValue()));
            }
            return result;
        }
    }
}
