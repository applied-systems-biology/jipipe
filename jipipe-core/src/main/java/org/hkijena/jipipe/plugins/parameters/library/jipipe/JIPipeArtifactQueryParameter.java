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

package org.hkijena.jipipe.plugins.parameters.library.jipipe;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;

/**
 * Parameter that allows the user to select or query an artifact
 */
@JsonDeserialize(using = JIPipeArtifactQueryParameter.Deserializer.class)
public class JIPipeArtifactQueryParameter {
    private String query;

    public JIPipeArtifactQueryParameter() {
    }

    public JIPipeArtifactQueryParameter(String query) {
        this.query = query;
    }

    public JIPipeArtifactQueryParameter(JIPipeArtifactQueryParameter other) {
        this.query = other.query;
    }

    @JsonGetter("query-v2")
    public String getQuery() {
        return query;
    }

    @JsonSetter("query-v2")
    public void setQuery(String query) {
        this.query = query;
    }

    @Override
    public String toString() {
        return query;
    }

    public static class Deserializer extends JsonDeserializer<JIPipeArtifactQueryParameter> {
        @Override
        public JIPipeArtifactQueryParameter deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            JsonNode node = p.readValueAsTree();
            JIPipeArtifactQueryParameter result = new JIPipeArtifactQueryParameter();
            if(node.has("query")) {
                // Upgrade to more generic query
                String value = node.findValue("query").asText();
                if(value != null) {
                    if(value.contains(":")) {
                        String versionClassifier = value.split(":")[1];
                        if(versionClassifier.contains("-")) {
                            value = value.split(":")[0] + ":" + versionClassifier.split("-")[0] + "-*";
                        }
                    }
                    result.setQuery(value);
                }
            }
            if(node.has("query-v2")) {
                String value = node.findValue("query-v2").asText();
                result.setQuery(value);
            }
            return result;
        }
    }
}
