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

package org.hkijena.jipipe.extensions.settings;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphEditorUI;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphViewMode;
import org.hkijena.jipipe.ui.grapheditor.layout.GraphAutoLayout;

/**
 * All settings for {@link JIPipeGraphEditorUI}
 */
public class GraphEditorUISettings implements JIPipeParameterCollection {

    public static String ID = "graph-editor-ui";

    private EventBus eventBus = new EventBus();
    private JIPipeGraphViewMode defaultViewMode = JIPipeGraphViewMode.Vertical;
    private GraphAutoLayout autoLayout = GraphAutoLayout.MST;
    private boolean switchPanningDirection = false;
    private boolean enableLayoutHelper = true;
    private boolean askOnDeleteNode = true;
    private boolean askOnDeleteCompartment = true;
    private boolean askOnDeleteParameter = true;
    private boolean drawOutsideEdges = true;

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @JIPipeDocumentation(name = "Default view mode", description = "Determines how the graph is displayed.")
    @JIPipeParameter("default-view-mode")
    public JIPipeGraphViewMode getDefaultViewMode() {
        return defaultViewMode;
    }

    @JIPipeParameter("default-view-mode")
    public void setDefaultViewMode(JIPipeGraphViewMode defaultViewMode) {
        this.defaultViewMode = defaultViewMode;

    }

    @JIPipeDocumentation(name = "Switch panning direction by default",
            description = "Changes the direction how panning (middle mouse button) affects the view.")
    @JIPipeParameter("switch-panning-direction")
    public boolean isSwitchPanningDirection() {
        return switchPanningDirection;
    }

    @JIPipeParameter("switch-panning-direction")
    public void setSwitchPanningDirection(boolean switchPanningDirection) {
        this.switchPanningDirection = switchPanningDirection;

    }

    @JIPipeDocumentation(name = "Enable layout helper by default",
            description = "Auto-layout layout on making data slot connections")
    @JIPipeParameter("enable-layout-helper")
    public boolean isEnableLayoutHelper() {
        return enableLayoutHelper;
    }

    @JIPipeParameter("enable-layout-helper")
    public void setEnableLayoutHelper(boolean enableLayoutHelper) {
        this.enableLayoutHelper = enableLayoutHelper;

    }

    @JIPipeDocumentation(name = "Auto-layout method",
            description = "Determines which method is used to applly auto-layout.")
    @JIPipeParameter("auto-layout-method")
    public GraphAutoLayout getAutoLayout() {
        return autoLayout;
    }

    @JIPipeParameter("auto-layout-method")
    public void setAutoLayout(GraphAutoLayout autoLayout) {
        this.autoLayout = autoLayout;
    }

    @JIPipeDocumentation(name = "Ask on deleting algorithms", description = "If enabled, users must confirm when an algorithm node is deleted")
    @JIPipeParameter("ask-on-delete-node")
    public boolean isAskOnDeleteNode() {
        return askOnDeleteNode;
    }

    @JIPipeParameter("ask-on-delete-node")
    public void setAskOnDeleteNode(boolean askOnDeleteNode) {
        this.askOnDeleteNode = askOnDeleteNode;
    }

    @JIPipeDocumentation(name = "Ask on deleting compartments", description = "If enabled, users must confirm when a graph compartment is deleted")
    @JIPipeParameter("ask-on-delete-compartment")
    public boolean isAskOnDeleteCompartment() {
        return askOnDeleteCompartment;
    }

    @JIPipeParameter("ask-on-delete-compartment")
    public void setAskOnDeleteCompartment(boolean askOnDeleteCompartment) {
        this.askOnDeleteCompartment = askOnDeleteCompartment;
    }

    @JIPipeDocumentation(name = "Ask on deleting parameters", description = "If enabled, users must confirm when a parameter is deleted")
    @JIPipeParameter("ask-on-delete-parameter")
    public boolean isAskOnDeleteParameter() {
        return askOnDeleteParameter;
    }

    @JIPipeParameter("ask-on-delete-parameter")
    public void setAskOnDeleteParameter(boolean askOnDeleteParameter) {
        this.askOnDeleteParameter = askOnDeleteParameter;
    }

    @JIPipeDocumentation(name = "Draw inter-compartment edges", description = "If enabled, compartment outputs draw edges that lead outside of the graph area.")
    @JIPipeParameter("draw-outside-edges")
    public boolean isDrawOutsideEdges() {
        return drawOutsideEdges;
    }

    @JIPipeParameter("draw-outside-edges")
    public void setDrawOutsideEdges(boolean drawOutsideEdges) {
        this.drawOutsideEdges = drawOutsideEdges;
    }


    public static GraphEditorUISettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, GraphEditorUISettings.class);
    }
}
