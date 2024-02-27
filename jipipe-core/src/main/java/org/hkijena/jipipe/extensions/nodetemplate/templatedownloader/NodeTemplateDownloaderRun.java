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

package org.hkijena.jipipe.extensions.nodetemplate.templatedownloader;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.extensions.settings.NodeTemplateSettings;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.WebUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class NodeTemplateDownloaderRun implements JIPipeRunnable {

    private final JIPipeWorkbench workbench;
    private final List<NodeTemplateDownloaderPackage> availablePackages = new ArrayList<>();
    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
    private Set<NodeTemplateDownloaderPackage> targetPackages = new HashSet<>();

    private boolean toProject = false;

    public NodeTemplateDownloaderRun(JIPipeWorkbench workbench) {
        this.workbench = workbench;
    }

    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }

    @Override
    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }

    @Override
    public void setProgressInfo(JIPipeProgressInfo progressInfo) {
        this.progressInfo = progressInfo;
    }

    @Override
    public String getTaskLabel() {
        return "Download project templates";
    }

    @Override
    public void run() {
        progressInfo.setProgress(0, 5);
        loadAvailablePackages(progressInfo.resolve("Load available packages"));
        progressInfo.setProgress(1);

        executeUserConfiguration();
        if (!targetPackages.isEmpty()) {
            try {
                executeArchiveDownload();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        progressInfo.setProgress(5);
    }

    private void executeArchiveDownload() throws IOException {
        progressInfo.log("Downloading selected templates ...");

        for (NodeTemplateDownloaderPackage targetPackage : targetPackages) {
            progressInfo.log("The following URL will be downloaded: " + targetPackage.getUrl());

            Path outputFile = RuntimeSettings.generateTempFile("template", ".json");
            try {
                WebUtils.download(new URL(targetPackage.getUrl()), outputFile, "Download repository", progressInfo.resolve("Download template"));
            } catch (MalformedURLException e) {
                progressInfo.log(e.toString());
                progressInfo.log(e.getMessage());
                progressInfo.log("-> Skipping template " + targetPackage.getUrl() + ". Please check the URL!");
                continue;
            }

            // Import the repository
            try {
                JsonNode node = JsonUtils.getObjectMapper().readerFor(JsonNode.class).readValue(outputFile.toFile());
                List<JIPipeNodeTemplate> templates = new ArrayList<>();
                for (JsonNode element : ImmutableList.copyOf(node.elements())) {
                    JIPipeNodeTemplate template = JsonUtils.getObjectMapper().readerFor(JIPipeNodeTemplate.class).readValue(element);
                    template.setSource(targetPackage.getUrl());
                    templates.add(template);
                }
                if (workbench instanceof JIPipeProjectWorkbench && toProject) {
                    ((JIPipeProjectWorkbench) workbench).getProject().getMetadata().getNodeTemplates().addAll(templates);
                    ((JIPipeProjectWorkbench) workbench).getProject().getMetadata().emitParameterChangedEvent("node-templates");
                } else {
                    // Store globally
                    NodeTemplateSettings.getInstance().getNodeTemplates().addAll(templates);
                    NodeTemplateSettings.getInstance().emitParameterChangedEvent("node-templates");
                    JIPipe.getSettings().save();
                }
                NodeTemplateSettings.triggerRefreshedEvent();
            } catch (IOException e) {
                progressInfo.log("Could not read template " + outputFile);
                progressInfo.log(e.toString());
            }

            try {
                Files.delete(outputFile);
            } catch (IOException e) {
                progressInfo.log("Could not clean up temporary file " + outputFile);
                progressInfo.log(e.toString());
            }
        }
    }

    private void executeUserConfiguration() {
        progressInfo.log("Waiting for user input ...");
        try {
            SwingUtilities.invokeAndWait(this::runSetupDialog);
        } catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        progressInfo.log("Setup complete.");
        progressInfo.setProgress(2);
    }

    public List<NodeTemplateDownloaderPackage> getAvailablePackages() {
        return Collections.unmodifiableList(availablePackages);
    }

    private void loadAvailablePackages(JIPipeProgressInfo progressInfo) {
        StringList repositories = NodeTemplateSettings.getInstance().getNodeTemplateDownloadRepositories();

        if (repositories.isEmpty()) {
            throw new UnsupportedOperationException("No repositories set! Cancelling.");
        }
        progressInfo.log("Following repositories will be contacted:");
        for (int i = 0; i < repositories.size(); i++) {
            String repository = repositories.get(i);
            progressInfo.log(" - [Repository " + i + "] " + repository);
        }
        for (int i = 0; i < repositories.size(); i++) {
            String repositoryURL = repositories.get(i);
            JIPipeProgressInfo repositoryProgress = progressInfo.resolve("Repository " + i);
            Path outputFile = RuntimeSettings.generateTempFile("repository", ".json");
            try {
                WebUtils.download(new URL(repositoryURL), outputFile, "Download repository", repositoryProgress);
            } catch (MalformedURLException e) {
                repositoryProgress.log(e.toString());
                repositoryProgress.log(e.getMessage());
                repositoryProgress.log("-> Skipping repository " + repositoryURL + ". Please check the URL!");
            }

            // Import the repository
            try {
                NodeTemplateDownloaderRepository repository = JsonUtils.getObjectMapper().readerFor(NodeTemplateDownloaderRepository.class).readValue(outputFile.toFile());
                availablePackages.addAll(repository.getFiles());
            } catch (IOException e) {
                repositoryProgress.log("Could not read repository " + outputFile);
                repositoryProgress.log(e.toString());
            }

            try {
                Files.delete(outputFile);
            } catch (IOException e) {
                repositoryProgress.log("Could not clean up temporary file " + outputFile);
                repositoryProgress.log(e.toString());
            }

        }
    }

    private void runSetupDialog() {
        NodeTemplateDownloaderDialog dialog = new NodeTemplateDownloaderDialog(this);
        dialog.setModal(true);
        dialog.pack();
        dialog.setSize(1024, 768);
        dialog.setLocationRelativeTo(getWorkbench().getWindow());
        dialog.setVisible(true);

        targetPackages = dialog.getTargetPackages();
        toProject = dialog.isAddToProject();
        if (targetPackages.isEmpty()) {
            getProgressInfo().log("Cancelled by user.");
        }
    }

}
