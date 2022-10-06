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
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphViewMode;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphEditorUI;
import org.hkijena.jipipe.ui.grapheditor.general.layout.GraphAutoLayout;

/**
 * All settings for {@link JIPipeGraphEditorUI}
 */
public class GraphEditorUISettings implements JIPipeParameterCollection {

    public static String ID = "graph-editor-ui";

    private final EventBus eventBus = new EventBus();
    private final SearchSettings searchSettings = new SearchSettings();
    private final AlgorithmFinderSettings algorithmFinderSettings = new AlgorithmFinderSettings();
    private JIPipeGraphViewMode defaultViewMode = JIPipeGraphViewMode.VerticalCompact;
    private GraphAutoLayout autoLayout = GraphAutoLayout.MST;
    private boolean switchPanningDirection = false;
    private boolean enableLayoutHelper = true;
    private boolean askOnDeleteNode = true;
    private boolean askOnDeleteCompartment = true;
    private boolean askOnDeleteParameter = true;
    private boolean drawOutsideEdges = true;
    private boolean notifyInvalidDragAndDrop = true;
    private boolean colorSelectedNodeEdges = true;
    private boolean autoLayoutMovesOtherNodes = false;
    private boolean showRunNodeButton = true;
    private boolean showSettingsNodeButton = false;
    private boolean accurateMiniMap = false;
    private boolean drawNodeShadows = true;
    private boolean drawImprovedEdges = true;

    private boolean drawArrowHeads = true;

