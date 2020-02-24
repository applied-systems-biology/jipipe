package org.hkijena.acaq5.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class DocumentedComponent extends JSplitPane {

    private MarkdownReader documentationPanel;

    public DocumentedComponent(boolean documentationBelow, String documentationPath, Component content) {
        super(documentationBelow ? JSplitPane.VERTICAL_SPLIT : JSplitPane.HORIZONTAL_SPLIT);

        documentationPanel = new MarkdownReader(false);
        documentationPanel.loadDefaultDocument(documentationPath);

        setDividerSize(3);
        setResizeWeight(0.33);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                setDividerLocation(0.66);
            }
        });

        setLeftComponent(content);
        setRightComponent(documentationPanel);
    }

    public MarkdownReader getDocumentationPanel() {
        return documentationPanel;
    }
}
