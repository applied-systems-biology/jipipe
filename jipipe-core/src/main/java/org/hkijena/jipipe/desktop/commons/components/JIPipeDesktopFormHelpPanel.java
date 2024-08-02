package org.hkijena.jipipe.desktop.commons.components;

import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopMarkdownReader;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopFormHelpPanel extends JPanel {

    private final JIPipeDesktopMarkdownReader contentReader = new JIPipeDesktopMarkdownReader(false);
    private final JButton showDefaultButton = new JButton("Back", UIUtils.getIconFromResources("actions/previous.png"));
    private MarkdownText defaultContent;
    private String currentDocumentName;
    private MarkdownText currentContent;

    public JIPipeDesktopFormHelpPanel() {
        initialize();
        updateContent();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        showDefaultButton.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
        showDefaultButton.addActionListener(e -> showDefaultContent());
    }

    private void updateContent() {
        removeAll();
        if(currentContent != null && currentDocumentName != null) {
            JToolBar toolBar = new JToolBar();
            toolBar.setFloatable(false);
            toolBar.add(showDefaultButton);
            add(toolBar, BorderLayout.NORTH);

            contentReader.setDocument(currentContent);
            add(contentReader, BorderLayout.CENTER);
        }
        else {
            contentReader.setDocument(defaultContent);
            add(contentReader, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
    }

    public MarkdownText getDefaultContent() {
        return defaultContent;
    }

    public void setDefaultContent(MarkdownText defaultContent) {
        this.defaultContent = defaultContent;
        updateContent();
    }

    public void showDefaultContent() {
        showContent(null, null);
    }

    public void showContent(MarkdownText content) {
        if(content != null) {
            String name;
            if (StringUtils.orElse(content.getMarkdown(), "").startsWith("#")) {
                String s = content.getMarkdown().split("\n")[0];
                s = s.substring(s.lastIndexOf('#') + 1);
                name = s;
            } else {
                name = "...";
            }
            showContent(name, content);
        }
        else {
            showDefaultContent();
        }
    }

    public void showContent(String documentName, MarkdownText content) {
        if(documentName == null || content == null) {
            this.currentDocumentName = null;
            this.currentContent = null;
        }
        else {
            this.currentDocumentName = documentName;
            this.currentContent = content;
        }
        updateContent();
    }
}
