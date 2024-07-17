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

package org.hkijena.jipipe.plugins.ijfilaments.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DGraphData;

import java.io.IOException;

public class FilamentsDataSerializer extends JsonSerializer<Filaments3DGraphData> {
    @Override
    public void serialize(Filaments3DGraphData filamentsData, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectFieldStart("vertices");
        for (FilamentVertex vertex : filamentsData.vertexSet()) {
            jsonGenerator.writeObjectField(vertex.getUuid().toString(), vertex);
        }
        jsonGenerator.writeEndObject();
        jsonGenerator.writeArrayFieldStart("edges");
        for (FilamentEdge edge : filamentsData.edgeSet()) {
            FilamentVertex edgeSource = filamentsData.getEdgeSource(edge);
            FilamentVertex edgeTarget = filamentsData.getEdgeTarget(edge);
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("source", edgeSource.getUuid().toString());
            jsonGenerator.writeStringField("target", edgeTarget.getUuid().toString());
            jsonGenerator.writeObjectField("data", edge);
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject();
    }
}
