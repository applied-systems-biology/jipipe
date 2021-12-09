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

package org.hkijena.jipipe.ui.compat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.compat.ImageJDatatypeImporter;
import org.hkijena.jipipe.api.compat.SingleImageJAlgorithmRun;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.history.JIPipeDummyGraphHistoryJournal;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.AddAlgorithmSlotPanel;
import org.hkijena.jipipe.ui.components.DocumentTabPane;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.JIPipeNodeInfoListCellRenderer;
import org.hkijena.jipipe.ui.components.SearchTextField;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.hkijena.jipipe.utils.scripting.MacroUtils;
import org.scijava.Context;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * UI for {@link SingleImageJAlgorithmRun}
 */
public class RunSingleAlgorithmDialog extends JDialog implements JIPipeWorkbench {
    private final Context context;
    private boolean canceled = true;
    private SingleImageJAlgorithmRun runSettings;
    private JList<JIPipeNodeInfo> algorithmList;
    private SearchTextField searchField;
    private FormPanel formPanel;
    private int numThreads = RuntimeSettings.getInstance().getDefaultRunThreads();
    private DocumentTabPane tabPane;
    private JIPipeNotificationInbox notificationInbox = new JIPipeNotificationInbox();

    /**
     * @param context SciJava context
     */
    public RunSingleAlgorithmDialog(Context context) {
        this.context = context;
        initialize(null);
        reloadAlgorithmList();
    }

    /**
     * @param context      SciJava context
     * @param selectedNode the node that should be run. If not null, no selection list will be added
     */
    public RunSingleAlgorithmDialog(Context context, JIPipeNodeInfo selectedNode) {
        this.context = context;
        initialize(selectedNode);
        reloadAlgorithmList();
    }

    /**
     * @param context      SciJava context
     * @param selectedNode the node that should be run. If not null, no selection list will be added
     */
    public RunSingleAlgorithmDialog(Context context, Class<? extends JIPipeGraphNode> selectedNode) {
        this.context = context;
        initialize(JIPipe.getNodes().getNodeInfosFromClass(selectedNode).iterator().next());
        reloadAlgorithmList();
    }

