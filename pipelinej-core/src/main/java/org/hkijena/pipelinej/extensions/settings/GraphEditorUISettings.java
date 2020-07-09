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

package org.hkijena.pipelinej.extensions.settings;

import com.google.common.eventbus.EventBus;
import org.hkijena.pipelinej.ACAQDefaultRegistry;
import org.hkijena.pipelinej.api.ACAQDocumentation;
import org.hkijena.pipelinej.api.parameters.ACAQParameter;
import org.hkijena.pipelinej.api.parameters.ACAQParameterCollection;
import org.hkijena.pipelinej.ui.grapheditor.ACAQGraphCanvasUI;
import org.hkijena.pipelinej.ui.grapheditor.ACAQGraphEditorUI;
import org.hkijena.pipelinej.ui.grapheditor.layout.GraphAutoLayout;

/**
 * All settings for {@link ACAQGraphEditorUI}
 */
public class GraphEditorUISettings implements ACAQParameterCollection {

    public static String ID = "graph-editor-ui";

    private EventBus eventBus = new EventBus();
    private ACAQGraphCanvasUI.ViewMode defaultViewMode = ACAQGraphCanvasUI.ViewMode.Vertical;
    private GraphAutoLayout autoLayout = GraphAutoLayout.MST;
    private boolean switchPanningDirection = false;
    private boolean enableLayoutHelper = true;

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @ACAQDocumentation(name = "Default view mode", description = "Determines how the graph is displayed.")
    @ACAQParameter("default-view-mode")
    public ACAQGraphCanvasUI.ViewMode getDefaultViewMode() {
        return defaultViewMode;
    }

    @ACAQParameter("default-view-mode")
    public void setDefaultViewMode(ACAQGraphCanvasUI.ViewMode defaultViewMode) {
        this.defaultViewMode = defaultViewMode;

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

    }

    @ACAQDocumentation(name = "Auto-layout method",
            description = "Determines which method is used to applly auto-layout.")
    @ACAQParameter("auto-layout-method")
    public GraphAutoLayout getAutoLayout() {
        return autoLayout;
    }

    @ACAQParameter("auto-layout-method")
    public void setAutoLayout(GraphAutoLayout autoLayout) {
        this.autoLayout = autoLayout;
    }

    public static GraphEditorUISettings getInstance() {
        return ACAQDefaultRegistry.getInstance().getSettingsRegistry().getSettings(ID, GraphEditorUISettings.class);
    }
}
