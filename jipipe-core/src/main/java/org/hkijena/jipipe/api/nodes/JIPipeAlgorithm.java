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
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeFixedThreadPool;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeSlotConfiguration;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.utils.ParameterUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

/**
 * An {@link JIPipeGraphNode} that contains a non-empty workload.
 * This class contains additional parameters to control the workload behavior.
 * Please prefer to use this class or its derivatives if you write your algorithms.
 */
public abstract class JIPipeAlgorithm extends JIPipeGraphNode {
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
    @JIPipeParameter(value = "jipipe:algorithm:enabled", pinned = true)
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
    @JIPipeParameter(value = "jipipe:algorithm:pass-through", pinned = true)
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

    @Override
    public boolean functionallyEquals(JIPipeGraphNode other) {
        if(!super.functionallyEquals(other))
            return false;

        // Compare slots and their data type (other properties do not matter)
        for (JIPipeInputDataSlot inputSlot : getInputSlots()) {
            JIPipeInputDataSlot otherInputSlot = other.getInputSlot(inputSlot.getName());
            if(otherInputSlot == null || otherInputSlot.getInfo().getDataClass() != inputSlot.getInfo().getDataClass()) {
                return false;
            }
        }
        for (JIPipeInputDataSlot inputSlot : other.getInputSlots()) {
            if(getInputSlot(inputSlot.getName()) == null) {
                return false;
            }
        }
        for (JIPipeOutputDataSlot outputSlot : getOutputSlots()) {
            JIPipeDataSlot otherOutputSlot = other.getOutputSlot(outputSlot.getName());
            if(otherOutputSlot == null || otherOutputSlot.getInfo().getDataClass() != outputSlot.getInfo().getDataClass()) {
                return false;
            }
        }
        for (JIPipeOutputDataSlot outputSlot : other.getOutputSlots()) {
            if(getOutputSlot(outputSlot.getName()) == null) {
                return false;
            }
        }

        // Compare functional parameters
        JIPipeParameterTree here = new JIPipeParameterTree(this);
        JIPipeParameterTree there = new JIPipeParameterTree(other);

        if(!here.getParameters().keySet().equals(there.getParameters().keySet()))
            return false;

        for (String key : here.getParameters().keySet()) {
            Object hereObj = here.getParameters().get(key).get(Object.class);
            Object thereObj = there.getParameters().get(key).get(Object.class);
            if(hereObj == null && thereObj == null) {
                // Continue
            }
            else if(hereObj == null || thereObj == null) {
                // Not equal
                return false;
            }
            else {
                String serializedHere = JsonUtils.toJsonString(hereObj);
                String serializedThere = JsonUtils.toJsonString(thereObj);
                if(!serializedThere.equals(serializedHere)) {
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
            JIPipeIssueReport report = new JIPipeIssueReport();
            getSlotConfiguration().setTo(node.getSlotConfiguration());
            ParameterUtils.deserializeParametersFromJson(this, node2, report);
            getSlotConfiguration().setTo(node.getSlotConfiguration());
            if (!report.isValid()) {
                report.print();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
