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

package org.hkijena.jipipe.api.nodes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeFunctionallyComparable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeSlotConfiguration;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.parameters.*;
import org.hkijena.jipipe.api.runtimepartitioning.RuntimePartitionReferenceParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.plugins.expressions.custom.JIPipeCustomExpressionVariablesParameter;
import org.hkijena.jipipe.utils.ParameterUtils;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.Objects;

/**
 * An {@link JIPipeGraphNode} that contains a non-empty workload.
 * This class contains additional parameters to control the workload behavior.
 * Please prefer to use this class or its derivatives if you write your algorithms.
 */
public abstract class JIPipeAlgorithm extends JIPipeGraphNode {
    private final JIPipeCustomExpressionVariablesParameter customExpressionVariables;
    private boolean enabled = true;
    private boolean skipped = false;
    private boolean passThrough = false;
    private RuntimePartitionReferenceParameter runtimePartition = new RuntimePartitionReferenceParameter();

    /**
     * Initializes a new node type instance and sets a custom slot configuration
     *
     * @param info              The algorithm info
     * @param slotConfiguration The slot configuration
     */
    public JIPipeAlgorithm(JIPipeNodeInfo info, JIPipeSlotConfiguration slotConfiguration) {
        super(info, slotConfiguration);
        this.customExpressionVariables = new JIPipeCustomExpressionVariablesParameter();
        registerSubParameter(customExpressionVariables);
    }

    /**
     * Initializes a new node type instance
     *
     * @param info The algorithm info
     */
    public JIPipeAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.customExpressionVariables = new JIPipeCustomExpressionVariablesParameter();
        registerSubParameter(customExpressionVariables);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public JIPipeAlgorithm(JIPipeAlgorithm other) {
        super(other);
        this.skipped = other.skipped;
        this.enabled = other.enabled;
        this.passThrough = other.passThrough;
        this.runtimePartition = new RuntimePartitionReferenceParameter(other.runtimePartition);
        this.customExpressionVariables = new JIPipeCustomExpressionVariablesParameter(other.customExpressionVariables);
        registerSubParameter(customExpressionVariables);
    }

    @Override
    public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        if (passThrough && canAutoPassThrough()) {
            progressInfo.log("Data passed through to output");
            runPassThrough(runContext, progressInfo);
        }
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {

    }

    /**
     * Runs the pass through. Override this for custom implementations if you want
     *
     * @param runContext   the context of the running operation
     * @param progressInfo the progress
     */
    protected void runPassThrough(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        if (!canAutoPassThrough()) {
            throw new RuntimeException("Auto pass through not allowed!");
        }
        if (getInputSlots().isEmpty())
            return;
        if (getOutputSlots().isEmpty())
            return;
        getFirstOutputSlot().addDataFromSlot(getFirstInputSlot(), progressInfo);
    }

    /**
     * Returns true if the algorithm can automatically apply pass-through
     * This is only possible if there is at most one input and at most one output.
     * Input must be compatible to the output.
     *
     * @return if the algorithm can automatically apply pass-through
     */
    protected boolean canAutoPassThrough() {
        return getDataInputSlots().size() <= 1 && getOutputSlots().size() <= 1 && (getDataInputSlots().isEmpty() || getOutputSlots().isEmpty() ||
                JIPipe.getDataTypes().isConvertible(getFirstInputSlot().getAcceptedDataType(), getFirstOutputSlot().getAcceptedDataType()));

    }

    /**
     * Returns true if the algorithm can apply pass-through.
     * Override this method to implement your own checks
     *
     * @return true if the algorithm can apply pass-through
     */
    public boolean canPassThrough() {
        return canAutoPassThrough();
    }

    /**
     * Used internally to mark an algorithm as (not) executed without triggering isFunctionallyEquals()
     * Affects {@link JIPipeGraph}'s getDeactivatedNodes
     * This is not serialized, but copied
     *
     * @return if the node should be skipped in the next runs
     */
    public boolean isSkipped() {
        return skipped;
    }

