package org.hkijena.acaq5.api;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.events.CompartmentAddedEvent;
import org.hkijena.acaq5.api.events.CompartmentRemovedEvent;
import org.hkijena.acaq5.api.events.CompartmentRenamedEvent;
import org.hkijena.acaq5.utils.JsonUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An ACAQ5 project.
 * It contains all information to setup and run an analysis
 */
@JsonSerialize(using = ACAQProject.Serializer.class)
@JsonDeserialize(using = ACAQProject.Deserializer.class)
public class ACAQProject implements ACAQValidatable {
    private EventBus eventBus = new EventBus();

    private ACAQAlgorithmGraph graph = new ACAQAlgorithmGraph();
    private BiMap<String, ACAQProjectCompartment> compartments = HashBiMap.create();
    private List<String> compartmentOrder = new ArrayList<>();

    public ACAQProject() {
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public ACAQAlgorithmGraph getGraph() {
        return graph;
    }

    public void saveProject(Path fileName) throws IOException {
        ObjectMapper mapper = JsonUtils.getObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(fileName.toFile(), this);
    }

    public static ACAQProject loadProject(Path fileName) throws IOException {
        return JsonUtils.getObjectMapper().readerFor(ACAQProject.class).readValue(fileName.toFile());
    }

    public BiMap<String, ACAQProjectCompartment> getCompartments() {
        return ImmutableBiMap.copyOf(compartments);
    }

    public ACAQProjectCompartment addCompartmentAfter(String name, String parent) {
        if(compartments.containsKey(name)) {
            return compartments.get(name);
        }
        else {
            ACAQProjectCompartment sample = new ACAQProjectCompartment(this, name);
            compartments.put(name, sample);
            if(parent == null)
                compartmentOrder.add(name);
            else {
                int parentIndex = compartmentOrder.indexOf(parent);
                if(parentIndex == compartmentOrder.size() - 1)
                    compartmentOrder.add(name);
                else
                    compartmentOrder.add(parentIndex + 1, name);
            }
            eventBus.post(new CompartmentAddedEvent(sample));
            return sample;
        }
    }

    public boolean removeCompartment(ACAQProjectCompartment compartment) {
        String name = compartment.getName();
        if(compartments.containsKey(name)) {
            compartments.remove(name);
            compartmentOrder.remove(name);
            eventBus.post(new CompartmentRemovedEvent(compartment));
            return true;
        }
        return false;
    }

    public boolean renameCompartment(ACAQProjectCompartment sample, String name) {
        return false;
//        if(name == null)
//            return false;
//        name = name.trim();
//        if(name.isEmpty() || samples.containsKey(name))
//            return false;
//        samples.remove(sample.getName());
//        samples.put(name, sample);
//        eventBus.post(new SampleRenamedEvent(sample));
//        return true;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        graph.reportValidity(report.forCategory("Analysis"));
    }

    public List<String> getCompartmentOrder() {
        return Collections.unmodifiableList(compartmentOrder);
    }

    public void addCompartment(String compartmentName) {
        addCompartmentAfter(compartmentName, null);
    }

    public static class Serializer extends JsonSerializer<ACAQProject> {
        @Override
        public void serialize(ACAQProject project, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("acaq:project-type", "project");
            jsonGenerator.writeObjectField("algorithm-graph", project.graph);
            jsonGenerator.writeEndObject();
        }
    }

    public static class Deserializer extends JsonDeserializer<ACAQProject> {

        @Override
        public ACAQProject deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            ACAQProject project = new ACAQProject();

//            JsonNode node = jsonParser.getCodec().readTree(jsonParser);
//
//            // First load the slot configuration
//            // This is required (!) to fill the preprocessing slots!
//            {
//                JsonNode slotConfigNode = node.path("algorithm").path("preprocessing-output-slots");
//                if(!slotConfigNode.isMissingNode()) {
//                    project.preprocessingOutputConfiguration.fromJson(slotConfigNode);
//                }
//            }
//
//            if(node.has("samples"))
//                readSamples(project, node.get("samples"));
//
//            if(node.has("algorithm"))
//                readAlgorithm(project, node.get("algorithm"));

            return project;
        }
    }
}
