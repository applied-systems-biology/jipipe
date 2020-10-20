package org.hkijena.jipipe.extensions.settings;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;

public class GeneralDataSettings implements JIPipeParameterCollection {
    public static String ID = "general-data";
    private final EventBus eventBus = new EventBus();

    private boolean autoSaveLastImporter = true;
    private boolean autoSaveLastDisplay = true;

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @JIPipeDocumentation(name = "Remember last results data importer for type",
            description = "If enabled, JIPipe will remember the last used results data importer as default.")
    @JIPipeParameter("auto-save-last-importer")
    public boolean isAutoSaveLastImporter() {
        return autoSaveLastImporter;
    }

    @JIPipeParameter("auto-save-last-importer")
    public void setAutoSaveLastImporter(boolean autoSaveLastImporter) {
        this.autoSaveLastImporter = autoSaveLastImporter;
    }

    @JIPipeDocumentation(name = "Remember last cache display for type",
    description = "If enabled, JIPipe will remember the last used cache display method as default.")
    @JIPipeParameter("auto-save-last-display")
    public boolean isAutoSaveLastDisplay() {
        return autoSaveLastDisplay;
    }

    @JIPipeParameter("auto-save-last-display")
    public void setAutoSaveLastDisplay(boolean autoSaveLastDisplay) {
        this.autoSaveLastDisplay = autoSaveLastDisplay;
    }

    public static GeneralDataSettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, GeneralDataSettings.class);
    }
}
