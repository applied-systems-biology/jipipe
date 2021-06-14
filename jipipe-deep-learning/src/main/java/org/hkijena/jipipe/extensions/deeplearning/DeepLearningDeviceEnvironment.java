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

package org.hkijena.jipipe.extensions.deeplearning;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.environments.ExternalEnvironment;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.collections.ListParameter;
import org.hkijena.jipipe.extensions.parameters.generators.IntegerRange;
import org.hkijena.jipipe.extensions.parameters.generators.OptionalIntegerRange;
import org.hkijena.jipipe.utils.JsonUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

public class DeepLearningDeviceEnvironment extends ExternalEnvironment {

    private OptionalIntegerRange cpuIds = new OptionalIntegerRange(new IntegerRange("0"), false);
    private OptionalIntegerRange gpuIds = new OptionalIntegerRange(new IntegerRange("0"), false);
    private boolean withGPU = true;
    private boolean logDevicePlacement = false;

    public DeepLearningDeviceEnvironment() {
        setName("Default");
    }

    public DeepLearningDeviceEnvironment(DeepLearningDeviceEnvironment other) {
        super(other);
        this.cpuIds = new OptionalIntegerRange(other.cpuIds);
        this.gpuIds = new OptionalIntegerRange(other.gpuIds);
        this.withGPU = other.withGPU;
        this.logDevicePlacement = other.logDevicePlacement;
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {

    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("devices/cpu.png");
    }

    @Override
    public String getInfo() {
        if (isWithGPU()) {
            String gpuString = "GPU";
            if (getGpuIds().isEnabled())
                gpuString += " (" + getGpuIds().getContent().getValue() + ")";
            String cpuString = "CPU";
            if (getCpuIds().isEnabled())
                cpuString += " (" + getCpuIds().getContent().getValue() + ")";
            return gpuString + " + " + cpuString;
        } else {
            if (getCpuIds().isEnabled())
                return "CPU only (" + getCpuIds().getContent().getValue() + ")";
            else
                return "CPU only";
        }
    }

    @JIPipeDocumentation(name = "Limit to CPUs", description = "Allows to limit the processing to certain CPU IDs (first ID is 0)")
    @JIPipeParameter("cpu-ids")
    public OptionalIntegerRange getCpuIds() {
        return cpuIds;
    }

    @JIPipeParameter("cpu-ids")
    public void setCpuIds(OptionalIntegerRange cpuIds) {
        this.cpuIds = cpuIds;
    }

    @JIPipeDocumentation(name = "Limit to GPUs", description = "Allows to limit the processing to certain GPU Ids (first is 0)")
    @JIPipeParameter("gpu-ids")
    public OptionalIntegerRange getGpuIds() {
        return gpuIds;
    }

    @JIPipeParameter("gpu-ids")
    public void setGpuIds(OptionalIntegerRange gpuIds) {
        this.gpuIds = gpuIds;
    }

    @JIPipeDocumentation(name = "Enable GPU processing", description = "If enabled, the GPU will be utilized to increase the speed of processing")
    @JIPipeParameter("with-gpu")
    public boolean isWithGPU() {
        return withGPU;
    }

    @JIPipeParameter("with-gpu")
    public void setWithGPU(boolean withGPU) {
        this.withGPU = withGPU;
    }

    @JIPipeDocumentation(name = "Log device placement", description = "If enabled, the Python library will report about which exact hardware is utilized for each task")
    @JIPipeParameter("log-device-placement")
    public boolean isLogDevicePlacement() {
        return logDevicePlacement;
    }

    @JIPipeParameter("log-device-placement")
    public void setLogDevicePlacement(boolean logDevicePlacement) {
        this.logDevicePlacement = logDevicePlacement;
    }

    /**
     * Converts the settings into a dltoolbox device configuration
     *
     * @return the device configuration
     */
    public JsonNode toDeviceConfigurationJSON() {
        ObjectNode root = JsonUtils.getObjectMapper().createObjectNode();
        root.set("log-device-placement", isLogDevicePlacement() ? BooleanNode.TRUE : BooleanNode.FALSE);
        if (isWithGPU()) {
            if (getGpuIds().isEnabled())
                root.set("gpus", JsonUtils.getObjectMapper().convertValue(getCpuIds().getContent().getIntegers(), JsonNode.class));
            else
                root.set("gpus", JsonUtils.getObjectMapper().convertValue("all", JsonNode.class));
        } else {
            root.set("gpus", JsonUtils.getObjectMapper().convertValue(new ArrayList<>(), JsonNode.class));
        }
        if (getCpuIds().isEnabled())
            root.set("cpus", JsonUtils.getObjectMapper().convertValue(getCpuIds().getContent().getIntegers(), JsonNode.class));
        else
            root.set("cpus", JsonUtils.getObjectMapper().convertValue("all", JsonNode.class));
        return root;
    }

    /**
     * Saves the device configuration in dltoolbox JSON format
     *
     * @param outputPath the JSON file
     */
    public void saveAsJson(Path outputPath) {
        try {
            JsonUtils.getObjectMapper().writeValue(outputPath.toFile(), toDeviceConfigurationJSON());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A list of {@link DeepLearningDeviceEnvironment}
     */
    public static class List extends ListParameter<DeepLearningDeviceEnvironment> {
        public List() {
            super(DeepLearningDeviceEnvironment.class);
        }

        public List(DeepLearningDeviceEnvironment.List other) {
            super(DeepLearningDeviceEnvironment.class);
            for (DeepLearningDeviceEnvironment environment : other) {
                add(new DeepLearningDeviceEnvironment(environment));
            }
        }
    }
}
