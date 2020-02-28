package org.hkijena.acaq5.ui.grapheditor;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQCompartmentOutput;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.events.AlgorithmGraphChangedEvent;
import org.hkijena.acaq5.ui.events.AlgorithmFinderSuccessEvent;
import org.hkijena.acaq5.ui.grapheditor.algorithmfinder.ACAQAlgorithmFinderUI;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.util.Set;
import java.util.function.Consumer;

import static org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI.SLOT_UI_HEIGHT;
import static org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI.SLOT_UI_WIDTH;

public class ACAQDataSlotUI extends JPanel {
    private ACAQAlgorithmGraph graph;
    private String compartment;
    private ACAQDataSlot slot;
    private JButton assignButton;
    private JPopupMenu assignButtonMenu;
    private ACAQDataSlotTraitUI traitUI;

    public ACAQDataSlotUI(ACAQAlgorithmGraph graph, String compartment, ACAQDataSlot slot) {
        this.graph = graph;
        this.compartment = compartment;
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
                Set<ACAQDataSlot> availableSources = graph.getAvailableSources(slot);
                availableSources.removeIf(slot -> !slot.getAlgorithm().isVisibleIn(compartment));
                for(ACAQDataSlot source : availableSources) {
                    if(!source.getAlgorithm().isVisibleIn(compartment))
                        continue;
                    JMenuItem connectButton = new JMenuItem(source.getNameWithAlgorithmName(),
                            ACAQUIDatatypeRegistry.getInstance().getIconFor(source.getAcceptedDataType()));
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
            Set<ACAQDataSlot> targetSlots = graph.getTargetSlots(slot);
            if(!targetSlots.isEmpty()) {

                boolean allowDisconnect = false;
                for (ACAQDataSlot targetSlot : targetSlots) {
                    if(graph.canUserDisconnect(slot, targetSlot)) {
                        allowDisconnect = true;
                        break;
                    }
                }

                if(allowDisconnect) {
                    JMenuItem disconnectButton = new JMenuItem("Disconnect all", UIUtils.getIconFromResources("remove.png"));
                    disconnectButton.addActionListener(e -> disconnectSlot());
                    assignButtonMenu.add(disconnectButton);

                    assignButtonMenu.addSeparator();
                }
            }
            Set<ACAQDataSlot> availableTargets = graph.getAvailableTargets(slot);
            availableTargets.removeIf(slot -> !slot.getAlgorithm().isVisibleIn(compartment));

            JMenuItem findAlgorithmButton = new JMenuItem("Find matching algorithm ...", UIUtils.getIconFromResources("search.png"));
            findAlgorithmButton.setToolTipText("Opens a tool to find a matching algorithm based on the data");
            findAlgorithmButton.addActionListener(e -> findAlgorithm(slot));
            assignButtonMenu.add(findAlgorithmButton);
            if(!availableTargets.isEmpty())
                assignButtonMenu.addSeparator();

            for(ACAQDataSlot target : availableTargets) {
                JMenuItem connectButton = new JMenuItem(target.getNameWithAlgorithmName(),
                        ACAQUIDatatypeRegistry.getInstance().getIconFor(target.getAcceptedDataType()));
                connectButton.addActionListener(e -> connectSlot(slot, target));
                connectButton.setToolTipText(TooltipUtils.getAlgorithmTooltip(target.getAlgorithm().getDeclaration()));
                assignButtonMenu.add(connectButton);
            }
        }
    }

    private void findAlgorithm(ACAQDataSlot slot) {
        ACAQAlgorithmFinderUI algorithmFinderUI = new ACAQAlgorithmFinderUI(slot, graph, compartment);
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Find matching algorithm");
        dialog.setModal(true);
        dialog.setContentPane(algorithmFinderUI);
        dialog.pack();
        dialog.setSize(640, 480);
        dialog.setLocationRelativeTo(this);

        algorithmFinderUI.getEventBus().register(new Consumer<AlgorithmFinderSuccessEvent>() {
            @Override
            @Subscribe
            public void accept(AlgorithmFinderSuccessEvent event) {
                dialog.setVisible(false);
            }
        });

        dialog.setVisible(true);
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

    private void connectSlot(ACAQDataSlot source, ACAQDataSlot target) {
        if(graph.canConnect(source, target)) {
            graph.connect(source, target);
        }
    }

    private void disconnectSlot() {
        graph.disconnectAll(slot, true);
    }

    private void initialize() {
        setLayout(new BorderLayout());

        assignButton = new JButton(UIUtils.getIconFromResources("chevron-right.png"));
        assignButton.setPreferredSize(new Dimension(25, SLOT_UI_HEIGHT));
        assignButtonMenu = UIUtils.addPopupMenuToComponent(assignButton);
        UIUtils.makeFlat(assignButton);

        if(slot.getAlgorithm() instanceof ACAQCompartmentOutput) {
            if(slot.getAlgorithm().getCompartment().equals(compartment)) {
                if(slot.isOutput()) {
                    assignButton.setEnabled(false);
                }
            }
            else {
                if(slot.isInput()) {
                    assignButton.setEnabled(false);
                }
            }
        }

        JPanel centerPanel = new JPanel();
//        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setLayout(new GridLayout(2, 1));
        centerPanel.setOpaque(false);

        JLabel nameLabel = new JLabel(getDisplayedName());
        nameLabel.setToolTipText(TooltipUtils.getSlotInstanceTooltip(slot, graph, false));
        nameLabel.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
        nameLabel.setIcon(ACAQUIDatatypeRegistry.getInstance().getIconFor(slot.getAcceptedDataType()));
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

    public String getDisplayedName() {
        if(slot.getAlgorithm() instanceof ACAQCompartmentOutput) {
            if(slot.isOutput()) {
                return slot.getName().substring("Output ".length());
            }
            else {
                return slot.getName();
            }
        }
        else {
            return slot.getName();
        }
    }

    public int calculateWidth() {
        // First calculate the width caused by the label width
        FontRenderContext frc = new FontRenderContext(null, false, false);
        TextLayout layout = new TextLayout(getDisplayedName(), getFont(), frc);
        double w = layout.getBounds().getWidth();
        int labelWidth = (int)Math.ceil(w * 1.0 / SLOT_UI_WIDTH) * SLOT_UI_WIDTH;
        int traitWidth = (int)Math.ceil(traitUI.calculateWidth() * 1.0 / SLOT_UI_WIDTH) * SLOT_UI_WIDTH;
        int width = Math.max(labelWidth, traitWidth) + 75;

        return width;
    }

    public ACAQDataSlot getSlot() {
        return slot;
    }

    @Subscribe
    public void onAlgorithmGraphChanged(AlgorithmGraphChangedEvent event) {
        if(graph.containsNode(slot)) {
            reloadPopupMenu();
            reloadButtonStatus();
        }
    }

    public String getCompartment() {
        return compartment;
    }
}
