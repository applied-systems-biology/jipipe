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
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalPathParameter;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class JIPipeArtifactApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {
    public static final String ID = "org.hkijena.jipipe:artifacts";

    private boolean autoDownload = true;
    private boolean preferGPU = true;
    private JIPipeArtifactRepositoryReferenceList repositories = new JIPipeArtifactRepositoryReferenceList();
    private OptionalPathParameter overrideInstallationPath = new OptionalPathParameter();

    public JIPipeArtifactApplicationSettings() {
        repositories.add(new JIPipeArtifactRepositoryReference("https://jipipe.hki-jena.de/nexus/", "jipipe-artifacts", JIPipeArtifactRepositoryType.SonatypeNexus));
    }

    public static JIPipeArtifactApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, JIPipeArtifactApplicationSettings.class);
    }

    @SetJIPipeDocumentation(name = "Prefer GPU versions of artifacts", description = "If enabled, automated matching of artifacts will prefer GPU versions")
    @JIPipeParameter("prefer-gpu")
    public boolean isPreferGPU() {
        return preferGPU;
    }

    @JIPipeParameter("prefer-gpu")
    public void setPreferGPU(boolean preferGPU) {
        this.preferGPU = preferGPU;
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
