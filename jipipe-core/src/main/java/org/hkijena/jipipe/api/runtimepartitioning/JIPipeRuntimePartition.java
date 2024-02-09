package org.hkijena.jipipe.api.runtimepartitioning;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;

import java.awt.*;

public class JIPipeRuntimePartition extends AbstractJIPipeParameterCollection {
    private String name = "Unnamed";
    private HTMLText description = new HTMLText();
    private OptionalColorParameter color = new OptionalColorParameter(Color.RED, true);

//    private OutputSettings outputSettings;

    public JIPipeRuntimePartition() {
//        this.outputSettings = new OutputSettings();
//        registerSubParameter(outputSettings);
    }

    public JIPipeRuntimePartition(JIPipeRuntimePartition other) {
        this.name = other.name;
        this.description = new HTMLText(other.description);
        this.color = new OptionalColorParameter(other.color);
//        this.outputSettings = new OutputSettings(other.outputSettings);
//        registerSubParameter(outputSettings);
    }

    public void setTo(JIPipeRuntimePartition other) {
        this.name = other.name;
        this.description = new HTMLText(other.description);
        this.color = new OptionalColorParameter(other.color);
//        this.outputSettings.setTo(other.outputSettings);
    }

    @JIPipeDocumentation(name = "Name", description = "Name of the partition")
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

    @JIPipeDocumentation(name = "Description", description = "Description for this partition")
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

    @JIPipeDocumentation(name = "Color", description = "If enabled, color nodes in this partition according to the color")
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

//    @JIPipeDocumentation(name = "Outputs", description = "Settings related to how generated outputs are exported or further processed")
//    @JIPipeParameter("output-settings")
//    @JsonGetter("output-settings")
//    public OutputSettings getOutputSettings() {
//        return outputSettings;
//    }
//
//    @JsonSetter("output-settings")
//    public void setOutputSettings(OutputSettings outputSettings) {
//        this.outputSettings = outputSettings;
//    }

//    public static class OutputSettings extends AbstractJIPipeParameterCollection {
//        private boolean exportLightweightData = true;
//        private boolean exportHeavyData = true;
//
//        public OutputSettings() {
//        }
//
//        public OutputSettings(OutputSettings other) {
//            this.exportLightweightData = other.exportLightweightData;
//            this.exportHeavyData = other.exportHeavyData;
//        }
//
//        public void setTo(OutputSettings other) {
//            this.exportLightweightData = other.exportLightweightData;
//            this.exportHeavyData = other.exportHeavyData;
//        }
//
//        @JIPipeDocumentation(name = "Auto-export lightweight data", description = "If enabled, save data that is generally small, e.g., tables, 2D ROI, or text files.")
//        @JIPipeParameter("export-lightweight-data")
//        @JsonGetter("export-lightweight-data")
//        public boolean isExportLightweightData() {
//            return exportLightweightData;
//        }
//
//        @JIPipeParameter("export-lightweight-data")
//        @JsonSetter("export-lightweight-data")
//        public void setExportLightweightData(boolean exportLightweightData) {
//            this.exportLightweightData = exportLightweightData;
//        }
//
//        @JIPipeDocumentation(name = "Auto-export heavy data", description = "If enabled, save data that is generally large, e.g., images or 3D ROI.")
//        @JIPipeParameter("export-heavy-data")
//        @JsonGetter("export-heavy-data")
//        public boolean isExportHeavyData() {
//            return exportHeavyData;
//        }
//
//        @JIPipeParameter("export-heavy-data")
//        @JsonSetter("export-heavy-data")
//        public void setExportHeavyData(boolean exportHeavyData) {
//            this.exportHeavyData = exportHeavyData;
//        }
//
//    }
}
