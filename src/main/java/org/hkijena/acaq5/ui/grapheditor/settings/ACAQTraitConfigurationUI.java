package org.hkijena.acaq5.ui.grapheditor.settings;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.traits.ACAQMutableTraitConfiguration;
import org.hkijena.acaq5.api.data.traits.ACAQTraitModificationOperation;
import org.hkijena.acaq5.api.events.SlotAnnotationsChanged;
import org.hkijena.acaq5.api.registries.ACAQTraitRegistry;
import org.hkijena.acaq5.ui.components.ACAQTraitPicker;

import javax.swing.*;
import java.awt.BorderLayout;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * UI for {@link org.hkijena.acaq5.api.data.traits.ACAQDataSlotTraitConfiguration}
 */
public class ACAQTraitConfigurationUI extends JPanel {

    private ACAQDataSlot slot;
    private ACAQTraitPicker traitPicker;
    private boolean isLoading = false;

    /**
     * @param slot targeted slot
     */
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
        boolean canEditTraits = slot.isOutput() && slot.getAlgorithm().getTraitConfiguration() instanceof ACAQMutableTraitConfiguration &&
                !((ACAQMutableTraitConfiguration) slot.getAlgorithm().getTraitConfiguration()).isTraitModificationsSealed();
        traitPicker = new ACAQTraitPicker(canEditTraits ? ACAQTraitPicker.Mode.Multiple : ACAQTraitPicker.Mode.NonInteractive,
                new HashSet<>(ACAQTraitRegistry.getInstance().getRegisteredTraits().values().stream().filter(d -> !d.isHidden()).collect(Collectors.toSet())));
        traitPicker.getEventBus().register(this);
        add(traitPicker, BorderLayout.CENTER);
    }

    /**
     * Triggered when a trait is picked
     *
     * @param event Generated event
     */
    @Subscribe
    public void onTraitSelected(ACAQTraitPicker.TraitSelectedEvent event) {
        if (isLoading)
            return;
        if (!slot.getSlotAnnotations().contains(event.getTraitDeclaration())) {
            slot.setSlotTraitToTraitConfiguration(event.getTraitDeclaration(), ACAQTraitModificationOperation.Add);
        }
    }

    /**
     * Triggered when a trait is deselected
     *
     * @param event Generated event
     */
    @Subscribe
    public void onTraitDeselected(ACAQTraitPicker.TraitDeselectedEvent event) {
        if (isLoading)
            return;
        if (slot.getSlotAnnotations().contains(event.getTraitDeclaration())) {
            slot.setSlotTraitToTraitConfiguration(event.getTraitDeclaration(), ACAQTraitModificationOperation.Ignore);
        }
    }

    /**
     * Triggered when the algorithm traits are changed
     *
     * @param event Generated event
     */
    @Subscribe
    public void onTraitsChanged(SlotAnnotationsChanged event) {
        if (slot.getAlgorithm().getGraph() != null && slot.getAlgorithm().getGraph().isUpdatingSlotTraits())
            return;
        isLoading = true;
        reloadTraitList();
        isLoading = false;
    }
}
