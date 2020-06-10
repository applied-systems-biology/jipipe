package org.hkijena.acaq5.ui.components;

import org.hkijena.acaq5.utils.UIUtils;
import org.jdesktop.swingx.JXTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * A {@link JComboBox} that implements a search behavior
 *
 * @param <T>
 */
public class SearchBox<T> extends JPanel {

    private BiPredicate<T, String> filterFunction = (x, s) -> true;
    private FilteringModel<T> filteringModel;
    private JComboBox<T> comboBox = new JComboBox<>();

    /**
     * Creates a new instance
     */
    public SearchBox() {
        initialize();
    }

    public JComboBox<T> getComboBox() {
        return comboBox;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEtchedBorder());

        comboBox.setEditable(true);
        comboBox.setEditor(new Editor());
        JXTextField textField = (JXTextField) comboBox.getEditor().getEditorComponent();
        textField.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                SwingUtilities.invokeLater(() -> {
                    comboBox.setPopupVisible(false);
                    filteringModel.updateFilter();
                    comboBox.setPopupVisible(true);
                });
            }
        });
        textField.setBorder(null);
        comboBox.addItemListener(e -> {
            comboBox.setPopupVisible(false);
            getRootPane().requestFocusInWindow();
        });
        add(comboBox, BorderLayout.CENTER);

        for (int i = 0; i < comboBox.getComponentCount(); i++) {
            Component component = comboBox.getComponent(i);
            if (component instanceof AbstractButton) {
                UIUtils.makeFlat((AbstractButton) component);
                ((AbstractButton) component).setBorder(null);
                ((AbstractButton) component).setOpaque(true);
                component.setBackground(Color.WHITE);
                break;
            }
        }

        JButton clearButton = new JButton(UIUtils.getIconFromResources("clear.png"));
        clearButton.setOpaque(false);
        clearButton.setToolTipText("Clear");
        clearButton.addActionListener(e -> clearSearch());
        UIUtils.makeFlat25x25(clearButton);
        clearButton.setBorder(null);
        add(clearButton, BorderLayout.EAST);

        textField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                comboBox.setPopupVisible(true);
            }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        return comboBox.getEditor().getEditorComponent().getPreferredSize();
    }

    public BiPredicate<T, String> getFilterFunction() {
        return filterFunction;
    }

    public void setFilterFunction(BiPredicate<T, String> filterFunction) {
        this.filterFunction = filterFunction;
    }

    /**
     * Clears the search field
     */
    public void clearSearch() {
        JXTextField textField = (JXTextField) comboBox.getEditor().getEditorComponent();
        textField.setText(null);
    }

    public ComboBoxModel<T> getModel() {
        return filteringModel.unfilteredModel;
    }

    public void setModel(ComboBoxModel<T> aModel) {
        filteringModel = new FilteringModel<>(aModel, this);
        comboBox.setModel(filteringModel);
    }

    public void setRenderer(ListCellRenderer<T> renderer) {
        comboBox.setRenderer(renderer);
    }

    /**
     * Adds an item listener
     *
     * @param listener the listener
     */
    public void addItemListener(ItemListener listener) {
        comboBox.addItemListener(listener);
    }

    public T getSelectedItem() {
        return (T) comboBox.getSelectedItem();
    }

    public void setSelectedItem(T item) {
        comboBox.setSelectedItem(item);
    }

    /**
     * Model that implements filtering
     *
     * @param <T> type
     */
    private static class FilteringModel<T> extends AbstractListModel<T> implements ComboBoxModel<T>, ListDataListener {
        private ComboBoxModel<T> unfilteredModel;
        private SearchBox<T> parent;
        private List<T> data = new ArrayList<>();
        private Object selectedItem;
        private boolean isLoading = false;
        private List<ListDataListener> listDataListeners = new ArrayList<>();

        private FilteringModel(ComboBoxModel<T> unfilteredModel, SearchBox<T> parent) {
            this.unfilteredModel = unfilteredModel;
            this.parent = parent;

            unfilteredModel.addListDataListener(this);
        }

        @Override
        public void intervalAdded(ListDataEvent e) {
            updateFilter();
        }

        @Override
        public void intervalRemoved(ListDataEvent e) {
            updateFilter();
        }

        @Override
        public void contentsChanged(ListDataEvent e) {
            updateFilter();
        }

        private String[] getSearchStrings() {
            String[] searchStrings = null;
            JXTextField editor = (JXTextField) parent.comboBox.getEditor().getEditorComponent();
            if (editor.getText() != null) {
                String str = editor.getText().trim().toLowerCase();
                if (!str.isEmpty()) {
                    searchStrings = str.split(" ");
                }
            }
            return searchStrings;
        }

        public void updateFilter() {
            isLoading = true;
            data.clear();
            String[] searchStrings = getSearchStrings();

            if (searchStrings == null || searchStrings.length == 0) {
                for (int i = 0; i < unfilteredModel.getSize(); ++i) {
                    data.add(unfilteredModel.getElementAt(i));
                }
            } else {
                for (int i = 0; i < unfilteredModel.getSize(); ++i) {
                    T element = unfilteredModel.getElementAt(i);
                    boolean matches = true;
                    for (String searchString : searchStrings) {
                        if (!parent.filterFunction.test(element, searchString)) {
                            matches = false;
                            break;
                        }
                    }
                    if (matches) {
                        data.add(element);
                    }
                }
            }
            if (selectedItem != null && !data.contains(selectedItem)) {
                selectedItem = null;
            }
            isLoading = false;
            fireContentsChanged(this, -1, -1);
        }

        @Override
        public Object getSelectedItem() {
            return selectedItem;
        }

        @Override
        public void setSelectedItem(Object anItem) {
            if (isLoading)
                return;
            selectedItem = anItem;
            fireContentsChanged(this, -1, -1);
        }

        @Override
        public int getSize() {
            return data.size();
        }

        @Override
        public T getElementAt(int index) {
            return data.get(index);
        }

    }

    /**
     * Custom editor component that shows a prompt
     */
    private static class Editor extends JXTextField implements ComboBoxEditor {

        public Editor() {
            setPrompt("Search ...");
        }

        @Override
        public Component getEditorComponent() {
            return this;
        }

        @Override
        public Object getItem() {
            return null;
        }

        @Override
        public void setItem(Object anObject) {
        }
    }
}
