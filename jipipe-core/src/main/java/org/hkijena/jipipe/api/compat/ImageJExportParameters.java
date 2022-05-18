package org.hkijena.jipipe.api.compat;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;

/**
 * Describes how data should be exported into ImageJ
 */
public class ImageJExportParameters implements JIPipeParameterCollection {
    private final EventBus eventBus = new EventBus();
    private boolean activate;
    private boolean noWindows;
    private boolean append;
    private boolean duplicate;
    private String name;

    public ImageJExportParameters() {
    }

    public ImageJExportParameters(boolean activate, boolean noWindows, boolean append, String name) {
        this.activate = activate;
        this.noWindows = noWindows;
        this.append = append;
        this.name = name;
    }

    public ImageJExportParameters(ImageJExportParameters other) {
        this.activate = other.activate;
        this.noWindows = other.noWindows;
        this.append = other.append;
        this.name = other.name;
        this.duplicate = other.duplicate;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @JIPipeDocumentation(name = "Activate", description = "If enabled, the associated window(s) are put into the foreground")
    @JIPipeParameter("activate")
    @JsonGetter("activate")
    public boolean isActivate() {
        return activate;
    }

    @JIPipeParameter("activate")
    @JsonSetter("activate")
    public void setActivate(boolean activate) {
        this.activate = activate;
    }

    @JIPipeDocumentation(name = "Avoid creating windows", description = "If enabled, no windows should be created")
    @JIPipeParameter("no-windows")
    @JsonGetter("no-windows")
    public boolean isNoWindows() {
        return noWindows;
    }

    @JIPipeParameter("no-windows")
    @JsonSetter("no-windows")
    public void setNoWindows(boolean noWindows) {
        this.noWindows = noWindows;
    }

    @JIPipeDocumentation(name = "Name", description = "The name associated to this data")
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

    @JIPipeDocumentation(name = "Append data", description = "If enabled, data is appended if possible")
    @JIPipeParameter("append")
    @JsonGetter("append")
    public boolean isAppend() {
        return append;
    }

    @JIPipeParameter("append")
    @JsonSetter("append")
    public void setAppend(boolean append) {
        this.append = append;
    }

    @JIPipeDocumentation(name = "Duplicate data", description = "If enabled, a duplicate is exported if possible")
    @JIPipeParameter("duplicate")
    public boolean isDuplicate() {
        return duplicate;
    }

    @JIPipeParameter("duplicate")
    public void setDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
    }

    public void copyTo(ImageJExportParameters other) {
        other.name = this.name;
        other.activate = this.activate;
        other.append = this.append;
        other.noWindows = this.noWindows;
        other.duplicate = this.duplicate;
    }

    @Override
    public String toString() {
        return "ImageJExportParameters{" +
                "activate=" + activate +
                ", noWindows=" + noWindows +
                ", append=" + append +
                ", duplicate=" + duplicate +
                ", name='" + name + '\'' +
                '}';
    }
}
