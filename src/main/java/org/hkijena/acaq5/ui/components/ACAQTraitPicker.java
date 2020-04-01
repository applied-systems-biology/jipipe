package org.hkijena.acaq5.ui.components;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.ui.registries.ACAQUITraitRegistry;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;
import org.jdesktop.swingx.JXTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Panel that allows to pick {@link ACAQTraitDeclaration}
 */
public class ACAQTraitPicker extends JPanel {

    private Mode mode;

    private EventBus eventBus = new EventBus();
    private JXTextField searchField;
    private JPanel traitList;
    private Set<ACAQTraitDeclaration> availableTraits;
    private Set<ACAQTraitDeclaration> selectedTraits = new HashSet<>();

    /**
     * @param mode            the mode
     * @param availableTraits list of available trait types
     */
    public ACAQTraitPicker(Mode mode, Set<ACAQTraitDeclaration> availableTraits) {
        this.mode = mode;
        this.availableTraits = availableTraits;
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

        traitList = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(ACAQTraitPicker.this.getWidth() - 16,
                        super.getPreferredSize().height);
            }
        };
        traitList.setLayout(new FlowLayout(FlowLayout.LEFT));
        add(new JScrollPane(traitList), BorderLayout.CENTER);
    }

    /**
     * Refreshes the list
     */
    public void refreshTraitList() {
        traitList.removeAll();
        String[] searchStrings = getSearchStrings();

        for (ACAQTraitDeclaration trait : availableTraits) {
            if (!searchStringsMatches(trait, searchStrings))
                continue;

            JToggleButton traitButton = new JToggleButton(trait.getName(),
                    ACAQUITraitRegistry.getInstance().getIconFor(trait));
            traitButton.setSelected(selectedTraits.contains(trait));
            traitButton.setToolTipText(TooltipUtils.getTraitTooltip(trait));
            UIUtils.makeFlat(traitButton);
            if (mode != Mode.NonInteractive)
                makeToggleToEditor(trait, traitButton);
            else
                UIUtils.makeToggleReadonly(traitButton);

            traitList.add(traitButton);
        }

        traitList.revalidate();
        traitList.repaint();
    }

    private void makeToggleToEditor(ACAQTraitDeclaration trait, JToggleButton traitButton) {
        traitButton.addActionListener(e -> {
            if (selectedTraits.contains(trait)) {
                deselectTrait(trait);
            } else {
                selectTrait(trait);
            }
            if (mode != Mode.Single)
                traitButton.setSelected(selectedTraits.contains(trait));
            else
                refreshTraitList();
        });
    }

    /**
     * Selects a trait
     *
     * @param traitDeclaration selected trait
     */
    public void selectTrait(ACAQTraitDeclaration traitDeclaration) {
        if (selectedTraits.contains(traitDeclaration))
            return;
        if (mode == Mode.Single && selectedTraits.size() > 0) {
            for (ACAQTraitDeclaration declaration : ImmutableList.copyOf(selectedTraits)) {
                deselectTrait(declaration);
            }
        }
        selectedTraits.add(traitDeclaration);
        eventBus.post(new TraitSelectedEvent(this, traitDeclaration));
        eventBus.post(new SelectedTraitsChangedEvent(this));
    }

    /**
     * Deselect a trait
     *
     * @param traitDeclaration trait
     */
    public void deselectTrait(ACAQTraitDeclaration traitDeclaration) {
        if (!selectedTraits.contains(traitDeclaration))
            return;
        selectedTraits.remove(traitDeclaration);
        eventBus.post(new TraitDeselectedEvent(this, traitDeclaration));
        eventBus.post(new SelectedTraitsChangedEvent(this));
    }

    private boolean searchStringsMatches(ACAQTraitDeclaration trait, String[] strings) {
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
}
