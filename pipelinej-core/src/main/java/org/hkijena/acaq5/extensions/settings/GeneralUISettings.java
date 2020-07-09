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
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;
import org.hkijena.acaq5.ui.grapheditor.ACAQGraphEditorUI;

/**
 * All settings for {@link ACAQGraphEditorUI}
 */
public class GeneralUISettings implements ACAQParameterCollection {

    public static String ID = "general-ui";

    private EventBus eventBus = new EventBus();
    private boolean showIntroduction = true;
    private boolean showProjectInfo = true;
    private LookAndFeel lookAndFeel = LookAndFeel.Metal;
    private boolean showParameterSearchBar = true;
    private boolean neverAskOnClosingTabs = false;
    private boolean validateOnSave = true;
    private boolean projectInfoGeneratesPreview = true;

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

    }

    @ACAQDocumentation(name = "Theme", description = "Allows you to select a theme (Restart ImageJ to apply changes)")
    @ACAQParameter("look-and-feel")
    public LookAndFeel getLookAndFeel() {
        return lookAndFeel;
    }

    @ACAQParameter("look-and-feel")
    public void setLookAndFeel(LookAndFeel lookAndFeel) {
        this.lookAndFeel = lookAndFeel;

    }

    @ACAQDocumentation(name = "Show parameter search bar", description = "If enabled, you can search parameters (Re-open parameters to apply changes)")
    @ACAQParameter("show-parameter-search-bar")
    public boolean isShowParameterSearchBar() {
        return showParameterSearchBar;
    }

    @ACAQParameter("show-parameter-search-bar")
    public void setShowParameterSearchBar(boolean showParameterSearchBar) {
        this.showParameterSearchBar = showParameterSearchBar;

    }

    @ACAQDocumentation(name = "Never ask on closing tabs", description = "If enabled, you do not need to confirm when closing tabs like the quick run, plots, and results.")
    @ACAQParameter("never-ask-on-closing-tabs")
    public boolean isNeverAskOnClosingTabs() {
        return neverAskOnClosingTabs;
    }

    @ACAQParameter("never-ask-on-closing-tabs")
    public void setNeverAskOnClosingTabs(boolean neverAskOnClosingTabs) {
        this.neverAskOnClosingTabs = neverAskOnClosingTabs;

    }

    @ACAQDocumentation(name = "Validate project on save", description = "If enabled, the whole project is validated on saving and a report is shown if an issue was found.")
    @ACAQParameter("validate-on-save")
    public boolean isValidateOnSave() {
        return validateOnSave;
    }

    @ACAQParameter("validate-on-save")
    public void setValidateOnSave(boolean validateOnSave) {
        this.validateOnSave = validateOnSave;

    }

    @ACAQDocumentation(name = "Show info on opening a project", description = "If enabled, show the project info screen on opening a project.")
    @ACAQParameter("show-project-info")
    public boolean isShowProjectInfo() {
        return showProjectInfo;
    }

    @ACAQParameter("show-project-info")
    public void setShowProjectInfo(boolean showProjectInfo) {
        this.showProjectInfo = showProjectInfo;
    }

    @ACAQDocumentation(name = "Project overview generates graph preview", description = "If enabled, the 'Project overview' tab generates a preview of the current pipeline.")
    @ACAQParameter("project-info-generates-preview")
    public boolean isProjectInfoGeneratesPreview() {
        return projectInfoGeneratesPreview;
    }

    @ACAQParameter("project-info-generates-preview")
    public void setProjectInfoGeneratesPreview(boolean projectInfoGeneratesPreview) {
        this.projectInfoGeneratesPreview = projectInfoGeneratesPreview;
    }

    public static GeneralUISettings getInstance() {
        return ACAQDefaultRegistry.getInstance().getSettingsRegistry().getSettings(ID, GeneralUISettings.class);
    }

    /**
     * Available designs
     */
    public enum LookAndFeel {
        Metal
    }
}
