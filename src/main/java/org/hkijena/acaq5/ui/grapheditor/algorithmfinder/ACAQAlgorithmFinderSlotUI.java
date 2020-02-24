package org.hkijena.acaq5.ui.grapheditor.algorithmfinder;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.ui.events.AlgorithmFinderSuccessEvent;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

import static org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI.SLOT_UI_HEIGHT;

public class ACAQAlgorithmFinderSlotUI extends JPanel {

    private ACAQDataSlot<?> outputSlot;
    private ACAQAlgorithmGraph graph;
    private ACAQDataSlot<?> inputSlot;
    private boolean isExistingInstance;
    private EventBus eventBus = new EventBus();

    private JButton assignButton;
    private JPopupMenu assignButtonMenu;

    public ACAQAlgorithmFinderSlotUI(ACAQDataSlot<?> outputSlot, ACAQAlgorithmGraph graph, ACAQDataSlot<?> inputSlot, boolean isExistingInstance) {
        this.outputSlot = outputSlot;
        this.graph = graph;
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
        nameLabel.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
        nameLabel.setIcon(ACAQRegistryService.getInstance().getUIDatatypeRegistry().getIconFor(inputSlot.getAcceptedDataType()));

        add(nameLabel, BorderLayout.CENTER);

        add(assignButton, BorderLayout.WEST);
        nameLabel.setHorizontalAlignment(JLabel.LEFT);
        nameLabel.setHorizontalTextPosition(JLabel.RIGHT);

        reloadAssignMenu();
    }

    private void reloadAssignMenu() {
        assignButtonMenu.removeAll();

        if(isExistingInstance) {
            if(graph.getSourceSlot(inputSlot) != null) {
                JMenuItem disconnectExistingButton = new JMenuItem("Disconnect existing: " + graph.getSourceSlot(inputSlot).getFullName(), UIUtils.getIconFromResources("remove.png"));
                disconnectExistingButton.addActionListener(e -> disconnectAllExistingInstance());
                assignButtonMenu.add(disconnectExistingButton);
            }
            else {
                JMenuItem connectButton = new JMenuItem(inputSlot.getFullName(), ACAQRegistryService.getInstance().getUIDatatypeRegistry().getIconFor(inputSlot.getAcceptedDataType()));
                connectButton.addActionListener(e -> connectToExistingInstance());
                assignButtonMenu.add(connectButton);
            }
        }
        else {
            JMenuItem connectButton = new JMenuItem(inputSlot.getFullName(), ACAQRegistryService.getInstance().getUIDatatypeRegistry().getIconFor(inputSlot.getAcceptedDataType()));
            connectButton.addActionListener(e -> connectToNewInstance());
            assignButtonMenu.add(connectButton);
        }
    }

    private void connectToNewInstance() {
        graph.insertNode(inputSlot.getAlgorithm());
        graph.connect(outputSlot, inputSlot);
        eventBus.post(new AlgorithmFinderSuccessEvent(outputSlot, inputSlot));
    }

    private void disconnectAllExistingInstance() {
        graph.disconnectAll(inputSlot);
        reloadAssignMenu();
    }

    private void connectToExistingInstance() {
        if(graph.canConnect(outputSlot, inputSlot)) {
            graph.connect(outputSlot, inputSlot);
            eventBus.post(new AlgorithmFinderSuccessEvent(outputSlot, inputSlot));
        }
        else {
            JOptionPane.showMessageDialog(this, "The data slots could not be connected. Is this connection causing loops?", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public EventBus getEventBus() {
        return eventBus;
    }
}
