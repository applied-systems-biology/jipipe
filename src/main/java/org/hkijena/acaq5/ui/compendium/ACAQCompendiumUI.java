package org.hkijena.acaq5.ui.compendium;

import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.components.MarkdownReader;
import org.hkijena.acaq5.ui.components.SearchTextField;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import java.util.*;

/**
 * A browsable list of help pages
 */
public abstract class ACAQCompendiumUI<T> extends JPanel {
    private JList<T> itemList;
    private SearchTextField searchField;
    private JSplitPane splitPane;
    private MarkdownReader markdownReader;
    private final Map<Object, MarkdownDocument> compendiumCache = new HashMap<>();
    private final MarkdownDocument defaultDocument;

    /**
     * Creates a new instance
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

        add(toolBar, BorderLayout.NORTH);

        contentPanel.add(toolBar, BorderLayout.NORTH);
    }

    /**
     * Returns all items that should be available
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
     * @param item the item
     * @return compendium page
     */
    protected abstract MarkdownDocument generateCompendiumFor(T item);
}
