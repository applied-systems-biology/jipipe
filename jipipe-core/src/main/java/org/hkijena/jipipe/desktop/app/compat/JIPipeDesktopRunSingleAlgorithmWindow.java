/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop.app.compat;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeRunAlgorithmCommand;
import org.hkijena.jipipe.api.compat.SingleImageJAlgorithmRunConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopDummyWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.documentation.JIPipeDataTypeCompendiumUI;
import org.hkijena.jipipe.desktop.app.documentation.JIPipeDesktopAlgorithmCompendiumUI;
import org.hkijena.jipipe.desktop.commons.components.renderers.JIPipeNodeInfoListCellRenderer;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextField;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.plugins.settings.JIPipeRuntimeApplicationSettings;
import org.hkijena.jipipe.utils.JIPipeDesktopSplitPane;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.scripting.MacroUtils;
import org.scijava.Context;
import org.scijava.command.CommandService;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * UI for {@link SingleImageJAlgorithmRunConfiguration}
 */
public class JIPipeDesktopRunSingleAlgorithmWindow extends JFrame implements JIPipeDesktopWorkbench {
    public static final String HELP_URL = "https://www.jipipe.org/documentation/imagej-integration/#running-a-single-node";

    private final Context context;
    private final JIPipeNotificationInbox notificationInbox = new JIPipeNotificationInbox();
    private final JCheckBox keepWindowToggle = new JCheckBox("Keep window open", true);
    private JList<JIPipeNodeInfo> algorithmList;
    private JIPipeDesktopSearchTextField searchField;
    private JPanel settingsPanel;
    private JIPipeDesktopRunSingleAlgorithmSettingsPanel currentRunSettingsPanel;
    private int numThreads = JIPipeRuntimeApplicationSettings.getInstance().getDefaultRunThreads();
    private JIPipeDesktopTabPane tabPane;

    /**
     * @param context SciJava context
     */
    public JIPipeDesktopRunSingleAlgorithmWindow(Context context) {
        this.context = context;
        initialize();
        selectNode(null);
        reloadAlgorithmList();
    }

    /**
     * @param context      SciJava context
     * @param selectedNode the node that should be run. If not null, no selection list will be added
     */
    public JIPipeDesktopRunSingleAlgorithmWindow(Context context, JIPipeNodeInfo selectedNode) {
        this.context = context;
        reloadAlgorithmList();
        selectNode(selectedNode);
    }

    /**
     * @param context      SciJava context
     * @param selectedNode the node that should be run. If not null, no selection list will be added
     */
    public JIPipeDesktopRunSingleAlgorithmWindow(Context context, Class<? extends JIPipeGraphNode> selectedNode) {
        this.context = context;
        reloadAlgorithmList();
        selectNode(JIPipe.getNodes().getNodeInfosFromClass(selectedNode).iterator().next());
    }

    private void initialize() {
        JPanel contentPanel = new JPanel(new BorderLayout());
        JPanel listPanel = new JPanel(new BorderLayout());
        JPanel settingsContainer = new JPanel(new BorderLayout());
        this.settingsPanel = new JPanel(new BorderLayout());
        settingsContainer.add(settingsPanel, BorderLayout.CENTER);

        tabPane = new JIPipeDesktopTabPane(true, JIPipeDesktopTabPane.TabPlacement.Top);
        setContentPane(tabPane);

        JIPipeDesktopSplitPane splitPane = new JIPipeDesktopSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPanel, settingsContainer, JIPipeDesktopSplitPane.RATIO_1_TO_3);
        contentPanel.add(splitPane, BorderLayout.CENTER);

        initializeMenu();
        initializeToolbar(listPanel);
        initializeList(listPanel);
        initializeButtonPanel(settingsContainer);

