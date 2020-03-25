package org.hkijena.acaq5.ui;

import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.components.MarkdownReader;

import java.awt.*;

public class ACAQInfoUI extends ACAQProjectUIPanel {

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
