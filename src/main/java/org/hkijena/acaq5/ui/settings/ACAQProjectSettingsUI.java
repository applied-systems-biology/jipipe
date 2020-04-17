package org.hkijena.acaq5.ui.settings;

import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQProjectWorkbenchPanel;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.parameters.ACAQParameterAccessUI;

import java.awt.*;

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
        ACAQParameterAccessUI metadataUI = new ACAQParameterAccessUI(getProjectWorkbench(),
                getProject().getMetadata(),
                MarkdownDocument.fromPluginResource("documentation/project-settings.md"),
                false,
                true);
        add(metadataUI, BorderLayout.CENTER);
    }
}
