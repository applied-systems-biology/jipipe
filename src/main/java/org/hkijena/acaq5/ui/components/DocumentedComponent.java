package org.hkijena.acaq5.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * Wrapper around a component that adds a split pane that shows documentation
 */
public class DocumentedComponent extends JSplitPane {

    private MarkdownReader documentationPanel;

    /**
     * @param documentationBelow if the documentation should be displayed below
     * @param documentation      the documentation. can be null.
     * @param content            the wrapped component
     */
    public DocumentedComponent(boolean documentationBelow, MarkdownDocument documentation, Component content) {
        super(documentationBelow ? JSplitPane.VERTICAL_SPLIT : JSplitPane.HORIZONTAL_SPLIT);

        documentationPanel = new MarkdownReader(false);
        documentationPanel.setDocument(documentation);

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
