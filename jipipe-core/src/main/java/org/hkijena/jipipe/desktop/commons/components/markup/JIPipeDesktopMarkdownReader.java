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
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernThemeStyle;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.ThemeUtils;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Panel that allows reading of Markdown data
 */
public class JIPipeDesktopMarkdownReader extends JPanel {

    public static final MutableDataHolder OPTIONS = new MutableDataSet()
            .set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), AutolinkExtension.create(), TocExtension.create()));
    private final List<String> cssRules = new ArrayList<>();
    private final JToolBar toolBar = new JToolBar();
    private JScrollPane scrollPane;
    private JTextPane content;
    private MarkdownText document;
    private MarkdownText temporaryDocument;

    public JIPipeDesktopMarkdownReader() {
        this(true, null);
    }

    /**
     * @param withToolbar if a toolbar should be shown
     */
    public JIPipeDesktopMarkdownReader(boolean withToolbar) {
        this(withToolbar, null);
    }

    /**
     * @param withToolbar if a toolbar should be shown
     * @param document    initialize with a document
     */
    public JIPipeDesktopMarkdownReader(boolean withToolbar, MarkdownText document) {
        initializeDefaultCSSRules();
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

    private void initializeDefaultCSSRules() {
        JIPipeDesktopModernThemeStyle style = ThemeUtils.getCurrentStyle();
        cssRules.add("body { font-family: \"Dialog\"; font-size: " + style.getFontSizeNormal() + "pt; color: " + ColorUtils.colorToHexString(style.getTextForeground()) + "; }");
        cssRules.add("pre { background-color: " + ColorUtils.colorToHexString(style.getSelectionBackground()) + "; border: 3px " + ColorUtils.colorToHexString(style.getBorderColor()) + " solid; }");
        cssRules.add("code { background-color: " + ColorUtils.colorToHexString(style.getFormBackground()) + "; border: none; }");
        cssRules.add("h1 { padding-top: 5px; font-weight bolder; font-size: " + style.getFontSizeHuge() + "pt }");
        cssRules.add("h2 { padding-top: 20px; font-size: " + style.getFontSizeLarge() + "pt }");
        cssRules.add("h3 { padding-top: 20px; font-size: " + style.getFontSizeLarge() + "pt }");
        cssRules.add("h4 { padding-top: 20px; font-size: " + style.getFontSizeNormal() + "pt }");
        cssRules.add("h5 { padding-top: 20px; font-size: " + style.getFontSizeNormal() + "pt }");
        cssRules.add("th { border-bottom: 1px solid " + ColorUtils.colorToHexString(style.getBorderColor()) + "; }");
        cssRules.add("a { color: " + ColorUtils.colorToHexString(style.getTextLink()) + "; }");
        cssRules.add(".toc-list { list-style: none; }");
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
                    UIUtils.desktopOpenURL(e.getURL().toString(), true);
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

            JButton exportButton = new JButton("Export", JIPipe.RESOURCES.getIcon16("actions/filesave.png"));
            JPopupMenu exportMenu = UIUtils.addPopupMenuToButton(exportButton);

            JMenuItem saveMarkdown = new JMenuItem("as Markdown (*.md)", JIPipe.RESOURCES.getIcon16("mimetypes/text-markdown.png"));
            saveMarkdown.addActionListener(e -> {
                Path selectedPath = JIPipeDesktop.saveFile(this, null, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, "Save as Markdown (*.md)", HTMLText.EMPTY, PathUtils.EXTENSION_FILTER_MD);
                if (selectedPath != null) {
                    try {
                        Files.write(selectedPath, document.getMarkdown().getBytes(Charsets.UTF_8));
                    } catch (IOException e1) {
                        throw new RuntimeException(e1);
                    }
                }
            });
            exportMenu.add(saveMarkdown);

            JMenuItem saveHTML = new JMenuItem("as HTML (*.html)", JIPipe.RESOURCES.getIcon16("mimetypes/text-html.png"));
            saveHTML.addActionListener(e -> {
                Path selectedPath = JIPipeDesktop.saveFile(this, null, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, "Save as HTML (*.html)", HTMLText.EMPTY, PathUtils.EXTENSION_FILTER_HTML);
                if (selectedPath != null) {
                    try {
                        Files.write(selectedPath, toHTML().getBytes(Charsets.UTF_8));
                    } catch (IOException e1) {
                        throw new RuntimeException(e1);
                    }
                }
            });
            exportMenu.add(saveHTML);

            JMenuItem savePDF = new JMenuItem("as PDF (*.pdf)", JIPipe.RESOURCES.getIcon16("mimetypes/application-pdf.png"));
            savePDF.addActionListener(e -> {
                Path selectedPath = JIPipeDesktop.saveFile(this, null, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, "Save as Portable Document Format (*.pdf)", HTMLText.EMPTY, PathUtils.EXTENSION_FILTER_PDF);
                if (selectedPath != null) {
                    PdfConverterExtension.exportToPdf(selectedPath.toString(), toHTML(), "", OPTIONS);
                }
            });
            exportMenu.add(savePDF);

            toolBar.add(exportButton);

//        JButton printButton = new JButton("Print", JIPipe.RESOURCES.getIcon16("print.png"));
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
        for (String rule : cssRules) {
            styleSheet.addRule(rule);
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
