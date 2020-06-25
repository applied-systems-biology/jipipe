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

package org.hkijena.acaq5.extensions.settings;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphCanvasUI;

/**
 * All settings for {@link org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphEditorUI}
 */
public class GraphEditorUISettings implements ACAQParameterCollection {

    public static String ID = "graph-editor-ui";

    private EventBus eventBus = new EventBus();
    private ACAQAlgorithmGraphCanvasUI.ViewMode defaultViewMode = ACAQAlgorithmGraphCanvasUI.ViewMode.Vertical;
    private boolean switchPanningDirection = false;
    private boolean enableLayoutHelper = true;

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @ACAQDocumentation(name = "Default view mode", description = "Determines how the graph is displayed.")
    @ACAQParameter("default-view-mode")
    public ACAQAlgorithmGraphCanvasUI.ViewMode getDefaultViewMode() {
        return defaultViewMode;
    }

    @ACAQParameter("default-view-mode")
    public void setDefaultViewMode(ACAQAlgorithmGraphCanvasUI.ViewMode defaultViewMode) {
        this.defaultViewMode = defaultViewMode;
        getEventBus().post(new ParameterChangedEvent(this, "default-view-mode"));
    }

    @ACAQDocumentation(name = "Switch panning direction by default",
            description = "Changes the direction how panning (middle mouse button) affects the view.")
    @ACAQParameter("switch-panning-direction")
    public boolean isSwitchPanningDirection() {
        return switchPanningDirection;
    }

    @ACAQParameter("switch-panning-direction")
    public void setSwitchPanningDirection(boolean switchPanningDirection) {
        this.switchPanningDirection = switchPanningDirection;
        getEventBus().post(new ParameterChangedEvent(this, "switch-panning-direction"));
    }

    @ACAQDocumentation(name = "Enable layout helper by default",
            description = "Auto-layout layout on making data slot connections")
    @ACAQParameter("enable-layout-helper")
    public boolean isEnableLayoutHelper() {
        return enableLayoutHelper;
    }

    @ACAQParameter("enable-layout-helper")
    public void setEnableLayoutHelper(boolean enableLayoutHelper) {
        this.enableLayoutHelper = enableLayoutHelper;
        getEventBus().post(new ParameterChangedEvent(this, "enable-layout-helper"));
    }

    public static GraphEditorUISettings getInstance() {
        return ACAQDefaultRegistry.getInstance().getSettingsRegistry().getSettings(ID, GraphEditorUISettings.class);
    }
}
