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
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmVisibility;
import org.hkijena.acaq5.api.compartments.ACAQAlgorithmInputDeclaration;
import org.hkijena.acaq5.api.compartments.ACAQAnalysisInputSlotConfiguration;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQAlgorithmInput;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQPreprocessingOutput;
import org.hkijena.acaq5.api.compartments.ACAQPreprocessingOutputSlotConfiguration;
import org.hkijena.acaq5.api.compartments.dataslots.ACAQPreprocessingOutputDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.events.SampleAddedEvent;
import org.hkijena.acaq5.api.events.SampleRemovedEvent;
import org.hkijena.acaq5.api.events.SampleRenamedEvent;
import org.hkijena.acaq5.api.traits.ACAQMutableTraitGenerator;
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
public class ACAQProject implements ACAQValidatable {
    private EventBus eventBus = new EventBus();
    private BiMap<String, ACAQProjectSample> samples = HashBiMap.create();

    private ACAQMutableSlotConfiguration preprocessingOutputConfiguration;
    private ACAQAnalysisInputSlotConfiguration analysisInputSlotConfiguration;
    private ACAQMutableTraitGenerator preprocessingTraitConfiguration;
    private ACAQAlgorithmInput algorithmInput;
    private ACAQAlgorithmGraph graph = new ACAQAlgorithmGraph(ACAQAlgorithmVisibility.Analysis);

    public ACAQProject() {
        preprocessingOutputConfiguration = ACAQMutableSlotConfiguration.builder()
                .addOutputSlot("Data", ACAQPreprocessingOutputDataSlot.class).sealOutput().build();
        preprocessingTraitConfiguration = new ACAQMutableTraitGenerator(preprocessingOutputConfiguration);

        analysisInputSlotConfiguration = new ACAQAnalysisInputSlotConfiguration(preprocessingOutputConfiguration);
        algorithmInput = ACAQAlgorithmInputDeclaration.createInstance(analysisInputSlotConfiguration,
                preprocessingTraitConfiguration);
        graph.insertNode(algorithmInput,
                ACAQAlgorithmGraph.COMPARTMENT_ANALYSIS);
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public ACAQAlgorithmGraph getGraph() {
        return graph;
    }

    public ACAQMutableSlotConfiguration getPreprocessingOutputConfiguration() {
        return preprocessingOutputConfiguration;
    }

    public ACAQMutableTraitGenerator getPreprocessingTraitConfiguration() {
        return preprocessingTraitConfiguration;
    }

    public BiMap<String, ACAQProjectSample> getSamples() {
        return ImmutableBiMap.copyOf(samples);
    }

    public ACAQProjectSample getOrCreate(String sampleName) {
        if(samples.containsKey(sampleName)) {
            return samples.get(sampleName);
        }
        else {
            ACAQProjectSample sample = new ACAQProjectSample(this, sampleName);
            samples.put(sampleName, sample);
            analysisInputSlotConfiguration.addInputSlot("__" + sampleName, ACAQPreprocessingOutputDataSlot.class);
            graph.connect(sample.getPreprocessingOutput().getSlots().get("Data"),
                    algorithmInput.getSlots().get("__" + sampleName));
            eventBus.post(new SampleAddedEvent(sample));
            return sample;
        }
    }

    public boolean removeSample(ACAQProjectSample sample) {
        String name = sample.getName();
        if(samples.containsKey(name)) {
            graph.removeCompartment(name);
            samples.remove(name);
            analysisInputSlotConfiguration.removeSlot("__" + name);
            eventBus.post(new SampleRemovedEvent(sample));
            return true;
        }
        return false;
    }

    public boolean renameSample(ACAQProjectSample sample, String name) {
        throw new UnsupportedOperationException();
//        if(name == null)
//            return false;
//        name = name.trim();
//        if(name.isEmpty() || samples.containsKey(name))
//            return false;
//        samples.remove(sample.getName());
//        samples.put(name, sample);
//        sample.setName(name);
//        eventBus.post(new SampleRenamedEvent(sample));
//        return true;
    }

    public void saveProject(Path fileName) throws IOException {
        ObjectMapper mapper = JsonUtils.getObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(fileName.toFile(), this);
    }

    public static ACAQProject loadProject(Path fileName) throws IOException {
        return JsonUtils.getObjectMapper().readerFor(ACAQProject.class).readValue(fileName.toFile());
    }

    public void duplicateSample(String name, String newSampleName) {
        if(samples.containsKey(newSampleName))
            return;
        ACAQProjectSample original = samples.get(name);
        ACAQProjectSample copy =  new ACAQProjectSample(original);
        samples.put(newSampleName, copy);
        eventBus.post(new SampleAddedEvent(copy));
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if(samples.isEmpty())
            report.forCategory("Data").reportIsInvalid("There are no samples! Please add at least one sample.");
        for(Map.Entry<String, ACAQProjectSample> entry : samples.entrySet()) {
            entry.getValue().reportValidity(report.forCategory("Data").forCategory(entry.getKey()));
        }
        graph.reportValidity(report.forCategory("Analysis"));
    }

    public ACAQAlgorithmInput getAlgorithmInput() {
        return algorithmInput;
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
            jsonGenerator.writeObjectField("preprocessing-output-slot-traits", project.preprocessingTraitConfiguration);
            jsonGenerator.writeObjectField("algorithm-graph", project.graph);

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
                project.graph.fromJson(node.get("algorithm-graph"));
            }
            if(node.has("preprocessing-output-slot-traits")) {
                project.preprocessingTraitConfiguration.fromJson(node.get("preprocessing-output-slot-traits"));
            }
        }

        private void readSamples(ACAQProject project, JsonNode node) {
            for(Map.Entry<String, JsonNode> kv : ImmutableList.copyOf(node.fields())) {
                ACAQProjectSample sample = project.getOrCreate(kv.getKey());
                sample.fromJson(kv.getValue());
            }
        }
    }
}
