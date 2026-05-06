package org.hkijena.jipipe.desktop.commons.components.ai;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifact;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifactRepositoryApplyInstallUninstallRun;
import org.hkijena.jipipe.api.artifacts.JIPipeRemoteArtifact;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunExecuteUI;
import org.hkijena.jipipe.desktop.commons.components.ai.setup.ModelConfigurationComponent;
import org.hkijena.jipipe.desktop.commons.components.panels.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.plugins.ai.AIApplicationSettings;
import org.hkijena.jipipe.plugins.ai.environments.EmbeddingModelEnvironment;
import org.hkijena.jipipe.plugins.ai.environments.OptionalEmbeddingModelEnvironment;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JIPipeDesktopAISetupDialog extends JDialog {
    private final JIPipeDesktopWorkbench workbench;
    private final JCheckBox termsCheckBox;
    private boolean userConfirmed;
    private JButton okButton;
    private final ModelConfigurationComponent embeddingModelConfigurationComponent;

    public JIPipeDesktopAISetupDialog(JIPipeDesktopWorkbench workbench) {
        super(workbench.getWindow());
        this.workbench = workbench;
        this.termsCheckBox = new JCheckBox("I understand that AI models can make mistakes and will carefully check what they suggest");
        this.embeddingModelConfigurationComponent = new ModelConfigurationComponent(AIApplicationSettings.getInstance().getEmbeddingModelEnvironment(), workbench);
        initialize();
        updateOkButton();
    }

    private void initialize() {
        setTitle("JIPipe - AI setup");
        setIconImage(UIUtils.getJIPipeIcon128());

        JPanel contentPanel = new JPanel(new BorderLayout(8,8));
        contentPanel.setBorder(UIUtils.createEmptyBorder(8));
        contentPanel.setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());
        setContentPane(contentPanel);

        JIPipeDesktopFormPanel mainPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
        contentPanel.add(UIUtils.wrapInIslandPanelIfNeeded(mainPanel), BorderLayout.CENTER);

        mainPanel.addWideToForm(UIUtils.createInfoLabel("AI requires additional configuration", "Please review the following configuration options and notes about the AI features", JIPipe.RESOURCES.getIcon64("check-circle-green.png")));
        mainPanel.addWideToForm(UIUtils.createInfoLabel("Will my data be shared?", "By default JIPipe will use local models that run on your computer, meaning that nothing will be sent to any third party.", JIPipe.RESOURCES.getIcon16("actions/help.png")));
        mainPanel.addWideToForm(UIUtils.createInfoLabel("Can I turn off AI?", "Yes, click the 'Disable AI' button and all AI features will be disabled and hidden.", JIPipe.RESOURCES.getIcon16("actions/help.png")));
        mainPanel.addWideToForm(UIUtils.createInfoLabel("What is an embedding?", "A small AI model that allows to convert a concept into an easily comparable numeric vector. This is used for search features.", JIPipe.RESOURCES.getIcon16("actions/help.png")));
        mainPanel.addWideToForm(UIUtils.createInfoLabel("Do I need internet?", "Only for downloading the model. When the model is set up it will run entirely offline.", JIPipe.RESOURCES.getIcon16("actions/help.png")));
        mainPanel.addWideToForm(Box.createVerticalStrut(32));
        mainPanel.addWideToForm(embeddingModelConfigurationComponent);
        mainPanel.addVerticalGlue();

        this.okButton = UIUtils.setFontSize(UIUtils.createButton("OK", JIPipe.RESOURCES.getIcon16("actions/check.png"), this::confirmConfiguration), 16);
        JPanel buttonPanel = UIUtils.boxHorizontal(
                UIUtils.setFontSize(UIUtils.createButton("Disable AI", JIPipe.RESOURCES.getIcon16("actions/cancel.png"), this::disableAI), 16),
                Box.createHorizontalGlue(),
                termsCheckBox,
                Box.createHorizontalStrut(8),
                UIUtils.setFontSize(UIUtils.createButton("Cancel", JIPipe.RESOURCES.getIcon16("actions/dialog-cancel.png"), () -> {
                    userConfirmed = false;
                    setVisible(false);
                }), 16),
                okButton
        );
        UIUtils.makeNonOpaque(buttonPanel, true);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        termsCheckBox.addActionListener(e -> updateOkButton());
    }

    private void disableAI() {
        if(JOptionPane.showConfirmDialog(this, "All AI-related features will be disabled and fully hidden.\nYou will need to go to the application settings to re-enable AI.\nContinue?",
                "Disable AI", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
            AIApplicationSettings.getInstance().setEnableAI(false);
            userConfirmed = false;
            setVisible(false);
            JIPipe.getSettings().saveLater();
            JOptionPane.showMessageDialog(this, "To fully disable all AI features, JIPipe needs to be restarted.", "Disable AI", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void updateOkButton() {
        okButton.setEnabled(termsCheckBox.isSelected());
    }

    private void confirmConfiguration() {
        EmbeddingModelEnvironment embeddingModelEnvironment = (EmbeddingModelEnvironment) embeddingModelConfigurationComponent.getParameterCollection();
        if(!embeddingModelEnvironment.isValid()) {
            JOptionPane.showMessageDialog(this, "The embedding environment is not correctly configured!", "AI setup", JOptionPane.ERROR_MESSAGE);
        }

        // Update the global settings
        AIApplicationSettings settings = AIApplicationSettings.getInstance();
        settings.setOverrideEmbeddingModelEnvironment(new OptionalEmbeddingModelEnvironment(embeddingModelEnvironment, true));

        JIPipe.getSettings().saveLater();

        // Download all missing artifacts
        List<JIPipeRemoteArtifact> toInstall = new ArrayList<>();
        {
            String query = embeddingModelEnvironment.getArtifactQuery().getQuery();
            JIPipeArtifact artifact = JIPipe.getInstance().getArtifacts()
                    .queryPreferredCachedArtifact(query);
            if(artifact instanceof JIPipeRemoteArtifact remoteArtifact) {
                toInstall.add(remoteArtifact);
            }
            else if(artifact == null) {
                JOptionPane.showMessageDialog(this, "Artifact '" + query + "' not found! (No internet?)", "AI setup", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        if(!toInstall.isEmpty()) {
            if(JOptionPane.showConfirmDialog(this, "JIPipe needs to download local models. This needs to be done only once.\nContinue?", "AI setup", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
                return;
            }
            JIPipeArtifactRepositoryApplyInstallUninstallRun run = new JIPipeArtifactRepositoryApplyInstallUninstallRun(toInstall, Collections.emptyList());
            JIPipeDesktopRunExecuteUI.runInDialog(workbench, this, run);
        }

        userConfirmed = true;
        setVisible(false);
    }

    public static boolean checkFirstTimeSetup(JIPipeDesktopWorkbench workbench) {
        if(JIPipe.getInstance().getAiService().hasConfiguredAndReadyEmbeddingModel()) {
            return true;
        }
        // User is shown the dialog
        JIPipeDesktopAISetupDialog dialog = new JIPipeDesktopAISetupDialog(workbench);
        dialog.pack();
        dialog.setSize(1024,768);
        dialog.setLocationRelativeTo(workbench.getWindow());
        dialog.setModal(true);
        dialog.setVisible(true);
        return dialog.userConfirmed;
    }
}
