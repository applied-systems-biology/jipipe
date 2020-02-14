/*
 * Copyright by Ruman Gerst
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Insitute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * This code is licensed under BSD 2-Clause
 * See the LICENSE file provided with this code for the full license.
 */

package org.hkijena.acaq5.ui.components;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.pdf.converter.PdfConverterExtension;
import com.vladsch.flexmark.util.options.MutableDataHolder;
import com.vladsch.flexmark.util.options.MutableDataSet;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.hkijena.acaq5.utils.UIUtils;

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
import java.util.Arrays;

/**
 * Panel that allows reading of Markdown data
 */
public class MarkdownReader extends JPanel {

    static final MutableDataHolder OPTIONS = new MutableDataSet()
            .set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), AutolinkExtension.create(), TocExtension.create()));
    static final String[] CSS_RULES = {"body { font-family: \"Sans-serif\"; }",
            "pre { background-color: #f5f2f0; border: 3px #f5f2f0 solid; }",
            "code { background-color: #f5f2f0; }",
            "h2 { padding-top: 30px; }",
            "h3 { padding-top: 30px; }",
            "th { border-bottom: 1px solid #c8c8c8; }",
            ".toc-list { list-style: none; }"};

    private JScrollPane scrollPane;
    private JTextPane content;
    private String markdown;
    private String defaultDocumentMarkdown;

    public MarkdownReader(boolean withToolbar) {
        initialize(withToolbar);
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

        if(withToolbar) {
            JToolBar toolBar = new JToolBar();

            JButton exportButton = new JButton("Export", UIUtils.getIconFromResources("save.png"));
            JPopupMenu exportMenu = UIUtils.addPopupMenuToComponent(exportButton);

            JMenuItem saveMarkdown = new JMenuItem("as Markdown (*.md)", UIUtils.getIconFromResources("filetype-markdown.png"));
            saveMarkdown.addActionListener(e -> {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Save as Markdown");
                if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    try {
                        Files.write(fileChooser.getSelectedFile().toPath(), markdown.getBytes(Charsets.UTF_8));
                    } catch (IOException e1) {
                        throw new RuntimeException(e1);
                    }
                }
            });
            exportMenu.add(saveMarkdown);

            JMenuItem saveHTML = new JMenuItem("as HTML (*.html)", UIUtils.getIconFromResources("filetype-html.png"));
            saveHTML.addActionListener(e -> {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Save as HTML");
                if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    try {
                        Files.write(fileChooser.getSelectedFile().toPath(), toHTML().getBytes(Charsets.UTF_8));
                    } catch (IOException e1) {
                        throw new RuntimeException(e1);
                    }
                }
            });
            exportMenu.add(saveHTML);

            JMenuItem savePDF = new JMenuItem("as PDF (*.pdf)", UIUtils.getIconFromResources("filetype-pdf.png"));
            savePDF.addActionListener(e -> {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Save as PDF");
                if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    PdfConverterExtension.exportToPdf(fileChooser.getSelectedFile().toString(), toHTML(), "", OPTIONS);
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

    /**
     * Custom "scroll to reference"
     *
     * @param var1
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

    public void setMarkdown(String markdown) {
        if(markdown == null)
            markdown = "";
        if(markdown.equals(this.markdown))
            return;
        this.markdown = markdown;
        Parser parser = Parser.builder(OPTIONS).build();
        Node document = parser.parse(markdown);
        HtmlRenderer renderer = HtmlRenderer.builder(OPTIONS).build();
        String html = renderer.render(document);
        content.setText(html);
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));
    }

    public void loadFromResource(String resourcePath) {
        if(resourcePath == null) {
            setMarkdown(defaultDocumentMarkdown);
            return;
        }
        try {
            String md = Resources.toString(ResourceUtils.getPluginResource(resourcePath), Charsets.UTF_8);
            md = md.replace("image://", ResourceUtils.getPluginResource("").toString());
            setMarkdown(md);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getMarkdown() {
        return markdown;
    }

    public String getDefaultDocumentMarkdown() {
        return defaultDocumentMarkdown;
    }

    public void setDefaultDocumentMarkdown(String defaultDocumentMarkdown) {
        this.defaultDocumentMarkdown = defaultDocumentMarkdown;
        setMarkdown(defaultDocumentMarkdown);
    }

    public void loadDefaultDocument(String resourcePath) {
        if(resourcePath == null)
            return;
        try {
            String md = Resources.toString(ResourceUtils.getPluginResource(resourcePath), Charsets.UTF_8);
            md = md.replace("image://", ResourceUtils.getPluginResource("").toString());
            setDefaultDocumentMarkdown(md);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