    public static GraphEditorUISettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, GraphEditorUISettings.class);
    }

    @JIPipeDocumentation(name = "Algorithm finder settings", description = "Settings related to the algorithm finder")
    @JIPipeParameter("algorithm-finder-settings")
    public AlgorithmFinderSettings getAlgorithmFinderSettings() {
        return algorithmFinderSettings;
    }

    @JIPipeDocumentation(name = "Search settings", description = "Settings related to the search bar")
    @JIPipeParameter("search-settings")
    public SearchSettings getSearchSettings() {
        return searchSettings;
    }

    @JIPipeDocumentation(name = "Improve edge drawing", description = "If enabled, edges are drawn with outlines to make them easier distinguishable. " +
            "If you have issues with the performance, you should disable this setting.")
    @JIPipeParameter("draw-improved-edges")
    public boolean isDrawImprovedEdges() {
        return drawImprovedEdges;
    }

    @JIPipeParameter("draw-improved-edges")
    public void setDrawImprovedEdges(boolean drawImprovedEdges) {
        this.drawImprovedEdges = drawImprovedEdges;
    }

    @JIPipeDocumentation(name = "Draw node shadows", description = "If enabled, shadows are drawn for nodes as visual guide. " +
            "If you have issues with the performance, you should disable this setting.")
    @JIPipeParameter("draw-node-shadows")
    public boolean isDrawNodeShadows() {
        return drawNodeShadows;
    }

    @JIPipeParameter("draw-node-shadows")
    public void setDrawNodeShadows(boolean drawNodeShadows) {
        this.drawNodeShadows = drawNodeShadows;
    }

    @JIPipeDocumentation(name = "Draw arrow heads", description = "If enabled, draw arrow heads on connection targets")
    @JIPipeParameter("draw-arrow-heads")
    public boolean isDrawArrowHeads() {
        return drawArrowHeads;
    }

    @JIPipeParameter("draw-arrow-heads")
    public void setDrawArrowHeads(boolean drawArrowHeads) {
        this.drawArrowHeads = drawArrowHeads;
    }

    @JIPipeDocumentation(name = "Accurate minimap", description = "If enabled, the minimap shows a screenshot of the whole graph. " +
            "Please note that this is slower than the standard overview map. To apply this setting, you must re-open the graph or reload the project.")
    @JIPipeParameter("accurate-mini-map")
    public boolean isAccurateMiniMap() {
        return accurateMiniMap;
    }

    @JIPipeParameter("accurate-mini-map")
    public void setAccurateMiniMap(boolean accurateMiniMap) {
        this.accurateMiniMap = accurateMiniMap;
    }

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

    @JIPipeDocumentation(name = "Switch panning direction",
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

    @JIPipeDocumentation(name = "Notify users about invalid drops", description = "If enabled, a message box will be displayed if invalid data is dropped into a graph.")
    @JIPipeParameter("notify-invalid-drag-and-drop")
    public boolean isNotifyInvalidDragAndDrop() {
        return notifyInvalidDragAndDrop;
    }

    @JIPipeParameter("notify-invalid-drag-and-drop")
    public void setNotifyInvalidDragAndDrop(boolean notifyInvalidDragAndDrop) {
        this.notifyInvalidDragAndDrop = notifyInvalidDragAndDrop;
    }

    @JIPipeDocumentation(name = "Color selected node edges", description = "If enabled, node edges are assigned a unique color while a connected node is selected. " +
            "This is to distinguish them better.")
    @JIPipeParameter("color-selected-node-edges")
    public boolean isColorSelectedNodeEdges() {
        return colorSelectedNodeEdges;
    }

    @JIPipeParameter("color-selected-node-edges")
    public void setColorSelectedNodeEdges(boolean colorSelectedNodeEdges) {
        this.colorSelectedNodeEdges = colorSelectedNodeEdges;
    }

    @JIPipeDocumentation(name = "Auto layout moves other nodes", description = "If enabled, the auto layout function will move other nodes to make " +
            "space for newly inserted nodes.")
    @JIPipeParameter("auto-layout-moves-other-nodes")
    public boolean isAutoLayoutMovesOtherNodes() {
        return autoLayoutMovesOtherNodes;
    }

    @JIPipeParameter("auto-layout-moves-other-nodes")
    public void setAutoLayoutMovesOtherNodes(boolean autoLayoutMovesOtherNodes) {
        this.autoLayoutMovesOtherNodes = autoLayoutMovesOtherNodes;
    }

    @JIPipeDocumentation(name = "Show 'Run' button in node", description = "If enabled, a green 'run' button is shown in each node.")
    @JIPipeParameter("show-run-node-button")
    public boolean isShowRunNodeButton() {
        return showRunNodeButton;
    }

    @JIPipeParameter("show-run-node-button")
    public void setShowRunNodeButton(boolean showRunNodeButton) {
        this.showRunNodeButton = showRunNodeButton;
    }

    @JIPipeDocumentation(name = "Show 'Context menu' button in nodes", description = "If enabled, a wrench button is shown in each node that " +
            "opens the context menu. This might be useful for accessibility.")
    @JIPipeParameter("show-open-context-menu-button")
    public boolean isShowSettingsNodeButton() {
        return showSettingsNodeButton;
    }

    @JIPipeParameter("show-open-context-menu-button")
    public void setShowSettingsNodeButton(boolean showSettingsNodeButton) {
        this.showSettingsNodeButton = showSettingsNodeButton;
    }

    public static class AlgorithmFinderSettings implements JIPipeParameterCollection {
        private final EventBus eventBus = new EventBus();
        private boolean searchFindNewNodes = true;
        private boolean searchFindExistingNodes = true;

        @Override
        public EventBus getEventBus() {
            return eventBus;
        }

        @JIPipeDocumentation(name = "Search: Create nodes", description = "If enabled, the search will allow to create new nodes")
        @JIPipeParameter("search-find-new-nodes")
        public boolean isSearchFindNewNodes() {
            return searchFindNewNodes;
        }

        @JIPipeParameter("search-find-new-nodes")
        public void setSearchFindNewNodes(boolean searchFindNewNodes) {
            this.searchFindNewNodes = searchFindNewNodes;
        }

        @JIPipeDocumentation(name = "Search: Existing nodes", description = "If enabled, the search will allow to find existing nodes")
        @JIPipeParameter("search-find-existing-nodes")
        public boolean isSearchFindExistingNodes() {
            return searchFindExistingNodes;
        }

        @JIPipeParameter("search-find-existing-nodes")
        public void setSearchFindExistingNodes(boolean searchFindExistingNodes) {
            this.searchFindExistingNodes = searchFindExistingNodes;
        }
    }

    public static class SearchSettings implements JIPipeParameterCollection {

        private final EventBus eventBus = new EventBus();
        private boolean enableSearch = true;
        private boolean searchFindNewNodes = true;
        private boolean searchFindExistingNodes = true;

        @Override
        public EventBus getEventBus() {
            return eventBus;
        }

        @JIPipeDocumentation(name = "Enable search", description = "If enabled, a search box is shown in the menu bar. Only applies to newly opened editors.")
        @JIPipeParameter("enable-search")
        public boolean isEnableSearch() {
            return enableSearch;
        }

        @JIPipeParameter("enable-search")
        public void setEnableSearch(boolean enableSearch) {
            this.enableSearch = enableSearch;
        }

        @JIPipeDocumentation(name = "Search: Create nodes", description = "If enabled, the search will allow to create new nodes")
        @JIPipeParameter("search-find-new-nodes")
        public boolean isSearchFindNewNodes() {
            return searchFindNewNodes;
        }

        @JIPipeParameter("search-find-new-nodes")
        public void setSearchFindNewNodes(boolean searchFindNewNodes) {
            this.searchFindNewNodes = searchFindNewNodes;
        }

        @JIPipeDocumentation(name = "Search: Existing nodes", description = "If enabled, the search will allow to find existing nodes")
        @JIPipeParameter("search-find-existing-nodes")
        public boolean isSearchFindExistingNodes() {
            return searchFindExistingNodes;
        }

        @JIPipeParameter("search-find-existing-nodes")
        public void setSearchFindExistingNodes(boolean searchFindExistingNodes) {
            this.searchFindExistingNodes = searchFindExistingNodes;
        }
    }
}
