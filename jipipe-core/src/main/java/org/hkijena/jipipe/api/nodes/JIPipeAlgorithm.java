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
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeFixedThreadPool;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeSlotConfiguration;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterVisibility;
import org.hkijena.jipipe.utils.JsonUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * An {@link JIPipeGraphNode} that contains a non-empty workload.
 * This class contains additional parameters to control the workload behavior.
 * Please prefer to use this class or its derivatives if you write your algorithms.
 */
public abstract class JIPipeAlgorithm extends JIPipeGraphNode {

    private static final StateSerializer STATE_SERIALIZER = new StateSerializer();
    private boolean enabled = true;
    private boolean passThrough = false;
    private boolean saveOutputs = true;
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
        this.saveOutputs = other.saveOutputs;
    }

    @Override
    public void run(JIPipeProgressInfo progress) {
        if (passThrough && canAutoPassThrough()) {
            progress.log("Data passed through to output");
            runPassThrough();
        }
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        if (passThrough && !canPassThrough()) {
            report.forCategory("Pass through").reportIsInvalid("Pass through is not supported!",
                    "The algorithm reports that it does not support pass through. This is often the case for multi-output algorithms or " +
                            "algorithms that apply a conversion.",
                    "This cannot be changed. Please contact the algorithm author.",
                    this);
        }
    }

    /**
     * Runs the pass through. Override this for custom implementations if you want
     */
    protected void runPassThrough() {
        if (!canAutoPassThrough()) {
            throw new RuntimeException("Auto pass through not allowed!");
        }
        if (getInputSlots().isEmpty())
            return;
        if (getOutputSlots().isEmpty())
            return;
        for (int row = 0; row < getFirstInputSlot().getRowCount(); row++) {
            getFirstOutputSlot().addData(getFirstInputSlot().getData(row, JIPipeData.class), getFirstInputSlot().getAnnotations(row), JIPipeAnnotationMergeStrategy.Merge);
        }
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
    @JIPipeParameter(value = "jipipe:algorithm:enabled", visibility = JIPipeParameterVisibility.Visible)
    public boolean isEnabled() {
        return enabled;
    }

    @JIPipeParameter("jipipe:algorithm:enabled")
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

    }

    @JIPipeDocumentation(name = "Pass through", description = "If enabled, the algorithm will pass the input data directly to the output data without any processing. " +
            "This is different from enabling/disabling the algorithm as this will not disable dependent algorithms.")
    @JIPipeParameter(value = "jipipe:algorithm:pass-through", visibility = JIPipeParameterVisibility.Visible)
    public boolean isPassThrough() {
        return passThrough;
    }

    @JIPipeParameter("jipipe:algorithm:pass-through")
    public void setPassThrough(boolean passThrough) {
        this.passThrough = passThrough;

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
            JsonGenerator generator = JsonUtils.getObjectMapper().getFactory().createGenerator(writer);
            STATE_SERIALIZER.serialize(this, generator, null);
            return writer.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Indicates if the node should be run before all other nodes or after all other nodes.
     * Only applies if there is no input and output.
     * Defaults to false.
     * @return If the node should behave like a preprocessor.
     */
    public boolean isPreprocessor() {
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

    @JIPipeDocumentation(name = "Save outputs", description = "If disabled, the output data is not written into the output folder.")
    @JIPipeParameter(value = "jipipe:algorithm:save-outputs", visibility = JIPipeParameterVisibility.Visible)
    public boolean isSaveOutputs() {
        return saveOutputs;
    }

    @JIPipeParameter("jipipe:algorithm:save-outputs")
    public void setSaveOutputs(boolean saveOutputs) {
        this.saveOutputs = saveOutputs;
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
            if(algorithm.getGraph() != null)
                jsonGenerator.writeStringField("jipipe:node-id", algorithm.getIdInGraph());
//            jsonGenerator.writeObjectField("jipipe:cache-state:source-nodes", sources);
            JIPipeParameterCollection.serializeParametersToJson(algorithm, jsonGenerator, this::serializeParameter);
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
                    && !entry.getKey().equals("jipipe:node:description")
                    && !entry.getKey().equals("jipipe:algorithm:save-outputs");
        }
    }
}
