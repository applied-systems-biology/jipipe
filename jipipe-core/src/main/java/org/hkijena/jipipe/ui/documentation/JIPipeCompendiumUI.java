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

package org.hkijena.jipipe.ui.documentation;

import com.google.common.base.Charsets;
import com.vladsch.flexmark.pdf.converter.PdfConverterExtension;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.markdown.MarkdownReader;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
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

import static org.hkijena.jipipe.ui.components.markdown.MarkdownReader.OPTIONS;

/**
 * A browsable list of help pages
 */
public abstract class JIPipeCompendiumUI<T> extends JPanel {
    private final Map<Object, MarkdownDocument> compendiumCache = new HashMap<>();
    private final MarkdownDocument defaultDocument;
    private JList<T> itemList;
    private SearchTextField searchField;
    private JSplitPane splitPane;
    private MarkdownReader markdownReader;

    /**
     * Creates a new instance
     *
     * @param defaultDocument the documentation that is shown by default
     */
    public JIPipeCompendiumUI(MarkdownDocument defaultDocument) {
        this.defaultDocument = defaultDocument;
        initialize();
        reloadAlgorithmList();
        itemList.setSelectedValue(null, false);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JPanel listPanel = new JPanel(new BorderLayout());
        markdownReader = new MarkdownReader(true);

        splitPane = new AutoResizeSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPanel, markdownReader, AutoResizeSplitPane.RATIO_1_TO_3);

        initializeToolbar(listPanel);
        initializeList(listPanel);

        add(splitPane, BorderLayout.CENTER);
    }

    private void initializeToolbar(JPanel contentPanel) {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        searchField = new SearchTextField();
        searchField.addActionListener(e -> reloadAlgorithmList());
        toolBar.add(searchField);

        JButton exportButton = new JButton(UIUtils.getIconFromResources("actions/document-export.png"));
        exportButton.setToolTipText("Export whole compendium");
        JPopupMenu exportMenu = UIUtils.addPopupMenuToComponent(exportButton);
        JMenuItem saveMarkdown = new JMenuItem("as Markdown (*.md)", UIUtils.getIconFromResources("mimetypes/text-markdown.png"));
        saveMarkdown.addActionListener(e -> {
            Path selectedPath = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Projects, "Save as Markdown (*.md)", UIUtils.EXTENSION_FILTER_MD);
            if (selectedPath != null) {
                try (BusyCursor cursor = new BusyCursor(this)) {
                    MarkdownDocument wholeCompendium = generateWholeCompendium();
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
            Path selectedPath = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Projects, "Save as HTML (*.html)", UIUtils.EXTENSION_FILTER_HTML);
            if (selectedPath != null) {
                try (BusyCursor cursor = new BusyCursor(this)) {
                    try {
                        MarkdownDocument wholeCompendium = generateWholeCompendium();
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
            Path selectedPath = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Projects, "Save as Portable Document Format (*.pdf)", UIUtils.EXTENSION_FILTER_PDF);
            if (selectedPath != null) {
                try (BusyCursor cursor = new BusyCursor(this)) {
                    MarkdownDocument wholeCompendium = generateWholeCompendium();
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
     * @return the document
     */
    public MarkdownDocument generateWholeCompendium() {
        StringBuilder builder = new StringBuilder();
        builder.append(defaultDocument.getMarkdown()).append("\n\n");
        for (T item : getFilteredItems()) {
            builder.append(generateCompendiumFor(item).getMarkdown()).append("\n\n");
        }
        return new MarkdownDocument(builder.toString());
    }

    /**
     * Returns all items that should be available
     *
     * @return all items that should be available
     */
    protected abstract List<T> getFilteredItems();

    private void reloadAlgorithmList() {
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

    public SearchTextField getSearchField() {
        return searchField;
    }

    /**
     * Returns the renderer for the items
     *
     * @return
     */
    protected abstract ListCellRenderer<T> getItemListRenderer();

    private void initializeList(JPanel listPanel) {
        itemList = new JList<>();
        itemList.setBorder(BorderFactory.createEtchedBorder());
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
        if (item != itemList.getSelectedValue()) {
            itemList.setSelectedValue(item, true);
        }
        if (item != null) {
            MarkdownDocument document = compendiumCache.getOrDefault(item, null);
            if (document == null) {
                document = generateCompendiumFor(item);
                compendiumCache.put(item, document);
            }
            markdownReader.setDocument(document);
        } else {
            markdownReader.setDocument(defaultDocument);
        }
    }

    /**
     * Generates the compendium page for the item
     *
     * @param item the item
     * @return compendium page
     */
    protected abstract MarkdownDocument generateCompendiumFor(T item);
}
