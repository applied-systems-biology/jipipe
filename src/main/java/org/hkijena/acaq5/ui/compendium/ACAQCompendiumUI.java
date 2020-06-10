package org.hkijena.acaq5.ui.compendium;

import com.google.common.base.Charsets;
import com.vladsch.flexmark.pdf.converter.PdfConverterExtension;
import org.hkijena.acaq5.extensions.settings.FileChooserSettings;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.components.MarkdownReader;
import org.hkijena.acaq5.ui.components.SearchTextField;
import org.hkijena.acaq5.utils.BusyCursor;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hkijena.acaq5.ui.components.MarkdownReader.OPTIONS;

/**
 * A browsable list of help pages
 */
public abstract class ACAQCompendiumUI<T> extends JPanel {
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
    public ACAQCompendiumUI(MarkdownDocument defaultDocument) {
        this.defaultDocument = defaultDocument;
        initialize();
        reloadAlgorithmList();
        itemList.setSelectedValue(null, false);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JPanel listPanel = new JPanel(new BorderLayout());
        markdownReader = new MarkdownReader(true);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPanel, markdownReader);
        splitPane.setDividerSize(3);
        splitPane.setResizeWeight(0.33);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                splitPane.setDividerLocation(0.33);
            }
        });

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

        JButton exportButton = new JButton(UIUtils.getIconFromResources("export.png"));
        exportButton.setToolTipText("Export whole compendium");
        JPopupMenu exportMenu = UIUtils.addPopupMenuToComponent(exportButton);
        JMenuItem saveMarkdown = new JMenuItem("as Markdown (*.md)", UIUtils.getIconFromResources("filetype-markdown.png"));
        saveMarkdown.addActionListener(e -> {
            Path selectedPath = FileChooserSettings.saveFile(this, FileChooserSettings.KEY_PROJECT, "Save as Markdown (*.md)");
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

        JMenuItem saveHTML = new JMenuItem("as HTML (*.html)", UIUtils.getIconFromResources("filetype-html.png"));
        saveHTML.addActionListener(e -> {
            Path selectedPath = FileChooserSettings.saveFile(this, FileChooserSettings.KEY_PROJECT, "Save as HTML (*.html)");
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

        JMenuItem savePDF = new JMenuItem("as PDF (*.pdf)", UIUtils.getIconFromResources("filetype-pdf.png"));
        savePDF.addActionListener(e -> {
            Path selectedPath = FileChooserSettings.saveFile(this, FileChooserSettings.KEY_PROJECT, "Save as Portable Document Format (*.pdf)");
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
        for (T item : getFilteredItems(null)) {
            builder.append(generateCompendiumFor(item).getMarkdown()).append("\n\n");
        }
        return new MarkdownDocument(builder.toString());
    }

    /**
     * Returns all items that should be available
     *
     * @param searchStrings the search strings
     * @return all items that should be available
     */
    protected abstract List<T> getFilteredItems(String[] searchStrings);

    private void reloadAlgorithmList() {
        DefaultListModel<T> model = new DefaultListModel<>();
        for (T item : getFilteredItems(searchField.getSearchStrings())) {
            model.addElement(item);
        }
        itemList.setModel(model);

        if (!model.isEmpty())
            itemList.setSelectedIndex(0);
        else
            selectItem(null);
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
        listPanel.add(new JScrollPane(itemList), BorderLayout.CENTER);
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
