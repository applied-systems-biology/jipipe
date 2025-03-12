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

package org.hkijena.jipipe.plugins.artifacts;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifactRepositoryReference;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifactRepositoryReferenceList;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifactRepositoryType;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalPathParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.vectors.Vector2iParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.vectors.VectorParameterSettings;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class JIPipeArtifactApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {
    public static final String ID = "org.hkijena.jipipe:artifacts";

    private boolean autoDownload = true;
    private JIPipeArtifactAccelerationPreference accelerationPreference = JIPipeArtifactAccelerationPreference.CPU;
    private Vector2iParameter accelerationPreferenceVersions = new Vector2iParameter();
    private JIPipeArtifactRepositoryReferenceList repositories = new JIPipeArtifactRepositoryReferenceList();
    private OptionalPathParameter overrideInstallationPath = new OptionalPathParameter();
    private boolean autoConfigureAccelerationOnNextStartup = true;
    private boolean showConnectionIssueBallon = true;

    public JIPipeArtifactApplicationSettings() {
        repositories.add(new JIPipeArtifactRepositoryReference("https://jipipe.hki-jena.de/nexus/", "jipipe-artifacts", JIPipeArtifactRepositoryType.SonatypeNexus));
    }

    public static JIPipeArtifactApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, JIPipeArtifactApplicationSettings.class);
    }

    @SetJIPipeDocumentation(name = "Show warning on connection issues", description = "If enabled, show a warning if repositories could not be contacted")
    @JIPipeParameter("show-connection-issue-balloon")
    public boolean isShowConnectionIssueBallon() {
        return showConnectionIssueBallon;
    }

    @JIPipeParameter("show-connection-issue-balloon")
    public void setShowConnectionIssueBallon(boolean showConnectionIssueBallon) {
        this.showConnectionIssueBallon = showConnectionIssueBallon;
    }

    @SetJIPipeDocumentation(name = "Auto-configure acceleration on next startup", description = "Attempts to automatically determine the acceleration during the next JIPipe startup.")
    @JIPipeParameter("auto-configure-acceleration-on-next-startup")
    public boolean isAutoConfigureAccelerationOnNextStartup() {
        return autoConfigureAccelerationOnNextStartup;
    }

    @JIPipeParameter("auto-configure-acceleration-on-next-startup")
    public void setAutoConfigureAccelerationOnNextStartup(boolean autoConfigureAccelerationOnNextStartup) {
        this.autoConfigureAccelerationOnNextStartup = autoConfigureAccelerationOnNextStartup;
    }

    @SetJIPipeDocumentation(name = "Acceleration mode", description = "Determines if JIPipe should prefer artifacts with a specific acceleration type. " +
            "For maximum compatibility, choose CPU (will run slowest). For Nvidia GPUs, select CUDA. For AMD GPUs select ROCm. " +
            "If no compatible artifact is found, CPU will be automatically selected.")
    @JIPipeParameter("acceleration-mode")
    public JIPipeArtifactAccelerationPreference getAccelerationPreference() {
        return accelerationPreference;
    }

    @JIPipeParameter("acceleration-mode")
    public void setAccelerationPreference(JIPipeArtifactAccelerationPreference accelerationPreference) {
        this.accelerationPreference = accelerationPreference;
    }

    @SetJIPipeDocumentation(name = "Acceleration version limits (GPU only)", description = "Determines version limits for GPU acceleration modes. " +
            "This is important for certain GPUs that will not run GPU accelerated code from older versions. " +
            "If a value is set to zero, no limit is assigned to the minimum or maximum. " +
            "Input the three-digit numeric version, for example 112 for CUDA version 11.2")
    @JIPipeParameter("acceleration-mode-version-limit")
    @VectorParameterSettings(xLabel = "Min", yLabel = "Max")
    public Vector2iParameter getAccelerationPreferenceVersions() {
        return accelerationPreferenceVersions;
    }

    @JIPipeParameter("acceleration-mode-version-limit")
    public void setAccelerationPreferenceVersions(Vector2iParameter accelerationPreferenceVersions) {
        this.accelerationPreferenceVersions = accelerationPreferenceVersions;
    }

    @SetJIPipeDocumentation(name = "Auto download", description = "If enabled, automatically download missing artifacts")
    @JIPipeParameter("auto-download")
    public boolean isAutoDownload() {
        return autoDownload;
    }

    @JIPipeParameter("auto-download")
    public void setAutoDownload(boolean autoDownload) {
        this.autoDownload = autoDownload;
    }

    @SetJIPipeDocumentation(name = "Repositories", description = "List of repositories to query")
    @JIPipeParameter("repositories")
    public JIPipeArtifactRepositoryReferenceList getRepositories() {
        return repositories;
    }

    @JIPipeParameter("repositories")
    public void setRepositories(JIPipeArtifactRepositoryReferenceList repositories) {
        this.repositories = repositories;
    }

    @SetJIPipeDocumentation(name = "Override installation path", description = "If enabled, store downloaded artifacts in a different directory. " +
            "By default, they will be stored in %APPDATA%/JIPipe/artifacts (Windows), $HOME/.local/share/JIPipe/artifacts (Linux), and $HOME/Library/Application Support/")
    @JIPipeParameter("override-installation-path")
    public OptionalPathParameter getOverrideInstallationPath() {
        return overrideInstallationPath;
    }

    @JIPipeParameter("override-installation-path")
    public void setOverrideInstallationPath(OptionalPathParameter overrideInstallationPath) {
        this.overrideInstallationPath = overrideInstallationPath;
    }

    @Override
    public JIPipeDefaultApplicationSettingsSheetCategory getDefaultCategory() {
        return JIPipeDefaultApplicationSettingsSheetCategory.General;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/run-install.png");
    }

    @Override
    public String getName() {
        return "Artifacts";
    }

    @Override
    public String getDescription() {
        return "Settings for the artifact downloader";
    }
}
