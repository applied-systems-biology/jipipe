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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A {@link JComboBox} that implements a search behavior
 */
public class NodeDatabaseSearchBox extends JIPipeWorkbenchPanel {
    private final JIPipeGraphCanvasUI canvasUI;
    private final SelectedEventEmitter selectedEventEmitter = new SelectedEventEmitter();
    private final JComboBox<JIPipeNodeDatabaseEntry> comboBox = new JComboBox<>();
    private final JIPipeNodeDatabaseRole databaseRole;
    private final JIPipeNodeDatabase database;
    private boolean allowExisting = true;
    private boolean allowNew = true;
    private final AtomicBoolean isReloading = new AtomicBoolean(false);

    /**
     * Creates a new instance
     */
    public NodeDatabaseSearchBox(JIPipeWorkbench workbench, JIPipeGraphCanvasUI canvasUI, JIPipeNodeDatabaseRole databaseRole, JIPipeNodeDatabase database) {
        super(workbench);
        this.canvasUI = canvasUI;
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
        try {
            if(isReloading.getAndSet(true)) {
                return;
            }
            JXTextField textField = (JXTextField) comboBox.getEditor().getEditorComponent();
            DefaultComboBoxModel<JIPipeNodeDatabaseEntry> model = new DefaultComboBoxModel<>();
            for (JIPipeNodeDatabaseEntry entry : database.query(textField.getText(), databaseRole, allowExisting, allowNew)) {
                if (entry instanceof ExistingPipelineNodeDatabaseEntry) {
                    // Filter out from other compartments
                    JIPipeGraphNode graphNode = ((ExistingPipelineNodeDatabaseEntry) entry).getGraphNode();
                    if (canvasUI.getCompartment() != null && !Objects.equals(canvasUI.getCompartment(), graphNode.getCompartmentUUIDInParentGraph())) {
                        continue;
                    }
                }
                model.addElement(entry);
            }
            comboBox.setModel(model);
        }
        finally {
            isReloading.set(false);
        }
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

    public static class SelectedEventEmitter extends JIPipeEventEmitter<SelectedEvent, SelectedEventListener> {
        @Override
        protected void call(SelectedEventListener tSelectedEventListener, SelectedEvent event) {
            tSelectedEventListener.onSearchBoxSelectedEvent(event);
        }
    }
}
