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
import org.hkijena.jipipe.api.compat.ImageJDataImporter;
import org.hkijena.jipipe.api.compat.ImageJImportParameters;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalBooleanParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalStringParameter;

import java.util.Objects;

/**
 * Helper to allow easy serialization of {@link ImageJDataImporter} references
 */
public class ImageJDataImportOperationRef extends AbstractJIPipeParameterCollection implements JIPipeValidatable {
    private String id;
    private OptionalStringParameter name = new OptionalStringParameter();
    private OptionalBooleanParameter duplicate = new OptionalBooleanParameter();

    public ImageJDataImportOperationRef(String id) {
        this.id = id;
    }

    /**
     * New instance
     */
    public ImageJDataImportOperationRef() {

    }

    public ImageJDataImportOperationRef(ImageJDataImportOperationRef other) {
        this.id = other.id;
        this.name = new OptionalStringParameter(other.name);
        this.duplicate = new OptionalBooleanParameter(other.duplicate);
    }

    public ImageJDataImportOperationRef(ImageJDataImporter importer) {
        this(JIPipe.getImageJAdapters().getIdOf(importer));
    }

    @JsonGetter("id")
    public String getId() {
        return id;
    }

    @JsonSetter("id")
    public void setId(String id) {
        this.id = id;
    }

    @JIPipeDocumentation(name = "Override name", description = "If enabled, override the default name")
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

    @JIPipeParameter("duplicate")
    @JsonGetter("duplicate")
    @JIPipeDocumentation(name = "Override duplication", description = "If enabled, override the default duplication parameter")
    public OptionalBooleanParameter getDuplicate() {
        return duplicate;
    }

    @JIPipeParameter("duplicate")
    @JsonSetter("duplicate")
    public void setDuplicate(OptionalBooleanParameter duplicate) {
        this.duplicate = duplicate;
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

    public ImageJDataImporter getInstance() {
        return JIPipe.getImageJAdapters().getImporterById(getId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImageJDataImportOperationRef that = (ImageJDataImportOperationRef) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(duplicate, that.duplicate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, duplicate);
    }

    /**
     * Overrides parameters if enabled
     *
     * @param parameters the parameters
     */
    public void configure(ImageJImportParameters parameters) {
        if (name.isEnabled()) {
            parameters.setName(name.getContent());
        }
        if (duplicate.isEnabled()) {
            parameters.setDuplicate(duplicate.getContent());
        }
    }
}
