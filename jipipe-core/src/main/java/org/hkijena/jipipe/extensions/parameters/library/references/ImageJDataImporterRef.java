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
import org.hkijena.jipipe.api.compat.ImageJDataExporter;
import org.hkijena.jipipe.api.compat.ImageJDataImporter;

import java.io.IOException;
import java.util.Objects;

/**
 * Helper to allow easy serialization of {@link org.hkijena.jipipe.api.compat.ImageJDataImporter} references
 */
@JsonSerialize(using = ImageJDataImporterRef.Serializer.class)
@JsonDeserialize(using = ImageJDataImporterRef.Deserializer.class)
public class ImageJDataImporterRef implements JIPipeValidatable {

    private String id;

    public ImageJDataImporterRef(String id) {
        this.id = id;
    }

    /**
     * New instance
     */
    public ImageJDataImporterRef() {

    }

    public ImageJDataImporterRef(ImageJDataImporterRef other) {
        this.id = other.id;
    }

    public ImageJDataImporterRef(ImageJDataImporter importer) {
        this(JIPipe.getImageJAdapters().getIdOf(importer));
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        if (id == null)
            report.reportIsInvalid("No operation is selected!",
                    "You have to select an operation.",
                    "Please select an operation.",
                    this);
    }

    @Override
    public String toString() {
        if (id != null)
            return id;
        else
            return "<Null>";
    }

    public ImageJDataImporter getInstance() {
        return JIPipe.getImageJAdapters().getImporterById(getId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImageJDataImporterRef that = (ImageJDataImporterRef) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Serializes the reference as ID
     */
    public static class Serializer extends JsonSerializer<ImageJDataImporterRef> {

        @Override
        public void serialize(ImageJDataImporterRef ref, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeString(ref.getId());
        }

    }

    /**
     * Deserializes the reference from a string
     */
    public static class Deserializer extends JsonDeserializer<ImageJDataImporterRef> {

        @Override
        public ImageJDataImporterRef deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode node = jsonParser.readValueAsTree();
            ImageJDataImporterRef result = new ImageJDataImporterRef();
            if (!node.isNull()) {
                result.setId(node.textValue());
            }
            return result;
        }
    }
}
