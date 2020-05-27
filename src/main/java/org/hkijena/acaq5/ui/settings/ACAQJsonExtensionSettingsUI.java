package org.hkijena.acaq5.ui.settings;

import org.hkijena.acaq5.ui.ACAQJsonExtensionWorkbench;
import org.hkijena.acaq5.ui.ACAQJsonExtensionWorkbenchPanel;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.parameters.ParameterPanel;

import java.awt.*;

/**
 * Panel containing algorithm settings when algorithms are edited in a {@link org.hkijena.acaq5.ACAQJsonExtension}
 */
public class ACAQJsonExtensionSettingsUI extends ACAQJsonExtensionWorkbenchPanel {
    /**
     * @param workbenchUI The workbench UI
     */
    public ACAQJsonExtensionSettingsUI(ACAQJsonExtensionWorkbench workbenchUI) {
        super(workbenchUI);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        ParameterPanel metadataUI = new ParameterPanel(getExtensionWorkbenchUI(),
                getProject(),
                MarkdownDocument.fromPluginResource("documentation/project-settings.md"),
                ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.WITH_SCROLLING);
        add(metadataUI, BorderLayout.CENTER);
    }
}
