package org.hkijena.acaq5.ui.grapheditor.settings;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.events.SlotAnnotationsChanged;
import org.hkijena.acaq5.api.registries.ACAQTraitRegistry;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.api.traits.global.ACAQMutableTraitConfiguration;
import org.hkijena.acaq5.api.traits.global.ACAQTraitModificationOperation;
import org.hkijena.acaq5.ui.components.DocumentChangeListener;
import org.hkijena.acaq5.ui.registries.ACAQUITraitRegistry;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;
import org.jdesktop.swingx.JXTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.Set;

public class ACAQTraitConfigurationUI extends JPanel {

    private ACAQDataSlot slot;
    private JXTextField searchField;
    private JPanel traitList;

    public ACAQTraitConfigurationUI(ACAQDataSlot slot) {
        this.slot = slot;
        initialize();
        reloadTraitList();
        slot.getEventBus().register(this);
    }

    private void reloadTraitList() {
        traitList.removeAll();
        String[] searchStrings = getSearchStrings();

        boolean canEditTraits = slot.isOutput() && slot.getAlgorithm().getTraitConfiguration() instanceof ACAQMutableTraitConfiguration &&
                !((ACAQMutableTraitConfiguration) slot.getAlgorithm().getTraitConfiguration()).isTraitModificationsSealed();
        Set<ACAQTraitDeclaration> selectedTraits = slot.getSlotAnnotations();

        for (ACAQTraitDeclaration trait : ACAQTraitRegistry.getInstance().getRegisteredTraits().values()) {
            if(trait.isHidden())
                continue;
            if(!searchStringsMatches(trait, searchStrings))
                continue;

            JToggleButton traitButton = new JToggleButton(trait.getName(),
                    ACAQUITraitRegistry.getInstance().getIconFor(trait));
            traitButton.setSelected(selectedTraits.contains(trait));
            traitButton.setToolTipText(TooltipUtils.getTraitTooltip(trait));
            UIUtils.makeFlat(traitButton);
            if(canEditTraits)
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
            if(slot.getSlotAnnotations().contains(trait)) {
                slot.setSlotTraitToTraitConfiguration(trait, ACAQTraitModificationOperation.Ignore);
                traitButton.setSelected(false);
            }
            else {
                slot.setSlotTraitToTraitConfiguration(trait, ACAQTraitModificationOperation.Add);
                traitButton.setSelected(true);
            }
        });
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

    private void initialize() {
        setLayout(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        searchField = new JXTextField("Search ...");
        searchField.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                reloadTraitList();
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
                return new Dimension(ACAQTraitConfigurationUI.this.getWidth() - 16,
                        super.getPreferredSize().height);
            }
        };
        traitList.setLayout(new FlowLayout(FlowLayout.LEFT));
        add(new JScrollPane(traitList), BorderLayout.CENTER);
    }

    @Subscribe
    public void onTraitsChanged(SlotAnnotationsChanged event) {
        reloadTraitList();
    }
}
