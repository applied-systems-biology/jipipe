package org.hkijena.acaq5.ui.settings;

import org.hkijena.acaq5.ui.ACAQJsonExtensionUI;
import org.hkijena.acaq5.ui.ACAQJsonExtensionUIPanel;
import org.hkijena.acaq5.ui.components.ACAQParameterAccessUI;
import org.hkijena.acaq5.ui.components.MarkdownDocument;

import java.awt.*;

public class ACAQJsonExtensionSettingsUI extends ACAQJsonExtensionUIPanel {
    public ACAQJsonExtensionSettingsUI(ACAQJsonExtensionUI workbenchUI) {
        super(workbenchUI);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        ACAQParameterAccessUI metadataUI = new ACAQParameterAccessUI(null,
                getProject(),
                MarkdownDocument.fromPluginResource("documentation/project-settings.md"),
                false,
                true);
        add(metadataUI, BorderLayout.CENTER);
    }
}
