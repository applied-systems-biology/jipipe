package org.hkijena.acaq5.extensions.settings;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;

/**
 * Settings related to how algorithms are executed
 */
public class RuntimeSettings implements ACAQParameterCollection {
    public static final String ID = "runtime";

    private EventBus eventBus = new EventBus();
    private boolean allowSkipAlgorithmsWithoutInput = true;

    /**
     * Creates a new instance
     */
    public RuntimeSettings() {
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @ACAQDocumentation(name = "Automatically skip algorithms without input", description = "If enabled, algorithms and their dependents without " +
            "input are silently ignored. If disabled, a project is not considered valid if such conditions happen.")
    @ACAQParameter("allow-skip-algorithms-without-input")
    public boolean isAllowSkipAlgorithmsWithoutInput() {
        return allowSkipAlgorithmsWithoutInput;
    }

    @ACAQParameter("allow-skip-algorithms-without-input")
    public void setAllowSkipAlgorithmsWithoutInput(boolean allowSkipAlgorithmsWithoutInput) {
        this.allowSkipAlgorithmsWithoutInput = allowSkipAlgorithmsWithoutInput;
        eventBus.post(new ParameterChangedEvent(this, "allow-skip-algorithms-without-input"));
    }

    public static RuntimeSettings getInstance() {
        return ACAQDefaultRegistry.getInstance().getSettingsRegistry().getSettings(ID, RuntimeSettings.class);
    }
}
