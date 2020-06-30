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

package org.hkijena.acaq5.extensions.parameters.references;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.data.ACAQDataDeclaration;

import java.io.IOException;
import java.util.Objects;

/**
 * Helper to allow easy serialization of {@link ACAQDataDeclaration} references
 */
@JsonSerialize(using = ACAQDataDeclarationRef.Serializer.class)
@JsonDeserialize(using = ACAQDataDeclarationRef.Deserializer.class)
public class ACAQDataDeclarationRef implements ACAQValidatable {

    private ACAQDataDeclaration declaration;

    /**
     * Initializes from data ID
     *
     * @param id data id
     */
    public ACAQDataDeclarationRef(String id) {
        this(ACAQDataDeclaration.getInstance(id));
    }

    /**
     * @param declaration The referenced declaration
     */
    public ACAQDataDeclarationRef(ACAQDataDeclaration declaration) {
        this.declaration = declaration;
    }

    /**
     * New instance
     */
    public ACAQDataDeclarationRef() {

    }

    /**
     * Creates a copy
     *
     * @param other the other
     */
    public ACAQDataDeclarationRef(ACAQDataDeclarationRef other) {
        if (other != null) {
            this.declaration = other.declaration;
        }
    }

    public ACAQDataDeclaration getDeclaration() {
        return declaration;
    }

    public void setDeclaration(ACAQDataDeclaration declaration) {
        this.declaration = declaration;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
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
        ACAQDataDeclarationRef that = (ACAQDataDeclarationRef) o;
        return Objects.equals(declaration, that.declaration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(declaration);
    }

    /**
     * Serializes the reference as ID
     */
    public static class Serializer extends JsonSerializer<ACAQDataDeclarationRef> {

        @Override
        public void serialize(ACAQDataDeclarationRef ref, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeString(ref.getDeclaration() != null ? ref.getDeclaration().getId() : null);
        }

    }

    /**
     * Deserializes the reference from a string
     */
    public static class Deserializer extends JsonDeserializer<ACAQDataDeclarationRef> {

        @Override
        public ACAQDataDeclarationRef deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode node = jsonParser.readValueAsTree();
            ACAQDataDeclarationRef result = new ACAQDataDeclarationRef();
            if (!node.isNull()) {
                result.setDeclaration(ACAQDataDeclaration.getInstance(node.textValue()));
            }
            return result;
        }
    }
}
