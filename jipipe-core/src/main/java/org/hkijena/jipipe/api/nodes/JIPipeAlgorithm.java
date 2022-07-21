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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.*;
import org.hkijena.jipipe.api.data.JIPipeSlotConfiguration;
import org.hkijena.jipipe.api.parameters.*;
import org.hkijena.jipipe.extensions.nodeexamples.JIPipeNodeExamplePickerDialog;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.ParameterUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * An {@link JIPipeGraphNode} that contains a non-empty workload.
 * This class contains additional parameters to control the workload behavior.
 * Please prefer to use this class or its derivatives if you write your algorithms.
 */
public abstract class JIPipeAlgorithm extends JIPipeGraphNode {

    private static final StateSerializer STATE_SERIALIZER = new StateSerializer();
    private boolean enabled = true;
    private boolean passThrough = false;
    private JIPipeFixedThreadPool threadPool;

    /**
     * Initializes a new node type instance and sets a custom slot configuration
     *
     * @param info              The algorithm info
     * @param slotConfiguration The slot configuration
     */
    public JIPipeAlgorithm(JIPipeNodeInfo info, JIPipeSlotConfiguration slotConfiguration) {
        super(info, slotConfiguration);
    }

    /**
     * Initializes a new node type instance
     *
     * @param info The algorithm info
     */
    public JIPipeAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public JIPipeAlgorithm(JIPipeAlgorithm other) {
        super(other);
        this.enabled = other.enabled;
        this.passThrough = other.passThrough;
    }

    @Override
    public void run(JIPipeProgressInfo progressInfo) {
        if (passThrough && canAutoPassThrough()) {
            progressInfo.log("Data passed through to output");
            runPassThrough(progressInfo);
        }
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        if (passThrough && !canPassThrough()) {
            report.resolve("Pass through").reportIsInvalid("Pass through is not supported!",
                    "The algorithm reports that it does not support pass through. This is often the case for multi-output algorithms or " +
                            "algorithms that apply a conversion.",
                    "This cannot be changed. Please contact the algorithm author.",
                    this);
        }
    }

    /**
     * Runs the pass through. Override this for custom implementations if you want
     *
     * @param progressInfo the progress
     */
    protected void runPassThrough(JIPipeProgressInfo progressInfo) {
        if (!canAutoPassThrough()) {
            throw new RuntimeException("Auto pass through not allowed!");
        }
        if (getInputSlots().isEmpty())
            return;
        if (getOutputSlots().isEmpty())
            return;
        getFirstOutputSlot().addData(getFirstInputSlot(), progressInfo);
    }

    /**
     * Returns true if the algorithm can automatically apply pass-through
     * This is only possible if there is at most one input and at most one output.
     * Input must be compatible to the output.
     *
     * @return if the algorithm can automatically apply pass-through
     */
    protected boolean canAutoPassThrough() {
        return getInputSlots().size() <= 1 && getOutputSlots().size() <= 1 && (getInputSlots().isEmpty() || getOutputSlots().isEmpty() ||
                JIPipe.getDataTypes().isConvertible(getFirstInputSlot().getAcceptedDataType(), getFirstOutputSlot().getAcceptedDataType()));

    }

    /**
     * Returns true if the algorithm can apply pass-through.
     * Override this method to implement your own checks
     *
     * @return true if the algorithm can apply pass-through
     */
    protected boolean canPassThrough() {
        return canAutoPassThrough();
    }

    @JIPipeDocumentation(name = "Enabled", description = "If disabled, this algorithm will be skipped in a run. " +
            "Please note that this will also disable all algorithms dependent on this algorithm.")
    @JIPipeParameter(value = "jipipe:algorithm:enabled")
    public boolean isEnabled() {
        return enabled;
    }

