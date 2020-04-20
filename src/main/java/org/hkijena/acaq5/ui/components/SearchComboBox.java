package org.hkijena.acaq5.ui.components;

import org.jdesktop.swingx.JXTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * A {@link JComboBox} that implements a search behavior
 *
 * @param <T>
 */
public class SearchComboBox<T> extends JComboBox<T> {

    private BiPredicate<T, String> filterFunction = (x, s) -> true;
    private FilteringModel<T> filteringModel;

    /**
     * Creates a new instance
     */
    public SearchComboBox() {
        setEditable(true);
        setEditor(new Editor());
        getEditor().getEditorComponent().addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (isDisplayable())
                    SearchComboBox.this.showPopup();
            }
        });
        JXTextField textField = (JXTextField) getEditor().getEditorComponent();
        textField.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                SwingUtilities.invokeLater(() -> {
                    setPopupVisible(false);
                    filteringModel.updateFilter();
                    setPopupVisible(true);
                });
            }
        });
        addItemListener(e -> setPopupVisible(false));
    }

    @Override
    public Dimension getPreferredSize() {
        return getEditor().getEditorComponent().getPreferredSize();
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
        JXTextField textField = (JXTextField) getEditor().getEditorComponent();
        textField.setText(null);
    }

    @Override
    public ComboBoxModel<T> getModel() {
        return filteringModel.unfilteredModel;
    }

    @Override
    public void setModel(ComboBoxModel<T> aModel) {
        filteringModel = new FilteringModel<>(aModel, this);
        super.setModel(filteringModel);
    }

    /**
     * Model that implements filtering
     *
     * @param <T> type
     */
    private static class FilteringModel<T> extends AbstractListModel<T> implements ComboBoxModel<T>, ListDataListener {
        private ComboBoxModel<T> unfilteredModel;
        private SearchComboBox<T> parent;
        private List<T> data = new ArrayList<>();
        private Object selectedItem;
        private boolean isLoading = false;
        private List<ListDataListener> listDataListeners = new ArrayList<>();

        private FilteringModel(ComboBoxModel<T> unfilteredModel, SearchComboBox<T> parent) {
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
            JXTextField editor = (JXTextField) parent.getEditor().getEditorComponent();
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
