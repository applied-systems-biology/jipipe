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
import org.hkijena.jipipe.plugins.artifacts.oras.OptionalOrasEnvironment;
import org.hkijena.jipipe.plugins.parameters.library.filesystem.PathParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalPathParameter;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;

import javax.swing.*;

public class JIPipeArtifactApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {
    public static final String ID = "org.hkijena.jipipe:artifacts";

    private JIPipeArtifactRepositoryReferenceList repositories = new JIPipeArtifactRepositoryReferenceList();
    private OptionalPathParameter overrideInstallationPath = new OptionalPathParameter();
    private OptionalOrasEnvironment orasCliEnvironment = new OptionalOrasEnvironment();

    private boolean showConnectionIssueBallon = true;

    public JIPipeArtifactApplicationSettings() {
        repositories.add(new JIPipeArtifactRepositoryReference("JIPipe Main Artifacts", "https://applied-systems-biology.github.io/jipipe-artifacts/index.json", "", JIPipeArtifactRepositoryType.JSONv1));
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

    @SetJIPipeDocumentation(name = "Repositories", description = "List of repositories to query")
    @JIPipeParameter("repositories-v2")
    public JIPipeArtifactRepositoryReferenceList getRepositories() {
        return repositories;
    }

    @JIPipeParameter("repositories-v2")
    public void setRepositories(JIPipeArtifactRepositoryReferenceList repositories) {
        this.repositories = repositories;
    }

    @SetJIPipeDocumentation(name = "Override installation path", description = "If enabled, store downloaded artifacts in a different directory. " +
            "By default, they will be stored in %APPDATA%/JIPipe/artifacts (Windows), $HOME/.local/share/JIPipe/artifacts (Linux), and $HOME/Library/Application Support/")
    @JIPipeParameter("override-installation-path")
    @PathParameterSettings(ioMode = PathIOMode.Open, pathMode = PathType.DirectoriesOnly)
    public OptionalPathParameter getOverrideInstallationPath() {
        return overrideInstallationPath;
    }

    @JIPipeParameter("override-installation-path")
    public void setOverrideInstallationPath(OptionalPathParameter overrideInstallationPath) {
        this.overrideInstallationPath = overrideInstallationPath;
    }

    @Override
    public JIPipeDefaultApplicationSettingsSheetCategory getDefaultCategory() {
        return JIPipeDefaultApplicationSettingsSheetCategory.Environments;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Icon getIcon() {
        return JIPipe.RESOURCES.getIcon16("actions/run-install.png");
    }

    @Override
    public String getName() {
        return "Artifacts";
    }

    @Override
    public String getDescription() {
        return "Settings for the artifact downloader";
    }

    @SetJIPipeDocumentation(name = "Override ORAS CLI", description = "Allows to override the default ORAS CLI that JIPipe uses to install ORAS artifacts.")
    @JIPipeParameter("oras-cli-environment")
    public OptionalOrasEnvironment getOrasCliEnvironment() {
        return orasCliEnvironment;
    }

    @JIPipeParameter("oras-cli-environment")
    public void setOrasCliEnvironment(OptionalOrasEnvironment orasCliEnvironment) {
        this.orasCliEnvironment = orasCliEnvironment;
    }
}
