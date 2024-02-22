package org.hkijena.jipipe.api.environments;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidatable;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;

/**
 * Defines an environment
 */
public abstract class JIPipeEnvironment extends AbstractJIPipeParameterCollection implements JIPipeValidatable {
    private String name;
    private String version = "unknown";
    private String source = "NA";

    public JIPipeEnvironment() {
    }

    public JIPipeEnvironment(JIPipeEnvironment other) {
        this.name = other.name;
        this.version = other.version;
        this.source = other.source;
    }

    /**
     * Returns the icon displayed in the UI for the current status
     *
     * @return the icon
     */
    public abstract Icon getIcon();

    /**
     * Returns more detailed information (e.g., the executed script environment path).
     * Displayed inside a text field.
     *
     * @return the info string
     */
    public abstract String getInfo();

    @SetJIPipeDocumentation(name = "Name", description = "This environment's name")
    @JIPipeParameter("name")
    @JsonGetter("name")
    public String getName() {
        return StringUtils.nullToEmpty(name);
    }

    @JIPipeParameter("name")
    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
    }

    @SetJIPipeDocumentation(name = "Version", description = "The version of this environment")
    @JIPipeParameter("version")
    @JsonGetter("version")
    public String getVersion() {
        return version;
    }

    @JIPipeParameter("version")
    @JsonSetter("version")
    public void setVersion(String version) {
        this.version = version;
    }

    @SetJIPipeDocumentation(name = "Source", description = "Information about where this environment was sourced from")
    @JIPipeParameter("source")
    @JsonGetter("source")
    public String getSource() {
        return source;
    }

    @JIPipeParameter("source")
    @JsonSetter("source")
    public void setSource(String source) {
        this.source = source;
    }
}
