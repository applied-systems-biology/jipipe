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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidatable;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.utils.ParameterUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages a single algorithm run
 */
public class SingleImageJAlgorithmRunConfiguration implements JIPipeValidatable, JIPipeGraphNode.NodeSlotsChangedEventListener {
    private final JIPipeGraphNode algorithm;
    private final Map<String, ImageJDataImportOperation> inputSlotImporters = new HashMap<>();
    private final Map<String, ImageJDataExportOperation> outputSlotExporters = new HashMap<>();
    private int numThreads = 1;

    private final JIPipeGraphNode.NodeSlotsChangedEventEmitter nodeSlotsChangedEventEmitter = new JIPipeGraphNode.NodeSlotsChangedEventEmitter();

    public SingleImageJAlgorithmRunConfiguration(String nodeId, String parameters, String inputs, String outputs, int threads) {
        this.algorithm = JIPipe.createNode(nodeId);
        this.numThreads = threads;

        // Read parameters
        importParameterString(parameters);
        importInputString(inputs);
        importOutputString(outputs);
    }

    /**
     * @param algorithm the algorithm to be run
     */
    public SingleImageJAlgorithmRunConfiguration(JIPipeGraphNode algorithm) {
        this.algorithm = algorithm;
        updateSlots();
        algorithm.getNodeSlotsChangedEventEmitter().subscribeWeak(this);
    }

    private void importInputString(String inputString) {
        JsonNode jsonNode = JsonUtils.readFromString(inputString, JsonNode.class);
        JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) algorithm.getSlotConfiguration();

        // Create/remove slots
        if (slotConfiguration.canModifyInputSlots()) {
            for (String slotName : Sets.symmetricDifference(ImmutableSet.copyOf(jsonNode.fieldNames()), algorithm.getInputSlotMap().keySet()).immutableCopy()) {
                if (slotConfiguration.hasInputSlot(slotName)) {
                    // Need to remove it
                    slotConfiguration.removeInputSlot(slotName, true);
                } else {
                    // Need to add it
                    JsonNode slotInfoNode = jsonNode.get(slotName);
                    String dataType = slotInfoNode.get("data-type").textValue();
                    Class<? extends JIPipeData> dataClass = JIPipe.getDataTypes().getById(dataType);
                    slotConfiguration.addSlot(new JIPipeDataSlotInfo(dataClass, JIPipeSlotType.Input, slotName, ""), true);
                }
            }
        }