    /**
     * Used internally to mark an algorithm as (not) executed without triggering isFunctionallyEquals()
     * Affects {@link JIPipeGraph}'s getDeactivatedNodes
     * This is not serialized, but copied
     *
     * @param skipped if the node should be skipped in the next runs
     */
    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }

    @SetJIPipeDocumentation(name = "Enabled", description = "If disabled, this algorithm will be skipped in a run. " +
            "Please note that this will also disable all algorithms dependent on this algorithm.")
    @JIPipeParameter(value = "jipipe:algorithm:enabled", pinned = true)
    public boolean isEnabled() {
        return enabled;
    }

    @JIPipeParameter("jipipe:algorithm:enabled")
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @SetJIPipeDocumentation(name = "Partition", description = "Allows to move the node into a different runtime partition, which determine how the workload is executed.")
    @JIPipeParameter(value = "jipipe:algorithm:runtime-partition", pinned = true)
    public RuntimePartitionReferenceParameter getRuntimePartition() {
        return runtimePartition;
    }

    @JIPipeParameter("jipipe:algorithm:runtime-partition")
    public void setRuntimePartition(RuntimePartitionReferenceParameter runtimePartition) {
        this.runtimePartition = runtimePartition;
    }

    @SetJIPipeDocumentation(name = "Pass through", description = "If enabled, the algorithm will pass the input data directly to the output data without any processing. " +
            "This is different from enabling/disabling the algorithm as this will not disable dependent algorithms.\n" +
            "Please note that setting this parameter via adaptive parameters (if available) does not always yield the expected result. " +
            "The reason behind this is that pass-through is not trivial for certain nodes. We recommend to use a split node if node execution should be made adaptive.")
    @JIPipeParameter(value = "jipipe:algorithm:pass-through", pinned = true)
    public boolean isPassThrough() {
        return passThrough;
    }

    @JIPipeParameter("jipipe:algorithm:pass-through")
    public void setPassThrough(boolean passThrough) {
        this.passThrough = passThrough;
    }

    @SetJIPipeDocumentation(name = "Custom variables", description = "Here you can add parameters that will be included into the expressions as variables <code>custom.[key]</code>. Alternatively, you can access them via <code>GET_ITEM(custom, \"[key]\")</code>.")
    @JIPipeParameter(value = "jipipe:algorithm:custom-expression-variables", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/insert-math-expression.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/insert-math-expression.png", persistence = JIPipeParameterSerializationMode.Object)
    public JIPipeCustomExpressionVariablesParameter getDefaultCustomExpressionVariables() {
        return customExpressionVariables;
    }

    @Override
    protected void onDeserialized(JsonNode node, JIPipeValidationReport issues, JIPipeNotificationInbox notifications) {
        super.onDeserialized(node, issues, notifications);

        if (isEnableDefaultCustomExpressionVariables() && customExpressionVariables.getParameters().isEmpty()) {

            if ("ij1-roi-filter-statistics".equals(getInfo().getId())) {
                System.out.println();
            }

            // Transfer parameters "custom-expression-variables" and "custom-filter-variables" into the standard expression storage if they are found
            try {
                deserializeLegacyCustomExpressionVariables(node, "custom-expression-variables");
                deserializeLegacyCustomExpressionVariables(node, "custom-filter-variables");
                deserializeLegacyCustomExpressionVariables(node, "custom-variables");
            } catch (Throwable e) {
                issues.report(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                        new GraphNodeValidationReportContext(this),
                        "Error while reading parameters",
                        "A parameter could not be upgraded to the JIPipe 3.x format. Please report this to the JIPipe developer.",
                        "Please report this to the JIPipe developer",
                        e.toString()));
                e.printStackTrace();
            }
        }
    }

    private void deserializeLegacyCustomExpressionVariables(JsonNode node, String jsonPropertyKey) throws IOException {
        if (node.has(jsonPropertyKey)) {
            JIPipeDynamicParameterCollection value = JsonUtils.getObjectMapper().readerFor(JIPipeCustomExpressionVariablesParameter.class).readValue(node.get(jsonPropertyKey));
            for (Map.Entry<String, JIPipeParameterAccess> entry : value.getParameters().entrySet()) {
                customExpressionVariables.addParameter((JIPipeMutableParameterAccess) entry.getValue());
            }
            JIPipe.getInstance().getLogService().info(getInfo().getId() + ": Transferred '" + jsonPropertyKey + "' into 'jipipe:custom-expression-variables'");
        }
    }

    /**
     * Returns true if the default custom expression variables are shown in the UI
     * (defaults to true)
     *
     * @return if the custom expression variables are shown in the UI
     */
    public boolean isEnableDefaultCustomExpressionVariables() {
        return true;
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if (ParameterUtils.isHiddenLocalParameter(tree, access, "jipipe:algorithm:enabled", "jipipe:algorithm:pass-through", "jipipe:algorithm:runtime-partition")) {
            return false;
        }
        if (access.getSource() == this && "jipipe:algorithm:pass-through".equals(access.getKey()) && !canPassThrough()) {
            return false;
        }
        return super.isParameterUIVisible(tree, access);
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterCollection subParameter) {
        if (subParameter == customExpressionVariables) {
            return isEnableDefaultCustomExpressionVariables();
        }
        return super.isParameterUIVisible(tree, subParameter);
    }

    @Override
    public boolean functionallyEquals(Object other) {
        if (!super.functionallyEquals(other))
            return false;

        JIPipeGraphNode otherNode = (JIPipeGraphNode) other;

        // Compare slots and their data type (other properties do not matter)
        for (JIPipeInputDataSlot inputSlot : getInputSlots()) {
            JIPipeInputDataSlot otherInputSlot = otherNode.getInputSlot(inputSlot.getName());
            if (otherInputSlot == null || otherInputSlot.getInfo().getDataClass() != inputSlot.getInfo().getDataClass()) {
                return false;
            }
        }
        for (JIPipeInputDataSlot inputSlot : otherNode.getInputSlots()) {
            if (getInputSlot(inputSlot.getName()) == null) {
                return false;
            }
        }
        for (JIPipeOutputDataSlot outputSlot : getOutputSlots()) {
            JIPipeDataSlot otherOutputSlot = otherNode.getOutputSlot(outputSlot.getName());
            if (otherOutputSlot == null || otherOutputSlot.getInfo().getDataClass() != outputSlot.getInfo().getDataClass()) {
                return false;
            }
        }
        for (JIPipeOutputDataSlot outputSlot : otherNode.getOutputSlots()) {
            if (getOutputSlot(outputSlot.getName()) == null) {
                return false;
            }
        }

        // Compare functional parameters
        JIPipeParameterTree here = new JIPipeParameterTree(this);
        JIPipeParameterTree there = new JIPipeParameterTree(otherNode);

        if (!here.getParameters().keySet().equals(there.getParameters().keySet()))
            return false;

        for (String key : here.getParameters().keySet()) {
            Object hereObj = here.getParameters().get(key).get(Object.class);
            Object thereObj = there.getParameters().get(key).get(Object.class);
            if (hereObj == null && thereObj == null) {
                // Continue
            } else if (hereObj == null || thereObj == null) {
                // Not equal
                return false;
            } else if (Objects.equals(hereObj, thereObj)) {
                // Continue
            } else if (hereObj instanceof JIPipeFunctionallyComparable) {
                if (!((JIPipeFunctionallyComparable) hereObj).functionallyEquals(thereObj)) {
                    return false;
                }
            } else {
                // Serialization-based comparison (slow, but very portable)
                String serializedHere = JsonUtils.toJsonString(hereObj);
                String serializedThere = JsonUtils.toJsonString(thereObj);
                if (!serializedThere.equals(serializedHere)) {
                    return false;
                }
            }
        }

        return true;
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

    /**
     * Loads an example.
     * Warning: This method will not ask for confirmation
     *
     * @param example the example
     */
    public void loadExample(JIPipeNodeExample example) {
        JIPipeGraph graph = example.getNodeTemplate().getGraph();
        JIPipeGraphNode node = graph.getGraphNodes().iterator().next();
        if (node.getInfo() != getInfo()) {
            throw new RuntimeException("Cannot load example from wrong node type!");
        }
        try (StringWriter writer = new StringWriter()) {
            try (JsonGenerator generator = JsonUtils.getObjectMapper().createGenerator(writer)) {
                generator.writeStartObject();
                ParameterUtils.serializeParametersToJson(node, generator, entry -> !entry.getKey().startsWith("jipipe:"));
                generator.writeEndObject();
            }
            String jsonString = writer.toString();
            JsonNode node2 = JsonUtils.readFromString(jsonString, JsonNode.class);
            JIPipeValidationReport report = new JIPipeValidationReport();
            getSlotConfiguration().setTo(node.getSlotConfiguration());
            ParameterUtils.deserializeParametersFromJson(this, node2, new UnspecifiedValidationReportContext(), report);
            getSlotConfiguration().setTo(node.getSlotConfiguration());
            if (!report.isEmpty()) {
                report.print();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
