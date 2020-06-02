package org.hkijena.acaq5.ui.settings;

import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQProjectWorkbenchPanel;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.parameters.ParameterPanel;

import java.awt.BorderLayout;

/**
 * UI around the metadata of an {@link org.hkijena.acaq5.api.ACAQProject}
 */
public class ACAQProjectSettingsUI extends ACAQProjectWorkbenchPanel {
    /**
     * @param workbenchUI The workbench UI
     */
    public ACAQProjectSettingsUI(ACAQProjectWorkbench workbenchUI) {
        super(workbenchUI);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        ParameterPanel metadataUI = new ParameterPanel(getProjectWorkbench(),
                getProject().getMetadata(),
                MarkdownDocument.fromPluginResource("documentation/project-settings.md"),
                ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.WITH_SCROLLING);
        add(metadataUI, BorderLayout.CENTER);
    }
}
