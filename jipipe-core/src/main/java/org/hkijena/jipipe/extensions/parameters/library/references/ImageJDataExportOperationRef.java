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

package org.hkijena.jipipe.extensions.parameters.library.references;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeValidatable;
import org.hkijena.jipipe.api.compat.ImageJDataExporter;
import org.hkijena.jipipe.api.compat.ImageJExportParameters;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalBooleanParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalStringParameter;

import java.util.Objects;

/**
 * Helper to allow easy serialization of {@link ImageJDataExporter} references
 */
public class ImageJDataExportOperationRef implements JIPipeValidatable, JIPipeParameterCollection {

    private final EventBus eventBus = new EventBus();
    private String id;
    private OptionalBooleanParameter activate = new OptionalBooleanParameter();
    private OptionalBooleanParameter noWindows = new OptionalBooleanParameter();
    private OptionalBooleanParameter append = new OptionalBooleanParameter();
    private OptionalBooleanParameter duplicate = new OptionalBooleanParameter();
    private OptionalStringParameter name = new OptionalStringParameter();

    public ImageJDataExportOperationRef(String id) {
        this.id = id;
    }

    /**
     * New instance
     */
    public ImageJDataExportOperationRef() {

    }

    public ImageJDataExportOperationRef(ImageJDataExporter exporter) {
        this(JIPipe.getImageJAdapters().getIdOf(exporter));
    }

    public ImageJDataExportOperationRef(ImageJDataExportOperationRef other) {
        this.id = other.id;
        this.activate = new OptionalBooleanParameter(other.activate);
        this.noWindows = new OptionalBooleanParameter(other.noWindows);
        this.append = new OptionalBooleanParameter(other.append);
        this.duplicate = new OptionalBooleanParameter(other.duplicate);
        this.name = new OptionalStringParameter(other.name);
    }

    @JsonGetter("id")
    public String getId() {
        return id;
    }

    @JsonSetter("id")
    public void setId(String id) {
        this.id = id;
    }

    @JIPipeDocumentation(name = "Override activation", description = "If enabled, overrides the activation status")
    @JIPipeParameter("activate")
    @JsonGetter("activate")
    public OptionalBooleanParameter getActivate() {
        return activate;
    }

    @JIPipeParameter("activate")
    @JsonSetter("activate")
    public void setActivate(OptionalBooleanParameter activate) {
        this.activate = activate;
    }

    @JIPipeDocumentation(name = "Override no windows", description = "If enabled, overrides the 'no windows' status")
    @JIPipeParameter("no-windows")
    @JsonGetter("no-windows")
    public OptionalBooleanParameter getNoWindows() {
        return noWindows;
    }

    @JIPipeParameter("no-windows")
    @JsonSetter("no-windows")
    public void setNoWindows(OptionalBooleanParameter noWindows) {
        this.noWindows = noWindows;
    }

    @JIPipeDocumentation(name = "Override append", description = "If enabled, overrides the 'append' status")
    @JIPipeParameter("append")
    @JsonGetter("append")
    public OptionalBooleanParameter getAppend() {
        return append;
    }

    @JIPipeParameter("append")
    @JsonSetter("append")
    public void setAppend(OptionalBooleanParameter append) {
        this.append = append;
    }

    @JIPipeDocumentation(name = "Override duplicate", description = "If enabled, overrides the 'duplicate' status")
    @JIPipeParameter("duplicate")
    @JsonGetter("duplicate")
    public OptionalBooleanParameter getDuplicate() {
        return duplicate;
    }

    @JIPipeParameter("duplicate")
    @JsonSetter("duplicate")
    public void setDuplicate(OptionalBooleanParameter duplicate) {
        this.duplicate = duplicate;
    }

    @JIPipeDocumentation(name = "Override name", description = "If enabled, overrides the name")
    @JIPipeParameter("name")
    @JsonGetter("name")
    public OptionalStringParameter getName() {
        return name;
    }

    @JIPipeParameter("name")
    @JsonSetter("name")
    public void setName(OptionalStringParameter name) {
        this.name = name;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    public ImageJDataExporter getInstance() {
        return JIPipe.getImageJAdapters().getExporterById(getId());
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        if (id == null)
            report.reportIsInvalid("No operation is selected!",
                    "You have to select an operation.",
                    "Please select an operation.",
                    this);
    }

    @Override
    public String toString() {
        if (id != null)
            return id;
        else
            return "<Null>";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImageJDataExportOperationRef that = (ImageJDataExportOperationRef) o;
        return Objects.equals(id, that.id) && Objects.equals(activate, that.activate) && Objects.equals(noWindows, that.noWindows) && Objects.equals(append, that.append) && Objects.equals(duplicate, that.duplicate) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, activate, noWindows, append, duplicate, name);
    }

    /**
     * Overrides parameters if enabled
     *
     * @param parameters the parameters
     */
    public void configure(ImageJExportParameters parameters) {
        if (name.isEnabled()) {
            parameters.setName(name.getContent());
        }
        if (duplicate.isEnabled()) {
            parameters.setDuplicate(duplicate.getContent());
        }
        if (activate.isEnabled()) {
            parameters.setActivate(activate.getContent());
        }
        if (append.isEnabled()) {
            parameters.setAppend(append.getContent());
        }
        if (noWindows.isEnabled()) {
            parameters.setNoWindows(noWindows.isEnabled());
        }
    }
}
