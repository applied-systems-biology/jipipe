package org.hkijena.acaq5.ui.grapheditor;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.events.AlgorithmGraphChangedEvent;
import org.hkijena.acaq5.api.traits.ACAQMutablePreprocessingTraitConfiguration;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

public class ACAQDataSlotTraitUI extends JPanel {
    private ACAQAlgorithmGraph graph;
    private ACAQDataSlot<?> slot;

    public ACAQDataSlotTraitUI(ACAQAlgorithmGraph graph, ACAQDataSlot<?> slot) {
        this.graph = graph;
        this.slot = slot;
        initialize();
        reloadButtons();
        this.graph.getEventBus().register(this);
    }

    private void reloadButtons() {
        removeAll();
        Set<Class<? extends ACAQTrait>> traits = graph.getAlgorithmTraits().get(slot);
        boolean canEditTraits = slot.getAlgorithm().getTraitConfiguration() instanceof ACAQMutablePreprocessingTraitConfiguration;

        for(Class<? extends ACAQTrait> trait : traits) {
            JButton traitButton = new JButton(ACAQRegistryService.getInstance().getUITraitRegistry().getIconFor(trait));
            traitButton.setToolTipText(ACAQTrait.getTooltipOf(trait));
            traitButton.setPreferredSize(new Dimension(25, 25));

            if(canEditTraits) {
                UIUtils.makeFlat25x25(traitButton);
                JPopupMenu menu = UIUtils.addPopupMenuToComponent(traitButton);
                JMenuItem removeTraitButton = new JMenuItem("Remove this annotation", UIUtils.getIconFromResources("remove.png"));
                removeTraitButton.addActionListener(e -> removeTrait(trait));
                menu.add(removeTraitButton);
            }
            else
                UIUtils.makeBorderlessWithoutMargin(traitButton);

            add(traitButton);
        }

        // Create button to add traits
        if(canEditTraits) {
            JButton addTraitButton = new JButton(UIUtils.getIconFromResources("label.png"));
            addTraitButton.setToolTipText("Annotate this data");
            addTraitButton.addActionListener(e -> addTrait());
            UIUtils.makeFlat25x25(addTraitButton);
            add(addTraitButton);
        }
    }

    private void removeTrait(Class<? extends ACAQTrait> trait) {
        ACAQMutablePreprocessingTraitConfiguration traitConfiguration = (ACAQMutablePreprocessingTraitConfiguration)slot.getAlgorithm().getTraitConfiguration();
        traitConfiguration.removeTraitFrom(slot.getName(), trait);
    }

    private void addTrait() {
        ACAQMutablePreprocessingTraitConfiguration traitConfiguration = (ACAQMutablePreprocessingTraitConfiguration)slot.getAlgorithm().getTraitConfiguration();
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Annotate data");
        dialog.setModal(true);
        dialog.setContentPane(new ACAQMutablePreprocessingTraitConfigurationSlotUI(slot.getName(), traitConfiguration));
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

    @Subscribe
    public void onAlgorithmGraphChanged(AlgorithmGraphChangedEvent event) {
        if(event.getAlgorithmGraph().containsNode(slot)) {
            reloadButtons();
        }
    }

    public int calculateWidth() {
        Set<Class<? extends ACAQTrait>> traits = graph.getAlgorithmTraits().get(slot);
        boolean canEditTraits = slot.getAlgorithm().getTraitConfiguration() instanceof ACAQMutablePreprocessingTraitConfiguration;
        int width = 25 * traits.size();
        if(canEditTraits)
            width += 25;
        return width;
    }
}
