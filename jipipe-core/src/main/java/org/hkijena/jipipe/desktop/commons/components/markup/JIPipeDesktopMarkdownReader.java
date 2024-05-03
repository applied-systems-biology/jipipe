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

package org.hkijena.jipipe.desktop.commons.components.markup;

import com.google.common.base.Charsets;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.pdf.converter.PdfConverterExtension;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.settings.FileChooserSettings;
import org.hkijena.jipipe.plugins.settings.GeneralUISettings;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.*;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Panel that allows reading of Markdown data
 */
public class JIPipeDesktopMarkdownReader extends JPanel {

    public static final List<String> CSS_RULES = Arrays.asList("body { font-family: \"Sans-serif\"; }",
            "pre { background-color: #f5f2f0; border: 3px #f5f2f0 solid; }",
            "code { background-color: #ffffff; border: none; }",
            "h2 { padding-top: 30px; }",
            "h3 { padding-top: 30px; }",
            "th { border-bottom: 1px solid #c8c8c8; }",
            ".toc-list { list-style: none; }");
    public static final List<String> CSS_RULES_DARK = Arrays.asList("body { font-family: \"Sans-serif\"; color: #eeeeee; }",
            "pre { background-color: #333333; border: 3px #333333 solid; }",
            "code { background-color: #121212; border: none; }",
            "a { color: #65a4e3; }",
            "h2 { padding-top: 30px; }",
            "h3 { padding-top: 30px; }",
            "th { border-bottom: 1px solid #c8c8c8; }",
            ".toc-list { list-style: none; }");
    public static final MutableDataHolder OPTIONS = new MutableDataSet()
            .set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), AutolinkExtension.create(), TocExtension.create()));
    private final List<String> cssRules;
    private final List<String> cssRulesDark;
    private final JToolBar toolBar = new JToolBar();
    private JScrollPane scrollPane;
    private JTextPane content;
    private MarkdownText document;
    private MarkdownText temporaryDocument;

    public JIPipeDesktopMarkdownReader() {
        this(true, null, CSS_RULES, CSS_RULES_DARK);
    }

    /**
     * @param withToolbar if a toolbar should be shown
     */
    public JIPipeDesktopMarkdownReader(boolean withToolbar) {
        this(withToolbar, null, CSS_RULES, CSS_RULES_DARK);
    }

    /**
     * @param withToolbar if a toolbar should be shown
     * @param document    initialize with document
     */
    public JIPipeDesktopMarkdownReader(boolean withToolbar, MarkdownText document) {
        this(withToolbar, document, CSS_RULES, CSS_RULES_DARK);
    }

    /**
     * @param withToolbar if a toolbar should be shown
     * @param document    initialize with document
     */
    public JIPipeDesktopMarkdownReader(boolean withToolbar, MarkdownText document, List<String> cssRules, List<String> cssRulesDark) {
        this.cssRules = cssRules;
        this.cssRulesDark = cssRulesDark;
        initialize(withToolbar);
        if (document != null) {
            this.setDocument(document);
        }
    }

    public static JIPipeDesktopMarkdownReader showDialog(MarkdownText document, boolean withToolbar, String title, Component parent, boolean modal) {
        JIPipeDesktopMarkdownReader reader = new JIPipeDesktopMarkdownReader(withToolbar, document);
        Window owner = parent != null ? SwingUtilities.getWindowAncestor(parent) : null;
        JDialog dialog = new JDialog(owner);
        dialog.setIconImage(UIUtils.getJIPipeIcon128());
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        panel.add(reader, BorderLayout.CENTER);

        dialog.setContentPane(panel);
        dialog.setTitle(title);
        dialog.setModal(modal);
        dialog.pack();
        dialog.setSize(new Dimension(640, 480));
        dialog.setLocationRelativeTo(owner);
        UIUtils.addEscapeListener(dialog);
        dialog.setVisible(true);
        return reader;
    }

    private void initialize(boolean withToolbar) {
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

        if (withToolbar) {
            toolBar.setFloatable(false);

            JButton exportButton = new JButton("Export", UIUtils.getIconFromResources("actions/save.png"));
            JPopupMenu exportMenu = UIUtils.addPopupMenuToButton(exportButton);

            JMenuItem saveMarkdown = new JMenuItem("as Markdown (*.md)", UIUtils.getIconFromResources("mimetypes/text-markdown.png"));
            saveMarkdown.addActionListener(e -> {
                Path selectedPath = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Projects, "Save as Markdown (*.md)", UIUtils.EXTENSION_FILTER_MD);
                if (selectedPath != null) {
                    try {
                        Files.write(selectedPath, document.getMarkdown().getBytes(Charsets.UTF_8));
                    } catch (IOException e1) {
                        throw new RuntimeException(e1);
                    }
                }
            });
            exportMenu.add(saveMarkdown);

            JMenuItem saveHTML = new JMenuItem("as HTML (*.html)", UIUtils.getIconFromResources("mimetypes/text-html.png"));
            saveHTML.addActionListener(e -> {
                Path selectedPath = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Projects, "Save as HTML (*.html)", UIUtils.EXTENSION_FILTER_HTML);
                if (selectedPath != null) {
                    try {
                        Files.write(selectedPath, toHTML().getBytes(Charsets.UTF_8));
                    } catch (IOException e1) {
                        throw new RuntimeException(e1);
                    }
                }
            });
            exportMenu.add(saveHTML);

            JMenuItem savePDF = new JMenuItem("as PDF (*.pdf)", UIUtils.getIconFromResources("mimetypes/application-pdf.png"));
            savePDF.addActionListener(e -> {
                Path selectedPath = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Projects, "Save as Portable Document Format (*.pdf)", UIUtils.EXTENSION_FILTER_PDF);
                if (selectedPath != null) {
                    PdfConverterExtension.exportToPdf(selectedPath.toString(), toHTML(), "", OPTIONS);
                }
            });
            exportMenu.add(savePDF);

            toolBar.add(exportButton);

//        JButton printButton = new JButton("Print", UIUtils.getIconFromResources("print.png"));
//        printButton.addActionListener(e -> {
//            try {
//                content.print();
//            } catch (PrinterException e1) {
//                throw new RuntimeException(e1);
//            }
//        });
//        toolBar.add(printButton);

            add(toolBar, BorderLayout.NORTH);
        }
    }

    public JToolBar getToolBar() {
        return toolBar;
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
        for (String rule : cssRules) {
            stylesheet.append(rule).append(" ");
        }
        html = "<html><head><style>" + stylesheet + "</style></head><body>" + html + "</body></html>";
        return html;
    }

    private void initializeStyleSheet(StyleSheet styleSheet) {
        try {
            if (JIPipe.getInstance() != null && GeneralUISettings.getInstance() != null && GeneralUISettings.getInstance().getTheme().isDark()) {
                for (String rule : cssRulesDark) {
                    styleSheet.addRule(rule);
                }
            } else {
                for (String rule : cssRules) {
                    styleSheet.addRule(rule);
                }
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            for (String rule : cssRules) {
                styleSheet.addRule(rule);
            }
        }
    }

    public MarkdownText getDocument() {
        return document;
    }

    public void setDocument(MarkdownText document) {
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

    public MarkdownText getTemporaryDocument() {
        return temporaryDocument;
    }

    /**
     * Sets the document to some temporary one without changing the reference to the main document
     *
     * @param temporaryDocument if not null, render the temporary document. Otherwise, render the main document
     */
    public void setTemporaryDocument(MarkdownText temporaryDocument) {
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
