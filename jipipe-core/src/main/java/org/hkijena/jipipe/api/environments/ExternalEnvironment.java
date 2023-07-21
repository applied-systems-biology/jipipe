package org.hkijena.jipipe.api.environments;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidatable;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;

/**
 * Defines an external environment
 */
public abstract class ExternalEnvironment extends AbstractJIPipeParameterCollection implements JIPipeValidatable {
    private String name;

    public ExternalEnvironment() {
    }

    public ExternalEnvironment(ExternalEnvironment other) {
        this.name = other.name;
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

    @JIPipeDocumentation(name = "Name", description = "This environment's name")
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
}
