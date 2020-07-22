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

package org.hkijena.jipipe.api.nodes;

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
 * An empty algorithm info.
 * Use this when you initialize an {@link JIPipeGraphNode} manually within another algorithm.
 * Warning: May break the algorithm.
 */
@JsonSerialize(using = JIPipeEmptyNodeInfo.Serializer.class)
public class JIPipeEmptyNodeInfo implements JIPipeNodeInfo {
    @Override
    public String getId() {
        return "jipipe:empty";
    }

    @Override
    public Class<? extends JIPipeGraphNode> getInstanceClass() {
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
    public JIPipeNodeCategory getCategory() {
        return JIPipeNodeCategory.Internal;
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
     * Serializes the empty algorithm info
     */
    public static class Serializer extends JsonSerializer<JIPipeEmptyNodeInfo> {
        @Override
        public void serialize(JIPipeEmptyNodeInfo emptyNodeInfo, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeEndObject();
        }
    }
}