        // Set importer
        for (Map.Entry<String, JsonNode> entry : ImmutableList.copyOf(jsonNode.fields())) {
            if (slotConfiguration.hasInputSlot(entry.getKey())) {
                // Parse
                try {
                    ImageJDataImportOperation operation = JsonUtils.getObjectMapper().readerFor(ImageJDataImportOperation.class).readValue(entry.getValue());
                    inputSlotImporters.put(entry.getKey(), operation);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void importOutputString(String outputString) {
        JsonNode jsonNode = JsonUtils.readFromString(outputString, JsonNode.class);
        JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) algorithm.getSlotConfiguration();

        // Create/remove slots
        if (slotConfiguration.canAddOutputSlot()) {
            for (String slotName : Sets.symmetricDifference(ImmutableSet.copyOf(jsonNode.fieldNames()), algorithm.getOutputSlotMap().keySet()).immutableCopy()) {
                if (slotConfiguration.hasOutputSlot(slotName)) {
                    // Need to remove it
                    slotConfiguration.removeOutputSlot(slotName, true);
                } else {
                    // Need to add it
                    JsonNode slotInfoNode = jsonNode.get(slotName);
                    String dataType = slotInfoNode.get("data-type").textValue();
                    Class<? extends JIPipeData> dataClass = JIPipe.getDataTypes().getById(dataType);
                    slotConfiguration.addSlot(new JIPipeDataSlotInfo(dataClass, JIPipeSlotType.Output, slotName, ""), true);
                }
            }
        }

        // Set importer
        for (Map.Entry<String, JsonNode> entry : ImmutableList.copyOf(jsonNode.fields())) {
            if (slotConfiguration.hasOutputSlot(entry.getKey())) {
                // Parse
                try {
                    ImageJDataExportOperation operation = JsonUtils.getObjectMapper().readerFor(ImageJDataExportOperation.class).readValue(entry.getValue());
                    outputSlotExporters.put(entry.getKey(), operation);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public JIPipeGraphNode.NodeSlotsChangedEventEmitter getNodeSlotsChangedEventEmitter() {
        return nodeSlotsChangedEventEmitter;
    }

    private void importParameterString(String parametersString) {
        JsonNode jsonNode = JsonUtils.readFromString(parametersString, JsonNode.class);
        ParameterUtils.deserializeParametersFromJson(algorithm, jsonNode, new JIPipeIssueReport());
    }

    public int getNumThreads() {
        return numThreads;
    }

    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
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

    public Map<String, ImageJDataImportOperation> getInputSlotImporters() {
        return Collections.unmodifiableMap(inputSlotImporters);
    }

    public Map<String, ImageJDataExportOperation> getOutputSlotExporters() {
        return Collections.unmodifiableMap(outputSlotExporters);
    }

    private void updateSlots() {
        for (String slotName : Sets.symmetricDifference(algorithm.getInputSlotMap().keySet(), inputSlotImporters.keySet()).immutableCopy()) {
            if (algorithm.getInputSlotMap().containsKey(slotName)) {
                // Need to add
                ImageJDataImportOperation operation = new ImageJDataImportOperation(
                        JIPipe.getImageJAdapters().getDefaultImporterFor(algorithm.getInputSlot(slotName).getAcceptedDataType()));
                inputSlotImporters.put(slotName, operation);
            } else {
                // Need to remove
                inputSlotImporters.remove(slotName);
            }
        }
        for (String slotName : Sets.symmetricDifference(algorithm.getOutputSlotMap().keySet(), outputSlotExporters.keySet()).immutableCopy()) {
            if (algorithm.getOutputSlotMap().containsKey(slotName)) {
                // Need to add
                ImageJDataExportOperation operation = new ImageJDataExportOperation(
                        JIPipe.getImageJAdapters().getDefaultExporterFor(algorithm.getOutputSlot(slotName).getAcceptedDataType()));
                operation.setName(slotName);
                operation.setActivate(true);
                outputSlotExporters.put(slotName, operation);
            } else {
                // Need to remove
                outputSlotExporters.remove(slotName);
            }
        }
    }

    /**
     * Pushes selected ImageJ data into the algorithm input slots
     *
     * @param progressInfo the progress info
     */
    public void importInputsFromImageJ(JIPipeProgressInfo progressInfo) {
        for (Map.Entry<String, ImageJDataImportOperation> entry : inputSlotImporters.entrySet()) {
            JIPipeDataSlot slot = algorithm.getInputSlot(entry.getKey());
            slot.clearData();
            JIPipeDataTable dataTable = entry.getValue().apply(null, progressInfo.resolve(entry.getKey()));
            for (int row = 0; row < dataTable.getRowCount(); row++) {
                slot.addData(dataTable.getDataItemStore(row).duplicate(progressInfo),
                        dataTable.getTextAnnotations(row),
                        JIPipeTextAnnotationMergeMode.OverwriteExisting,
                        dataTable.getDataAnnotations(row),
                        JIPipeDataAnnotationMergeMode.OverwriteExisting);
            }
        }
    }

    /**
     * Extracts algorithm output into ImageJ.
     *
     * @param progressInfo the progress info
     */
    public void exportOutputToImageJ(JIPipeProgressInfo progressInfo) {
        for (JIPipeDataSlot outputSlot : algorithm.getOutputSlots()) {
            ImageJDataExportOperation exportOperation = outputSlotExporters.get(outputSlot.getName());
            exportOperation.apply(outputSlot, progressInfo.resolve(outputSlot.getName()));
        }
    }

    public String getParametersString() {
        return JsonUtils.toJsonString(algorithm);
    }

    public String getInputsString() {
        return JsonUtils.toJsonString(inputSlotImporters);
    }

    public String getOutputsString() {
        return JsonUtils.toJsonString(outputSlotExporters);
    }

    @Override
    public void onNodeSlotsChanged(JIPipeGraphNode.NodeSlotsChangedEvent event) {
        updateSlots();
        nodeSlotsChangedEventEmitter.emit(event);
    }
}
