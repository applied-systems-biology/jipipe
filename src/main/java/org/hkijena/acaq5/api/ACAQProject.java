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
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.events.ACAQSampleAddedEvent;
import org.hkijena.acaq5.api.events.ACAQSampleRemovedEvent;
import org.hkijena.acaq5.api.events.ACAQSampleRenamedEvent;
import org.hkijena.acaq5.utils.JsonUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * An ACAQ5 project.
 * It contains all information to setup and run an analysis
 */
@JsonSerialize(using = ACAQProject.Serializer.class)
@JsonDeserialize(using = ACAQProject.Deserializer.class)
public class ACAQProject {
    private EventBus eventBus = new EventBus();
    private BiMap<String, ACAQProjectSample> samples = HashBiMap.create();
    private ACAQMutableSlotConfiguration preprocessingOutputConfiguration = ACAQMutableSlotConfiguration.builder().withoutOutput().build();
    private ACAQMutableSlotConfiguration analysisOutputConfiguration = ACAQMutableSlotConfiguration.builder().withoutOutput().build();
    private ACAQAlgorithmGraph analysis = new ACAQAlgorithmGraph();

    public ACAQProject() {
        analysis.insertNode(new ACAQPreprocessingOutput(new ACAQInputAsOutputSlotConfiguration(preprocessingOutputConfiguration)));
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public ACAQAlgorithmGraph getAnalysis() {
        return analysis;
    }

    public ACAQMutableSlotConfiguration getPreprocessingOutputConfiguration() {
        return preprocessingOutputConfiguration;
    }

    public ACAQMutableSlotConfiguration getAnalysisOutputConfiguration() {
        return analysisOutputConfiguration;
    }

    public BiMap<String, ACAQProjectSample> getSamples() {
        return ImmutableBiMap.copyOf(samples);
    }

    public ACAQProjectSample addSample(String sampleName) {
        if(samples.containsKey(sampleName)) {
            return samples.get(sampleName);
        }
        else {
            ACAQProjectSample sample = new ACAQProjectSample(this);
            samples.put(sampleName, sample);
            eventBus.post(new ACAQSampleAddedEvent(sample));
            return sample;
        }
    }

    public boolean removeSample(ACAQProjectSample sample) {
        String name = sample.getName();
        if(samples.containsKey(name)) {
            samples.remove(name);
            eventBus.post(new ACAQSampleRemovedEvent(sample));
            return true;
        }
        return false;
    }

    public boolean renameSample(ACAQProjectSample sample, String name) {
        if(name == null)
            return false;
        name = name.trim();
        if(name.isEmpty() || samples.containsKey(name))
            return false;
        samples.remove(sample.getName());
        samples.put(name, sample);
        eventBus.post(new ACAQSampleRenamedEvent(sample));
        return true;
    }

    public void saveProject(Path fileName) throws IOException {
        ObjectMapper mapper = JsonUtils.getObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(fileName.toFile(), this);
    }

    public static ACAQProject loadProject(Path fileName) throws IOException {
        return JsonUtils.getObjectMapper().readerFor(ACAQProject.class).readValue(fileName.toFile());
    }

    public static class Serializer extends JsonSerializer<ACAQProject> {
        @Override
        public void serialize(ACAQProject project, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            serializeSamples(project, jsonGenerator);
            serializeAlgorithm(project, jsonGenerator);
            jsonGenerator.writeEndObject();
        }

        private void serializeAlgorithm(ACAQProject project, JsonGenerator jsonGenerator) throws IOException {
            jsonGenerator.writeFieldName("algorithm");
            jsonGenerator.writeStartObject();

            jsonGenerator.writeObjectField("preprocessing-output-slots", project.preprocessingOutputConfiguration);
            jsonGenerator.writeObjectField("algorithm-graph", project.analysis);

            jsonGenerator.writeEndObject();
        }

        private void serializeSamples(ACAQProject project, JsonGenerator jsonGenerator) throws IOException {
            jsonGenerator.writeFieldName("samples");
            jsonGenerator.writeStartObject();
            for(Map.Entry<String, ACAQProjectSample> kv : project.getSamples().entrySet()) {
                jsonGenerator.writeObjectField(kv.getKey(), kv.getValue());
            }
            jsonGenerator.writeEndObject();
        }
    }

    public static class Deserializer extends JsonDeserializer<ACAQProject> {

        @Override
        public ACAQProject deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            ACAQProject project = new ACAQProject();

            JsonNode node = jsonParser.getCodec().readTree(jsonParser);

            // First load the slot configuration
            // This is required (!) to fill the preprocessing slots!
            {
                JsonNode slotConfigNode = node.path("algorithm").path("preprocessing-output-slots");
                if(!slotConfigNode.isMissingNode()) {
                    project.preprocessingOutputConfiguration.fromJson(slotConfigNode);
                }
            }

            if(node.has("samples"))
                readSamples(project, node.get("samples"));

            if(node.has("algorithm"))
                readAlgorithm(project, node.get("algorithm"));

            return project;
        }

        private void readAlgorithm(ACAQProject project, JsonNode node) {
            if(node.has("algorithm-graph")) {
                project.analysis.fromJson(node.get("algorithm-graph"));
            }
        }

        private void readSamples(ACAQProject project, JsonNode node) {
            for(Map.Entry<String, JsonNode> kv : ImmutableList.copyOf(node.fields())) {
                ACAQProjectSample sample = project.addSample(kv.getKey());
                sample.fromJson(kv.getValue());
            }
        }
    }
}
