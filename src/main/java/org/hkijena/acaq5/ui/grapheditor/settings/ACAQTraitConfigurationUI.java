package org.hkijena.acaq5.ui.grapheditor.settings;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.traits.ACAQMutableTraitConfiguration;
import org.hkijena.acaq5.api.data.traits.ACAQTraitModificationOperation;
import org.hkijena.acaq5.api.events.SlotAnnotationsChanged;
import org.hkijena.acaq5.api.registries.ACAQTraitRegistry;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.ui.components.ACAQTraitPicker;
import org.hkijena.acaq5.ui.components.DocumentChangeListener;
import org.hkijena.acaq5.ui.registries.ACAQUITraitRegistry;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;
import org.jdesktop.swingx.JXTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class ACAQTraitConfigurationUI extends JPanel {

    private ACAQDataSlot slot;
    private ACAQTraitPicker traitPicker;
    private boolean isLoading = false;

    public ACAQTraitConfigurationUI(ACAQDataSlot slot) {
        this.slot = slot;
        initialize();
        reloadTraitList();
        slot.getEventBus().register(this);
    }

    private void reloadTraitList() {
        boolean canEditTraits = slot.isOutput() && slot.getAlgorithm().getTraitConfiguration() instanceof ACAQMutableTraitConfiguration &&
                !((ACAQMutableTraitConfiguration) slot.getAlgorithm().getTraitConfiguration()).isTraitModificationsSealed();
        traitPicker.setMode(canEditTraits ? ACAQTraitPicker.Mode.Multiple : ACAQTraitPicker.Mode.NonInteractive);
        traitPicker.setSelectedTraits(slot.getSlotAnnotations());
    }

    private void initialize() {
        setLayout(new BorderLayout());
        traitPicker = new ACAQTraitPicker(ACAQTraitPicker.Mode.Multiple,
                new HashSet<>(ACAQTraitRegistry.getInstance().getRegisteredTraits().values()));
        traitPicker.getEventBus().register(this);
        add(traitPicker, BorderLayout.CENTER);
    }

    @Subscribe
    public void onTraitSelected(ACAQTraitPicker.TraitSelectedEvent event) {
        if(isLoading)
            return;
        if(!slot.getSlotAnnotations().contains(event.getTraitDeclaration())) {
            slot.setSlotTraitToTraitConfiguration(event.getTraitDeclaration(), ACAQTraitModificationOperation.Add);
        }
    }

    @Subscribe
    public void onTraitDeselected(ACAQTraitPicker.TraitDeselectedEvent event) {
        if(isLoading)
            return;
        if(slot.getSlotAnnotations().contains(event.getTraitDeclaration())) {
            slot.setSlotTraitToTraitConfiguration(event.getTraitDeclaration(), ACAQTraitModificationOperation.Ignore);
        }
    }

    @Subscribe
    public void onTraitsChanged(SlotAnnotationsChanged event) {
        isLoading = true;
        reloadTraitList();
        isLoading = false;
    }
}
