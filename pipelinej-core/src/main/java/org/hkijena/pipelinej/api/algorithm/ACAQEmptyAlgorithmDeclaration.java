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

package org.hkijena.pipelinej.api.algorithm;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.pipelinej.ACAQDependency;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * An empty algorithm declaration.
 * Use this when you initialize an {@link ACAQGraphNode} manually within another algorithm.
 * Warning: May break the algorithm.
 */
@JsonSerialize(using = ACAQEmptyAlgorithmDeclaration.Serializer.class)
public class ACAQEmptyAlgorithmDeclaration implements ACAQAlgorithmDeclaration {
    @Override
    public String getId() {
        return "acaq:empty";
    }

    @Override
    public Class<? extends ACAQGraphNode> getAlgorithmClass() {
        return null;
    }

    @Override
    public ACAQGraphNode newInstance() {
        return null;
    }

    @Override
    public ACAQGraphNode clone(ACAQGraphNode algorithm) {
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
    public ACAQAlgorithmCategory getCategory() {
        return ACAQAlgorithmCategory.Internal;
    }

    @Override
    public List<AlgorithmInputSlot> getInputSlots() {
        return Collections.emptyList();
    }

    @Override
    public List<AlgorithmOutputSlot> getOutputSlots() {
        return Collections.emptyList();
    }

    @Override
    public Set<ACAQDependency> getDependencies() {
        return Collections.emptySet();
    }

    @Override
    public boolean isHidden() {
        return true;
    }

    /**
     * Serializes the empty algorithm declaration
     */
    public static class Serializer extends JsonSerializer<ACAQEmptyAlgorithmDeclaration> {
        @Override
        public void serialize(ACAQEmptyAlgorithmDeclaration acaqEmptyAlgorithmDeclaration, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeEndObject();
        }
    }
}
