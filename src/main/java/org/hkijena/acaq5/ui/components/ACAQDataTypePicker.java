package org.hkijena.acaq5.ui.components;

import com.google.common.eventbus.EventBus;
import com.google.common.primitives.Ints;
import org.hkijena.acaq5.api.data.ACAQDataDeclaration;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.hkijena.acaq5.utils.StringUtils;
import org.hkijena.acaq5.utils.UIUtils;
import org.jdesktop.swingx.JXTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Panel that allows to pick {@link org.hkijena.acaq5.api.data.ACAQDataDeclaration}
 */
public class ACAQDataTypePicker extends JPanel {

    boolean reloading = false;
    private Mode mode;
    private EventBus eventBus = new EventBus();
    private JXTextField searchField;
    private JList<ACAQDataDeclaration> dataTypeList;
    private Set<ACAQDataDeclaration> hiddenDataTypes = new HashSet<>();
    private List<ACAQDataDeclaration> availableDataTypes;
    private Set<ACAQDataDeclaration> selectedDataTypes = new HashSet<>();

    /**
     * @param mode               the mode
     * @param availableDataTypes list of available trait types
     */
    public ACAQDataTypePicker(Mode mode, Set<ACAQDataDeclaration> availableDataTypes) {
        this.mode = mode;
        this.availableDataTypes = new ArrayList<>();
        this.availableDataTypes.addAll(availableDataTypes.stream().sorted(Comparator.comparing(ACAQDataDeclaration::getName)).collect(Collectors.toList()));
        initialize();
        refreshTraitList();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        searchField = new JXTextField("Search ...");
        searchField.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                refreshTraitList();
            }
        });
        toolBar.add(searchField);

        JButton clearSearchButton = new JButton(UIUtils.getIconFromResources("clear.png"));
        clearSearchButton.addActionListener(e -> searchField.setText(null));
        toolBar.add(clearSearchButton);

        add(toolBar, BorderLayout.NORTH);

        dataTypeList = new JList<>(new DefaultListModel<>());
        dataTypeList.setCellRenderer(new Renderer());
        if (mode == Mode.NonInteractive) {
            dataTypeList.setEnabled(false);
        } else if (mode == Mode.Single) {
            dataTypeList.setSelectionModel(new SingleSelectionModel());
            dataTypeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        } else {
            dataTypeList.setSelectionModel(new MultiSelectionModel());
            dataTypeList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        }
        dataTypeList.addListSelectionListener(e -> updateSelection());
        add(new JScrollPane(dataTypeList), BorderLayout.CENTER);
    }

    private void updateSelection() {
        if (reloading)
            return;
        boolean changed = false;
        for (ACAQDataDeclaration trait : availableDataTypes) {
            if (hiddenDataTypes.contains(trait))
                continue;
            boolean uiSelected = dataTypeList.getSelectedValuesList().contains(trait);
            boolean modelSelected = selectedDataTypes.contains(trait);
            if (uiSelected != modelSelected) {
                if (modelSelected) {
                    selectedDataTypes.remove(trait);
                    eventBus.post(new DataTypeDeselectedEvent(this, trait));
                    changed = true;
                }
                if (uiSelected) {
                    selectedDataTypes.add(trait);
                    eventBus.post(new DataTypeSelectedEvent(this, trait));
                    changed = true;
                }
            }
        }

        if (changed) {
            eventBus.post(new SelectedDataTypesChangedEvent(this));
        }
    }

    /**
     * Refreshes the list
     */
    public void refreshTraitList() {
        if (reloading)
            return;
        reloading = true;
        DefaultListModel<ACAQDataDeclaration> model = (DefaultListModel<ACAQDataDeclaration>) dataTypeList.getModel();
        hiddenDataTypes.clear();
        model.clear();
        String[] searchStrings = getSearchStrings();
        List<Integer> selectedIndices = new ArrayList<>();

        for (ACAQDataDeclaration trait : availableDataTypes) {
            if (!searchStringsMatches(trait, searchStrings)) {
                hiddenDataTypes.add(trait);
                continue;
            }

            model.addElement(trait);
            if (selectedDataTypes.contains(trait)) {
                selectedIndices.add(model.size() - 1);
            }
        }
        dataTypeList.setSelectedIndices(Ints.toArray(selectedIndices));
        reloading = false;
    }

    private boolean searchStringsMatches(ACAQDataDeclaration trait, String[] strings) {
        if (trait == null)
            return true;
        if (strings == null)
            return true;
        String traitName = trait.getName() + " " + trait.getDescription();
        for (String str : strings) {
            if (traitName.toLowerCase().contains(str.toLowerCase()))
                return true;
        }
        return false;
    }

    private String[] getSearchStrings() {
        String[] searchStrings = null;
        if (searchField.getText() != null) {
            String str = searchField.getText().trim();
            if (!str.isEmpty()) {
                searchStrings = str.split(" ");
            }
        }
        return searchStrings;
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        refreshTraitList();
    }

    public Set<ACAQDataDeclaration> getSelectedDataTypes() {
        return Collections.unmodifiableSet(selectedDataTypes);
    }

    public void setSelectedDataTypes(Set<ACAQDataDeclaration> traits) {
        this.selectedDataTypes = new HashSet<>(traits);
        eventBus.post(new SelectedDataTypesChangedEvent(this));
        refreshTraitList();
    }

    /**
     * Shows a dialog to pick traits
     *
     * @param parent          parent component
     * @param mode            mode
     * @param availableTraits list of available traits
     * @return picked traits
     */
    public static Set<ACAQDataDeclaration> showDialog(Component parent, Mode mode, Set<ACAQDataDeclaration> availableTraits) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent));
        ACAQDataTypePicker picker = new ACAQDataTypePicker(mode, availableTraits);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(picker, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("remove.png"));
        cancelButton.addActionListener(e -> {
            picker.setSelectedDataTypes(Collections.emptySet());
            dialog.setVisible(false);
        });
        buttonPanel.add(cancelButton);

        JButton confirmButton = new JButton("Pick", UIUtils.getIconFromResources("checkmark.png"));
        confirmButton.addActionListener(e -> dialog.setVisible(false));
        buttonPanel.add(confirmButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.setTitle("Pick annotation");
        dialog.setModal(true);
        dialog.pack();
        dialog.setSize(new Dimension(500, 600));
        dialog.setLocationRelativeTo(parent);
        UIUtils.addEscapeListener(dialog);
        dialog.setVisible(true);

        return picker.getSelectedDataTypes();
    }

    /**
     * The mode of the picker
     */
    public enum Mode {
        NonInteractive,
        Single,
        Multiple
    }

    /**
     * Generated when a trait is selected
     */
    public static class SelectedDataTypesChangedEvent {
        private ACAQDataTypePicker dataTypePicker;

        /**
         * @param dataTypePicker event source
         */
        public SelectedDataTypesChangedEvent(ACAQDataTypePicker dataTypePicker) {
            this.dataTypePicker = dataTypePicker;
        }

        public ACAQDataTypePicker getDataTypePicker() {
            return dataTypePicker;
        }
    }

    /**
     * Renders an item
     */
    public static class Renderer extends JCheckBox implements ListCellRenderer<ACAQDataDeclaration> {

        /**
         * Creates a new renderer
         */
        public Renderer() {
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ACAQDataDeclaration> list, ACAQDataDeclaration value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value != null) {
                setText(StringUtils.createIconTextHTMLTable(value.getName(), ACAQUIDatatypeRegistry.getInstance().getIconURLFor(value)));
            } else {
                setText(StringUtils.createIconTextHTMLTable("Select none", ResourceUtils.getPluginResource("icons/remove.png")));
            }
            setSelected(isSelected);
            if (isSelected) {
                setBackground(new Color(184, 207, 229));
            } else {
                setBackground(new Color(255, 255, 255));
            }
            return this;
        }
    }

    /**
     * Generated when a trait is selected
     */
    public static class DataTypeSelectedEvent {
        private ACAQDataTypePicker dataTypePicker;
        private ACAQDataDeclaration dataDeclaration;

        /**
         * @param dataTypePicker  event source
         * @param dataDeclaration picked trait
         */
        public DataTypeSelectedEvent(ACAQDataTypePicker dataTypePicker, ACAQDataDeclaration dataDeclaration) {
            this.dataTypePicker = dataTypePicker;
            this.dataDeclaration = dataDeclaration;
        }

        public ACAQDataTypePicker getDataTypePicker() {
            return dataTypePicker;
        }

        public ACAQDataDeclaration getDataDeclaration() {
            return dataDeclaration;
        }
    }

    /**
     * Generated when a trait is deselected
     */
    public static class DataTypeDeselectedEvent {
        private ACAQDataTypePicker dataTypePicker;
        private ACAQDataDeclaration dataDeclaration;

        /**
         * @param dataTypePicker  event source
         * @param dataDeclaration deselected trait
         */
        public DataTypeDeselectedEvent(ACAQDataTypePicker dataTypePicker, ACAQDataDeclaration dataDeclaration) {
            this.dataTypePicker = dataTypePicker;
            this.dataDeclaration = dataDeclaration;
        }

        public ACAQDataTypePicker getDataTypePicker() {
            return dataTypePicker;
        }

        public ACAQDataDeclaration getDataDeclaration() {
            return dataDeclaration;
        }
    }

    /**
     * Describes how data is selected
     */
    public static class SingleSelectionModel extends DefaultListSelectionModel {
        @Override
        public void setSelectionInterval(int startIndex, int endIndex) {
            if (startIndex == endIndex) {
                if (multipleItemsAreCurrentlySelected()) {
                    clearSelection();
                }
                if (isSelectedIndex(startIndex)) {
                    clearSelection();
                } else {
                    super.setSelectionInterval(startIndex, endIndex);
                }
            }
            // User selected multiple items
            else {
                super.setSelectionInterval(startIndex, endIndex);
            }
        }

        private boolean multipleItemsAreCurrentlySelected() {
            return getMinSelectionIndex() != getMaxSelectionIndex();
        }
    }

    /**
     * Describes how data is selected
     */
    public static class MultiSelectionModel extends DefaultListSelectionModel {
        @Override
        public void setSelectionInterval(int startIndex, int endIndex) {
            if (startIndex == endIndex) {
                if (isSelectedIndex(startIndex)) {
                    removeSelectionInterval(startIndex, endIndex);
                } else {
                    super.addSelectionInterval(startIndex, endIndex);
                }
            } else {
                super.addSelectionInterval(startIndex, endIndex);
            }
        }

        private boolean multipleItemsAreCurrentlySelected() {
            return getMinSelectionIndex() != getMaxSelectionIndex();
        }
    }
}
