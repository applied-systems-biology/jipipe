package org.hkijena.jipipe.api.compat;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;

/**
 * Describes how data should be imported into ImageJ
 */
public class ImageJImportParameters implements JIPipeParameterCollection {
    private final EventBus eventBus = new EventBus();
    private String name;
    private boolean duplicate;

    public ImageJImportParameters() {
    }

    public ImageJImportParameters(String name) {
        this.name = name;
    }

    public ImageJImportParameters(ImageJImportParameters other) {
        this.name = other.name;
        this.duplicate = other.duplicate;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @JIPipeDocumentation(name = "Name", description = "The name associated to the imported data")
    @JIPipeParameter("name")
    @JsonGetter("name")
    public String getName() {
        return name;
    }

    @JsonSetter("name")
    @JIPipeParameter("name")
    public void setName(String name) {
        this.name = name;
    }

    @JIPipeDocumentation(name = "Duplicate data", description = "If enabled, a duplicate is imported if possible")
    @JIPipeParameter("duplicate")
    public boolean isDuplicate() {
        return duplicate;
    }

    @JIPipeParameter("duplicate")
    public void setDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
    }

    public void copyTo(ImageJImportParameters other) {
        other.name = this.name;
        other.duplicate = this.duplicate;
    }

    @Override
    public String toString() {
        return "ImageJImportParameters{" +
                "name='" + name + '\'' +
                ", duplicate=" + duplicate +
                '}';
    }
}
