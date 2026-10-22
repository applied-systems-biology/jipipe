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

package org.hkijena.jipipe.plugins.parameters.library.taxonomy;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.taxonomy.FunctionRegistry;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.api.validation.JIPipeValidatable;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportSettings;

import java.io.IOException;
import java.util.Objects;

/**
 * Reference to a node-taxonomy function, stored by its stable string id.
 * <p>
 * Mirrors {@link org.hkijena.jipipe.plugins.parameters.library.references.JIPipeDataInfoRef}:
 * a plain reference wrapper that serializes to the bare id string (or {@code null} when unset),
 * so saved JSON stays human-readable. The id is only resolved lazily against the live
 * {@link FunctionRegistry}; an unknown (stale) id never throws — it simply reports
 * {@link #isResolved() is unresolved} and {@link #getFunction()} returns {@code null}.
 */
@JsonSerialize(using = JIPipeTaxonomyFunctionRef.Serializer.class)
@JsonDeserialize(using = JIPipeTaxonomyFunctionRef.Deserializer.class)
public class JIPipeTaxonomyFunctionRef implements JIPipeValidatable {

    private String id;

    /**
     * New instance (unset)
     */
    public JIPipeTaxonomyFunctionRef() {

    }

    /**
     * Initializes from a function id
     *
     * @param id function id
     */
    public JIPipeTaxonomyFunctionRef(String id) {
        this.id = id;
    }

    /**
     * Creates a copy
     *
     * @param other the other
     */
    public JIPipeTaxonomyFunctionRef(JIPipeTaxonomyFunctionRef other) {
        if (other != null) {
            this.id = other.id;
        }
    }

    /**
     * @return the function id, or {@code null} if unset
     */
    public String getId() {
        return id;
    }

    /**
     * @return whether the referenced function id is set and resolves to a
     * registered function. Never throws: an unknown id (or an unbooted JIPipe
     * instance, as in unit tests) reports {@code false}.
     */
    public boolean isResolved() {
        return id != null
                && JIPipe.getInstance() != null
                && JIPipe.getNodes().getFunctions().byId(id) != null;
    }

    /**
     * @return the resolved function, or {@code null} if unset or unresolved. Never throws.
     */
    public JIPipeNodeTaxonomyFunction getFunction() {
        if (id == null) {
            return null;
        }
        if (JIPipe.getInstance() == null) {
            return null;
        }
        return JIPipe.getNodes().getFunctions().byId(id);
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReportSettings reportSettings, JIPipeValidationReport report, JIPipeProgressInfo progressInfo) {
        // Unset (or unbooted) is valid: the taxonomy allows unclassified items.
        if (id == null) {
            return;
        }
        // Set but unresolvable (stale id): report informationally, never throw.
        if (!isResolved()) {
            reportContext.on(JIPipeValidationReportEntryLevel.Info).title("Taxonomy function is not resolved!")
                    .explanation("The stored function id '" + id + "' does not match any registered taxonomy function.")
                    .solution("Please select a valid taxonomy function again.")
                    .report(report);
        }
    }

    @Override
    public String toString() {
        if (id != null)
            return id;
        else
            return "<Null>";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JIPipeTaxonomyFunctionRef that = (JIPipeTaxonomyFunctionRef) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Serializes the reference as the plain id string (or {@code null} when unset)
     */
    public static class Serializer extends JsonSerializer<JIPipeTaxonomyFunctionRef> {

        @Override
        public void serialize(JIPipeTaxonomyFunctionRef ref, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            if (ref.id == null) {
                jsonGenerator.writeNull();
            } else {
                jsonGenerator.writeString(ref.id);
            }
        }
    }

    /**
     * Deserializes the reference from a plain id string (or {@code null} → unset)
     */
    public static class Deserializer extends JsonDeserializer<JIPipeTaxonomyFunctionRef> {

        /**
         * Called by Jackson for a JSON {@code null} (including a top-level null):
         * map it to an unset reference rather than a {@code null} instance.
         */
        @Override
        public JIPipeTaxonomyFunctionRef getNullValue(DeserializationContext deserializationContext) {
            return new JIPipeTaxonomyFunctionRef();
        }

        @Override
        public JIPipeTaxonomyFunctionRef deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            String value = jsonParser.getValueAsString();
            if (value == null || value.isEmpty()) {
                return new JIPipeTaxonomyFunctionRef();
            }
            return new JIPipeTaxonomyFunctionRef(value);
        }
    }
}
