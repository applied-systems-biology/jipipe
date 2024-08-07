/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.settings;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdge;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.AbstractJIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.layout.JIPipepGraphAutoLayoutMethod;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

/**
 * All settings for {@link AbstractJIPipeDesktopGraphEditorUI}
 */
public class JIPipeGraphEditorUIApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {

    public static String ID = "org.hkijena.jipipe:graph-editor-ui";
    private final NodeSearchSettings nodeSearchSettings = new NodeSearchSettings();
    private final DockLayoutSettings dockLayoutSettings = new DockLayoutSettings();
    private JIPipepGraphAutoLayoutMethod autoLayout = JIPipepGraphAutoLayoutMethod.MST;
    private boolean switchPanningDirection = false;
    private boolean askOnDeleteNode = true;
    private boolean askOnDeleteCompartment = true;
    private boolean askOnDeleteParameter = true;
    private boolean drawOutsideEdges = true;
    private boolean notifyInvalidDragAndDrop = true;
    private boolean colorSelectedNodeEdges = true;
    private boolean autoLayoutMovesOtherNodes = false;
    private boolean drawNodeShadows = true;
    private boolean drawImprovedEdges = true;
    private boolean drawArrowHeads = true;
    private boolean layoutAfterAlgorithmFinder = true;
    private boolean layoutAfterConnect = false;
    private int autoHideEdgeDistanceThreshold = 512;
    private boolean autoMuteEdgesEnabled = true;
    private boolean drawLabelsOnHover = true;
    private boolean autoMuteBySelection = true;
    private double autoHideEdgeOverlapThreshold = 0.5;

    private boolean showToolInfo = true;

    private int toolInfoDistance = 16;

    private JIPipeGraphEdge.Visibility defaultEdgeVisibility = JIPipeGraphEdge.Visibility.Smart;

    public static JIPipeGraphEditorUIApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, JIPipeGraphEditorUIApplicationSettings.class);
    }

    @SetJIPipeDocumentation(name = "UI Layout", description = "Settings related to the UI layout")
    @JIPipeParameter("dock-layout")
    public DockLayoutSettings getDockLayoutSettings() {
        return dockLayoutSettings;
    }

    @SetJIPipeDocumentation(name = "Auto-mute edges by selection", description = "If enabled, all edges that are not part of the selected nodes are muted")
    @JIPipeParameter("auto-mute-by-selection")
    public boolean isAutoMuteBySelection() {
        return autoMuteBySelection;
    }

    @JIPipeParameter("auto-mute-by-selection")
    public void setAutoMuteBySelection(boolean autoMuteBySelection) {
        this.autoMuteBySelection = autoMuteBySelection;
    }

    @SetJIPipeDocumentation(name = "Show current tool info", description = "If enabled, show a tooltip text to the mouse cursor if a graph editor tool is active")
    @JIPipeParameter("show-tool-info")
    public boolean isShowToolInfo() {
        return showToolInfo;
    }

    @JIPipeParameter("show-tool-info")
    public void setShowToolInfo(boolean showToolInfo) {
        this.showToolInfo = showToolInfo;
    }

    @SetJIPipeDocumentation(name = "Tool info distance", description = "The distance of the current tool info to the mouse pointer")
    @JIPipeParameter("tool-info-distance")
    public int getToolInfoDistance() {
        return toolInfoDistance;
    }

    @JIPipeParameter("tool-info-distance")
    public void setToolInfoDistance(int toolInfoDistance) {
        this.toolInfoDistance = toolInfoDistance;
    }

    @SetJIPipeDocumentation(name = "Draw input labels on hovering nodes", description = "If enabled, display a label for node inputs if it is hovered by the mouse cursor")
    @JIPipeParameter("draw-labels-on-hover")
    public boolean isDrawLabelsOnHover() {
        return drawLabelsOnHover;
    }

    @JIPipeParameter("draw-labels-on-hover")
    public void setDrawLabelsOnHover(boolean drawLabelsOnHover) {
        this.drawLabelsOnHover = drawLabelsOnHover;
    }

    @SetJIPipeDocumentation(name = "Auto-mute edges", description = "Enabled/disables the automated muting of edges (drawing them as dashed lines)")
    @JIPipeParameter("auto-hide-edges-enabled")
    public boolean isAutoMuteEdgesEnabled() {
        return autoMuteEdgesEnabled;
    }

    @JIPipeParameter("auto-hide-edges-enabled")
    public void setAutoMuteEdgesEnabled(boolean autoMuteEdgesEnabled) {
        this.autoMuteEdgesEnabled = autoMuteEdgesEnabled;
    }

    @SetJIPipeDocumentation(name = "Auto-hide edges overlap threshold", description = "Determines how much overlap with an existing edge is considered as condition for hiding the longer connection")
    @JIPipeParameter("auto-hide-edge-overlap-threshold")
    public double getAutoHideEdgeOverlapThreshold() {
        return autoHideEdgeOverlapThreshold;
    }

    @JIPipeParameter("auto-hide-edge-overlap-threshold")
    public void setAutoHideEdgeOverlapThreshold(double autoHideEdgeOverlapThreshold) {
        this.autoHideEdgeOverlapThreshold = autoHideEdgeOverlapThreshold;
    }

    @SetJIPipeDocumentation(name = "Auto-hide edges distance threshold", description = "The minimum distance considered as long distance edge for the auto-hiding feature")
    @JIPipeParameter("auto-hide-edge-distance-threshold")
    public int getAutoHideEdgeDistanceThreshold() {
        return autoHideEdgeDistanceThreshold;
    }

    @JIPipeParameter("auto-hide-edge-distance-threshold")
    public void setAutoHideEdgeDistanceThreshold(int longDistanceEdgeThreshold) {
        this.autoHideEdgeDistanceThreshold = longDistanceEdgeThreshold;
    }

    @SetJIPipeDocumentation(name = "Default edge visibility", description = "Determines the default visibility of all newly created edges.")
    @JIPipeParameter("default-edge-visibility")
    public JIPipeGraphEdge.Visibility getDefaultEdgeVisibility() {
        return defaultEdgeVisibility;
    }

    @JIPipeParameter("default-edge-visibility")
    public void setDefaultEdgeVisibility(JIPipeGraphEdge.Visibility defaultEdgeVisibility) {
        this.defaultEdgeVisibility = defaultEdgeVisibility;
    }

    @SetJIPipeDocumentation(name = "Node search settings", description = "Settings related to the node search")
    @JIPipeParameter("algorithm-finder-settings")
    public NodeSearchSettings getNodeSearchSettings() {
        return nodeSearchSettings;
    }

    @SetJIPipeDocumentation(name = "Improve edge drawing", description = "If enabled, edges are drawn with outlines to make them easier distinguishable. " +
            "If you have issues with the performance, you should disable this setting.")
    @JIPipeParameter("draw-improved-edges")
    public boolean isDrawImprovedEdges() {
        return drawImprovedEdges;
    }

    @JIPipeParameter("draw-improved-edges")
    public void setDrawImprovedEdges(boolean drawImprovedEdges) {
        this.drawImprovedEdges = drawImprovedEdges;
    }

    @SetJIPipeDocumentation(name = "Draw node shadows", description = "If enabled, shadows are drawn for nodes as visual guide. " +
            "If you have issues with the performance, you should disable this setting.")
    @JIPipeParameter("draw-node-shadows")
    public boolean isDrawNodeShadows() {
        return drawNodeShadows;
    }

    @JIPipeParameter("draw-node-shadows")
    public void setDrawNodeShadows(boolean drawNodeShadows) {
        this.drawNodeShadows = drawNodeShadows;
    }

    @SetJIPipeDocumentation(name = "Draw arrow heads", description = "If enabled, draw arrow heads on connection targets")
    @JIPipeParameter("draw-arrow-heads")
    public boolean isDrawArrowHeads() {
        return drawArrowHeads;
    }

    @JIPipeParameter("draw-arrow-heads")
    public void setDrawArrowHeads(boolean drawArrowHeads) {
        this.drawArrowHeads = drawArrowHeads;
    }

    @SetJIPipeDocumentation(name = "Switch panning direction",
            description = "Changes the direction how panning (middle mouse button) affects the view.")
    @JIPipeParameter("switch-panning-direction")
    public boolean isSwitchPanningDirection() {
        return switchPanningDirection;
    }

    @JIPipeParameter("switch-panning-direction")
    public void setSwitchPanningDirection(boolean switchPanningDirection) {
        this.switchPanningDirection = switchPanningDirection;

    }

    @SetJIPipeDocumentation(name = "Layout nodes added by 'Find matching algorithm'", description = "Auto-layout nodes added by the 'Find matching algorithm' feature.")
    @JIPipeParameter("layout-after-algorithm-find")
    public boolean isLayoutAfterAlgorithmFinder() {
        return layoutAfterAlgorithmFinder;
    }

    @JIPipeParameter("layout-after-algorithm-find")
    public void setLayoutAfterAlgorithmFinder(boolean layoutAfterAlgorithmFinder) {
        this.layoutAfterAlgorithmFinder = layoutAfterAlgorithmFinder;
    }

    @SetJIPipeDocumentation(name = "Layout after connecting nodes", description = "Auto-layout the source/target node after a connection is created")
    @JIPipeParameter("layout-after-connect")
    public boolean isLayoutAfterConnect() {
        return layoutAfterConnect;
    }

    @JIPipeParameter("layout-after-connect")
    public void setLayoutAfterConnect(boolean layoutAfterConnect) {
        this.layoutAfterConnect = layoutAfterConnect;
    }

    @SetJIPipeDocumentation(name = "Auto-layout method",
            description = "Determines which method is used to applly auto-layout.")
    @JIPipeParameter("auto-layout-method")
    public JIPipepGraphAutoLayoutMethod getAutoLayout() {
        return autoLayout;
    }

    @JIPipeParameter("auto-layout-method")
    public void setAutoLayout(JIPipepGraphAutoLayoutMethod autoLayout) {
        this.autoLayout = autoLayout;
    }

    @SetJIPipeDocumentation(name = "Ask on deleting algorithms", description = "If enabled, users must confirm when an algorithm node is deleted")
    @JIPipeParameter("ask-on-delete-node")
    public boolean isAskOnDeleteNode() {
        return askOnDeleteNode;
    }

    @JIPipeParameter("ask-on-delete-node")
    public void setAskOnDeleteNode(boolean askOnDeleteNode) {
        this.askOnDeleteNode = askOnDeleteNode;
    }

    @SetJIPipeDocumentation(name = "Ask on deleting compartments", description = "If enabled, users must confirm when a graph compartment is deleted")
    @JIPipeParameter("ask-on-delete-compartment")
    public boolean isAskOnDeleteCompartment() {
        return askOnDeleteCompartment;
    }

    @JIPipeParameter("ask-on-delete-compartment")
    public void setAskOnDeleteCompartment(boolean askOnDeleteCompartment) {
        this.askOnDeleteCompartment = askOnDeleteCompartment;
    }

    @SetJIPipeDocumentation(name = "Ask on deleting parameters", description = "If enabled, users must confirm when a parameter is deleted")
    @JIPipeParameter("ask-on-delete-parameter")
    public boolean isAskOnDeleteParameter() {
        return askOnDeleteParameter;
    }

    @JIPipeParameter("ask-on-delete-parameter")
    public void setAskOnDeleteParameter(boolean askOnDeleteParameter) {
        this.askOnDeleteParameter = askOnDeleteParameter;
    }

    @SetJIPipeDocumentation(name = "Draw inter-compartment edges", description = "If enabled, compartment outputs draw edges that lead outside of the graph area.")
    @JIPipeParameter("draw-outside-edges")
    public boolean isDrawOutsideEdges() {
        return drawOutsideEdges;
    }

    @JIPipeParameter("draw-outside-edges")
    public void setDrawOutsideEdges(boolean drawOutsideEdges) {
        this.drawOutsideEdges = drawOutsideEdges;
    }

    @SetJIPipeDocumentation(name = "Notify users about invalid drops", description = "If enabled, a message box will be displayed if invalid data is dropped into a graph.")
    @JIPipeParameter("notify-invalid-drag-and-drop")
    public boolean isNotifyInvalidDragAndDrop() {
        return notifyInvalidDragAndDrop;
    }

    @JIPipeParameter("notify-invalid-drag-and-drop")
    public void setNotifyInvalidDragAndDrop(boolean notifyInvalidDragAndDrop) {
        this.notifyInvalidDragAndDrop = notifyInvalidDragAndDrop;
    }

    @SetJIPipeDocumentation(name = "Color selected node edges", description = "If enabled, node edges are assigned a unique color while a connected node is selected. " +
            "This is to distinguish them better.")
    @JIPipeParameter("color-selected-node-edges")
    public boolean isColorSelectedNodeEdges() {
        return colorSelectedNodeEdges;
    }

    @JIPipeParameter("color-selected-node-edges")
    public void setColorSelectedNodeEdges(boolean colorSelectedNodeEdges) {
        this.colorSelectedNodeEdges = colorSelectedNodeEdges;
    }

    @SetJIPipeDocumentation(name = "Auto layout moves other nodes", description = "If enabled, the auto layout function will move other nodes to make " +
            "space for newly inserted nodes.")
    @JIPipeParameter("auto-layout-moves-other-nodes")
    public boolean isAutoLayoutMovesOtherNodes() {
        return autoLayoutMovesOtherNodes;
    }

    @JIPipeParameter("auto-layout-moves-other-nodes")
    public void setAutoLayoutMovesOtherNodes(boolean autoLayoutMovesOtherNodes) {
        this.autoLayoutMovesOtherNodes = autoLayoutMovesOtherNodes;
    }

    @Override
    public JIPipeDefaultApplicationSettingsSheetCategory getDefaultCategory() {
        return JIPipeDefaultApplicationSettingsSheetCategory.UI;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/distribute-graph.png");
    }

    @Override
    public String getName() {
        return "Graph editor";
    }

    @Override
    public String getDescription() {
        return "Configures the pipeline editor UI (e.g., the default view mode)";
    }

    public static class NodeSearchSettings extends AbstractJIPipeParameterCollection {
        private boolean searchFindNewNodes = true;
        private boolean searchFindExistingNodes = true;
        private boolean showDescriptions = true;

        @SetJIPipeDocumentation(name = "Search: Create nodes", description = "If enabled, the search will allow to create new nodes")
        @JIPipeParameter("search-find-new-nodes")
        public boolean isSearchFindNewNodes() {
            return searchFindNewNodes;
        }

        @JIPipeParameter("search-find-new-nodes")
        public void setSearchFindNewNodes(boolean searchFindNewNodes) {
            this.searchFindNewNodes = searchFindNewNodes;
        }

        @SetJIPipeDocumentation(name = "Search: Existing nodes", description = "If enabled, the search will allow to find existing nodes")
        @JIPipeParameter("search-find-existing-nodes")
        public boolean isSearchFindExistingNodes() {
            return searchFindExistingNodes;
        }

        @JIPipeParameter("search-find-existing-nodes")
        public void setSearchFindExistingNodes(boolean searchFindExistingNodes) {
            this.searchFindExistingNodes = searchFindExistingNodes;
        }

        @SetJIPipeDocumentation(name = "Show node descriptions", description = "If enabled, show node description")
        @JIPipeParameter("show-node-descriptions")
        public boolean isShowDescriptions() {
            return showDescriptions;
        }

        @JIPipeParameter("show-node-descriptions")
        public void setShowDescriptions(boolean showDescriptions) {
            this.showDescriptions = showDescriptions;
        }
    }

    public static class DockLayoutSettings extends AbstractJIPipeParameterCollection {
        private String pipelineEditorDockLayout = "";
        private String compartmentsEditorDockLayout = "";

        @SetJIPipeDocumentation(name = "UI layout (pipeline editor)", description = "Contains the current UI layout of the pipeline editor. Please do not edit this parameter manually.")
        @JIPipeParameter("pipeline-editor-dock-layout")
        @StringParameterSettings(monospace = true, multiline = true, visible = false)
        public String getPipelineEditorDockLayout() {
            return pipelineEditorDockLayout;
        }

        @JIPipeParameter("pipeline-editor-dock-layout")
        public void setPipelineEditorDockLayout(String pipelineEditorDockLayout) {
            this.pipelineEditorDockLayout = pipelineEditorDockLayout;
        }

        @SetJIPipeDocumentation(name = "UI layout (compartment editor)", description = "Contains the current UI layout of the compartments editor. Please do not edit this parameter manually.")
        @JIPipeParameter("compartments-editor-dock-layout")
        @StringParameterSettings(monospace = true, multiline = true, visible = false)
        public String getCompartmentsEditorDockLayout() {
            return compartmentsEditorDockLayout;
        }

        @JIPipeParameter("compartments-editor-dock-layout")
        public void setCompartmentsEditorDockLayout(String compartmentsEditorDockLayout) {
            this.compartmentsEditorDockLayout = compartmentsEditorDockLayout;
        }
    }
}
