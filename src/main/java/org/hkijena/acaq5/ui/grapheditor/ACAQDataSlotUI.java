package org.hkijena.acaq5.ui.grapheditor;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.ACAQPreprocessingOutput;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.events.AlgorithmGraphChangedEvent;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;

import static org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI.SLOT_UI_HEIGHT;
import static org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI.SLOT_UI_WIDTH;

public class ACAQDataSlotUI extends JPanel {
    private ACAQAlgorithmGraph graph;
    private ACAQDataSlot<?> slot;
    private JButton assignButton;
    private JPopupMenu assignButtonMenu;
    private ACAQDataSlotTraitUI traitUI;

    public ACAQDataSlotUI(ACAQAlgorithmGraph graph, ACAQDataSlot<?> slot) {
        this.graph = graph;
        this.slot = slot;
        initialize();
        reloadPopupMenu();
        reloadButtonStatus();

        graph.getEventBus().register(this);
    }

    private void reloadPopupMenu() {
        assignButtonMenu.removeAll();

        if(slot.isInput()) {
            if(graph.getSourceSlot(slot) == null) {
                for(ACAQDataSlot<?> source : graph.getAvailableSources(slot)) {
                    JMenuItem connectButton = new JMenuItem(source.getFullName(),
                            ACAQRegistryService.getInstance().getUIDatatypeRegistry().getIconFor(source.getAcceptedDataType()));
                    connectButton.addActionListener(e -> connectSlot(source, slot));
                    assignButtonMenu.add(connectButton);
                }
            }
            else {
                JMenuItem disconnectButton = new JMenuItem("Disconnect", UIUtils.getIconFromResources("remove.png"));
                disconnectButton.addActionListener(e -> disconnectSlot());
                assignButtonMenu.add(disconnectButton);
            }
        }
        else if(slot.isOutput()) {
            if(!graph.getTargetSlots(slot).isEmpty()) {
                JMenuItem disconnectButton = new JMenuItem("Disconnect all", UIUtils.getIconFromResources("remove.png"));
                disconnectButton.addActionListener(e -> disconnectSlot());
                assignButtonMenu.add(disconnectButton);

                assignButtonMenu.addSeparator();
            }

            for(ACAQDataSlot<?> target : graph.getAvailableTargets(slot)) {
                JMenuItem connectButton = new JMenuItem(target.getFullName(),
                        ACAQRegistryService.getInstance().getUIDatatypeRegistry().getIconFor(target.getAcceptedDataType()));
                connectButton.addActionListener(e -> connectSlot(slot, target));
                assignButtonMenu.add(connectButton);
            }
        }
    }

    private void reloadButtonStatus() {
        if(slot.isInput()) {
            if (graph.getSourceSlot(slot) == null) {
                assignButton.setIcon(UIUtils.getIconFromResources("chevron-right-thin.png"));
            }
            else {
                assignButton.setIcon(UIUtils.getIconFromResources("chevron-right.png"));
            }
        }
        else if(slot.isOutput()) {
            if (graph.getTargetSlots(slot).isEmpty()) {
                assignButton.setIcon(UIUtils.getIconFromResources("chevron-right-thin.png"));
            }
            else {
                assignButton.setIcon(UIUtils.getIconFromResources("chevron-right.png"));
            }
        }
    }

    private void connectSlot(ACAQDataSlot<?> source, ACAQDataSlot<?> target) {
        if(graph.canConnect(source, target)) {
            graph.connect(source, target);
        }
    }

    private void disconnectSlot() {
        graph.disconnectAll(slot);
    }

    private void initialize() {
        setLayout(new BorderLayout());

        assignButton = new JButton(UIUtils.getIconFromResources("chevron-right.png"));
        assignButton.setPreferredSize(new Dimension(25, SLOT_UI_HEIGHT));
        assignButtonMenu = UIUtils.addPopupMenuToComponent(assignButton);
        UIUtils.makeFlat(assignButton);

        JPanel centerPanel = new JPanel();
//        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setLayout(new GridLayout(2, 1));
        centerPanel.setOpaque(false);

        JLabel nameLabel = new JLabel(slot.getName());
        nameLabel.setToolTipText(ACAQData.getNameOf(slot.getAcceptedDataType()));
        nameLabel.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
        nameLabel.setIcon(ACAQRegistryService.getInstance().getUIDatatypeRegistry().getIconFor(slot.getAcceptedDataType()));
        centerPanel.add(nameLabel);
//        add(nameLabel, BorderLayout.CENTER);


        if(slot.isInput()) {
            add(assignButton, BorderLayout.WEST);
            nameLabel.setHorizontalAlignment(JLabel.LEFT);
            nameLabel.setHorizontalTextPosition(JLabel.RIGHT);

//            // For preprocessing output, also create traits per input
//            if(slot.getAlgorithm() instanceof ACAQPreprocessingOutput) {
//                ACAQDataSlotTraitUI traitUI = new ACAQDataSlotTraitUI(graph, slot);
//                add(traitUI, BorderLayout.EAST);
//            }
        }
        else if(slot.isOutput()) {
            add(assignButton, BorderLayout.EAST);
            nameLabel.setHorizontalAlignment(JLabel.RIGHT);
            nameLabel.setHorizontalTextPosition(JLabel.LEFT);

//            // Create trait UI
//            ACAQDataSlotTraitUI traitUI = new ACAQDataSlotTraitUI(graph, slot);
//            add(traitUI, BorderLayout.WEST);
        }

        traitUI = new ACAQDataSlotTraitUI(graph, slot);
        centerPanel.add(traitUI);

        add(centerPanel, BorderLayout.CENTER);
    }

    public int calculateWidth() {
        // First calculate the width caused by the label width
        FontRenderContext frc = new FontRenderContext(null, false, false);
        TextLayout layout = new TextLayout(ACAQData.getNameOf(slot.getAcceptedDataType()), getFont(), frc);
        double w = layout.getBounds().getWidth();
        int labelWidth = (int)Math.ceil(w * 1.0 / SLOT_UI_WIDTH) * SLOT_UI_WIDTH;
        int traitWidth = (int)Math.ceil(traitUI.calculateWidth() * 1.0 / SLOT_UI_WIDTH) * SLOT_UI_WIDTH;
        int width = Math.max(labelWidth, traitWidth) + 75;

        return width;
    }

    public ACAQDataSlot<?> getSlot() {
        return slot;
    }

    @Subscribe
    public void onAlgorithmGraphChanged(AlgorithmGraphChangedEvent event) {
        if(graph.containsNode(slot.getAlgorithm())) {
            reloadPopupMenu();
            reloadButtonStatus();
        }
    }
}
