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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphEditorUI;
import org.hkijena.jipipe.ui.theme.JIPipeUITheme;

/**
 * All settings for {@link JIPipeGraphEditorUI}
 */
public class GeneralUISettings extends AbstractJIPipeParameterCollection {

    public static String ID = "general-ui";
    private boolean showIntroduction = true;
    private boolean showProjectInfo = true;
    private boolean showParameterSearchBar = true;
    private boolean neverAskOnClosingTabs = false;
    private boolean validateOnSave = false;
    private boolean addContextActionsToContextMenu = true;
    private boolean maximizeWindows = true;
    private boolean openDataWindowsAlwaysOnTop = true;
    private boolean openUtilityWindowsAlwaysOnTop = true;
    private boolean allowDefaultCollapsedParameters = true;
    private JIPipeUITheme theme = JIPipeUITheme.ModernLight;

    public static GeneralUISettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, GeneralUISettings.class);
    }

    @SetJIPipeDocumentation(name = "Show introduction on startup",
            description = "If enabled, a tab containing a short introduction is shown when a new window is opened.")
    @JIPipeParameter("show-introduction")
    public boolean isShowIntroduction() {
        return showIntroduction;
    }

    @JIPipeParameter("show-introduction")
    public void setShowIntroduction(boolean showIntroduction) {
        this.showIntroduction = showIntroduction;
    }

    @SetJIPipeDocumentation(name = "Show parameter search bar", description = "If enabled, you can search parameters (Re-open parameters to apply changes)")
    @JIPipeParameter("show-parameter-search-bar")
    public boolean isShowParameterSearchBar() {
        return showParameterSearchBar;
    }

    @JIPipeParameter("show-parameter-search-bar")
    public void setShowParameterSearchBar(boolean showParameterSearchBar) {
        this.showParameterSearchBar = showParameterSearchBar;

    }

    @SetJIPipeDocumentation(name = "Never ask on closing tabs", description = "If enabled, you do not need to confirm when closing tabs like the quick run, plots, and results.")
    @JIPipeParameter("never-ask-on-closing-tabs")
    public boolean isNeverAskOnClosingTabs() {
        return neverAskOnClosingTabs;
    }

    @JIPipeParameter("never-ask-on-closing-tabs")
    public void setNeverAskOnClosingTabs(boolean neverAskOnClosingTabs) {
        this.neverAskOnClosingTabs = neverAskOnClosingTabs;

    }

    @SetJIPipeDocumentation(name = "Validate project on save", description = "If enabled, the whole project is validated on saving and a report is shown if an issue was found.")
    @JIPipeParameter("validate-on-save")
    public boolean isValidateOnSave() {
        return validateOnSave;
    }

    @JIPipeParameter("validate-on-save")
    public void setValidateOnSave(boolean validateOnSave) {
        this.validateOnSave = validateOnSave;

    }

    @SetJIPipeDocumentation(name = "Show info on opening a project", description = "If enabled, show the project info screen on opening a project.")
    @JIPipeParameter("show-project-info")
    public boolean isShowProjectInfo() {
        return showProjectInfo;
    }

    @JIPipeParameter("show-project-info")
    public void setShowProjectInfo(boolean showProjectInfo) {
        this.showProjectInfo = showProjectInfo;
    }

    @SetJIPipeDocumentation(name = "Node context actions appear in the context menu", description = "If enabled, node-specific context actions (e.g. loading example data) are added into the node's context menu.")
    @JIPipeParameter("add-context-actions-to-context-menu")
    public boolean isAddContextActionsToContextMenu() {
        return addContextActionsToContextMenu;
    }

    @JIPipeParameter("add-context-actions-to-context-menu")
    public void setAddContextActionsToContextMenu(boolean addContextActionsToContextMenu) {
        this.addContextActionsToContextMenu = addContextActionsToContextMenu;
    }

    @SetJIPipeDocumentation(name = "Maximize windows", description = "If enabled, the JIPipe main and extension editor windows are maximized on opening.")
    @JIPipeParameter("maximize-windows")
    public boolean isMaximizeWindows() {
        return maximizeWindows;
    }

    @JIPipeParameter("maximize-windows")
    public void setMaximizeWindows(boolean maximizeWindows) {
        this.maximizeWindows = maximizeWindows;
    }

    @SetJIPipeDocumentation(name = "Theme", description = "The theme that is used for the user interface. Requires a restart to take effect.")
    @JIPipeParameter("theme")
    public JIPipeUITheme getTheme() {
        return theme;
    }

    @JIPipeParameter("theme")
    public void setTheme(JIPipeUITheme theme) {
        this.theme = theme;
    }

    @SetJIPipeDocumentation(name = "Open data windows always on top", description = "If enabled, data that is opened in new windows will have the window always on top of other windows by default. " +
            "Please note that this setting only affects data displays if they support this feature.")
    @JIPipeParameter("open-data-windows-always-on-top")
    public boolean isOpenDataWindowsAlwaysOnTop() {
        return openDataWindowsAlwaysOnTop;
    }

    @JIPipeParameter("open-data-windows-always-on-top")
    public void setOpenDataWindowsAlwaysOnTop(boolean openDataWindowsAlwaysOnTop) {
        this.openDataWindowsAlwaysOnTop = openDataWindowsAlwaysOnTop;
    }

    @SetJIPipeDocumentation(name = "Open utility windows always on top", description = "If enabled, some utility windows will be always displayed on top of other windows by default.")
    @JIPipeParameter("open-utility-windows-always-on-top")
    public boolean isOpenUtilityWindowsAlwaysOnTop() {
        return openUtilityWindowsAlwaysOnTop;
    }

    @JIPipeParameter("open-utility-windows-always-on-top")
    public void setOpenUtilityWindowsAlwaysOnTop(boolean openUtilityWindowsAlwaysOnTop) {
        this.openUtilityWindowsAlwaysOnTop = openUtilityWindowsAlwaysOnTop;
    }

    @SetJIPipeDocumentation(name = "Allow default collapsed parameters", description = "If enabled, nodes can collapse parameter groups by default")
    @JIPipeParameter("allow-default-collapsed-parameters")
    public boolean isAllowDefaultCollapsedParameters() {
        return allowDefaultCollapsedParameters;
    }

    @JIPipeParameter("allow-default-collapsed-parameters")
    public void setAllowDefaultCollapsedParameters(boolean allowDefaultCollapsedParameters) {
        this.allowDefaultCollapsedParameters = allowDefaultCollapsedParameters;
    }
}
