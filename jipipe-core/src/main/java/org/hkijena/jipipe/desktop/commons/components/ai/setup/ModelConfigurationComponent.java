package org.hkijena.jipipe.desktop.commons.components.ai.setup;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifact;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.api.service.components.JIPipeEnvironmentsServiceComponent;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.parameters.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.plugins.ai.environments.EmbeddingModelEnvironment;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.JIPipeArtifactQueryParameter;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.WebUtils;

import javax.swing.*;
import java.awt.*;

public class ModelConfigurationComponent extends JPanel {
    private JIPipeParameterCollection parameterCollection;
    private final JIPipeDesktopWorkbench workbench;
    private final JPopupMenu configureMenu = new JPopupMenu();
    private final JLabel titleLabel = new JLabel("Title");
    private final JLabel infoPrivacyLabel = new JLabel(JIPipe.RESOURCES.getIcon16("actions/user-shield.png"));
    private final JLabel infoPerformanceLabel = new JLabel(JIPipe.RESOURCES.getIcon16("actions/speedometer.png"));

    public ModelConfigurationComponent(JIPipeParameterCollection parameterCollection, JIPipeDesktopWorkbench workbench) {
        this.parameterCollection = parameterCollection;
        this.workbench = workbench;
        initialize();
        updateLabels();
    }

    private void initialize() {
        setLayout(new BorderLayout(8, 8));
        setBorder(UIUtils.createControlBorder());

        add(new JLabel(JIPipe.RESOURCES.getIcon32("actions/environment.png")));
        infoPrivacyLabel.setToolTipText("Privacy");
        infoPerformanceLabel.setToolTipText("Performance impact");
        titleLabel.setFont(new Font(Font.DIALOG, Font.BOLD, ThemeUtils.getCurrentStyle().getFontSizeLarge()));
        JLabel iconLabel = new JLabel(JIPipe.RESOURCES.getIcon24("actions/ai.png"));
        iconLabel.setBorder(UIUtils.createEmptyBorder(8));
        add(UIUtils.boxHorizontal(iconLabel, UIUtils.borderNSEWC(titleLabel, UIUtils.boxHorizontal(infoPrivacyLabel, Box.createHorizontalStrut(8), infoPerformanceLabel), null, null, null)), BorderLayout.CENTER);

        JButton configureButton = new JButton("Configure ...", JIPipe.RESOURCES.getIcon16("actions/configure.png"));
        configureButton.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, ThemeUtils.getCurrentStyle().getBorderColor()), BorderFactory.createEmptyBorder(0, 8, 0, 0)));
        UIUtils.addReloadablePopupMenuToButton(configureButton, configureMenu, this::reloadConfigureMenu);
        add(configureButton, BorderLayout.EAST);
    }

    private void updateLabels() {
        if (parameterCollection instanceof EmbeddingModelEnvironment environment) {
            if (environment.isValid()) {
                switch (environment.getModelType()) {
                    case LocalOnnx -> {
                        if (environment.isLoadFromArtifact()) {
                            titleLabel.setText("Local embedding model " + environment.getArtifactQuery().getQuery());
                        } else {
                            titleLabel.setText("Custom local embedding model");
                        }
                        infoPrivacyLabel.setText("Best privacy");
                        infoPerformanceLabel.setText("Low performance");
                    }
                    case OpenAIAPI -> {
                        if (WebUtils.isLikelyRemoteUrl(environment.getApiBase())) {
                            titleLabel.setText("Custom API embedding model (Local)");
                            infoPrivacyLabel.setText("High privacy*");
                            infoPerformanceLabel.setText("Good performance*");
                        } else {
                            titleLabel.setText("Custom API embedding model (Third-party)");
                            infoPrivacyLabel.setText("Low privacy");
                            infoPerformanceLabel.setText("High performance");
                        }
                    }
                }
            } else {
                titleLabel.setText("<Not configured>");
                infoPrivacyLabel.setText("/");
                infoPerformanceLabel.setText("/");
            }
        }
    }

    public JIPipeParameterCollection getParameterCollection() {
        return parameterCollection;
    }

    private void reloadConfigureMenu() {
        configureMenu.removeAll();

        configureMenu.add(UIUtils.createMenuItem("Edit", "Edits the current environment", JIPipe.RESOURCES.getIcon16("actions/edit.png"), this::editEnvironment));
        configureMenu.addSeparator();

        Class<? extends JIPipeEnvironment> parameterClass = (Class<? extends JIPipeEnvironment>) parameterCollection.getClass();
        JIPipeEnvironmentsServiceComponent.EnvironmentInfo environmentInfo = JIPipe.getInstance().getEnvironments().getInfoByClass(parameterClass);
        // Only show version-pinned artifacts here (users can still select specific versions through "Edit")
        for (JIPipeArtifact artifact : JIPipe.getArtifacts().queryCachedVersionPinnedArtifacts(environmentInfo.getArtifactQuery())) {
            if (artifact.isCompatible()) {
                configureMenu.add(UIUtils.createMenuItem("Artifact " + artifact.getFullId(), "Uses the predefined artifact " + artifact.getFullId(), JIPipe.RESOURCES.getIcon16("actions/run-install.png"), () -> {
                    loadArtifact(artifact);
                }));
            }
        }
    }

    private void editEnvironment() {
        JIPipeParameterTypeInfo typeInfo = JIPipe.getInstance().getParameterTypes().getInfoByFieldClass(parameterCollection.getClass());
        JIPipeEnvironment parameter = (JIPipeEnvironment) typeInfo.duplicate(parameterCollection);
        boolean result = JIPipeDesktopParameterFormPanel.showDialog(workbench,
                parameter,
                null,
                "Edit environment",
                JIPipeDesktopParameterFormPanel.NO_GROUP_HEADERS | JIPipeDesktopParameterFormPanel.WITH_SEARCH_BAR |
                        JIPipeDesktopParameterFormPanel.WITH_SCROLLING | JIPipeDesktopParameterFormPanel.WITH_DOCUMENTATION |
                        JIPipeDesktopParameterFormPanel.DOCUMENTATION_NO_UI);
        if (result) {
           parameterCollection = parameter;
           updateLabels();
        }
    }

    private void loadArtifact(JIPipeArtifact artifact) {
        if (parameterCollection instanceof EmbeddingModelEnvironment environment) {
            environment.setLoadFromArtifact(true);
            environment.setArtifactQuery(new JIPipeArtifactQueryParameter(artifact.getFullId()));
            environment.setName("Artifact");
            environment.setSource(artifact.getFullId());
        }
        updateLabels();
    }
}
