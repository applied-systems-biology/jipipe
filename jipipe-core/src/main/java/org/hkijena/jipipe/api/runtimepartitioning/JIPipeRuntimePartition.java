package org.hkijena.jipipe.api.runtimepartitioning;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.grouping.JIPipeGraphWrapperAlgorithm;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithmIterationStepGenerationSettings;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithmIterationStepGenerationSettings;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;

import java.awt.*;

public class JIPipeRuntimePartition extends AbstractJIPipeParameterCollection {
    private String name = "Unnamed";
    private HTMLText description = new HTMLText();
    private OptionalColorParameter color = new OptionalColorParameter(Color.RED, true);
    private boolean enableParallelization = false;
    private JIPipeIteratingAlgorithmIterationStepGenerationSettings loopIterationIteratingSettings;
    private JIPipeMergingAlgorithmIterationStepGenerationSettings loopIterationMergingSettings;
    private OutputSettings outputSettings;
    private JIPipeGraphWrapperAlgorithm.IterationMode iterationMode = JIPipeGraphWrapperAlgorithm.IterationMode.PassThrough;
    private boolean continueOnFailure = false;

    public JIPipeRuntimePartition() {
        this.outputSettings = new OutputSettings();
        this.loopIterationMergingSettings = new JIPipeMergingAlgorithmIterationStepGenerationSettings();
        this.loopIterationIteratingSettings = new JIPipeIteratingAlgorithmIterationStepGenerationSettings();
        registerSubParameters(outputSettings, loopIterationMergingSettings, loopIterationIteratingSettings);
    }

    public JIPipeRuntimePartition(JIPipeRuntimePartition other) {
        this.name = other.name;
        this.description = new HTMLText(other.description);
        this.color = new OptionalColorParameter(other.color);
        this.enableParallelization = other.enableParallelization;
        this.iterationMode = other.iterationMode;
        this.continueOnFailure = other.continueOnFailure;
        this.outputSettings = new OutputSettings(other.outputSettings);
        this.loopIterationMergingSettings = new JIPipeMergingAlgorithmIterationStepGenerationSettings(other.loopIterationMergingSettings);
        this.loopIterationIteratingSettings = new JIPipeIteratingAlgorithmIterationStepGenerationSettings(other.loopIterationIteratingSettings);
        registerSubParameters(outputSettings, loopIterationMergingSettings, loopIterationIteratingSettings);
    }

    public void setTo(JIPipeRuntimePartition other) {
        this.name = other.name;
        this.description = new HTMLText(other.description);
        this.color = new OptionalColorParameter(other.color);
        this.enableParallelization = other.enableParallelization;
        this.iterationMode = other.iterationMode;
        this.outputSettings = new OutputSettings(other.outputSettings);
        this.loopIterationMergingSettings = new JIPipeMergingAlgorithmIterationStepGenerationSettings(other.loopIterationMergingSettings);
        this.loopIterationIteratingSettings = new JIPipeIteratingAlgorithmIterationStepGenerationSettings(other.loopIterationIteratingSettings);
        registerSubParameters(outputSettings, loopIterationMergingSettings, loopIterationIteratingSettings);
        emitParameterUIChangedEvent();
    }

    @SetJIPipeDocumentation(name = "Continue on failure", description = "If enabled, the pipeline will continue if a node within the partition fails. For pass-through iteration, " +
            "JIPipe will continue with the other partitions (no data is output from the current partition). If you enabled iteration, all successful results will be stored and passed to dependents.")
    @JIPipeParameter("continue-on-failure")
    public boolean isContinueOnFailure() {
        return continueOnFailure;
    }

    @JIPipeParameter("continue-on-failure")
    public void setContinueOnFailure(boolean continueOnFailure) {
        this.continueOnFailure = continueOnFailure;
    }

    @SetJIPipeDocumentation(name = "Enable parallelization", description = "If enabled, the nodes in this partition will be able to parallelize their workloads using JIPipe's parallelization system. " +
            "The underlying algorithms may still utilize parallelization even if this setting is disabled.")
    @JIPipeParameter("enable-parallelization")
    public boolean isEnableParallelization() {
        return enableParallelization;
    }

    @JIPipeParameter("enable-parallelization")
    public void setEnableParallelization(boolean enableParallelization) {
        this.enableParallelization = enableParallelization;
    }

    @SetJIPipeDocumentation(name = "Name", description = "Name of the partition")
    @JIPipeParameter(value = "name", pinned = true, uiOrder = -100)
    @JsonGetter("name")
    public String getName() {
        return name;
    }

