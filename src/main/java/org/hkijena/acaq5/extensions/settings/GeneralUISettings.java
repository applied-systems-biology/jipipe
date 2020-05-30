package org.hkijena.acaq5.extensions.settings;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;

/**
 * All settings for {@link org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphEditorUI}
 */
public class GeneralUISettings implements ACAQParameterCollection {

    public static String ID = "general-ui";

    private EventBus eventBus = new EventBus();
    private boolean showIntroduction = true;
    private LookAndFeel lookAndFeel = LookAndFeel.Metal;

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @ACAQDocumentation(name = "Show introduction on startup",
            description = "If enabled, a tab containing a short introduction is shown when a new window is opened.")
    @ACAQParameter("show-introduction")
    public boolean isShowIntroduction() {
        return showIntroduction;
    }

    @ACAQParameter("show-introduction")
    public void setShowIntroduction(boolean showIntroduction) {
        this.showIntroduction = showIntroduction;
        getEventBus().post(new ParameterChangedEvent(this, "show-introduction"));
    }

    @ACAQDocumentation(name = "Theme", description = "Allows you to select a theme (Restart ImageJ to apply changes)")
    @ACAQParameter("look-and-feel")
    public LookAndFeel getLookAndFeel() {
        return lookAndFeel;
    }

    @ACAQParameter("look-and-feel")
    public void setLookAndFeel(LookAndFeel lookAndFeel) {
        this.lookAndFeel = lookAndFeel;
        getEventBus().post(new ParameterChangedEvent(this, "look-and-feel"));
    }

    public static GeneralUISettings getInstance() {
        return ACAQDefaultRegistry.getInstance().getSettingsRegistry().getSettings(ID, GeneralUISettings.class);
    }

    /**
     * Available designs
     */
    public enum LookAndFeel {
        Metal,
        FlatIntelliJLaf
    }
}
