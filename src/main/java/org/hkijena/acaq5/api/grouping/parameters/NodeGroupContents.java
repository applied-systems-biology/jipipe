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

package org.hkijena.acaq5.api.grouping.parameters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.grouping.NodeGroup;
import org.hkijena.acaq5.utils.JsonUtils;

import java.io.IOException;

@JsonSerialize(using = NodeGroupContents.Serializer.class)
@JsonDeserialize(using = NodeGroupContents.Deserializer.class)
public class NodeGroupContents {
    private NodeGroup parent;
    private ACAQAlgorithmGraph wrappedGraph;

    /**
     * Creates a new empty instance
     */
    public NodeGroupContents() {

    }

    /**
     * Makes a copy
     *
     * @param other the original
     */
    public NodeGroupContents(NodeGroupContents other) {
        if (other.wrappedGraph != null)
            this.wrappedGraph = new ACAQAlgorithmGraph(other.wrappedGraph);
    }

    public NodeGroup getParent() {
        return parent;
    }

    public void setParent(NodeGroup parent) {
        this.parent = parent;
    }

    public ACAQAlgorithmGraph getWrappedGraph() {
        return wrappedGraph;
    }

    public void setWrappedGraph(ACAQAlgorithmGraph wrappedGraph) {
        this.wrappedGraph = wrappedGraph;
    }

    public static class Serializer extends JsonSerializer<NodeGroupContents> {
        @Override
        public void serialize(NodeGroupContents nodeGroupContents, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            if (nodeGroupContents.getParent() != null)
                jsonGenerator.writeObject(nodeGroupContents.getParent().getWrappedGraph());
            else
                jsonGenerator.writeObject(nodeGroupContents.getWrappedGraph());
        }
    }

    public static class Deserializer extends JsonDeserializer<NodeGroupContents> {
        @Override
        public NodeGroupContents deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            ACAQAlgorithmGraph graph = JsonUtils.getObjectMapper().readerFor(ACAQAlgorithmGraph.class).readValue(jsonParser);
            NodeGroupContents contents = new NodeGroupContents();
            contents.wrappedGraph = graph;
            return contents;
        }
    }
}
