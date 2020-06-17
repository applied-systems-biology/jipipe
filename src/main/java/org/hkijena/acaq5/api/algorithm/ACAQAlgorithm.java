package org.hkijena.acaq5.api.algorithm;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQFixedThreadPool;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQSlotConfiguration;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterTree;
import org.hkijena.acaq5.api.parameters.ACAQParameterVisibility;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.utils.JsonUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * An {@link ACAQGraphNode} that contains a non-empty workload.
 * This class contains additional parameters to control the workload behavior.
 * Please prefer to use this class or its derivatives if you write your algorithms.
 */
public abstract class ACAQAlgorithm extends ACAQGraphNode {

    private static final StateSerializer STATE_SERIALIZER = new StateSerializer();
    private boolean enabled = true;
    private boolean passThrough = false;
    private ACAQFixedThreadPool threadPool;

    /**
     * Initializes a new algorithm instance and sets a custom slot configuration
     *
     * @param declaration       The algorithm declaration
     * @param slotConfiguration The slot configuration
     */
    public ACAQAlgorithm(ACAQAlgorithmDeclaration declaration, ACAQSlotConfiguration slotConfiguration) {
        super(declaration, slotConfiguration);
    }

    /**
     * Initializes a new algorithm instance
     *
     * @param declaration The algorithm declaration
     */
    public ACAQAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ACAQAlgorithm(ACAQAlgorithm other) {
        super(other);
        this.enabled = other.enabled;
        this.passThrough = other.passThrough;
    }

    @Override
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        if (passThrough && canAutoPassThrough()) {
            algorithmProgress.accept(subProgress.resolve("Data passed through to output"));
            runPassThrough();
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
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
            getFirstOutputSlot().addData(getFirstInputSlot().getData(row, ACAQData.class), getFirstInputSlot().getAnnotations(row));
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
                ACAQDatatypeRegistry.getInstance().isConvertible(getFirstInputSlot().getAcceptedDataType(), getFirstOutputSlot().getAcceptedDataType()));

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

    @ACAQDocumentation(name = "Enabled", description = "If disabled, this algorithm will be skipped in a run. " +
            "Please note that this will also disable all algorithms dependent on this algorithm.")
    @ACAQParameter(value = "acaq:algorithm:enabled", visibility = ACAQParameterVisibility.Visible)
    public boolean isEnabled() {
        return enabled;
    }

    @ACAQParameter("acaq:algorithm:enabled")
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        getEventBus().post(new ParameterChangedEvent(this, "acaq:algorithm:enabled"));
    }

    @ACAQDocumentation(name = "Pass through", description = "If enabled, the algorithm will pass the input data directly to the output data without any processing. " +
            "This is different from enabling/disabling the algorithm as this will not disable dependent algorithms.")
    @ACAQParameter("acaq:algorithm:pass-through")
    public boolean isPassThrough() {
        return passThrough;
    }

    @ACAQParameter("acaq:algorithm:pass-through")
    public void setPassThrough(boolean passThrough) {
        this.passThrough = passThrough;
        getEventBus().post(new ParameterChangedEvent(this, "acaq:algorithm:pass-through"));
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

    public ACAQFixedThreadPool getThreadPool() {
        return threadPool;
    }

    /**
     * Sets the thread pool.
     * Depending on the implementation, the pool is just ignored.
     *
     * @param threadPool can be null (forces single-threaded run)
     */
    public void setThreadPool(ACAQFixedThreadPool threadPool) {
        this.threadPool = threadPool;
    }

    /**
     * Serializer used by getStateId()
     * It automatically skips name, compartment, description, slot configuration, and UI parameters that are not relevant to the state
     */
    public static class StateSerializer extends JsonSerializer<ACAQGraphNode> {
        @Override
        public void serialize(ACAQGraphNode algorithm, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("acaq:algorithm-type", algorithm.getDeclaration().getId());
            ACAQParameterTree parameterCollection = new ACAQParameterTree(algorithm);
            for (Map.Entry<String, ACAQParameterAccess> entry : parameterCollection.getParameters().entrySet()) {
                if (serializeParameter(entry))
                    jsonGenerator.writeObjectField(entry.getKey(), entry.getValue().get(Object.class));
            }

            // Dynamic parameter storage is not saved, as their values are stored into normal parameters

            jsonGenerator.writeEndObject();
        }

        /**
         * Returns true if the parameter should be serialized
         *
         * @param entry the parameter
         * @return if the parameter should be serialized
         */
        protected boolean serializeParameter(Map.Entry<String, ACAQParameterAccess> entry) {
            return !entry.getKey().equals("acaq:node:name") && !entry.getKey().equals("acaq:node:description");
        }
    }
}
