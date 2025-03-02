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

package org.hkijena.jipipe.desktop.app.documentation;

import com.google.common.base.Charsets;
import com.vladsch.flexmark.pdf.converter.PdfConverterExtension;
import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopMarkdownReader;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextField;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.utils.JIPipeDesktopSplitPane;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.BusyCursor;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopMarkdownReader.OPTIONS;

/**
 * A browsable list of help pages
 */
public abstract class JIPipeDesktopCompendiumUI<T> extends JPanel {
    private final Map<Object, MarkdownText> compendiumCache = new HashMap<>();
    private final MarkdownText defaultDocument;
    private JList<T> itemList;
    private JIPipeDesktopSearchTextField searchField;
    private JSplitPane splitPane;
    private JIPipeDesktopMarkdownReader markdownReader;

    /**
     * Creates a new instance
     *
     * @param defaultDocument the documentation that is shown by default
     */
    public JIPipeDesktopCompendiumUI(MarkdownText defaultDocument) {
        this.defaultDocument = defaultDocument;
        initialize();
        reloadList();
        itemList.setSelectedValue(null, false);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JPanel listPanel = new JPanel(new BorderLayout());
        markdownReader = new JIPipeDesktopMarkdownReader(true);

        splitPane = new JIPipeDesktopSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPanel, markdownReader, JIPipeDesktopSplitPane.RATIO_1_TO_3);

        initializeToolbar(listPanel);
        initializeList(listPanel);

        add(splitPane, BorderLayout.CENTER);
    }

    private void initializeToolbar(JPanel contentPanel) {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        searchField = new JIPipeDesktopSearchTextField();
        searchField.addActionListener(e -> reloadList());
        toolBar.add(searchField);

        JButton exportButton = new JButton(UIUtils.getIconFromResources("actions/document-export.png"));
        exportButton.setToolTipText("Export whole compendium");
        JPopupMenu exportMenu = UIUtils.addPopupMenuToButton(exportButton);
        JMenuItem saveMarkdown = new JMenuItem("as Markdown (*.md)", UIUtils.getIconFromResources("mimetypes/text-markdown.png"));
        saveMarkdown.addActionListener(e -> {
            Path selectedPath = JIPipeFileChooserApplicationSettings.saveFile(this, null, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, "Save as Markdown (*.md)", UIUtils.EXTENSION_FILTER_MD);
            if (selectedPath != null) {
                try (BusyCursor cursor = new BusyCursor(this)) {
                    MarkdownText wholeCompendium = generateWholeCompendium(false);
                    try {
                        Files.write(selectedPath, wholeCompendium.getMarkdown().getBytes(Charsets.UTF_8));
                    } catch (IOException e1) {
                        throw new RuntimeException(e1);
                    }
                }
            }
        });
        exportMenu.add(saveMarkdown);

        JMenuItem saveHTML = new JMenuItem("as HTML (*.html)", UIUtils.getIconFromResources("mimetypes/text-html.png"));
        saveHTML.addActionListener(e -> {
            Path selectedPath = JIPipeFileChooserApplicationSettings.saveFile(this, null, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, "Save as HTML (*.html)", UIUtils.EXTENSION_FILTER_HTML);
            if (selectedPath != null) {
                try (BusyCursor cursor = new BusyCursor(this)) {
                    try {
                        MarkdownText wholeCompendium = generateWholeCompendium(false);
                        Files.write(selectedPath, wholeCompendium.getRenderedHTML().getBytes(Charsets.UTF_8));
                    } catch (IOException e1) {
                        throw new RuntimeException(e1);
                    }
                }
            }
        });
        exportMenu.add(saveHTML);

        JMenuItem savePDF = new JMenuItem("as PDF (*.pdf)", UIUtils.getIconFromResources("mimetypes/application-pdf.png"));
        savePDF.addActionListener(e -> {
            Path selectedPath = JIPipeFileChooserApplicationSettings.saveFile(this, null, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, "Save as Portable Document Format (*.pdf)", UIUtils.EXTENSION_FILTER_PDF);
            if (selectedPath != null) {
                try (BusyCursor cursor = new BusyCursor(this)) {
                    MarkdownText wholeCompendium = generateWholeCompendium(true);
                    PdfConverterExtension.exportToPdf(selectedPath.toString(), wholeCompendium.getRenderedHTML(), "", OPTIONS);
                }
            }
        });
        exportMenu.add(savePDF);

        toolBar.add(exportButton);

        add(toolBar, BorderLayout.NORTH);

        contentPanel.add(toolBar, BorderLayout.NORTH);
    }

    /**
     * Generates a document that contains the whole compendium
     *
     * @param forJava if the generated markdown is intended for usage within Java. Otherwise, more modern HTML code can be used
     * @return the document
     */
    public MarkdownText generateWholeCompendium(boolean forJava) {
        StringBuilder builder = new StringBuilder();
        builder.append(defaultDocument.getMarkdown()).append("\n\n");
        for (T item : getFilteredItems()) {
            builder.append(generateCompendiumFor(item, forJava).getMarkdown()).append("\n\n");
        }
        return new MarkdownText(builder.toString());
    }

    /**
     * Returns all items that should be available
     *
     * @return all items that should be available
     */
    protected abstract List<T> getFilteredItems();

    public void reloadList() {
        DefaultListModel<T> model = new DefaultListModel<>();
        for (T item : getFilteredItems()) {
            model.addElement(item);
        }
        itemList.setModel(model);

        if (!model.isEmpty())
            itemList.setSelectedIndex(0);
        else
            selectItem(null);
    }

    public JIPipeDesktopSearchTextField getSearchField() {
        return searchField;
    }

    /**
     * Returns the renderer for the items
     *
     * @return the renderer
     */
    protected abstract ListCellRenderer<T> getItemListRenderer();

    private void initializeList(JPanel listPanel) {
        itemList = new JList<>();
        itemList.setBorder(UIUtils.createControlBorder());
        itemList.setCellRenderer(getItemListRenderer());
        itemList.setModel(new DefaultListModel<>());
        itemList.addListSelectionListener(e -> {
            selectItem(itemList.getSelectedValue());
        });
        JScrollPane scrollPane = new JScrollPane(itemList);
        listPanel.add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Displays the documentation of the specified algorithm
     *
     * @param item the algorithm. if null, the compendium documentation is shown
     */
    public void selectItem(T item) {
        if (item == null)
            return;
        if (item != itemList.getSelectedValue()) {
            itemList.setSelectedValue(item, true);
        }
        MarkdownText document = compendiumCache.getOrDefault(item, null);
        if (document == null) {
            document = generateCompendiumFor(item, true);
            compendiumCache.put(item, document);
        }
        markdownReader.setDocument(document);
    }

    /**
     * Generates the compendium page for the item
     *
     * @param item    the item
     * @param forJava if the generated markdown is intended for usage within Java. Otherwise, more modern HTML code can be used
     * @return compendium page
     */
    public abstract MarkdownText generateCompendiumFor(T item, boolean forJava);
}
