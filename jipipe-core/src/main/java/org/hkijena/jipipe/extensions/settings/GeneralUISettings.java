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
import org.hkijena.jipipe.JIPipeDefaultRegistry;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphEditorUI;

/**
 * All settings for {@link JIPipeGraphEditorUI}
 */
public class GeneralUISettings implements JIPipeParameterCollection {

    public static String ID = "general-ui";

    private EventBus eventBus = new EventBus();
    private boolean showIntroduction = true;
    private boolean showProjectInfo = true;
    private boolean showParameterSearchBar = true;
    private boolean neverAskOnClosingTabs = false;
    private boolean validateOnSave = true;
    private boolean projectInfoGeneratesPreview = true;
    private boolean forceCrossPlatformLookAndFeel = true;
    private boolean modernizeCrossPlatformLookAndFeel = true;
    private boolean generateCachePreviews = true;
    private int previewWidth = 64;
    private int previewHeight = 64;

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @JIPipeDocumentation(name = "Show introduction on startup",
            description = "If enabled, a tab containing a short introduction is shown when a new window is opened.")
    @JIPipeParameter("show-introduction")
    public boolean isShowIntroduction() {
        return showIntroduction;
    }

    @JIPipeParameter("show-introduction")
    public void setShowIntroduction(boolean showIntroduction) {
        this.showIntroduction = showIntroduction;

    }

    @JIPipeDocumentation(name = "Show parameter search bar", description = "If enabled, you can search parameters (Re-open parameters to apply changes)")
    @JIPipeParameter("show-parameter-search-bar")
    public boolean isShowParameterSearchBar() {
        return showParameterSearchBar;
    }

    @JIPipeParameter("show-parameter-search-bar")
    public void setShowParameterSearchBar(boolean showParameterSearchBar) {
        this.showParameterSearchBar = showParameterSearchBar;

    }

    @JIPipeDocumentation(name = "Never ask on closing tabs", description = "If enabled, you do not need to confirm when closing tabs like the quick run, plots, and results.")
    @JIPipeParameter("never-ask-on-closing-tabs")
    public boolean isNeverAskOnClosingTabs() {
        return neverAskOnClosingTabs;
    }

    @JIPipeParameter("never-ask-on-closing-tabs")
    public void setNeverAskOnClosingTabs(boolean neverAskOnClosingTabs) {
        this.neverAskOnClosingTabs = neverAskOnClosingTabs;

    }

    @JIPipeDocumentation(name = "Validate project on save", description = "If enabled, the whole project is validated on saving and a report is shown if an issue was found.")
    @JIPipeParameter("validate-on-save")
    public boolean isValidateOnSave() {
        return validateOnSave;
    }

    @JIPipeParameter("validate-on-save")
    public void setValidateOnSave(boolean validateOnSave) {
        this.validateOnSave = validateOnSave;

    }

    @JIPipeDocumentation(name = "Show info on opening a project", description = "If enabled, show the project info screen on opening a project.")
    @JIPipeParameter("show-project-info")
    public boolean isShowProjectInfo() {
        return showProjectInfo;
    }

    @JIPipeParameter("show-project-info")
    public void setShowProjectInfo(boolean showProjectInfo) {
        this.showProjectInfo = showProjectInfo;
    }

    @JIPipeDocumentation(name = "Project overview generates graph preview", description = "If enabled, the 'Project overview' tab generates a preview of the current pipeline.")
    @JIPipeParameter("project-info-generates-preview")
    public boolean isProjectInfoGeneratesPreview() {
        return projectInfoGeneratesPreview;
    }

    @JIPipeParameter("project-info-generates-preview")
    public void setProjectInfoGeneratesPreview(boolean projectInfoGeneratesPreview) {
        this.projectInfoGeneratesPreview = projectInfoGeneratesPreview;
    }

    @JIPipeDocumentation(name = "Force cross-platform look & feel", description = "If enabled, the design is set to the 'Metal' look & feel that " +
            "looks the same on all platforms. We recommend to keep this setting, as other designs might cause flaws in the UI. " +
            "This requires an ImageJ restart to take effect.")
    @JIPipeParameter("force-cross-platform-look-and-feel")
    public boolean isForceCrossPlatformLookAndFeel() {
        return forceCrossPlatformLookAndFeel;
    }

    @JIPipeParameter("force-cross-platform-look-and-feel")
    public void setForceCrossPlatformLookAndFeel(boolean forceCrossPlatformLookAndFeel) {
        this.forceCrossPlatformLookAndFeel = forceCrossPlatformLookAndFeel;
    }

    @JIPipeDocumentation(name = "Modern cross-platform look & feel", description = "If enabled, the 'Metal' look & feel is modernized to a flat design.")
    @JIPipeParameter("modernize-cross-platform-look-and-feel")
    public boolean isModernizeCrossPlatformLookAndFeel() {
        return modernizeCrossPlatformLookAndFeel;
    }

    @JIPipeParameter("modernize-cross-platform-look-and-feel")
    public void setModernizeCrossPlatformLookAndFeel(boolean modernizeCrossPlatformLookAndFeel) {
        this.modernizeCrossPlatformLookAndFeel = modernizeCrossPlatformLookAndFeel;
    }

    @JIPipeDocumentation(name = "Generate previews in cache browser", description = "If enabled, cached items are previewed in JIPipe")
    @JIPipeParameter("generate-cache-previews")
    public boolean isGenerateCachePreviews() {
        return generateCachePreviews;
    }

    @JIPipeParameter("generate-cache-previews")
    public void setGenerateCachePreviews(boolean generateCachePreviews) {
        this.generateCachePreviews = generateCachePreviews;
    }

    @JIPipeDocumentation(name = "Preview width", description = "The width for data previews")
    @JIPipeParameter("preview-widths")
    public int getPreviewWidth() {
        return previewWidth;
    }

    @JIPipeParameter("preview-widths")
    public void setPreviewWidth(int previewWidth) {
        this.previewWidth = previewWidth;
    }

    @JIPipeDocumentation(name = "Preview height", description = "The height for data previews")
    @JIPipeParameter("preview-height")
    public int getPreviewHeight() {
        return previewHeight;
    }

    @JIPipeParameter("preview-height")
    public void setPreviewHeight(int previewHeight) {
        this.previewHeight = previewHeight;
    }

    public static GeneralUISettings getInstance() {
        return JIPipeDefaultRegistry.getInstance().getSettingsRegistry().getSettings(ID, GeneralUISettings.class);
    }
}
