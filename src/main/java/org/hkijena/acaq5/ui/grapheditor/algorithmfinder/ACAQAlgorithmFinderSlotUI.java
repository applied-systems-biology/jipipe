/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.acaq5.ui.grapheditor.algorithmfinder;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.algorithm.ACAQGraph;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQDefaultMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.ui.events.AlgorithmFinderSuccessEvent;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

import static org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI.SLOT_UI_HEIGHT;

/**
 * UI for one slot in the algorithm finder
 */
public class ACAQAlgorithmFinderSlotUI extends JPanel {

    private ACAQDataSlot outputSlot;
    private ACAQGraph graph;
    private String compartment;
    private ACAQDataSlot inputSlot;
    private boolean isExistingInstance;
    private EventBus eventBus = new EventBus();

    private JButton assignButton;
    private JPopupMenu assignButtonMenu;

    /**
     * Creates a slot UI
     *
     * @param outputSlot         The slot
     * @param graph              The graph
     * @param compartment        The compartment
     * @param inputSlot          The target slot
     * @param isExistingInstance If true, the algorithm already exists within the graph
     */
    public ACAQAlgorithmFinderSlotUI(ACAQDataSlot outputSlot, ACAQGraph graph, String compartment, ACAQDataSlot inputSlot, boolean isExistingInstance) {
        this.outputSlot = outputSlot;
        this.graph = graph;
        this.compartment = compartment;
        this.inputSlot = inputSlot;
        this.isExistingInstance = isExistingInstance;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        assignButton = new JButton(UIUtils.getIconFromResources("chevron-right.png"));
        assignButton.setPreferredSize(new Dimension(25, SLOT_UI_HEIGHT));
        assignButtonMenu = UIUtils.addPopupMenuToComponent(assignButton);
        UIUtils.makeFlat(assignButton);

        JLabel nameLabel = new JLabel(inputSlot.getName());
        nameLabel.setToolTipText(ACAQData.getNameOf(inputSlot.getAcceptedDataType()));
        nameLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        nameLabel.setIcon(ACAQUIDatatypeRegistry.getInstance().getIconFor(inputSlot.getAcceptedDataType()));

        add(nameLabel, BorderLayout.CENTER);

        add(assignButton, BorderLayout.WEST);
        nameLabel.setHorizontalAlignment(JLabel.LEFT);
        nameLabel.setHorizontalTextPosition(JLabel.RIGHT);

        reloadAssignMenu();
    }

    private void reloadAssignMenu() {
        assignButtonMenu.removeAll();

        if (isExistingInstance) {
            if (graph.getSourceSlot(inputSlot) != null) {
                JMenuItem disconnectExistingButton = new JMenuItem("Disconnect existing: " + graph.getSourceSlot(inputSlot).getNameWithAlgorithmName(), UIUtils.getIconFromResources("remove.png"));
                disconnectExistingButton.addActionListener(e -> disconnectAllExistingInstance());
                assignButtonMenu.add(disconnectExistingButton);
            } else {
                JMenuItem connectButton = new JMenuItem(inputSlot.getNameWithAlgorithmName(), ACAQUIDatatypeRegistry.getInstance().getIconFor(inputSlot.getAcceptedDataType()));
                connectButton.addActionListener(e -> connectToExistingInstance());
                assignButtonMenu.add(connectButton);
            }
        } else {
            JMenuItem connectButton = new JMenuItem(inputSlot.getNameWithAlgorithmName(), ACAQUIDatatypeRegistry.getInstance().getIconFor(inputSlot.getAcceptedDataType()));
            connectButton.addActionListener(e -> connectToNewInstance());
            assignButtonMenu.add(connectButton);
        }

        if (inputSlot.getAlgorithm().getSlotConfiguration() instanceof ACAQMutableSlotConfiguration) {
            ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration) inputSlot.getAlgorithm().getSlotConfiguration();
            if (slotConfiguration.canModifyInputSlots()) {
                if (assignButtonMenu.getComponentCount() > 0)
                    assignButtonMenu.addSeparator();
                JMenuItem deleteButton = new JMenuItem("Delete this slot", UIUtils.getIconFromResources("remove.png"));
                deleteButton.addActionListener(e -> deleteSlot());
                assignButtonMenu.add(deleteButton);
            }
        }
    }

    private void deleteSlot() {
        ACAQDefaultMutableSlotConfiguration slotConfiguration = (ACAQDefaultMutableSlotConfiguration) inputSlot.getAlgorithm().getSlotConfiguration();
        slotConfiguration.removeInputSlot(inputSlot.getName(), true);
    }

    private void connectToNewInstance() {
        graph.insertNode(inputSlot.getAlgorithm(), compartment);
        graph.connect(outputSlot, inputSlot);
        eventBus.post(new AlgorithmFinderSuccessEvent(outputSlot, inputSlot));
    }

    private void disconnectAllExistingInstance() {
        graph.disconnectAll(inputSlot, true);
        reloadAssignMenu();
    }

    private void connectToExistingInstance() {
        if (graph.canConnect(outputSlot, inputSlot, true)) {
            graph.connect(outputSlot, inputSlot);
            eventBus.post(new AlgorithmFinderSuccessEvent(outputSlot, inputSlot));
        } else {
            JOptionPane.showMessageDialog(this, "The data slots could not be connected. Is this connection causing loops?", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Returns the event bus.
     *
     * @return Event bus instance.
     */
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Returns the compartment ID
     *
     * @return Compartment ID
     */
    public String getCompartment() {
        return compartment;
    }
}
