package org.hkijena.acaq5.ui.components;

import com.google.common.eventbus.EventBus;
import com.google.common.primitives.Ints;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.ui.registries.ACAQUITraitRegistry;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.hkijena.acaq5.utils.StringUtils;
import org.hkijena.acaq5.utils.UIUtils;
import org.jdesktop.swingx.JXTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Panel that allows to pick {@link ACAQTraitDeclaration}
 */
public class ACAQTraitPicker extends JPanel {

    private Mode mode;

    private EventBus eventBus = new EventBus();
    private JXTextField searchField;
    private JList<ACAQTraitDeclaration> traitList;
    private Set<ACAQTraitDeclaration> hiddenTraits = new HashSet<>();
    private List<ACAQTraitDeclaration> availableTraits;
    private Set<ACAQTraitDeclaration> selectedTraits = new HashSet<>();
    boolean reloading = false;

    /**
     * @param mode            the mode
     * @param availableTraits list of available trait types
     */
    public ACAQTraitPicker(Mode mode, Set<ACAQTraitDeclaration> availableTraits) {
        this.mode = mode;
        this.availableTraits = new ArrayList<>();
        this.availableTraits.addAll(availableTraits.stream().sorted(Comparator.comparing(ACAQTraitDeclaration::getName)).collect(Collectors.toList()));
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

        traitList = new JList<>(new DefaultListModel<>());
        traitList.setCellRenderer(new Renderer());
        if(mode == Mode.NonInteractive) {
            traitList.setEnabled(false);
        }
        else if(mode == Mode.Single) {
            traitList.setSelectionModel(new SingleSelectionModel());
            traitList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        }
        else {
            traitList.setSelectionModel(new MultiSelectionModel());
            traitList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        }
        traitList.addListSelectionListener(e -> updateSelection());
        add(new JScrollPane(traitList), BorderLayout.CENTER);
    }

    private void updateSelection() {
        if(reloading)
            return;
        boolean changed = false;
        for (ACAQTraitDeclaration trait : availableTraits) {
            if(hiddenTraits.contains(trait))
                continue;
            boolean uiSelected = traitList.getSelectedValuesList().contains(trait);
            boolean modelSelected = selectedTraits.contains(trait);
            if(uiSelected != modelSelected) {
                if(modelSelected) {
                    selectedTraits.remove(trait);
                    eventBus.post(new TraitDeselectedEvent(this, trait));
                    changed = true;
                }
                if(uiSelected) {
                    selectedTraits.add(trait);
                    eventBus.post(new TraitSelectedEvent(this, trait));
                    changed = true;
                }
            }
        }

        if(changed) {
            eventBus.post(new SelectedTraitsChangedEvent(this));
        }
    }

    /**
     * Refreshes the list
     */
    public void refreshTraitList() {
        reloading = true;
        DefaultListModel<ACAQTraitDeclaration> model = (DefaultListModel<ACAQTraitDeclaration>) traitList.getModel();
        hiddenTraits.clear();
        model.clear();
        String[] searchStrings = getSearchStrings();
        List<Integer> selectedIndices = new ArrayList<>();

        for (ACAQTraitDeclaration trait : availableTraits) {
            if (!searchStringsMatches(trait, searchStrings)) {
                hiddenTraits.add(trait);
                continue;
            }

            model.addElement(trait);
            if(selectedTraits.contains(trait)) {
                selectedIndices.add(model.size() - 1);
            }
        }
        traitList.setSelectedIndices(Ints.toArray(selectedIndices));
        reloading = false;
    }

    private boolean searchStringsMatches(ACAQTraitDeclaration trait, String[] strings) {
        if(trait == null)
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

    public Set<ACAQTraitDeclaration> getSelectedTraits() {
        return Collections.unmodifiableSet(selectedTraits);
    }

    public void setSelectedTraits(Set<ACAQTraitDeclaration> traits) {
        this.selectedTraits = new HashSet<>(traits);
        eventBus.post(new SelectedTraitsChangedEvent(this));
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
    public static Set<ACAQTraitDeclaration> showDialog(Component parent, Mode mode, Set<ACAQTraitDeclaration> availableTraits) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent));
        ACAQTraitPicker picker = new ACAQTraitPicker(mode, availableTraits);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(picker, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("remove.png"));
        cancelButton.addActionListener(e -> {
            picker.setSelectedTraits(Collections.emptySet());
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

        return picker.getSelectedTraits();
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
    public static class SelectedTraitsChangedEvent {
        private ACAQTraitPicker traitPicker;

        /**
         * @param traitPicker event source
         */
        public SelectedTraitsChangedEvent(ACAQTraitPicker traitPicker) {
            this.traitPicker = traitPicker;
        }

        public ACAQTraitPicker getTraitPicker() {
            return traitPicker;
        }
    }

    public static class Renderer extends JCheckBox implements ListCellRenderer<ACAQTraitDeclaration> {

        public Renderer() {
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ACAQTraitDeclaration> list, ACAQTraitDeclaration value, int index, boolean isSelected, boolean cellHasFocus) {
            if(value != null) {
                setText(StringUtils.createIconTextHTMLTable(value.getName(), ACAQUITraitRegistry.getInstance().getIconURLFor(value)));
            }
            else{
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
    public static class TraitSelectedEvent {
        private ACAQTraitPicker traitPicker;
        private ACAQTraitDeclaration traitDeclaration;

        /**
         * @param traitPicker      event source
         * @param traitDeclaration picked trait
         */
        public TraitSelectedEvent(ACAQTraitPicker traitPicker, ACAQTraitDeclaration traitDeclaration) {
            this.traitPicker = traitPicker;
            this.traitDeclaration = traitDeclaration;
        }

        public ACAQTraitPicker getTraitPicker() {
            return traitPicker;
        }

        public ACAQTraitDeclaration getTraitDeclaration() {
            return traitDeclaration;
        }
    }

    /**
     * Generated when a trait is deselected
     */
    public static class TraitDeselectedEvent {
        private ACAQTraitPicker traitPicker;
        private ACAQTraitDeclaration traitDeclaration;

        /**
         * @param traitPicker      event source
         * @param traitDeclaration deselected trait
         */
        public TraitDeselectedEvent(ACAQTraitPicker traitPicker, ACAQTraitDeclaration traitDeclaration) {
            this.traitPicker = traitPicker;
            this.traitDeclaration = traitDeclaration;
        }

        public ACAQTraitPicker getTraitPicker() {
            return traitPicker;
        }

        public ACAQTraitDeclaration getTraitDeclaration() {
            return traitDeclaration;
        }
    }

    public static class SingleSelectionModel extends DefaultListSelectionModel {
        @Override
        public void setSelectionInterval(int startIndex, int endIndex) {
            if (startIndex == endIndex) {
                if (multipleItemsAreCurrentlySelected()) {
                    clearSelection();
                }
                if (isSelectedIndex(startIndex)) {
                    clearSelection();
                }
                else {
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

    public static class MultiSelectionModel extends DefaultListSelectionModel {
        @Override
        public void setSelectionInterval(int startIndex, int endIndex) {
            if (startIndex == endIndex) {
                if (isSelectedIndex(startIndex)) {
                    removeSelectionInterval(startIndex, endIndex);
                }
                else {
                    super.addSelectionInterval(startIndex, endIndex);
                }
            }
            else {
                super.addSelectionInterval(startIndex, endIndex);
            }
        }

        private boolean multipleItemsAreCurrentlySelected() {
            return getMinSelectionIndex() != getMaxSelectionIndex();
        }
    }
}
