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

package org.hkijena.jipipe.plugins.settings.application;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.layout.JIPipepGraphAutoLayoutMethod;
import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.pairs.StringAndStringPairParameterList;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;

import javax.swing.*;

/**
 * Where some unrelated miscellaneous presets are stored
 */
public class JIPipePresetsApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {

    public static String ID = "org.hkijena.jipipe:presets";
    private StringList pinnedNodes = new StringList();
    private String userPaletteColorsForegroundBackgroundSolid = "";
    private String userPaletteColorsForegroundBackgroundAlpha = "";
    private String userPaletteColorsForegroundSolid = "";
    private String userPaletteColorsForegroundAlpha = "";
    private final DockLayoutSettings dockLayoutSettings = new DockLayoutSettings();

    public JIPipePresetsApplicationSettings() {
        pinnedNodes.add("create-node-custom:jipipe:graph-compartment");
    }

    @SetJIPipeDocumentation(name = "UI Layout", description = "Settings related to the UI layout")
    @JIPipeParameter("dock-layout")
    public DockLayoutSettings getDockLayoutSettings() {
        return dockLayoutSettings;
    }

    @SetJIPipeDocumentation(name = "Pinned nodes", description = "List of pinned node database Ids")
    @JIPipeParameter("pinned-nodes")
    @ListParameterSettings(withScrollBar = true)
    @StringParameterSettings(monospace = true, visible = false)
    public StringList getPinnedNodes() {
        return pinnedNodes;
    }

    @JIPipeParameter("pinned-nodes")
    public void setPinnedNodes(StringList pinnedNodes) {
        this.pinnedNodes = pinnedNodes;
    }

    @SetJIPipeDocumentation(name = "Custom colors (fg+alpha)", description = "Custom colors")
    @JIPipeParameter("user-palette-colors-fg-a")
    @StringParameterSettings(monospace = true, visible = false)
    public String getUserPaletteColorsForegroundAlpha() {
        return userPaletteColorsForegroundAlpha;
    }

    @JIPipeParameter("user-palette-colors-fg-a")
    public void setUserPaletteColorsForegroundAlpha(String userPaletteColorsForegroundAlpha) {
        this.userPaletteColorsForegroundAlpha = userPaletteColorsForegroundAlpha;
    }

    @SetJIPipeDocumentation(name = "Custom colors (fg)", description = "Custom colors")
    @JIPipeParameter("user-palette-colors-fg")
    @StringParameterSettings(monospace = true, visible = false)
    public String getUserPaletteColorsForegroundSolid() {
        return userPaletteColorsForegroundSolid;
    }

    @JIPipeParameter("user-palette-colors-fg")
    public void setUserPaletteColorsForegroundSolid(String userPaletteColorsForegroundSolid) {
        this.userPaletteColorsForegroundSolid = userPaletteColorsForegroundSolid;
    }

    @SetJIPipeDocumentation(name = "Custom colors (fg+bg+alpha)", description = "Custom colors")
    @JIPipeParameter("user-palette-colors-fg-bg-a")
    @StringParameterSettings(monospace = true, visible = false)
    public String getUserPaletteColorsForegroundBackgroundAlpha() {
        return userPaletteColorsForegroundBackgroundAlpha;
    }

    @JIPipeParameter("user-palette-colors-fg-bg-a")
    public void setUserPaletteColorsForegroundBackgroundAlpha(String userPaletteColorsForegroundBackgroundAlpha) {
        this.userPaletteColorsForegroundBackgroundAlpha = userPaletteColorsForegroundBackgroundAlpha;
    }

    @SetJIPipeDocumentation(name = "Custom colors (fg+bg)", description = "Custom colors")
    @JIPipeParameter("user-palette-colors-fg-bg")
    @StringParameterSettings(monospace = true, visible = false)
    public String getUserPaletteColorsForegroundBackgroundSolid() {
        return userPaletteColorsForegroundBackgroundSolid;
    }

    @JIPipeParameter("user-palette-colors-fg-bg")
    public void setUserPaletteColorsForegroundBackgroundSolid(String userPaletteColorsForegroundBackgroundSolid) {
        this.userPaletteColorsForegroundBackgroundSolid = userPaletteColorsForegroundBackgroundSolid;
    }

    public static JIPipePresetsApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, JIPipePresetsApplicationSettings.class);
    }

    @Override
    public JIPipeDefaultApplicationSettingsSheetCategory getDefaultCategory() {
        return JIPipeDefaultApplicationSettingsSheetCategory.Miscellaneous;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Icon getIcon() {
        return JIPipe.RESOURCES.getIcon16("actions/format-text-code.png");
    }

    @Override
    public String getName() {
        return "Presets (internal)";
    }

    @Override
    public String getDescription() {
        return "Internally stored values";
    }

    public static class DockLayoutSettings extends AbstractJIPipeParameterCollection {
        private String pipelineEditorDockLayout = "";
        private String compartmentsEditorDockLayout = "";
        private StringAndStringPairParameterList pipelineEditorDockLayoutTemplates = new StringAndStringPairParameterList();
        private StringAndStringPairParameterList compartmentsEditorDockLayoutTemplates = new StringAndStringPairParameterList();
        private boolean showToolbarLabels = true;

        @SetJIPipeDocumentation(name = "Show toolbar labels", description = "Shows labels for the dock items")
        @JIPipeParameter("show-toolbar-labels")
        public boolean isShowToolbarLabels() {
            return showToolbarLabels;
        }

        @JIPipeParameter("show-toolbar-labels")
        public void setShowToolbarLabels(boolean showToolbarLabels) {
            this.showToolbarLabels = showToolbarLabels;
        }

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

        @SetJIPipeDocumentation(name = "UI layout templates (pipeline editor)", description = "Contains the layout templates for the pipeline editor. Please do not edit the values manually.")
        @JIPipeParameter("pipeline-editor-dock-layout-templates")
        @StringParameterSettings(monospace = true, multiline = true, visible = false)
        public StringAndStringPairParameterList getPipelineEditorDockLayoutTemplates() {
            return pipelineEditorDockLayoutTemplates;
        }

        @JIPipeParameter("pipeline-editor-dock-layout-templates")
        public void setPipelineEditorDockLayoutTemplates(StringAndStringPairParameterList pipelineEditorDockLayoutTemplates) {
            this.pipelineEditorDockLayoutTemplates = pipelineEditorDockLayoutTemplates;
        }

        @SetJIPipeDocumentation(name = "UI layout templates (compartment editor)", description = "Contains the layout templates for the compartments editor. Please do not edit the values manually.")
        @JIPipeParameter("compartments-editor-dock-layout-templates")
        @StringParameterSettings(monospace = true, multiline = true, visible = false)
        public StringAndStringPairParameterList getCompartmentsEditorDockLayoutTemplates() {
            return compartmentsEditorDockLayoutTemplates;
        }

        @JIPipeParameter("compartments-editor-dock-layout-templates")
        public void setCompartmentsEditorDockLayoutTemplates(StringAndStringPairParameterList compartmentsEditorDockLayoutTemplates) {
            this.compartmentsEditorDockLayoutTemplates = compartmentsEditorDockLayoutTemplates;
        }
    }
}