    @JIPipeParameter("name")
    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
    }

    @SetJIPipeDocumentation(name = "Description", description = "Description for this partition")
    @JIPipeParameter("description")
    @JsonGetter("description")
    public HTMLText getDescription() {
        return description;
    }
    @JIPipeParameter("description")
    @JsonSetter("description")
    public void setDescription(HTMLText description) {
        this.description = description;
    }

    @SetJIPipeDocumentation(name = "Color", description = "If enabled, color nodes in this partition according to the color")
    @JIPipeParameter(value = "color", pinned = true, uiOrder = -90)
    @JsonGetter("color")
    public OptionalColorParameter getColor() {
        return color;
    }

    @JIPipeParameter("color")
    @JsonSetter("color")
    public void setColor(OptionalColorParameter color) {
        this.color = color;
    }

    @SetJIPipeDocumentation(name = "Outputs", description = "Settings related to how generated outputs are exported or further processed")
    @JIPipeParameter("output-settings")
    @JsonGetter("output-settings")
    public OutputSettings getOutputSettings() {
        return outputSettings;
    }

    @JsonSetter("output-settings")
    public void setOutputSettings(OutputSettings outputSettings) {
        this.outputSettings = outputSettings;
    }

    @SetJIPipeDocumentation(name = "Iteration mode", description = "If not set to 'Pass-through', the contents of this graph partition are looped based on the annotations of incoming data from other partitions. " +
            "You will need at least two partitions to make use of looping. Loops cannot be nested.")
    @JIPipeParameter("iteration-mode")
    public JIPipeGraphWrapperAlgorithm.IterationMode getIterationMode() {
        return iterationMode;
    }

    @JIPipeParameter("iteration-mode")
    public void setIterationMode(JIPipeGraphWrapperAlgorithm.IterationMode iterationMode) {
        this.iterationMode = iterationMode;
        emitParameterUIChangedEvent();
    }

    @SetJIPipeDocumentation(name = "Loop iteration (multiple data per slot)", description = "Determine how iteration steps for looping partitions are created. Only applied if the 'Iteration mode' is set to 'Loop (multiple data per slot)'")
    @JIPipeParameter("loop-iteration-merging-settings")
    @JsonGetter("loop-iteration-merging-settings")
    public JIPipeMergingAlgorithmIterationStepGenerationSettings getLoopIterationMergingSettings() {
        return loopIterationMergingSettings;
    }

    @JsonSetter("loop-iteration-merging-settings")
    public void setLoopIterationMergingSettings(JIPipeMergingAlgorithmIterationStepGenerationSettings loopIterationMergingSettings) {
        this.loopIterationMergingSettings = loopIterationMergingSettings;
    }

    @SetJIPipeDocumentation(name = "Loop iteration (single data per slot))", description = "Determine how iteration steps for looping partitions are created. Only applied if the 'Iteration mode' is set to 'Loop (single data per slot)'")
    @JIPipeParameter("loop-iteration-iterating-settings")
    @JsonGetter("loop-iteration-iterating-settings")
    public JIPipeIteratingAlgorithmIterationStepGenerationSettings getLoopIterationIteratingSettings() {
        return loopIterationIteratingSettings;
    }

    @JsonGetter("loop-iteration-iterating-settings")
    public void setLoopIterationIteratingSettings(JIPipeIteratingAlgorithmIterationStepGenerationSettings loopIterationIteratingSettings) {
        this.loopIterationIteratingSettings = loopIterationIteratingSettings;
        emitParameterUIChangedEvent();
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterCollection subParameter) {
        switch (iterationMode) {
            case PassThrough: {
                if(subParameter == loopIterationIteratingSettings)
                    return false;
                if(subParameter == loopIterationMergingSettings)
                    return false;
            }
            break;
            case IteratingDataBatch: {
                if(subParameter == loopIterationMergingSettings)
                    return false;
            }
            break;
            case MergingDataBatch: {
                if(subParameter == loopIterationIteratingSettings)
                    return false;
            }
            break;
        }
        return super.isParameterUIVisible(tree, subParameter);
    }

    public static class OutputSettings extends AbstractJIPipeParameterCollection {
        private boolean exportLightweightData = true;
        private boolean exportHeavyData = true;

        public OutputSettings() {
        }

        public OutputSettings(OutputSettings other) {
            this.exportLightweightData = other.exportLightweightData;
            this.exportHeavyData = other.exportHeavyData;
        }

        @SetJIPipeDocumentation(name = "Auto-export lightweight data", description = "If enabled, save data that is generally small, e.g., tables, 2D ROI, or text files.")
        @JIPipeParameter("export-lightweight-data")
        @JsonGetter("export-lightweight-data")
        public boolean isExportLightweightData() {
            return exportLightweightData;
        }

        @JIPipeParameter("export-lightweight-data")
        @JsonSetter("export-lightweight-data")
        public void setExportLightweightData(boolean exportLightweightData) {
            this.exportLightweightData = exportLightweightData;
        }

        @SetJIPipeDocumentation(name = "Auto-export heavy data", description = "If enabled, save data that is generally large, e.g., images or 3D ROI.")
        @JIPipeParameter("export-heavy-data")
        @JsonGetter("export-heavy-data")
        public boolean isExportHeavyData() {
            return exportHeavyData;
        }

        @JIPipeParameter("export-heavy-data")
        @JsonSetter("export-heavy-data")
        public void setExportHeavyData(boolean exportHeavyData) {
            this.exportHeavyData = exportHeavyData;
        }

    }
}
