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

package org.hkijena.jipipe.desktop.commons.components.ai.monitor;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.ai.JIPipeAIModelRunnerStatus;
import org.hkijena.jipipe.api.service.components.JIPipeAIServiceComponent;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.commons.components.ai.JIPipeDesktopAISetupDialog;
import org.hkijena.jipipe.desktop.commons.components.panels.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.plugins.ai.AIApplicationSettings;
import org.hkijena.jipipe.plugins.ai.environments.EmbeddingModelEnvironment;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Overview page for the AI monitor window.
 * <p>
 * Displays the current status of AI models (embedding and future LLM models),
 * and provides start/stop model controls and a button to open the AI configuration.
 * <p>
 * Each model is represented by a {@link ModelSection} which encapsulates the UI
 * components and logic for that model. This design allows easy addition of new
 * model types (e.g., LLM models) by adding new sections.
 * <p>
 * Follows the same form-based layout pattern as {@code JIPipeDesktopCacheMonitorOverviewPage}.
 */
public class JIPipeDesktopAIMonitorOverviewPage extends JIPipeDesktopAIMonitorPage {

    private final List<ModelSection> modelSections = new ArrayList<>();
    private final Timer refreshTimer;

    public JIPipeDesktopAIMonitorOverviewPage(JIPipeDesktopAIMonitorWindow window) {
        super(window);
        this.refreshTimer = new Timer(5000, this::onRefreshTimer);
        initialize();
        refreshTimer.start();
    }

    private void onRefreshTimer(ActionEvent e) {
        if (isDisplayable()) {
            refresh();
        } else {
            refreshTimer.stop();
        }
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        refreshTimer.stop();
    }

    private void initialize() {
        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
        add(formPanel, BorderLayout.CENTER);

        // Embedding model section
        ModelSection embeddingSection = new ModelSection(
                "Embedding model",
                JIPipe.RESOURCES.getIcon16("actions/ai.png"));
        embeddingSection.addToForm(formPanel);
        modelSections.add(embeddingSection);

        // TODO: Add LLM model sections here when available
        // ModelSection llmSection = new ModelSection("LLM model", JIPipe.RESOURCES.getIcon16("actions/ai.png"));
        // llmSection.addToForm(formPanel);
        // modelSections.add(llmSection);

        // General actions section
        formPanel.addGroupHeader("Actions", JIPipe.RESOURCES.getIcon16("actions/system-run.png"));
        formPanel.addToForm(new JLabel("Open the AI configuration settings"),
                UIUtils.createButton("Configure AI ...", JIPipe.RESOURCES.getIcon16("actions/configure.png"), this::openAIConfig));
    }

    private void openAIConfig() {
        if (getDesktopWorkbench() instanceof JIPipeDesktopProjectWorkbench projectWorkbench) {
            projectWorkbench.openApplicationSettings("/General/AI");
        }
    }

    @Override
    public void refresh() {
        JIPipeAIServiceComponent aiService = JIPipe.getInstance().getAiService();

        // Refresh embedding model section
        if (!modelSections.isEmpty()) {
            ModelSection embeddingSection = modelSections.get(0);
            JIPipeAIModelRunnerStatus status = aiService.getEmbeddingModelStatus();

            // Update status
            embeddingSection.statusLabel.setText(renderStatus(status));

            // Update model type
            AIApplicationSettings settings = AIApplicationSettings.getInstance();
            EmbeddingModelEnvironment environment = settings.getEmbeddingModelEnvironment();
            embeddingSection.modelTypeLabel.setText(environment.getInfo());

            // Update model ID
            String modelId = aiService.getModelId();
            if (modelId == null) {
                modelId = environment.deriveModelId();
            }
            embeddingSection.modelIdLabel.setText(modelId != null ? modelId : "N/A");

            // Update error
            String error = aiService.getEmbeddingModelError();
            embeddingSection.errorLabel.setText(error != null ? error : "None");
            if (error != null) {
                embeddingSection.errorLabel.setForeground(Color.RED);
            } else {
                embeddingSection.errorLabel.setForeground(UIManager.getColor("Label.foreground"));
            }

            // Update button states
            embeddingSection.startButton.setEnabled(status == JIPipeAIModelRunnerStatus.Unloaded || status == JIPipeAIModelRunnerStatus.Failed);
            embeddingSection.stopButton.setEnabled(status == JIPipeAIModelRunnerStatus.Idle || status == JIPipeAIModelRunnerStatus.Busy);
        }

        // TODO: Refresh LLM model sections here when available
    }

    /**
     * Renders a model status enum to a human-readable string.
     *
     * @param status the model runner status
     * @return human-readable status text
     */
    private static String renderStatus(JIPipeAIModelRunnerStatus status) {
        return switch (status) {
            case Unloaded -> "Not loaded";
            case Loading -> "Loading...";
            case Idle -> "Ready";
            case Busy -> "Busy";
            case Unloading -> "Shutting down...";
            case Failed -> "Error";
        };
    }

    /**
     * Represents a section in the overview page for one AI model.
     * Each section has its own status, model type, model ID, error label,
     * and start/stop buttons.
     * <p>
     * This design allows easy addition of new model types (e.g., LLM models)
     * by creating additional {@link ModelSection} instances.
     */
    private class ModelSection {
        final JLabel statusLabel = new JLabel();
        final JLabel modelTypeLabel = new JLabel();
        final JLabel modelIdLabel = new JLabel();
        final JLabel errorLabel = new JLabel();
        JButton startButton;
        JButton stopButton;
        final String name;

        ModelSection(String name, Icon icon) {
            this.name = name;
            statusLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
            modelTypeLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
            modelIdLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
            errorLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
        }

        /**
         * Adds this model section's components to the form panel.
         *
         * @param formPanel the form panel to add components to
         */
        void addToForm(JIPipeDesktopFormPanel formPanel) {
            formPanel.addGroupHeader(name, JIPipe.RESOURCES.getIcon16("actions/ai.png"));

            formPanel.addToForm(statusLabel, new JLabel("Status"));
            formPanel.addToForm(modelTypeLabel, new JLabel("Model type"));
            formPanel.addToForm(modelIdLabel, new JLabel("Model ID"));
            formPanel.addToForm(errorLabel, new JLabel("Last error"));

            startButton = UIUtils.createButton("Start model",
                    JIPipe.RESOURCES.getIcon16("actions/circle-play.png"),
                    this::onStartModel);
            stopButton = UIUtils.createButton("Stop model",
                    JIPipe.RESOURCES.getIcon16("actions/circle-stop.png"),
                    this::onStopModel);

            formPanel.addToForm(new JLabel("Start the " + name.toLowerCase()),
                    startButton);
            formPanel.addToForm(new JLabel("Stop the " + name.toLowerCase()),
                    stopButton);
        }

        private void onStartModel() {
            if (!JIPipeDesktopAISetupDialog.checkFirstTimeSetup(getDesktopWorkbench())) {
                return;
            }
            JIPipe.getInstance().getAiService().tryStartEmbeddingModel();
        }

        private void onStopModel() {
            JIPipe.getInstance().getAiService().tryStopEmbeddingModel();
        }
    }
}
