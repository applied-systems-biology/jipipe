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

package org.hkijena.jipipe.ui.grapheditor.general.search;

import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.database.ExistingPipelineNodeDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabase;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseRole;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.DocumentChangeListener;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTextField;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A {@link JComboBox} that implements a search behavior
 */
public class NodeDatabaseSearchBox extends JIPipeWorkbenchPanel {
    private final SelectedEventEmitter selectedEventEmitter = new SelectedEventEmitter();
    private final JComboBox<JIPipeNodeDatabaseEntry> comboBox = new JComboBox<>();
    private final JIPipeNodeDatabaseRole databaseRole;
    private final JIPipeNodeDatabase database;
    private boolean allowExisting = true;
    private boolean allowNew = true;
    private final AtomicBoolean isReloading = new AtomicBoolean(false);
    private RankedDataModel rankedDataModel;

    /**
     * Creates a new instance
     */
    public NodeDatabaseSearchBox(JIPipeWorkbench workbench, JIPipeGraphCanvasUI canvasUI, JIPipeNodeDatabaseRole databaseRole, JIPipeNodeDatabase database) {
        super(workbench);
        this.databaseRole = databaseRole;
        this.database = database;
        initialize();
        reloadModel();
    }

    @Override
    public boolean requestFocusInWindow() {
        return comboBox.requestFocusInWindow();
    }

    public JComboBox<JIPipeNodeDatabaseEntry> getComboBox() {
        return comboBox;
    }

    public SelectedEventEmitter getSelectedEventEmitter() {
        return selectedEventEmitter;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setBackground(UIManager.getColor("TextField.background"));
        setBorder(BorderFactory.createEtchedBorder());

        comboBox.setEditable(true);
        comboBox.setEditor(new Editor());
        JXTextField textField = (JXTextField) comboBox.getEditor().getEditorComponent();
        textField.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                SwingUtilities.invokeLater(() -> {
                    comboBox.setPopupVisible(false);
                    reloadModel();
                    comboBox.setPopupVisible(true);
                });
            }
        });
        textField.setBorder(null);
        comboBox.setRenderer(new NodeDatabaseSearchBoxListCellRenderer());
//        comboBox.addItemListener(e -> {
////            comboBox.setPopupVisible(false);
////            getRootPane().requestFocusInWindow();
//        });
        comboBox.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                postItemSelectedEvent();
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {

            }
        });
        add(comboBox, BorderLayout.CENTER);

        for (int i = 0; i < comboBox.getComponentCount(); i++) {
            Component component = comboBox.getComponent(i);
            if (component instanceof AbstractButton) {
                UIUtils.setStandardButtonBorder((AbstractButton) component);
                ((AbstractButton) component).setBorder(null);
                ((AbstractButton) component).setOpaque(true);
                component.setBackground(UIManager.getColor("TextField.background"));
                break;
            }
        }

        JButton clearButton = new JButton(UIUtils.getIconFromResources("actions/edit-clear.png"));
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

        this.rankedDataModel = new RankedDataModel(this);
        comboBox.setModel(rankedDataModel);
    }

    public JIPipeNodeDatabase getDatabase() {
        return database;
    }

    public boolean isAllowExisting() {
        return allowExisting;
    }

    public void setAllowExisting(boolean allowExisting) {
        this.allowExisting = allowExisting;
        reloadModel();
    }

    public boolean isAllowNew() {
        return allowNew;
    }

    public void setAllowNew(boolean allowNew) {
        this.allowNew = allowNew;
        reloadModel();
    }

    private void reloadModel() {
        rankedDataModel.updateRankedEntries();
    }

    private void postItemSelectedEvent() {
        if(!isReloading.get()) {
            selectedEventEmitter.emit(new SelectedEvent(this, getSelectedItem()));
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return comboBox.getEditor().getEditorComponent().getPreferredSize();
    }

    /**
     * Clears the search field
     */
    public void clearSearch() {
        JXTextField textField = (JXTextField) comboBox.getEditor().getEditorComponent();
        textField.setText(null);
    }
    public JIPipeNodeDatabaseEntry getSelectedItem() {
        return (JIPipeNodeDatabaseEntry) comboBox.getSelectedItem();
    }

    public void setSelectedItem(JIPipeNodeDatabaseEntry item) {
        comboBox.setSelectedItem(item);
    }

    public interface SelectedEventListener {
        void onSearchBoxSelectedEvent(SelectedEvent event);
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

    public static class SelectedEvent extends AbstractJIPipeEvent {
        private final NodeDatabaseSearchBox searchBox;
        private final JIPipeNodeDatabaseEntry value;

        public SelectedEvent(NodeDatabaseSearchBox searchBox, JIPipeNodeDatabaseEntry value) {
            super(searchBox);
            this.searchBox = searchBox;
            this.value = value;
        }

        public NodeDatabaseSearchBox getSearchBox() {
            return searchBox;
        }

        public JIPipeNodeDatabaseEntry getValue() {
            return value;
        }
    }

    private static class RankedDataModel extends AbstractListModel<JIPipeNodeDatabaseEntry> implements ComboBoxModel<JIPipeNodeDatabaseEntry> {
        private final NodeDatabaseSearchBox parent;
        private Object selectedItem;
        private boolean isLoading = false;
        private List<JIPipeNodeDatabaseEntry> rankedEntries = new ArrayList<>();

        public RankedDataModel(NodeDatabaseSearchBox parent) {
            this.parent = parent;
            updateRankedEntries();
        }

        public void updateRankedEntries() {
            isLoading = true;

            JXTextField textField = (JXTextField) parent.comboBox.getEditor().getEditorComponent();
            rankedEntries = parent.database.query(textField.getText(), parent.databaseRole, parent.allowExisting, parent.allowNew);

            if (selectedItem != null && !rankedEntries.contains(selectedItem)) {
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
            return rankedEntries.size();
        }

        @Override
        public JIPipeNodeDatabaseEntry getElementAt(int index) {
            return rankedEntries.get(index);
        }

    }

    public static class SelectedEventEmitter extends JIPipeEventEmitter<SelectedEvent, SelectedEventListener> {
        @Override
        protected void call(SelectedEventListener tSelectedEventListener, SelectedEvent event) {
            tSelectedEventListener.onSearchBoxSelectedEvent(event);
        }
    }
}
