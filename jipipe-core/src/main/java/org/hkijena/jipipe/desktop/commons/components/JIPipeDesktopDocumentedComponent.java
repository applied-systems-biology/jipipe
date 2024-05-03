/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop.commons.components;

import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopMarkdownReader;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * Wrapper around a component that adds a split pane that shows documentation
 */
public class JIPipeDesktopDocumentedComponent extends JSplitPane {

    private JIPipeDesktopMarkdownReader documentationPanel;

    /**
     * @param documentationBelow if the documentation should be displayed below
     * @param documentation      the documentation. can be null.
     * @param content            the wrapped component
     */
    public JIPipeDesktopDocumentedComponent(boolean documentationBelow, MarkdownText documentation, Component content) {
        super(documentationBelow ? JSplitPane.VERTICAL_SPLIT : JSplitPane.HORIZONTAL_SPLIT);

        documentationPanel = new JIPipeDesktopMarkdownReader(false);
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

    public JIPipeDesktopMarkdownReader getDocumentationPanel() {
        return documentationPanel;
    }
}
