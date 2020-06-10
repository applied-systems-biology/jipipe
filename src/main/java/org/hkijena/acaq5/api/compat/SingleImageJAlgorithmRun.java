package org.hkijena.acaq5.api.compat;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.data.*;
import org.hkijena.acaq5.api.events.AlgorithmSlotsChangedEvent;
import org.hkijena.acaq5.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQTraversedParameterCollection;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.api.registries.ACAQImageJAdapterRegistry;
import org.hkijena.acaq5.utils.JsonUtils;

import java.io.IOException;
import java.util.*;

/**
 * Settings class used for a single algorithm run
 */
@JsonSerialize(using = SingleImageJAlgorithmRun.Serializer.class)
public class SingleImageJAlgorithmRun implements ACAQValidatable {

    private EventBus eventBus = new EventBus();
    private ACAQGraphNode algorithm;
    private Map<String, ImageJDatatypeImporter> inputSlotImporters = new HashMap<>();

    /**
     * @param algorithm the algorithm to be run
     */
    public SingleImageJAlgorithmRun(ACAQGraphNode algorithm) {
        this.algorithm = algorithm;
        updateSlots();
        algorithm.getEventBus().register(this);
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if (algorithm == null) {
            report.reportIsInvalid("No algorithm was provided!",
                    "This is an programming error.",
                    "Please contact the ACAQ5 author.",
                    this);
        }
    }

    public ACAQGraphNode getAlgorithm() {
        return algorithm;
    }

    public Map<String, ImageJDatatypeImporter> getInputSlotImporters() {
        return Collections.unmodifiableMap(inputSlotImporters);
    }

    private void updateSlots() {
        for (ACAQDataSlot inputSlot : algorithm.getInputSlots()) {
            if (!inputSlotImporters.containsKey(inputSlot.getName())) {
                inputSlotImporters.put(inputSlot.getName(), new ImageJDatatypeImporter(
                        ACAQImageJAdapterRegistry.getInstance().getAdapterForACAQData(inputSlot.getAcceptedDataType())));
            }
        }
        for (Map.Entry<String, ImageJDatatypeImporter> entry : ImmutableList.copyOf(inputSlotImporters.entrySet())) {
            ACAQDataSlot slot = algorithm.getSlots().getOrDefault(entry.getKey(), null);
            if (slot == null || !slot.isInput()) {
                inputSlotImporters.remove(entry.getKey());
            }
        }
    }

    /**
     * Pushes selected ImageJ data into the algorithm input slots
     */
    public void pushInput() {
        for (Map.Entry<String, ImageJDatatypeImporter> entry : inputSlotImporters.entrySet()) {
            ACAQDataSlot slot = algorithm.getInputSlot(entry.getKey());
            slot.clearData();
            slot.addData(entry.getValue().get());
        }
    }

    /**
     * Extracts algorithm output into ImageJ.
     * This will run convertMultipleACAQToImageJ to allow output condensation
     */
    public void pullOutput() {
        for (ACAQDataSlot outputSlot : algorithm.getOutputSlots()) {
            ImageJDatatypeAdapter adapter = ACAQImageJAdapterRegistry.getInstance().getAdapterForACAQData(outputSlot.getAcceptedDataType());
            List<ACAQData> acaqData = new ArrayList<>();
            for (int i = 0; i < outputSlot.getRowCount(); ++i) {
                ACAQData data = outputSlot.getData(i, ACAQData.class);
                acaqData.add(data);
            }
            adapter.convertMultipleACAQToImageJ(acaqData, true, false, outputSlot.getName());
        }
    }

    /**
     * Triggered when the algorithm's slots are changed
     *
     * @param event generated event
     */
    @Subscribe
    public void onAlgorithmSlotsChanged(AlgorithmSlotsChangedEvent event) {
        updateSlots();
        eventBus.post(event);
    }

