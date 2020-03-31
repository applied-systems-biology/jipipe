package org.hkijena.acaq5.ui;

import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.components.MarkdownReader;

import java.awt.*;

/**
 * UI that shows some introduction
 */
public class ACAQInfoUI extends ACAQProjectUIPanel {

    /**
     * Creates a new instance
     * @param workbenchUI The workbench UI
     */
    public ACAQInfoUI(ACAQProjectUI workbenchUI) {
        super(workbenchUI);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        MarkdownReader reader = new MarkdownReader(true);
        reader.setDocument(MarkdownDocument.fromPluginResource("documentation/introduction.md"));
        add(reader, BorderLayout.CENTER);
    }
}
