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

package org.hkijena.jipipe.api.environments;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.run.JIPipeGraphRun;
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

    /**
     * Executed at the start of the run and after artifacts are configured
     * @param run the run that executes the step
     * @param progressInfo the progress info
     */
    public void runPreconfigure(JIPipeGraphRun run, JIPipeProgressInfo progressInfo) {

    }

    /**
     * Executed after the end of the run (regardless if it failed or was successful)
     * @param run the run that executes the step
     * @param progressInfo the progress info
     */
    public void runPostprocessing(JIPipeGraphRun run, JIPipeProgressInfo progressInfo) {

    }
}
