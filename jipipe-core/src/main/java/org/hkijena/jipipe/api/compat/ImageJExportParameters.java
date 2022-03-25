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
    private String name;

    public ImageJExportParameters() {
    }

    public ImageJExportParameters(boolean activate, boolean noWindows, boolean append, String name) {
        this.activate = activate;
        this.noWindows = noWindows;
        this.append = append;
        this.name = name;
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
}
