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

package org.hkijena.jipipe.plugins.publish.rocrate;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.*;
import org.hkijena.jipipe.api.project.JIPipeProjectUserPaths;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.publish.JIPipeDesktopPublisherAssistant;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.publish.conditions.*;
import org.hkijena.jipipe.plugins.settings.application.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ROCratePublisherAssistant extends JIPipeDesktopPublisherAssistant {

    private final Settings settings = new Settings();

    public ROCratePublisherAssistant(JIPipeDesktopProjectWorkbench workbench) {
        super(workbench);
        addAssistantCondition(new ProjectValidationAssistantCondition(this));
        addAssistantCondition(new TitleAssistantCondition(this));
        addAssistantCondition(new LicenseAssistantCondition(this));
        addAssistantCondition(new DescriptionAssistantCondition(this));
        addAssistantCondition(new SummaryAssistantCondition(this));
        addAssistantCondition(new AuthorsAssistantCondition(this));
        addAssistantCondition(new AuthorAffiliationsAssistantCondition(this));
        addAssistantCondition(new ProjectDirectoriesAssistantCondition(this));
        addAssistantCondition(new SimpleParametersAssistantCondition(this));
        addAssistantCondition(new ArchiveAssistantCondition(this));
        addAssistantCondition(new SavedProjectAssistantCondition(this));

        postInit();
    }

    @Override
    public String getAssistantTitle() {
        return "Publish project as RO-Crate";
    }

    @Override
    public HTMLText getAssistantDescription() {
        return new HTMLText("This tool will guide you through the process of publishing your JIPipe project as RO-Crate with CWL. This will produce a *.zip file that contains the projects and all inputs together with all necessary instructions to run the project.");
    }

    @Override
    public List<BufferedImage> getAssistantLogos() {
        return List.of(JIPipe.RESOURCES.getVariantResourceAsImage("logos/ro-crate.png"),
                JIPipe.RESOURCES.getVariantResourceAsImage("logos/cwl.png"));
    }

    @Override
    public JIPipeRunnable createAssistantTask() {
        Path crateFile = JIPipeDesktop.saveFile(this,
                getDesktopWorkbench(),
                JIPipeFileChooserApplicationSettings.LastDirectoryKey.External,
                "Export as RO-Crate",
                new HTMLText("Please choose where the RO-Crate will be saved"),
                PathUtils.EXTENSION_FILTER_WORKFLOW_RO_CRATE);
        if (crateFile != null) {

            // Collect settings for project directories
            Map<String, JIPipeProjectUserPaths.Role> projectDirectorySettings = new HashMap<>();
            for (JIPipeProjectUserPaths.UserPathEntry userPathEntry : getProject().getMetadata().getUserPaths().getUserPathsAsInstance()) {
                if (!StringUtils.isNullOrEmpty(userPathEntry.getKey())) {
                    JIPipeParameterAccess access = settings.userDirectories.get("project-directory-" + userPathEntry.getKey());
                    if (access != null) {
                        projectDirectorySettings.put(userPathEntry.getKey(), access.get(JIPipeProjectUserPaths.Role.class));
                    }
                }
            }

            // Create the run
            return new CreateROCrateRun(getProject(),
                    getDesktopProjectWorkbench().getProjectWindow().getProjectSavePath(),
                    crateFile,
                    projectDirectorySettings);
        }
        return null;
    }

    @Override
    public void onPublicationFinished(JIPipeRunnable runnable) {
        if (runnable instanceof CreateROCrateRun run) {
            if (JOptionPane.showConfirmDialog(this, "<html>The RO-Crate was successfully exported to " + run.getRoCrateFile() + ".<br/>Do you want to open the containing directory?</html>",
                    "Export finished", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                UIUtils.desktopOpenFile(run.getRoCrateFile().getParent());
            }
        }
    }

    @Override
    public JIPipeParameterCollection getAssistantParameters() {

        // Update settings for input parameters
        updateUserDirectoryParameters();

        return settings;
    }

    private void updateUserDirectoryParameters() {
        List<JIPipeProjectUserPaths.UserPathEntry> directoryEntries = getProject().getMetadata().getUserPaths().getUserPathsAsInstance();
        for (JIPipeProjectUserPaths.UserPathEntry userPathEntry : directoryEntries) {
            if (!StringUtils.isNullOrEmpty(userPathEntry.getKey())) {
                String parameterKey = "project-directory-" + userPathEntry.getKey();
                if (!settings.userDirectories.containsKey(parameterKey)) {
                    JIPipeMutableParameterAccess access = settings.userDirectories.addParameter(parameterKey, JIPipeProjectUserPaths.Role.class, StringUtils.orElse(userPathEntry.getName(), userPathEntry.getKey()),
                            "If enabled, the path " + userPathEntry.getPath() + " and all its contents will be added into the RO-Crate.");
                    access.set(userPathEntry.getRole());
                }
            }
        }
        for (String key : ImmutableList.copyOf(settings.userDirectories.getParameters().keySet())) {
            if (key.startsWith("project-directory-")) {
                String directoryKey = key.substring("project-directory-".length());
                if (directoryEntries.stream().noneMatch(directoryEntry -> directoryEntry.getKey().equals(directoryKey))) {
                    settings.userDirectories.removeParameter(key);
                }
            }
        }
    }

    public static class Settings extends AbstractJIPipeParameterCollection {
        private final JIPipeDynamicParameterCollection userDirectories = new JIPipeDynamicParameterCollection();

        @SetJIPipeDocumentation(name = "Project user paths", description = "Each path must be either an input or an output.")
        @JIPipeParameter("user-directories")
        public JIPipeDynamicParameterCollection getUserDirectories() {
            return userDirectories;
        }
    }
}
