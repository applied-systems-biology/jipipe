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
import java.util.Iterator;
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
        int allowedCount = 6;
        boolean addMoreIndicator = false;
        boolean canEditTraits = slot.getAlgorithm().getTraitConfiguration() instanceof ACAQMutablePreprocessingTraitConfiguration;
        if(canEditTraits)
            --allowedCount;
        if(addMoreIndicator = traits.size() > allowedCount)
            --allowedCount;

        for(Class<? extends ACAQTrait> trait : traits) {
            JButton traitButton = new JButton(ACAQRegistryService.getInstance().getUITraitRegistry().getIconFor(trait));
            traitButton.setToolTipText(ACAQTrait.getTooltipOf(trait));
            traitButton.setPreferredSize(new Dimension(25, 25));

            if(canEditTraits) {
                UIUtils.makeFlat(traitButton);
            }
            else
                UIUtils.makeFlatWithoutMargin(traitButton);

            add(traitButton);

            --allowedCount;
//            if(allowedCount == 0)
//                break;
        }

        // Create "more available" button
        if(addMoreIndicator) {
        }

        // Create button to add traits
        if(canEditTraits) {
            JButton addTraitButton = new JButton(UIUtils.getIconFromResources("label.png"));
            addTraitButton.setToolTipText("Annotate this data");
            addTraitButton.addActionListener(e -> addTrait());
            addTraitButton.setPreferredSize(new Dimension(25, 25));
            UIUtils.makeFlat(addTraitButton);
            add(addTraitButton);
        }
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
        setLayout(new GridLayout(1, 6));
    }

    @Subscribe
    public void onAlgorithmGraphChanged(AlgorithmGraphChangedEvent event) {
        reloadButtons();
    }
}
