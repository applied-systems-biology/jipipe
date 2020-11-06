/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.installer.linux.ui.utils;

import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.ElementIterator;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Rectangle;
import java.util.Arrays;

/**
 * Panel that allows reading of Markdown data
 */
public class MarkdownReader extends JPanel {

    public static final String[] CSS_RULES = {"body { font-family: \"Sans-serif\"; }",
            "pre { background-color: #f5f2f0; border: 3px #f5f2f0 solid; }",
            "code { background-color: #f5f2f0; }",
            "h2 { padding-top: 30px; }",
            "h3 { padding-top: 30px; }",
            "th { border-bottom: 1px solid #c8c8c8; }",
            ".toc-list { list-style: none; }"};
    public static final MutableDataHolder OPTIONS = new MutableDataSet()
            .set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), AutolinkExtension.create(), TocExtension.create()));
    private JScrollPane scrollPane;
    private JTextPane content;
    private MarkdownDocument document;
    private MarkdownDocument temporaryDocument;

    /**
     */
    public MarkdownReader() {
        initialize();
    }

    /**
     * @param document    initialize with document
     */
    public MarkdownReader(MarkdownDocument document) {
        this();
        this.setDocument(document);
    }

    private void initialize() {
        setLayout(new BorderLayout());

        content = new JTextPane();
        content.setEditable(false);
        content.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                if (e.getDescription() != null && e.getDescription().startsWith("#")) {
                    SwingUtilities.invokeLater(() -> scrollToReference(e.getDescription().substring(1)));
                } else {
                    if (Desktop.isDesktopSupported()) {
                        try {
                            Desktop.getDesktop().browse(e.getURL().toURI());
                        } catch (Exception e1) {
                            throw new RuntimeException(e1);
                        }
                    }
                }
            }
        });

        HTMLEditorKit kit = new HTMLEditorKit();
        initializeStyleSheet(kit.getStyleSheet());

        content.setEditorKit(kit);
        content.setContentType("text/html");
        scrollPane = new JScrollPane(content);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Custom "scroll to reference"
     *
     * @param var1 reference index
     */
    private void scrollToReference(String var1) {
        Document var2 = content.getDocument();
        if (var2 instanceof HTMLDocument) {
            HTMLDocument html = (HTMLDocument) var2;

            Element element;
            ElementIterator iterator = new ElementIterator(html);
            while ((element = iterator.next()) != null) {
                AttributeSet attributes = element.getAttributes();
                String attribute = (String) attributes.getAttribute(HTML.Attribute.ID);
                if (attribute != null && attribute.equals(var1)) {
                    try {
                        int pos = element.getStartOffset();
                        Rectangle rectangle = content.modelToView(pos);
                        if (rectangle != null) {
                            Rectangle var9 = content.getVisibleRect();
                            rectangle.height = var9.height;
                            content.scrollRectToVisible(rectangle);
                            content.setCaretPosition(pos);
                        }
                    } catch (BadLocationException var10) {
                        UIManager.getLookAndFeel().provideErrorFeedback(content);
                    }
                }
            }
        }
    }

    /**
     * Renders the content as HTML
     *
     * @return HTML content
     */
    private String toHTML() {
        String html = content.getText();
        StringBuilder stylesheet = new StringBuilder();
        for (String rule : CSS_RULES) {
            stylesheet.append(rule).append(" ");
        }
        html = "<html><head><style>" + stylesheet + "</style></head><body>" + html + "</body></html>";
        return html;
    }

    private void initializeStyleSheet(StyleSheet styleSheet) {
        for (String rule : CSS_RULES) {
            styleSheet.addRule(rule);
        }
    }

    public MarkdownDocument getDocument() {
        return document;
    }

    public void setDocument(MarkdownDocument document) {
        this.document = document;
        if (document != null)
            content.setText(document.getRenderedHTML());
        else
            content.setText("<html></html>");
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));
    }

    public JTextPane getContent() {
        return content;
    }

    public MarkdownDocument getTemporaryDocument() {
        return temporaryDocument;
    }

    /**
     * Sets the document to some temporary one without changing the reference to the main document
     *
     * @param temporaryDocument if not null, render the temporary document. Otherwise render the main document
     */
    public void setTemporaryDocument(MarkdownDocument temporaryDocument) {
        if (temporaryDocument == null) {
            if (document != null)
                content.setText(document.getRenderedHTML());
            else
                content.setText("<html></html>");
        } else {
            content.setText(temporaryDocument.getRenderedHTML());
        }
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));
        this.temporaryDocument = temporaryDocument;
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }
}
