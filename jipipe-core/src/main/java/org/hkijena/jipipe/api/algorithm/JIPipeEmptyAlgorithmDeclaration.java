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

package org.hkijena.jipipe.api.algorithm;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.jipipe.JIPipeDependency;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * An empty algorithm declaration.
 * Use this when you initialize an {@link JIPipeGraphNode} manually within another algorithm.
 * Warning: May break the algorithm.
 */
@JsonSerialize(using = JIPipeEmptyAlgorithmDeclaration.Serializer.class)
public class JIPipeEmptyAlgorithmDeclaration implements JIPipeAlgorithmDeclaration {
    @Override
    public String getId() {
        return "jipipe:empty";
    }

    @Override
    public Class<? extends JIPipeGraphNode> getAlgorithmClass() {
        return null;
    }

    @Override
    public JIPipeGraphNode newInstance() {
        return null;
    }

    @Override
    public JIPipeGraphNode clone(JIPipeGraphNode algorithm) {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getMenuPath() {
        return "";
    }

    @Override
    public JIPipeAlgorithmCategory getCategory() {
        return JIPipeAlgorithmCategory.Internal;
    }

    @Override
    public List<JIPipeInputSlot> getInputSlots() {
        return Collections.emptyList();
    }

    @Override
    public List<JIPipeOutputSlot> getOutputSlots() {
        return Collections.emptyList();
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Collections.emptySet();
    }

    @Override
    public boolean isHidden() {
        return true;
    }

    /**
     * Serializes the empty algorithm declaration
     */
    public static class Serializer extends JsonSerializer<JIPipeEmptyAlgorithmDeclaration> {
        @Override
        public void serialize(JIPipeEmptyAlgorithmDeclaration jipipeEmptyAlgorithmDeclaration, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeEndObject();
        }
    }
}
