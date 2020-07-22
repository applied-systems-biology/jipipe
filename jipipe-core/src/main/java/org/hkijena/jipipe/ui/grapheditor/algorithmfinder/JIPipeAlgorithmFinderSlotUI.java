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

package org.hkijena.jipipe.ui.grapheditor.algorithmfinder;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.history.*;
import org.hkijena.jipipe.api.registries.JIPipeDatatypeRegistry;
import org.hkijena.jipipe.ui.events.AlgorithmFinderSuccessEvent;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.registries.JIPipeUIDatatypeRegistry;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Collections;

/**
 * UI for one slot in the algorithm finder
 */
public class JIPipeAlgorithmFinderSlotUI extends JPanel {

    private final JIPipeGraphCanvasUI canvasUI;
    private final JIPipeDataSlot outputSlot;
    private final JIPipeGraph graph;
    private final String compartment;
    private final JIPipeDataSlot inputSlot;
    private final boolean isExistingInstance;
    private final EventBus eventBus = new EventBus();

    private JButton assignButton;
    private JPopupMenu assignButtonMenu;

    /**
     * Creates a slot UI
     *
     * @param canvasUI           the canvas
     * @param outputSlot         The slot
     * @param inputSlot          The target slot
     * @param isExistingInstance If true, the algorithm already exists within the graph
     */
    public JIPipeAlgorithmFinderSlotUI(JIPipeGraphCanvasUI canvasUI, JIPipeDataSlot outputSlot, JIPipeDataSlot inputSlot, boolean isExistingInstance) {
        this.canvasUI = canvasUI;
        this.outputSlot = outputSlot;
        this.graph = canvasUI.getGraph();
        this.compartment = canvasUI.getCompartment();
        this.inputSlot = inputSlot;
        this.isExistingInstance = isExistingInstance;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        Icon icon;
        if (inputSlot.getNode().getGraph() == null || inputSlot.getNode().getGraph().getSourceSlot(inputSlot) == null)
            icon = UIUtils.getIconFromResources("emblems/slot-free-horizontal.png");
        else
            icon = UIUtils.getIconFromResources("emblems/slot-connected-input-horizontal.png");
        assignButton = new JButton(icon);
        assignButton.setEnabled(JIPipeDatatypeRegistry.getInstance().isConvertible(outputSlot.getAcceptedDataType(), inputSlot.getAcceptedDataType()));
        assignButton.setPreferredSize(new Dimension(25, 50));
        assignButton.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.GRAY));
        assignButtonMenu = UIUtils.addPopupMenuToComponent(assignButton);
        UIUtils.makeFlat(assignButton);

        JLabel nameLabel = new JLabel(inputSlot.getName());
        nameLabel.setToolTipText(JIPipeData.getNameOf(inputSlot.getAcceptedDataType()));
        nameLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        nameLabel.setIcon(JIPipeUIDatatypeRegistry.getInstance().getIconFor(inputSlot.getAcceptedDataType()));

        add(nameLabel, BorderLayout.CENTER);

        add(assignButton, BorderLayout.EAST);
        nameLabel.setHorizontalAlignment(JLabel.LEFT);
        nameLabel.setHorizontalTextPosition(JLabel.RIGHT);

        reloadAssignMenu();
    }

    private void reloadAssignMenu() {
        assignButtonMenu.removeAll();

        if (isExistingInstance) {
            if (graph.getSourceSlot(inputSlot) != null) {
                JMenuItem disconnectExistingButton = new JMenuItem("Disconnect existing: " + graph.getSourceSlot(inputSlot).getDisplayName(), UIUtils.getIconFromResources("actions/cancel.png"));
                disconnectExistingButton.addActionListener(e -> disconnectAllExistingInstance());
                assignButtonMenu.add(disconnectExistingButton);
            } else {
                JMenuItem connectButton = new JMenuItem(inputSlot.getDisplayName(), JIPipeUIDatatypeRegistry.getInstance().getIconFor(inputSlot.getAcceptedDataType()));
                connectButton.addActionListener(e -> connectToExistingInstance());
                assignButtonMenu.add(connectButton);
            }
        } else {
            JMenuItem connectButton = new JMenuItem(inputSlot.getDisplayName(), JIPipeUIDatatypeRegistry.getInstance().getIconFor(inputSlot.getAcceptedDataType()));
            connectButton.addActionListener(e -> connectToNewInstance());
            assignButtonMenu.add(connectButton);
        }

        if (inputSlot.getNode().getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration) {
            JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) inputSlot.getNode().getSlotConfiguration();
            if (slotConfiguration.canModifyInputSlots()) {
                if (assignButtonMenu.getComponentCount() > 0)
                    assignButtonMenu.addSeparator();
                JMenuItem deleteButton = new JMenuItem("Delete this slot", UIUtils.getIconFromResources("actions/delete.png"));
                deleteButton.addActionListener(e -> deleteSlot());
                assignButtonMenu.add(deleteButton);
            }
        }
    }

    private void deleteSlot() {
        JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) inputSlot.getNode().getSlotConfiguration();
        canvasUI.getGraphHistory().addSnapshotBefore(new SlotConfigurationHistorySnapshot(inputSlot.getNode(),
                "Remove slot '" + inputSlot.getDisplayName() + "'"));
        slotConfiguration.removeInputSlot(inputSlot.getName(), true);
    }

    private void connectToNewInstance() {
        canvasUI.getGraphHistory().addSnapshotBefore(new CompoundGraphHistorySnapshot(Arrays.asList(
                new AddNodeGraphHistorySnapshot(graph, Collections.singleton(inputSlot.getNode())),
                new EdgeConnectGraphHistorySnapshot(graph, outputSlot, inputSlot)
        )));
        graph.insertNode(inputSlot.getNode(), compartment);
        graph.connect(outputSlot, inputSlot);
        eventBus.post(new AlgorithmFinderSuccessEvent(outputSlot, inputSlot));
    }

    private void disconnectAllExistingInstance() {
        JIPipeDataSlot sourceSlot = graph.getSourceSlot(inputSlot);
        if (sourceSlot != null) {
            canvasUI.getGraphHistory().addSnapshotBefore(new EdgeDisconnectGraphHistorySnapshot(graph, sourceSlot, inputSlot));
        }
        graph.disconnectAll(inputSlot, true);
        reloadAssignMenu();
    }

    private void connectToExistingInstance() {
        if (graph.canConnect(outputSlot, inputSlot, true)) {
            canvasUI.getGraphHistory().addSnapshotBefore(new EdgeConnectGraphHistorySnapshot(graph, outputSlot, inputSlot));
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
