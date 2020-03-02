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
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.HashSet;
import java.util.Set;

public class ACAQTraitPicker extends JPanel {

    private Mode mode;

    private EventBus eventBus = new EventBus();
    private JXTextField searchField;
    private JPanel traitList;
    private Set<ACAQTraitDeclaration> availableTraits;
    private Set<ACAQTraitDeclaration> selectedTraits = new HashSet<>();

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

    public void refreshTraitList() {
        traitList.removeAll();
        String[] searchStrings = getSearchStrings();

        for (ACAQTraitDeclaration trait : availableTraits) {
            if (trait.isHidden())
                continue;
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
            if(mode != Mode.Single)
                traitButton.setSelected(selectedTraits.contains(trait));
            else
                refreshTraitList();
        });
    }

    public void selectTrait(ACAQTraitDeclaration traitDeclaration) {
        if(selectedTraits.contains(traitDeclaration))
            return;
        if(mode == Mode.Single && selectedTraits.size() > 0) {
            for (ACAQTraitDeclaration declaration : ImmutableList.copyOf(selectedTraits)) {
                deselectTrait(declaration);
            }
        }
        selectedTraits.add(traitDeclaration);
        eventBus.post(new TraitSelectedEvent(this, traitDeclaration));
        eventBus.post(new SelectedTraitsChangedEvent(this));
    }

    public void deselectTrait(ACAQTraitDeclaration traitDeclaration) {
        if(!selectedTraits.contains(traitDeclaration))
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

    public void setSelectedTraits(Set<ACAQTraitDeclaration> traits) {
        this.selectedTraits = new HashSet<>(traits);
        eventBus.post(new SelectedTraitsChangedEvent(this));
        refreshTraitList();
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

    public enum Mode {
        NonInteractive,
        Single,
        Multiple
    }

    public static class SelectedTraitsChangedEvent {
        private ACAQTraitPicker traitPicker;

        public SelectedTraitsChangedEvent(ACAQTraitPicker traitPicker) {
            this.traitPicker = traitPicker;
        }

        public ACAQTraitPicker getTraitPicker() {
            return traitPicker;
        }
    }

    public static class TraitSelectedEvent {
        private ACAQTraitPicker traitPicker;
        private ACAQTraitDeclaration traitDeclaration;

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

    public static class TraitDeselectedEvent {
        private ACAQTraitPicker traitPicker;
        private ACAQTraitDeclaration traitDeclaration;

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