        tabPane.addTab("Run JIPipe node", UIUtils.getIconFromResources("apps/jipipe.png"), contentPanel, JIPipeDesktopTabPane.CloseMode.withoutCloseButton);
    }

    private void initializeMenu() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(Box.createHorizontalGlue());

        JMenu helpMenu = new JMenu("Help");
        helpMenu.setPreferredSize(new Dimension(60, 32));
        helpMenu.setIcon(UIUtils.getIconFromResources("actions/help.png"));

        JMenuItem manualButton = new JMenuItem("Open online documentation", UIUtils.getIconFromResources("actions/help.png"));
        manualButton.setToolTipText("Opens the documentation for the single algorithm run feature. " + HELP_URL);
        manualButton.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI(HELP_URL));
            } catch (IOException | URISyntaxException ex) {
                ex.printStackTrace();
            }
        });
        helpMenu.add(manualButton);

        JMenuItem algorithmCompendiumButton = new JMenuItem("Open node documentation", UIUtils.getIconFromResources("data-types/node.png"));
        algorithmCompendiumButton.addActionListener(e -> {
            getDocumentTabPane().addTab("Node documentation",
                    UIUtils.getIconFromResources("actions/help.png"),
                    new JIPipeDesktopAlgorithmCompendiumUI(),
                    JIPipeDesktopTabPane.CloseMode.withSilentCloseButton,
                    true);
            getDocumentTabPane().switchToLastTab();
        });
        helpMenu.add(algorithmCompendiumButton);

        JMenuItem datatypeCompendiumButton = new JMenuItem("Open data type documentation", UIUtils.getIconFromResources("data-types/data-type.png"));
        datatypeCompendiumButton.addActionListener(e -> {
            getDocumentTabPane().addTab("Data type documentation",
                    UIUtils.getIconFromResources("actions/help.png"),
                    new JIPipeDataTypeCompendiumUI(),
                    JIPipeDesktopTabPane.CloseMode.withSilentCloseButton,
                    true);
            getDocumentTabPane().switchToLastTab();
        });
        helpMenu.add(datatypeCompendiumButton);

        menuBar.add(helpMenu);

        add(menuBar, BorderLayout.NORTH);
    }

    private void initializeToolbar(JPanel contentPanel) {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        searchField = new JIPipeDesktopSearchTextField();
        searchField.addActionListener(e -> reloadAlgorithmList());
        toolBar.add(searchField);
        contentPanel.add(toolBar, BorderLayout.NORTH);
    }

    private void initializeList(JPanel listPanel) {
        algorithmList = new JList<>();
        algorithmList.setBorder(UIUtils.createControlBorder());
        algorithmList.setCellRenderer(new JIPipeNodeInfoListCellRenderer());
        algorithmList.setModel(new DefaultListModel<>());
        algorithmList.addListSelectionListener(e -> selectNode(algorithmList.getSelectedValue()));
        JScrollPane scrollPane = new JScrollPane(algorithmList);
        listPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private void selectNode(JIPipeNodeInfo info) {
        settingsPanel.removeAll();
        currentRunSettingsPanel = null;
        if (info != null) {
            currentRunSettingsPanel = new JIPipeDesktopRunSingleAlgorithmSettingsPanel(this, info);
            settingsPanel.add(currentRunSettingsPanel, BorderLayout.CENTER);
        } else {

        }
        revalidate();
        repaint();
    }


    private void reloadAlgorithmList() {
        List<JIPipeNodeInfo> infos = getFilteredAndSortedInfos();
        DefaultListModel<JIPipeNodeInfo> model = new DefaultListModel<>();
        for (JIPipeNodeInfo info : infos) {
            model.addElement(info);
        }
        algorithmList.setModel(model);

        if (!model.isEmpty())
            algorithmList.setSelectedIndex(0);
        else
            selectNode(null);
    }

    private List<JIPipeNodeInfo> getFilteredAndSortedInfos() {
        Predicate<JIPipeNodeInfo> filterFunction = info -> searchField.test(info.getName() + " " + info.getDescription() + " " + info.getMenuPath());

        return JIPipe.getNodes().getRegisteredNodeInfos().values().stream().filter(filterFunction)
                .sorted(Comparator.comparing(JIPipeNodeInfo::getName)).collect(Collectors.toList());
    }

//    private void selectNodeInfo(JIPipeNodeInfo info) {
//        if (info != null) {
//            if (runSettings != null) {
//                runSettings.getEventBus().unregister(this);
//            }
//            runSettings = new SingleImageJAlgorithmRunConfiguration(info.newInstance());
//            reloadAlgorithmProperties();
//            runSettings.getEventBus().register(this);
//        } else {
//            formPanel.clear();
//        }
//    }

//    private void reloadAlgorithmProperties() {
//        formPanel.clear();
//
//        // Add some descriptions
//        JTextPane descriptions = new JTextPane();
//        descriptions.setContentType("text/html");
//        descriptions.setText(TooltipUtils.getAlgorithmTooltip(runSettings.getAlgorithm().getInfo(), false));
//        descriptions.setEditable(false);
//        descriptions.setBorder(null);
//        descriptions.setOpaque(false);
//        formPanel.addWideToForm(descriptions, null);
//
//        // Add runtime settings
//        reloadRuntimeSettings();
//
//        // Add slot importers
//        reloadInputSlots();
//
//        // Add output slots
//        reloadOutputSlots();
//
//        // Add parameter editor
//        formPanel.addGroupHeader("Algorithm parameters", UIUtils.getIconFromResources("actions/wrench.png"));
//        formPanel.addWideToForm(new ParameterPanel(this, runSettings.getAlgorithm(), null, ParameterPanel.NONE), null);
//
//        formPanel.addVerticalGlue();
//    }

//    private void reloadRuntimeSettings() {
//        formPanel.addGroupHeader("Runtime", UIUtils.getIconFromResources("actions/run-build.png"));
//        SpinnerNumberModel model = new SpinnerNumberModel(numThreads, 1, Integer.MAX_VALUE, 1);
//        JSpinner spinner = new JSpinner(model);
//        spinner.addChangeListener(e -> {
//            setNumThreads(model.getNumber().intValue());
//        });
//        formPanel.addToForm(spinner, new JLabel("Number of threads"), null);
//    }
//
//    private void reloadInputSlots() {
//        FormPanel.GroupHeaderPanel inputDataHeaderPanel = formPanel.addGroupHeader("Input data", UIUtils.getIconFromResources("data-types/data-type.png"));
//        boolean inputSlotsAreMutable = getAlgorithm().getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration;
//        boolean inputSlotsAreRemovable = false;
//        if (inputSlotsAreMutable) {
//            JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getAlgorithm().getSlotConfiguration();
//            if (slotConfiguration.canAddInputSlot()) {
//                JButton addButton = new JButton(UIUtils.getIconFromResources("actions/list-add.png"));
//                addButton.setToolTipText("Add new input");
//                UIUtils.makeFlat25x25(addButton);
//                addButton.addActionListener(e -> AddAlgorithmSlotPanel.showDialog(this, new JIPipeDummyGraphHistoryJournal(), getAlgorithm(), JIPipeSlotType.Input));
//                inputDataHeaderPanel.addColumn(addButton);
//            }
//            if (slotConfiguration.canModifyInputSlots()) {
//                inputSlotsAreRemovable = true;
//            }
//        }
//        for (Map.Entry<String, ImageJDatatypeImporter> entry : runSettings.getInputSlotImporters().entrySet()) {
//            ImageJDatatypeImporterUI ui = JIPipe.getImageJAdapters().getUIFor(entry.getValue());
//            Component slotName;
//            if (inputSlotsAreRemovable) {
//                JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) getAlgorithm().getSlotConfiguration();
//                JPanel panel = new JPanel(new BorderLayout(8, 0));
//                JButton removeButton = new JButton(UIUtils.getIconFromResources("actions/close-tab.png"));
//                UIUtils.makeBorderlessWithoutMargin(removeButton);
//                removeButton.setToolTipText("Remove input slot");
//                removeButton.addActionListener(e -> slotConfiguration.removeInputSlot(entry.getKey(), true));
//                panel.add(removeButton, BorderLayout.WEST);
//                panel.add(new JLabel(entry.getKey(),
//                        JIPipe.getDataTypes().getIconFor(entry.getValue().getAdapter().getJIPipeDatatype()),
//                        JLabel.LEFT), BorderLayout.CENTER);
//                slotName = panel;
//            } else {
//                slotName = new JLabel(entry.getKey(),
//                        JIPipe.getDataTypes().getIconFor(entry.getValue().getAdapter().getJIPipeDatatype()),
//                        JLabel.LEFT);
//            }
//            formPanel.addToForm(ui, slotName, null);
//        }
//    }
//
//    private void reloadOutputSlots() {
//        FormPanel.GroupHeaderPanel outputDataHeaderPanel = formPanel.addGroupHeader("Output data", UIUtils.getIconFromResources("data-types/data-type.png"));
//        boolean outputSlotsAreMutable = getAlgorithm().getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration;
//        boolean outputSlotsAreRemovable = false;
//        if (outputSlotsAreMutable) {
//            JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) getAlgorithm().getSlotConfiguration();
//            if (slotConfiguration.canAddInputSlot()) {
//                JButton addButton = new JButton(UIUtils.getIconFromResources("actions/list-add.png"));
//                addButton.setToolTipText("Add new output");
//                UIUtils.makeFlat25x25(addButton);
//                addButton.addActionListener(e -> AddAlgorithmSlotPanel.showDialog(this, new JIPipeDummyGraphHistoryJournal(), getAlgorithm(), JIPipeSlotType.Input));
//                outputDataHeaderPanel.addColumn(addButton);
//            }
//            if (slotConfiguration.canModifyInputSlots()) {
//                outputSlotsAreRemovable = true;
//            }
//        }
//        for (JIPipeDataSlot outputSlot : getAlgorithm().getOutputSlots()) {
//            Component slotName;
//            if (outputSlotsAreRemovable) {
//                JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) getAlgorithm().getSlotConfiguration();
//                JPanel panel = new JPanel(new BorderLayout(8, 0));
//                JButton removeButton = new JButton(UIUtils.getIconFromResources("actions/close-tab.png"));
//                UIUtils.makeBorderlessWithoutMargin(removeButton);
//                removeButton.setToolTipText("Remove output slot");
//                removeButton.addActionListener(e -> slotConfiguration.removeOutputSlot(outputSlot.getName(), true));
//                panel.add(removeButton, BorderLayout.WEST);
//                panel.add(new JLabel(outputSlot.getName(),
//                        JIPipe.getDataTypes().getIconFor(outputSlot.getAcceptedDataType()),
//                        JLabel.LEFT), BorderLayout.CENTER);
//                slotName = panel;
//            } else {
//                slotName = new JLabel(outputSlot.getName(),
//                        JIPipe.getDataTypes().getIconFor(outputSlot.getAcceptedDataType()),
//                        JLabel.LEFT);
//            }
//            formPanel.addWideToForm(slotName, null);
//        }
//    }

//    /**
//     * Triggered when algorithm slots are changed
//     *
//     * @param event Generated event
//     */
//    @Override
//    public void onAlgorithmSlotsChanged(JIPipeGraph.NodeSlotsChangedEvent event) {
//        reloadAlgorithmProperties();
//    }

    private void initializeButtonPanel(JPanel contentPanel) {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

        buttonPanel.add(keepWindowToggle);

        buttonPanel.add(Box.createHorizontalGlue());

        JButton copyCommandButton = new JButton("Copy command", UIUtils.getIconFromResources("actions/edit-copy.png"));
        copyCommandButton.addActionListener(e -> copyCommand());
        buttonPanel.add(copyCommandButton);

        buttonPanel.add(Box.createHorizontalStrut(8));

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/cancel.png"));
        cancelButton.addActionListener(e -> this.setVisible(false));
        buttonPanel.add(cancelButton);

        JButton confirmButton = new JButton("Run", UIUtils.getIconFromResources("actions/run-build.png"));
        confirmButton.addActionListener(e -> runNow());
        buttonPanel.add(confirmButton);

        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
    }

    private void copyCommand() {
        JIPipeValidationReport report = new JIPipeValidationReport();
        currentRunSettingsPanel.getRun().reportValidity(new UnspecifiedValidationReportContext(), report);
        if (!report.isEmpty()) {
            UIUtils.showValidityReportDialog(new JIPipeDesktopDummyWorkbench(),
                    this,
                    report,
                    "Issues with the run",
                    "The following issues have been detected:",
                    false);
            return;
        }
        String parameters = getRun().getParametersString();
        String inputs = getRun().getInputsString();
        String outputs = getRun().getOutputsString();
        String macro = String.format("run(\"Run JIPipe node\", \"nodeId=%s, threads=%d, parameters=%s, inputs=%s, outputs=%s\");",
                getAlgorithmId(),
                numThreads,
                MacroUtils.escapeString(parameters),
                MacroUtils.escapeString(inputs),
                MacroUtils.escapeString(outputs));
        StringSelection selection = new StringSelection(macro);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
    }

    private void runNow() {
        JIPipeValidationReport report = new JIPipeValidationReport();
        currentRunSettingsPanel.getRun().reportValidity(new UnspecifiedValidationReportContext(), report);
        if (!report.isEmpty()) {
            UIUtils.showValidityReportDialog(new JIPipeDesktopDummyWorkbench(),
                    this,
                    report,
                    "Issues with the run",
                    "The following issues have been detected:",
                    false);
            return;
        }
        if (!keepWindowToggle.isSelected())
            setVisible(false);
        String parameters = getRun().getParametersString();
        String inputs = getRun().getInputsString();
        String outputs = getRun().getOutputsString();
        CommandService commandService = getContext().getService(CommandService.class);
        Map<String, Object> map = new HashMap<>();
        map.put("nodeId", getAlgorithmId());
        map.put("threads", numThreads);
        map.put("parameters", parameters);
        map.put("inputs", inputs);
        map.put("outputs", outputs);
        commandService.run(JIPipeRunAlgorithmCommand.class, true, map);
    }

    public String getAlgorithmId() {
        return currentRunSettingsPanel.getNodeInfo().getId();
    }

    public JIPipeGraphNode getAlgorithm() {
        return currentRunSettingsPanel.getNode();
    }

    public SingleImageJAlgorithmRunConfiguration getRun() {
        return currentRunSettingsPanel.getRun();
    }

    public int getNumThreads() {
        return numThreads;
    }

    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }

    @Override
    public Window getWindow() {
        return this;
    }

    @Override
    public void sendStatusBarText(String text) {
    }

    @Override
    public boolean isProjectModified() {
        return false;
    }

    @Override
    public void setProjectModified(boolean modified) {

    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public JIPipeDesktopTabPane getDocumentTabPane() {
        return tabPane;
    }

    @Override
    public JIPipeNotificationInbox getNotificationInbox() {
        return notificationInbox;
    }

    @Override
    public void showMessageDialog(String message, String title) {
        JOptionPane.showMessageDialog(getWindow(), message, title, JOptionPane.PLAIN_MESSAGE);
    }

    @Override
    public void showErrorDialog(String message, String title) {
        JOptionPane.showMessageDialog(getWindow(), message, title, JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public JIPipeProject getProject() {
        return null;
    }
}
