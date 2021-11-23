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

package org.hkijena.jipipe.api.compat;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidatable;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Settings class used for a single algorithm run
 */
@JsonSerialize(using = SingleImageJAlgorithmRun.Serializer.class)
public class SingleImageJAlgorithmRun implements JIPipeValidatable {

    private EventBus eventBus = new EventBus();
    private JIPipeGraphNode algorithm;
    private Map<String, ImageJDatatypeImporter> inputSlotImporters = new HashMap<>();

    /**
     * @param algorithm the algorithm to be run
     */
    public SingleImageJAlgorithmRun(JIPipeGraphNode algorithm) {
        this.algorithm = algorithm;
        updateSlots();
        algorithm.getEventBus().register(this);
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        if (algorithm == null) {
            report.reportIsInvalid("No algorithm was provided!",
                    "This is an programming error.",
                    "Please contact the JIPipe author.",
                    this);
        }
    }

    public JIPipeGraphNode getAlgorithm() {
        return algorithm;
    }

    public Map<String, ImageJDatatypeImporter> getInputSlotImporters() {
        return Collections.unmodifiableMap(inputSlotImporters);
    }

    private void updateSlots() {
        for (JIPipeDataSlot inputSlot : algorithm.getInputSlots()) {
            if (!inputSlotImporters.containsKey(inputSlot.getName())) {
                inputSlotImporters.put(inputSlot.getName(), new ImageJDatatypeImporter(
                        JIPipe.getImageJAdapters().getAdapterForJIPipeData(inputSlot.getAcceptedDataType())));
            }
        }
        for (Map.Entry<String, ImageJDatatypeImporter> entry : ImmutableList.copyOf(inputSlotImporters.entrySet())) {
            JIPipeDataSlot slot = algorithm.getInputSlotMap().getOrDefault(entry.getKey(), null);
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
            JIPipeDataSlot slot = algorithm.getInputSlot(entry.getKey());
            slot.clearData();
            slot.addData(entry.getValue().get(), new JIPipeProgressInfo());
        }
    }

    /**
     * Extracts algorithm output into ImageJ.
     * This will run convertMultipleJIPipeToImageJ to allow output condensation
     */
    public void pullOutput() {
        for (JIPipeDataSlot outputSlot : algorithm.getOutputSlots()) {
            ImageJDatatypeAdapter adapter = JIPipe.getImageJAdapters().getAdapterForJIPipeData(outputSlot.getAcceptedDataType());
            List<JIPipeData> jipipeData = new ArrayList<>();
            for (int i = 0; i < outputSlot.getRowCount(); ++i) {
                JIPipeData data = outputSlot.getData(i, JIPipeData.class, new JIPipeProgressInfo());
                jipipeData.add(data);
            }
            adapter.convertMultipleJIPipeToImageJ(jipipeData, true, false, outputSlot.getName());
        }
    }

    /**
     * Triggered when the algorithm's slots are changed
     *
     * @param event generated event
     */
    @Subscribe
    public void onAlgorithmSlotsChanged(JIPipeGraph.NodeSlotsChangedEvent event) {
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
            Map<String, JIPipeParameterAccess> parameters = JIPipeParameterTree.getParameters(algorithm);
            for (Map.Entry<String, JsonNode> entry : ImmutableList.copyOf(jsonNode.get("parameters").fields())) {
                JIPipeParameterAccess access = parameters.getOrDefault(entry.getKey(), null);
                try {
                    access.set(JsonUtils.getObjectMapper().readerFor(access.getFieldClass()).readValue(entry.getValue()));
                } catch (IOException e) {
                    throw new UserFriendlyRuntimeException(e, "Unable to load parameters!", "JIPipe single-algorithm run", "The JSON data is invalid or incomplete.",
                            "Use the GUI to create a valid parameter set.");
                }
            }
        }
        if (jsonNode.has("add-input")) {
            JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) algorithm.getSlotConfiguration();
            for (Map.Entry<String, JsonNode> entry : ImmutableList.copyOf(jsonNode.get("add-input").fields())) {
                JIPipeDataInfo info = JIPipeDataInfo.getInstance(entry.getValue().textValue());
                slotConfiguration.addSlot(entry.getKey(), new JIPipeDataSlotInfo(info.getDataClass(),
                        JIPipeSlotType.Input,
                        entry.getKey(),
                        null), false);
            }
        }
        if (jsonNode.has("add-output")) {
            JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) algorithm.getSlotConfiguration();
            for (Map.Entry<String, JsonNode> entry : ImmutableList.copyOf(jsonNode.get("add-output").fields())) {
                JIPipeDataInfo info = JIPipeDataInfo.getInstance(entry.getValue().textValue());
                slotConfiguration.addSlot(entry.getKey(), new JIPipeDataSlotInfo(info.getDataClass(),
                        JIPipeSlotType.Output,
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
     * Serializes the run
     */
    public static class Serializer extends JsonSerializer<SingleImageJAlgorithmRun> {
        @Override
        public void serialize(SingleImageJAlgorithmRun run, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            // Instantiate an unchanged algorithm to reduce the output
            JIPipeGraphNode comparison = run.getAlgorithm().getInfo().newInstance();
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

        private void serializeOutputSlotConfiguration(SingleImageJAlgorithmRun run, JIPipeGraphNode comparison, JsonGenerator jsonGenerator) throws IOException {
            Map<String, String> serializedSlots = new HashMap<>();
            for (JIPipeDataSlot outputSlot : run.getAlgorithm().getOutputSlots()) {
                JIPipeDataSlot existingSlot = comparison.getOutputSlotMap().getOrDefault(outputSlot.getName(), null);
                if (existingSlot != null && existingSlot.isOutput())
                    continue;
                serializedSlots.put(outputSlot.getName(), JIPipe.getDataTypes().getIdOf(outputSlot.getAcceptedDataType()));
            }
            if (!serializedSlots.isEmpty()) {
                jsonGenerator.writeObjectField("add-output", serializedSlots);
            }
        }

        private void serializeInputSlotConfiguration(SingleImageJAlgorithmRun run, JIPipeGraphNode comparison, JsonGenerator jsonGenerator) throws IOException {
            Map<String, String> serializedSlots = new HashMap<>();
            for (JIPipeDataSlot inputSlot : run.getAlgorithm().getInputSlots()) {
                JIPipeDataSlot existingSlot = comparison.getInputSlotMap().getOrDefault(inputSlot.getName(), null);
                if (existingSlot != null && existingSlot.isInput())
                    continue;
                serializedSlots.put(inputSlot.getName(), JIPipe.getDataTypes().getIdOf(inputSlot.getAcceptedDataType()));
            }
            if (!serializedSlots.isEmpty()) {
                jsonGenerator.writeObjectField("add-input", serializedSlots);
            }
        }

        private void serializeParameters(SingleImageJAlgorithmRun run, JIPipeParameterCollection comparison, JsonGenerator jsonGenerator) throws IOException {

            Map<String, JIPipeParameterAccess> comparisonParameters = JIPipeParameterTree.getParameters(comparison);
            Map<String, Object> serializedParameters = new HashMap<>();

            for (Map.Entry<String, JIPipeParameterAccess> entry : JIPipeParameterTree.getParameters(run.getAlgorithm()).entrySet()) {
                JIPipeParameterAccess originalAccess = comparisonParameters.getOrDefault(entry.getKey(), null);
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