    @JIPipeParameter("jipipe:algorithm:enabled")
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

    }

    @JIPipeDocumentation(name = "Pass through", description = "If enabled, the algorithm will pass the input data directly to the output data without any processing. " +
            "This is different from enabling/disabling the algorithm as this will not disable dependent algorithms.\n" +
            "Please note that setting this parameter via adaptive parameters (if available) does not always yield the expected result. " +
            "The reason behind this is that pass-through is not trivial for certain nodes. We recommend to use a split node if node execution should be made adaptive.")
    @JIPipeParameter(value = "jipipe:algorithm:pass-through")
    public boolean isPassThrough() {
        return passThrough;
    }

    @JIPipeParameter("jipipe:algorithm:pass-through")
    public void setPassThrough(boolean passThrough) {
        this.passThrough = passThrough;
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if (ParameterUtils.isHiddenLocalParameter(tree, access, "jipipe:algorithm:enabled", "jipipe:algorithm:pass-through")) {
            return false;
        }
        return super.isParameterUIVisible(tree, access);
    }

    /**
     * Returns a unique identifier that represents the state of the algorithm.
     * Defaults to a JSON-serialized representation using the {@link StateSerializer}.
     * Override this method if you have external influences.
     *
     * @return the state id
     */
    public String getStateId() {
        try (StringWriter writer = new StringWriter()) {
            try (JsonGenerator generator = JsonUtils.getObjectMapper().getFactory().createGenerator(writer)) {
                STATE_SERIALIZER.serialize(this, generator, null);
            }
            return writer.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Indicates if the node should be run before all other nodes.
     * Only applies if there is no input.
     * Defaults to false.
     *
     * @return If the node should behave like a preprocessor.
     */
    public boolean isPreprocessor() {
        return false;
    }

    /**
     * Indicates if the node should be run after all other nodes.
     * Only applies if the output is not used or there is no output.
     * Defaults to false
     *
     * @return If the node should behave like a postprocessor.
     */
    public boolean isPostprocessor() {
        return false;
    }

    public JIPipeFixedThreadPool getThreadPool() {
        return threadPool;
    }

    /**
     * Sets the thread pool.
     * Depending on the implementation, the pool is just ignored.
     *
     * @param threadPool can be null (forces single-threaded run)
     */
    public void setThreadPool(JIPipeFixedThreadPool threadPool) {
        this.threadPool = threadPool;
    }

    @Override
    public List<JIPipeParameterCollectionContextAction> getContextActions() {
        List<JIPipeParameterCollectionContextAction> result = new ArrayList<>(super.getContextActions());
        Collection<JIPipeNodeExample> examples = JIPipe.getNodes().getNodeExamples(getInfo().getId());
        if(examples != null && !examples.isEmpty()) {
            JIPipeDefaultParameterCollectionContextAction action = new JIPipeDefaultParameterCollectionContextAction(
                    new JIPipeDefaultDocumentation("Load example", "Loads example parameters"),
                    UIUtils.getIconURLFromResources("actions/graduation-cap.png"),
                    this::showLoadExampleDialog
            );
            result.add(action);
        }
        return result;
    }

    /**
     * Opens a dialog where the user can select an example
     * @param workbench the workbench
     */
    public void showLoadExampleDialog(JIPipeWorkbench workbench) {
        Collection<JIPipeNodeExample> examples = JIPipe.getNodes().getNodeExamples(getInfo().getId());
        JIPipeNodeExamplePickerDialog pickerDialog = new JIPipeNodeExamplePickerDialog(workbench.getWindow());
        pickerDialog.setTitle("Load example");
        pickerDialog.setAvailableItems(examples.stream().sorted(Comparator.comparing(e -> e.getNodeTemplate().getName())).collect(Collectors.toList()));
        JIPipeNodeExample selection = pickerDialog.showDialog();
        if(selection != null) {
            loadExample(selection);
        }
    }

    /**
     * Loads an example.
     * Warning: This method will not ask for confirmation
     * @param example the example
     */
    public void loadExample(JIPipeNodeExample example) {
        JIPipeGraph graph = example.getNodeTemplate().getGraph();
        JIPipeGraphNode node = graph.getGraphNodes().iterator().next();
        if(node.getInfo() != getInfo()) {
            throw new RuntimeException("Cannot load example from wrong node type!");
        }
        try(StringWriter writer = new StringWriter()) {
            try(JsonGenerator generator = JsonUtils.getObjectMapper().createGenerator(writer)) {
                generator.writeStartObject();
                ParameterUtils.serializeParametersToJson(node, generator, entry -> !entry.getKey().startsWith("jipipe:"));
                generator.writeEndObject();
            }
            String jsonString = writer.toString();
            JsonNode node2 = JsonUtils.readFromString(jsonString, JsonNode.class);
            JIPipeIssueReport report = new JIPipeIssueReport();
            getSlotConfiguration().setTo(node.getSlotConfiguration());
            ParameterUtils.deserializeParametersFromJson(this, node2, report);
            getSlotConfiguration().setTo(node.getSlotConfiguration());
            if(!report.isValid()) {
                report.print();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Serializer used by getStateId()
     * It automatically skips name, compartment, description, slot configuration, and UI parameters that are not relevant to the state
     */
    public static class StateSerializer extends JsonSerializer<JIPipeGraphNode> {
        @Override
        public void serialize(JIPipeGraphNode algorithm, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            // Causes many cache misses
//            Map<String, String> sources = new HashMap<>();
//            if (algorithm.getGraph() != null) {
//                for (JIPipeDataSlot inputSlot : algorithm.getInputSlots()) {
//                    Set<JIPipeDataSlot> sourceSlots = algorithm.getGraph().getSourceSlots(inputSlot);
//                    if (!sourceSlots.isEmpty()) {
//                        sources.put(inputSlot.getName(), sourceSlots.getNode().getIdInGraph() + "/" + sourceSlots.getName());
//                    } else {
//                        sources.put(inputSlot.getName(), "");
//                    }
//                }
//            }
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("jipipe:node-info-id", algorithm.getInfo().getId());
//            if (algorithm.getGraph() != null) {
//                jsonGenerator.writeStringField("jipipe:node-uuid", StringUtils.nullToEmpty(algorithm.getUUIDInGraph()));
//                jsonGenerator.writeStringField("jipipe:node-alias-id", algorithm.getAliasIdInGraph());
//            }
//            jsonGenerator.writeObjectField("jipipe:cache-state:source-nodes", sources);
            ParameterUtils.serializeParametersToJson(algorithm, jsonGenerator, this::serializeParameter);
            jsonGenerator.writeEndObject();
        }

        /**
         * Returns true if the parameter should be serialized
         *
         * @param entry the parameter
         * @return if the parameter should be serialized
         */
        protected boolean serializeParameter(Map.Entry<String, JIPipeParameterAccess> entry) {
            return !entry.getKey().equals("jipipe:node:name")
                    && !entry.getKey().equals("jipipe:node:description");
        }
    }
}