    /**
     * Loads data from JSON
     *
     * @param jsonNode JSON data
     */
    public void fromJson(JsonNode jsonNode) {
        if (jsonNode.has("parameters")) {
            Map<String, ACAQParameterAccess> parameters = ACAQTraversedParameterCollection.getParameters(algorithm);
            for (Map.Entry<String, JsonNode> entry : ImmutableList.copyOf(jsonNode.get("parameters").fields())) {
                ACAQParameterAccess access = parameters.getOrDefault(entry.getKey(), null);
                try {
                    access.set(JsonUtils.getObjectMapper().readerFor(access.getFieldClass()).readValue(entry.getValue()));
                } catch (IOException e) {
                    throw new UserFriendlyRuntimeException(e, "Unable to load parameters!", "ACAQ single-algorithm run", "The JSON data is invalid or incomplete.",
                            "Use the GUI to create a valid parameter set.");
                }
            }
        }
        if (jsonNode.has("add-input")) {
            ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration) algorithm.getSlotConfiguration();
            for (Map.Entry<String, JsonNode> entry : ImmutableList.copyOf(jsonNode.get("add-input").fields())) {
                ACAQDataDeclaration declaration = ACAQDataDeclaration.getInstance(entry.getValue().textValue());
                slotConfiguration.addSlot(entry.getKey(), new ACAQSlotDefinition(declaration.getDataClass(),
                        ACAQDataSlot.SlotType.Input,
                        entry.getKey(),
                        null), false);
            }
        }
        if (jsonNode.has("add-output")) {
            ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration) algorithm.getSlotConfiguration();
            for (Map.Entry<String, JsonNode> entry : ImmutableList.copyOf(jsonNode.get("add-output").fields())) {
                ACAQDataDeclaration declaration = ACAQDataDeclaration.getInstance(entry.getValue().textValue());
                slotConfiguration.addSlot(entry.getKey(), new ACAQSlotDefinition(declaration.getDataClass(),
                        ACAQDataSlot.SlotType.Output,
                        entry.getKey(),
                        null), false);
            }
        }
        if (jsonNode.has("input")) {
            for (Map.Entry<String, JsonNode> entry : ImmutableList.copyOf(jsonNode.get("input").fields())) {
                ImageJDatatypeImporter importer = inputSlotImporters.get(entry.getKey());
                importer.setParameters(entry.getValue().textValue());
            }
        }
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Returns true if an algorithm can be run in a single ImageJ algorithm run
     *
     * @param declaration the algorithm type
     * @return if the algorithm is compatible
     */
    public static boolean isCompatible(ACAQAlgorithmDeclaration declaration) {
        switch (declaration.getCategory()) {
            case Internal:
            case Annotation:
                return false;
        }
        ACAQGraphNode algorithm = declaration.newInstance();
        for (ACAQDataSlot inputSlot : algorithm.getInputSlots()) {
            if (!ACAQImageJAdapterRegistry.getInstance().supportsACAQData(inputSlot.getAcceptedDataType()))
                return false;
        }
        for (ACAQDataSlot outputSlot : algorithm.getOutputSlots()) {
            if (!ACAQImageJAdapterRegistry.getInstance().supportsACAQData(outputSlot.getAcceptedDataType()))
                return false;
        }

        return true;
    }

    /**
     * Serializes the run
     */
    public static class Serializer extends JsonSerializer<SingleImageJAlgorithmRun> {
        @Override
        public void serialize(SingleImageJAlgorithmRun run, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            // Instantiate an unchanged algorithm to reduce the output
            ACAQGraphNode comparison = run.getAlgorithm().getDeclaration().newInstance();
            jsonGenerator.writeStartObject();

            serializeParameters(run, comparison, jsonGenerator);
            serializeInputSlotConfiguration(run, comparison, jsonGenerator);
            serializeInputSlotData(run, jsonGenerator);
            serializeOutputSlotConfiguration(run, comparison, jsonGenerator);

            jsonGenerator.writeEndObject();
        }

        private void serializeInputSlotData(SingleImageJAlgorithmRun run, JsonGenerator jsonGenerator) throws IOException {
            Map<String, String> slotData = new HashMap<>();
            for (Map.Entry<String, ImageJDatatypeImporter> entry : run.inputSlotImporters.entrySet()) {
                if (entry.getValue().getParameters() != null) {
                    slotData.put(entry.getKey(), entry.getValue().getParameters());
                }
            }
            if (!slotData.isEmpty()) {
                jsonGenerator.writeObjectField("input", slotData);
            }
        }

        private void serializeOutputSlotConfiguration(SingleImageJAlgorithmRun run, ACAQGraphNode comparison, JsonGenerator jsonGenerator) throws IOException {
            Map<String, String> serializedSlots = new HashMap<>();
            for (ACAQDataSlot outputSlot : run.getAlgorithm().getOutputSlots()) {
                ACAQDataSlot existingSlot = comparison.getSlots().getOrDefault(outputSlot.getName(), null);
                if (existingSlot != null && existingSlot.isOutput())
                    continue;
                serializedSlots.put(outputSlot.getName(), ACAQDatatypeRegistry.getInstance().getIdOf(outputSlot.getAcceptedDataType()));
            }
            if (!serializedSlots.isEmpty()) {
                jsonGenerator.writeObjectField("add-output", serializedSlots);
            }
        }

        private void serializeInputSlotConfiguration(SingleImageJAlgorithmRun run, ACAQGraphNode comparison, JsonGenerator jsonGenerator) throws IOException {
            Map<String, String> serializedSlots = new HashMap<>();
            for (ACAQDataSlot inputSlot : run.getAlgorithm().getInputSlots()) {
                ACAQDataSlot existingSlot = comparison.getSlots().getOrDefault(inputSlot.getName(), null);
                if (existingSlot != null && existingSlot.isInput())
                    continue;
                serializedSlots.put(inputSlot.getName(), ACAQDatatypeRegistry.getInstance().getIdOf(inputSlot.getAcceptedDataType()));
            }
            if (!serializedSlots.isEmpty()) {
                jsonGenerator.writeObjectField("add-input", serializedSlots);
            }
        }

        private void serializeParameters(SingleImageJAlgorithmRun run, ACAQParameterCollection comparison, JsonGenerator jsonGenerator) throws IOException {

            Map<String, ACAQParameterAccess> comparisonParameters = ACAQTraversedParameterCollection.getParameters(comparison);
            Map<String, Object> serializedParameters = new HashMap<>();

            for (Map.Entry<String, ACAQParameterAccess> entry : ACAQTraversedParameterCollection.getParameters(run.getAlgorithm()).entrySet()) {
                ACAQParameterAccess originalAccess = comparisonParameters.getOrDefault(entry.getKey(), null);
                Object originalValue = originalAccess != null ? originalAccess.get(Object.class) : null;
                Object value = entry.getValue().get(Object.class);
                if (!Objects.equals(originalValue, value)) {
                    serializedParameters.put(entry.getKey(), value);
                }
            }

            if (!serializedParameters.isEmpty()) {
                jsonGenerator.writeObjectField("parameters", serializedParameters);
            }
        }
    }
}
