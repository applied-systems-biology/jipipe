package org.hkijena.jipipe.ui.grapheditor.general.nodeui;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.properties.JIPipeDataSlotListCellRenderer;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class RewireConnectionsToolUI extends JDialog {

    private final JIPipeGraphCanvasUI graphCanvasUI;
    private final JIPipeDataSlot currentSlot;
    private final Set<JIPipeDataSlot> currentConnections;

    private final Set<JIPipeDataSlot> enabledConnections = new HashSet<>();

    private final SearchTextField searchTextField = new SearchTextField();

    private final JList<JIPipeDataSlot> alternativesList = new JList<>();

    private final UUID compartment;

    public RewireConnectionsToolUI(JIPipeGraphCanvasUI graphCanvasUI, JIPipeDataSlot currentSlot, Set<JIPipeDataSlot> currentConnections) {
        this.graphCanvasUI = graphCanvasUI;
        this.currentSlot = currentSlot;
        this.currentConnections = currentConnections;
        this.enabledConnections.addAll(currentConnections);
        this.compartment = graphCanvasUI.getCompartment();
        initialize();
        refreshAlternativesList();
    }

    private void initialize() {
        UIUtils.addEscapeListener(this);
        setIconImage(UIUtils.getJIPipeIcon128());
        setContentPane(new JPanel(new BorderLayout()));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        AutoResizeSplitPane splitPane = new AutoResizeSplitPane(AutoResizeSplitPane.LEFT_RIGHT, AutoResizeSplitPane.RATIO_1_TO_1);
        FormPanel connectionsList = new FormPanel(FormPanel.WITH_SCROLLING);
        FormPanel alternativesPanel = new FormPanel(FormPanel.NONE);

        splitPane.setLeftComponent(connectionsList);
        splitPane.setRightComponent(alternativesPanel);

        initializeConnectionsList(connectionsList);
        initializeAlternativesPanel(alternativesPanel, alternativesList);

        getContentPane().add(splitPane, BorderLayout.CENTER);

        JButton rewireButton = new JButton("Rewire", UIUtils.getIconFromResources("actions/checkmark.png"));
        rewireButton.addActionListener(e -> applyRewire());

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/cancel.png"));
        cancelButton.addActionListener(e -> setVisible(false));

        JPanel buttonPanel = UIUtils.boxHorizontal(Box.createHorizontalGlue(), cancelButton, rewireButton);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setModal(true);
        setSize(800, 600);
    }

    private void applyRewire() {
        JIPipeDataSlot selectedAlternative = alternativesList.getSelectedValue();
        if (selectedAlternative == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select an alternative target from the list.",
                    "No alternative selected",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        JIPipeGraph graph = currentSlot.getNode().getParentGraph();
        JIPipeGraph copyGraph = new JIPipeGraph(graph);

        // First simulate everything
        try {
            JIPipeDataSlot copyCurrentSlot = copyGraph.getEquivalentSlot(currentSlot);
            JIPipeDataSlot copySelectedAlternative = copyGraph.getEquivalentSlot(selectedAlternative);
            for (JIPipeDataSlot enabledConnection : enabledConnections) {
                JIPipeDataSlot copyEnabledConnection = copyGraph.getEquivalentSlot(enabledConnection);
                if (copyCurrentSlot.isOutput()) {
                    if (!copyGraph.disconnect(copyCurrentSlot, copyEnabledConnection, true)) {
                        throw new RuntimeException("Unable to disconnect!");
                    }
                } else {
                    if (!copyGraph.disconnect(copyEnabledConnection, copyCurrentSlot, true)) {
                        throw new RuntimeException("Unable to disconnect!");
                    }
                }
            }
            for (JIPipeDataSlot enabledConnection : enabledConnections) {
                JIPipeDataSlot copyEnabledConnection = copyGraph.getEquivalentSlot(enabledConnection);
                if (copyCurrentSlot.isOutput()) {
                    copyGraph.connect(copySelectedAlternative, copyEnabledConnection, true);
                } else {
                    copyGraph.connect(copyEnabledConnection, copySelectedAlternative, true);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "The rewire operation failed. No changes were applied.\nPlease check if the new connections lead to the creation of loops.",
                    "Rewire not possible",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Create snapshot
        if (graphCanvasUI.getHistoryJournal() != null) {
            graphCanvasUI.getHistoryJournal().snapshot("Rewire connection(s)",
                    "Rewire connections of " + currentSlot.getDisplayName() + " to " + selectedAlternative.getDisplayName(),
                    compartment,
                    UIUtils.getIconFromResources("actions/go-jump.png"));
        }

        // Simulation OK. Apply in real graph
        try {
            for (JIPipeDataSlot enabledConnection : enabledConnections) {
                if (currentSlot.isOutput()) {
                    if (!graph.disconnect(currentSlot, enabledConnection, true)) {
                        throw new RuntimeException("Unable to disconnect!");
                    }
                } else {
                    if (!graph.disconnect(enabledConnection, currentSlot, true)) {
                        throw new RuntimeException("Unable to disconnect!");
                    }
                }
            }
            for (JIPipeDataSlot enabledConnection : enabledConnections) {
                if (currentSlot.isOutput()) {
                    graph.connect(selectedAlternative, enabledConnection, true);
                } else {
                    graph.connect(enabledConnection, selectedAlternative, true);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "The rewire operation failed at phase 2. Please report this to the developer. JIPipe will attempt to undo the changes.",
                    "Rewire not possible",
                    JOptionPane.ERROR_MESSAGE);
            if (graphCanvasUI.getHistoryJournal() != null) {
                graphCanvasUI.getHistoryJournal().undo(compartment);
            }
        }

        // Select the targeted node
        graphCanvasUI.selectOnly(graphCanvasUI.getNodeUIs().get(selectedAlternative.getNode()));

        setVisible(false);
    }

    private void initializeAlternativesPanel(FormPanel alternativesPanel, JList<JIPipeDataSlot> alternativesList) {
        FormPanel.GroupHeaderPanel groupHeader = alternativesPanel.addGroupHeader("Compatible targets", UIUtils.getIconFromResources("actions/go-jump.png"));
        groupHeader.setDescription("Please select a compatible slot where the selected connections will be moved to. Please note that this list will be empty of no compatible targets can be found.");
        JScrollPane scrollPane = new JScrollPane(alternativesList);
        alternativesList.setCellRenderer(new JIPipeDataSlotListCellRenderer());


        JPanel listPanel = new JPanel(new BorderLayout());
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(searchTextField);
        listPanel.add(toolBar, BorderLayout.NORTH);
        listPanel.add(scrollPane, BorderLayout.CENTER);

        listPanel.setBorder(UIUtils.createControlBorder());

        alternativesPanel.addVerticalGlue(listPanel, null);

        searchTextField.addActionListener(e -> refreshAlternativesList());
        alternativesList.addListSelectionListener(e -> {
            if (alternativesList.getSelectedValue() != null) {
                // Select the alternative node
                graphCanvasUI.selectOnly(graphCanvasUI.getNodeUIs().get(alternativesList.getSelectedValue().getNode()));
            } else {
                // Select the source node
                graphCanvasUI.selectOnly(graphCanvasUI.getNodeUIs().get(currentSlot.getNode()));
            }
        });
    }

    private void refreshAlternativesList() {
        Set<JIPipeGraphNode> currentConnectionNodes = enabledConnections.stream().map(JIPipeDataSlot::getNode).collect(Collectors.toSet());
        currentConnectionNodes.add(currentSlot.getNode());

        Set<JIPipeDataSlot> alternatives = new HashSet<>();
        for (JIPipeDataSlot enabledConnection : enabledConnections) {
            if (enabledConnection.isInput()) {
                // Search for another output that produces compatible data
                for (JIPipeDataSlot availableSource : enabledConnection.getNode().getParentGraph().getAvailableSources(enabledConnection, true, false)) {
                    if (availableSource == currentSlot)
                        continue;
                    if (!availableSource.getNode().getInfo().isRunnable())
                        continue;
                    if (availableSource.getNode().isVisibleIn(compartment)) {
                        alternatives.add(availableSource);
                    }
                }
            } else {
                // Search for another input that is compatible to the produced data
                for (JIPipeDataSlot availableTarget : enabledConnection.getNode().getParentGraph().getAvailableTargets(enabledConnection, true, false)) {
                    if (availableTarget == currentSlot)
                        continue;
                    if (!availableTarget.getNode().getInfo().isRunnable())
                        continue;
                    if (availableTarget.getNode().isVisibleIn(compartment)) {
                        alternatives.add(availableTarget);
                    }
                }
            }
        }
        alternatives.remove(currentSlot);

        // Search filter
        alternatives.removeIf(alternative -> {
            for (JIPipeDataSlot enabledConnection : enabledConnections) {
                if (enabledConnection.isInput()) {
                    if (!JIPipe.getDataTypes().isConvertible(alternative.getAcceptedDataType(), enabledConnection.getAcceptedDataType())) {
                        return true;
                    }
                } else {
                    if (!JIPipe.getDataTypes().isConvertible(enabledConnection.getAcceptedDataType(), alternative.getAcceptedDataType())) {
                        return true;
                    }
                }
            }
            return !searchTextField.test(alternative.getName() + " " + alternative.getNode().getName());
        });
        DefaultListModel<JIPipeDataSlot> model = new DefaultListModel<>();
        alternatives.stream().sorted(Comparator.comparing((JIPipeDataSlot alternative) -> {
                    int sumDistance = 0;
                    for (JIPipeDataSlot enabledConnection : enabledConnections) {
                        if (enabledConnection.isInput()) {
                            int d = JIPipe.getDataTypes().getConversionDistance(alternative.getAcceptedDataType(), enabledConnection.getAcceptedDataType());
                            sumDistance += d;
                        } else {
                            int d = JIPipe.getDataTypes().getConversionDistance(enabledConnection.getAcceptedDataType(), alternative.getAcceptedDataType());
                            sumDistance += d;
                        }
                    }
                    return sumDistance;
                })
                .thenComparing((JIPipeDataSlot slot) -> slot.getNode().getName())
                .thenComparing(JIPipeDataSlot::getName)).forEach(model::addElement);

        alternativesList.setModel(model);
    }


    private void initializeConnectionsList(FormPanel connectionsList) {
        FormPanel.GroupHeaderPanel groupHeader = connectionsList.addGroupHeader("List of rewired connections", UIUtils.getIconFromResources("actions/lines-connector.png"));
        groupHeader.setDescription("You have the option to only move specific connections to another output.");
        for (JIPipeDataSlot currentConnection : currentConnections) {
            JCheckBox checkBox;
            if (currentConnection.isInput()) {
                // Output to input
                checkBox = new JCheckBox("<html>" + "<span style=\"color: gray;\">To input</span> " + currentConnection.getName() + "<br/><small>" +
                        StringUtils.createIconTextHTMLTable(currentConnection.getNode().getDisplayName(), JIPipe.getNodes().getIconURLFor(currentConnection.getNode().getInfo()))
                        + "</small></html>");
            } else {
                // Input to output
                checkBox = new JCheckBox("<html>" + "<span style=\"color: gray;\">From output</span> " + currentConnection.getName() + "<br/><small>" +
                        StringUtils.createIconTextHTMLTable(currentConnection.getNode().getDisplayName(), JIPipe.getNodes().getIconURLFor(currentConnection.getNode().getInfo()))
                        + "</small></html>");
            }
            checkBox.setSelected(true);
            checkBox.addActionListener(e -> {
                if (checkBox.isSelected()) {
                    enabledConnections.add(currentConnection);
                } else {
                    enabledConnections.remove(currentConnection);
                }
                refreshAlternativesList();
            });
            connectionsList.addWideToForm(checkBox);
        }
        connectionsList.addVerticalGlue();
    }
}