    private void initialize(JIPipeNodeInfo selectedNode) {
        JPanel contentPanel = new JPanel(new BorderLayout(8, 8));

        JPanel listPanel = new JPanel(new BorderLayout());
        formPanel = new FormPanel(null, FormPanel.WITH_SCROLLING);

        if (selectedNode == null) {
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPanel, formPanel);
            splitPane.setDividerSize(3);
            splitPane.setResizeWeight(0.33);
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    super.componentResized(e);
                    splitPane.setDividerLocation(0.33);
                }
            });
            contentPanel.add(splitPane, BorderLayout.CENTER);
        } else {
            contentPanel.add(formPanel, BorderLayout.CENTER);
        }

        initializeToolbar(listPanel);
        initializeList(listPanel);
        initializeButtonPanel(contentPanel);

        tabPane = new DocumentTabPane();
        tabPane.addTab("Run single algorithm",
                UIUtils.getIconFromResources("actions/run-build.png"),
                contentPanel,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);
        setContentPane(tabPane);

        if (selectedNode != null) {
            reloadAlgorithmList();
        } else {
            selectNodeInfo(selectedNode);
        }
    }

    private void initializeToolbar(JPanel contentPanel) {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        searchField = new SearchTextField();
        searchField.addActionListener(e -> reloadAlgorithmList());
        toolBar.add(searchField);

        add(toolBar, BorderLayout.NORTH);

        contentPanel.add(toolBar, BorderLayout.NORTH);
    }

    private void reloadAlgorithmList() {
        List<JIPipeNodeInfo> infos = getFilteredAndSortedInfos();
        DefaultListModel<JIPipeNodeInfo> model = new DefaultListModel<>();
        for (JIPipeNodeInfo info : infos) {
            if (info.isCompatibleWithImageJ())
                model.addElement(info);
        }
        algorithmList.setModel(model);

        if (!model.isEmpty())
            algorithmList.setSelectedIndex(0);
        else
            selectNodeInfo(null);
    }

    private List<JIPipeNodeInfo> getFilteredAndSortedInfos() {
        Predicate<JIPipeNodeInfo> filterFunction = info -> searchField.test(info.getName() + " " + info.getDescription() + " " + info.getMenuPath());

        return JIPipe.getNodes().getRegisteredNodeInfos().values().stream().filter(filterFunction)
                .sorted(Comparator.comparing(JIPipeNodeInfo::getName)).collect(Collectors.toList());
    }

    private void initializeList(JPanel listPanel) {
        algorithmList = new JList<>();
        algorithmList.setBorder(BorderFactory.createEtchedBorder());
        algorithmList.setCellRenderer(new JIPipeNodeInfoListCellRenderer());
        algorithmList.setModel(new DefaultListModel<>());
        algorithmList.addListSelectionListener(e -> selectNodeInfo(algorithmList.getSelectedValue()));
        JScrollPane scrollPane = new JScrollPane(algorithmList);
        listPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private void selectNodeInfo(JIPipeNodeInfo info) {
        if (info != null) {
            if (runSettings != null) {
                runSettings.getEventBus().unregister(this);
            }
            runSettings = new SingleImageJAlgorithmRun(info.newInstance());
            reloadAlgorithmProperties();
            runSettings.getEventBus().register(this);
        } else {
            formPanel.clear();
        }
    }

    private void reloadAlgorithmProperties() {
        formPanel.clear();

        // Add some descriptions
        JTextPane descriptions = new JTextPane();
        descriptions.setContentType("text/html");
        descriptions.setText(TooltipUtils.getAlgorithmTooltip(runSettings.getAlgorithm().getInfo(), false));
        descriptions.setEditable(false);
        descriptions.setBorder(null);
        descriptions.setOpaque(false);
        formPanel.addWideToForm(descriptions, null);

        // Add runtime settings
        reloadRuntimeSettings();

        // Add slot importers
        reloadInputSlots();

        // Add output slots
        reloadOutputSlots();

        // Add parameter editor
        formPanel.addGroupHeader("Algorithm parameters", UIUtils.getIconFromResources("actions/wrench.png"));
        formPanel.addWideToForm(new ParameterPanel(this, runSettings.getAlgorithm(), null, ParameterPanel.NONE), null);

        formPanel.addVerticalGlue();
    }

    private void reloadRuntimeSettings() {
        formPanel.addGroupHeader("Runtime", UIUtils.getIconFromResources("actions/run-build.png"));
        SpinnerNumberModel model = new SpinnerNumberModel(numThreads, 1, Integer.MAX_VALUE, 1);
        JSpinner spinner = new JSpinner(model);
        spinner.addChangeListener(e -> {
            setNumThreads(model.getNumber().intValue());
        });
        formPanel.addToForm(spinner, new JLabel("Number of threads"), null);
    }

    private void reloadInputSlots() {
        FormPanel.GroupHeaderPanel inputDataHeaderPanel = formPanel.addGroupHeader("Input data", UIUtils.getIconFromResources("data-types/data-type.png"));
        boolean inputSlotsAreMutable = getAlgorithm().getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration;
        boolean inputSlotsAreRemovable = false;
        if (inputSlotsAreMutable) {
            JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getAlgorithm().getSlotConfiguration();
            if (slotConfiguration.canAddInputSlot()) {
                JButton addButton = new JButton(UIUtils.getIconFromResources("actions/list-add.png"));
                addButton.setToolTipText("Add new input");
                UIUtils.makeFlat25x25(addButton);
                addButton.addActionListener(e -> AddAlgorithmSlotPanel.showDialog(this, new JIPipeDummyGraphHistoryJournal(), getAlgorithm(), JIPipeSlotType.Input));
                inputDataHeaderPanel.addColumn(addButton);
            }
            if (slotConfiguration.canModifyInputSlots()) {
                inputSlotsAreRemovable = true;
            }
        }
        for (Map.Entry<String, ImageJDatatypeImporter> entry : runSettings.getInputSlotImporters().entrySet()) {
            ImageJDatatypeImporterUI ui = JIPipe.getImageJAdapters().getUIFor(entry.getValue());
            Component slotName;
            if (inputSlotsAreRemovable) {
                JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) getAlgorithm().getSlotConfiguration();
                JPanel panel = new JPanel(new BorderLayout(8, 0));
                JButton removeButton = new JButton(UIUtils.getIconFromResources("actions/close-tab.png"));
                UIUtils.makeBorderlessWithoutMargin(removeButton);
                removeButton.setToolTipText("Remove input slot");
                removeButton.addActionListener(e -> slotConfiguration.removeInputSlot(entry.getKey(), true));
                panel.add(removeButton, BorderLayout.WEST);
                panel.add(new JLabel(entry.getKey(),
                        JIPipe.getDataTypes().getIconFor(entry.getValue().getAdapter().getJIPipeDatatype()),
                        JLabel.LEFT), BorderLayout.CENTER);
                slotName = panel;
            } else {
                slotName = new JLabel(entry.getKey(),
                        JIPipe.getDataTypes().getIconFor(entry.getValue().getAdapter().getJIPipeDatatype()),
                        JLabel.LEFT);
            }
            formPanel.addToForm(ui, slotName, null);
        }
    }

    private void reloadOutputSlots() {
        FormPanel.GroupHeaderPanel outputDataHeaderPanel = formPanel.addGroupHeader("Output data", UIUtils.getIconFromResources("data-types/data-type.png"));
        boolean outputSlotsAreMutable = getAlgorithm().getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration;
        boolean outputSlotsAreRemovable = false;
        if (outputSlotsAreMutable) {
            JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) getAlgorithm().getSlotConfiguration();
            if (slotConfiguration.canAddInputSlot()) {
                JButton addButton = new JButton(UIUtils.getIconFromResources("actions/list-add.png"));
                addButton.setToolTipText("Add new output");
                UIUtils.makeFlat25x25(addButton);
                addButton.addActionListener(e -> AddAlgorithmSlotPanel.showDialog(this, new JIPipeDummyGraphHistoryJournal(), getAlgorithm(), JIPipeSlotType.Input));
                outputDataHeaderPanel.addColumn(addButton);
            }
            if (slotConfiguration.canModifyInputSlots()) {
                outputSlotsAreRemovable = true;
            }
        }
        for (JIPipeDataSlot outputSlot : getAlgorithm().getOutputSlots()) {
            Component slotName;
            if (outputSlotsAreRemovable) {
                JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) getAlgorithm().getSlotConfiguration();
                JPanel panel = new JPanel(new BorderLayout(8, 0));
                JButton removeButton = new JButton(UIUtils.getIconFromResources("actions/close-tab.png"));
                UIUtils.makeBorderlessWithoutMargin(removeButton);
                removeButton.setToolTipText("Remove output slot");
                removeButton.addActionListener(e -> slotConfiguration.removeOutputSlot(outputSlot.getName(), true));
                panel.add(removeButton, BorderLayout.WEST);
                panel.add(new JLabel(outputSlot.getName(),
                        JIPipe.getDataTypes().getIconFor(outputSlot.getAcceptedDataType()),
                        JLabel.LEFT), BorderLayout.CENTER);
                slotName = panel;
            } else {
                slotName = new JLabel(outputSlot.getName(),
                        JIPipe.getDataTypes().getIconFor(outputSlot.getAcceptedDataType()),
                        JLabel.LEFT);
            }
            formPanel.addWideToForm(slotName, null);
        }
    }

    /**
     * Triggered when algorithm slots are changed
     *
     * @param event Generated event
     */
    @Subscribe
    public void onAlgorithmSlotsChanged(JIPipeGraph.NodeSlotsChangedEvent event) {
        reloadAlgorithmProperties();
    }

    private void initializeButtonPanel(JPanel contentPanel) {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        JButton copyCommandButton = new JButton("Copy command", UIUtils.getIconFromResources("actions/edit-copy.png"));
        copyCommandButton.addActionListener(e -> copyCommand());
        buttonPanel.add(copyCommandButton);

        buttonPanel.add(Box.createHorizontalStrut(8));

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/cancel.png"));
        cancelButton.addActionListener(e -> {
            this.canceled = true;
            this.setVisible(false);
        });
        buttonPanel.add(cancelButton);

        JButton confirmButton = new JButton("Run", UIUtils.getIconFromResources("actions/run-build.png"));
        confirmButton.addActionListener(e -> runNow());
        buttonPanel.add(confirmButton);

        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
    }

    private void copyCommand() {
        JIPipeIssueReport report = new JIPipeIssueReport();
        runSettings.reportValidity(report);
        if (!report.isValid()) {
            UIUtils.openValidityReportDialog(this, report, false);
            return;
        }
        String json = getAlgorithmParametersJson();
        String macro = String.format("run(\"Run JIPipe algorithm\", \"nodeId=%s, threads=%d, parameters=%s\");", getAlgorithmId(), numThreads, MacroUtils.escapeString(json));
        StringSelection selection = new StringSelection(macro);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
    }

    private void runNow() {
        JIPipeIssueReport report = new JIPipeIssueReport();
        runSettings.reportValidity(report);
        if (!report.isValid()) {
            UIUtils.openValidityReportDialog(this, report, false);
            return;
        }
        canceled = false;
        setVisible(false);
    }

    public boolean isCanceled() {
        return canceled;
    }

    public String getAlgorithmId() {
        return runSettings.getAlgorithm().getInfo().getId();
    }

    public String getAlgorithmParametersJson() {
        try {
            return JsonUtils.getObjectMapper().writeValueAsString(runSettings);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public JIPipeGraphNode getAlgorithm() {
        return runSettings.getAlgorithm();
    }

    public SingleImageJAlgorithmRun getRunSettings() {
        return runSettings;
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
    public DocumentTabPane getDocumentTabPane() {
        return tabPane;
    }

    @Override
    public JIPipeNotificationInbox getNotificationInbox() {
        return notificationInbox;
    }
}
