package org.hkijena.acaq5.ui.grapheditor;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.traits.ACAQMutableTraitConfiguration;
import org.hkijena.acaq5.api.data.traits.ACAQTraitModificationOperation;
import org.hkijena.acaq5.api.events.SlotAnnotationsChanged;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQTraitConfigurationUI;
import org.hkijena.acaq5.ui.registries.ACAQUITraitRegistry;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.Dimension;
import java.util.Set;

/**
 * UI that shows and manages slot traits
 */
public class ACAQDataSlotTraitUI extends JPanel {
    private ACAQAlgorithmGraph graph;
    private ACAQDataSlot slot;

    /**
     * Creates a new UI
     *
     * @param graph The graph
     * @param slot  The slot
     */
    public ACAQDataSlotTraitUI(ACAQAlgorithmGraph graph, ACAQDataSlot slot) {
        this.graph = graph;
        this.slot = slot;
        initialize();
        reloadButtons();
        this.slot.getEventBus().register(this);
    }

    private void reloadButtons() {
        removeAll();
        Set<ACAQTraitDeclaration> traits = slot.getSlotAnnotations();
        boolean canEditTraits = slot.isOutput() && slot.getAlgorithm().getTraitConfiguration() instanceof ACAQMutableTraitConfiguration &&
                !((ACAQMutableTraitConfiguration) slot.getAlgorithm().getTraitConfiguration()).isTraitModificationsSealed();

        for (ACAQTraitDeclaration trait : traits) {
            JButton traitButton = new JButton(ACAQUITraitRegistry.getInstance().getIconFor(trait));
            traitButton.setToolTipText(TooltipUtils.getTraitTooltip(trait));
            traitButton.setPreferredSize(new Dimension(25, 25));

            if (canEditTraits) {
                UIUtils.makeFlat25x25(traitButton);
                JPopupMenu menu = UIUtils.addPopupMenuToComponent(traitButton);
                JMenuItem removeTraitButton = new JMenuItem("Remove this annotation", UIUtils.getIconFromResources("remove.png"));
                removeTraitButton.addActionListener(e -> removeTrait(trait));
                menu.add(removeTraitButton);
            } else
                UIUtils.makeBorderlessWithoutMargin(traitButton);

            add(traitButton);
        }

        // Create button to add traits
        if (canEditTraits) {
            JButton addTraitButton = new JButton(UIUtils.getIconFromResources("label.png"));
            addTraitButton.setToolTipText("Annotate this data");
            addTraitButton.addActionListener(e -> addTrait());
            UIUtils.makeFlat25x25(addTraitButton);
            add(addTraitButton);
        }
    }

    private void removeTrait(ACAQTraitDeclaration trait) {
        slot.setSlotTraitToTraitConfiguration(trait, ACAQTraitModificationOperation.Ignore);
    }

    private void addTrait() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Annotate data");
        dialog.setModal(true);
        dialog.setContentPane(new ACAQTraitConfigurationUI(slot));
        dialog.pack();
        dialog.setSize(640, 480);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void initialize() {
        setOpaque(false);
//        setLayout(new GridLayout(1, 6));
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    }

    /**
     * Should be triggered when an algorithm's slot annotations are changed
     *
     * @param event Generated event
     */
    @Subscribe
    public void onSlotsTraitsChanged(SlotAnnotationsChanged event) {
        if (slot.getAlgorithm().getGraph() != null && slot.getAlgorithm().getGraph().isUpdatingSlotTraits())
            return;
        reloadButtons();
    }

    /**
     * @return Width needed to display the annotations
     */
    public int calculateWidth() {
        Set<ACAQTraitDeclaration> traits = slot.getSlotAnnotations();
        boolean canEditTraits = false;
        if (slot.getAlgorithm().getTraitConfiguration() instanceof ACAQMutableTraitConfiguration) {
            canEditTraits = !((ACAQMutableTraitConfiguration) slot.getAlgorithm().getTraitConfiguration()).isTraitModificationsSealed();
        }
        int width = 25 * traits.size();
        if (canEditTraits)
            width += 25;
        return width;
    }
}
