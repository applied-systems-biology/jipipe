package org.hkijena.acaq5.ui.settings;

import org.hkijena.acaq5.ui.ACAQProjectUI;
import org.hkijena.acaq5.ui.ACAQProjectUIPanel;
import org.hkijena.acaq5.ui.components.ACAQParameterAccessUI;
import org.hkijena.acaq5.ui.components.MarkdownDocument;

import java.awt.*;

/**
 * UI around the metadata of an {@link org.hkijena.acaq5.api.ACAQProject}
 */
public class ACAQProjectSettingsUI extends ACAQProjectUIPanel {
    /**
     * @param workbenchUI The workbench UI
     */
    public ACAQProjectSettingsUI(ACAQProjectUI workbenchUI) {
        super(workbenchUI);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        ACAQParameterAccessUI metadataUI = new ACAQParameterAccessUI(getWorkbenchUI(),
                getProject().getMetadata(),
                MarkdownDocument.fromPluginResource("documentation/project-settings.md"),
                false,
                true);
        add(metadataUI, BorderLayout.CENTER);
    }
}
