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

package org.hkijena.acaq5.ui.compat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.compat.ImageJDatatypeImporter;
import org.hkijena.acaq5.api.compat.SingleImageJAlgorithmRun;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQDefaultMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQSlotType;
import org.hkijena.acaq5.api.events.AlgorithmSlotsChangedEvent;
import org.hkijena.acaq5.api.history.ACAQAlgorithmGraphHistory;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.extensions.settings.RuntimeSettings;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.components.*;
import org.hkijena.acaq5.ui.parameters.ParameterPanel;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.ui.registries.ACAQUIImageJDatatypeAdapterRegistry;
import org.hkijena.acaq5.utils.JsonUtils;
import org.hkijena.acaq5.utils.MacroUtils;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;
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
public class RunSingleAlgorithmDialog extends JDialog implements ACAQWorkbench {
    private Context context;
    private boolean canceled = true;
    private SingleImageJAlgorithmRun runSettings;
    private JList<ACAQAlgorithmDeclaration> algorithmList;
    private SearchTextField searchField;
    private JSplitPane splitPane;
    private FormPanel formPanel;
    private int numThreads = RuntimeSettings.getInstance().getDefaultRunThreads();
    private DocumentTabPane tabPane;

    /**
     * @param context SciJava context
     */
    public RunSingleAlgorithmDialog(Context context) {
        this.context = context;
        initialize();
    }

    private void initialize() {
        JPanel contentPanel = new JPanel(new BorderLayout(8, 8));

        JPanel listPanel = new JPanel(new BorderLayout());
        formPanel = new FormPanel(null, FormPanel.WITH_SCROLLING);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPanel, formPanel);
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

        initializeToolbar(listPanel);
        initializeList(listPanel);
        initializeButtonPanel(contentPanel);

        tabPane = new DocumentTabPane();
        tabPane.addTab("Run single algorithm",
                UIUtils.getIconFromResources("run.png"),
                contentPanel,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);
        setContentPane(tabPane);

        reloadAlgorithmList();
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
        List<ACAQAlgorithmDeclaration> declarations = getFilteredAndSortedDeclarations();
        DefaultListModel<ACAQAlgorithmDeclaration> model = new DefaultListModel<>();
        for (ACAQAlgorithmDeclaration declaration : declarations) {
            if (SingleImageJAlgorithmRun.isCompatible(declaration))
                model.addElement(declaration);
        }
        algorithmList.setModel(model);

        if (!model.isEmpty())
            algorithmList.setSelectedIndex(0);
        else
            selectAlgorithmDeclaration(null);
    }

    private List<ACAQAlgorithmDeclaration> getFilteredAndSortedDeclarations() {
        String[] searchStrings = searchField.getSearchStrings();
        Predicate<ACAQAlgorithmDeclaration> filterFunction = declaration -> {
            if (searchStrings != null && searchStrings.length > 0) {
                boolean matches = true;
                String name = declaration.getName() + " " + declaration.getDescription() + " " + declaration.getMenuPath();
                for (String searchString : searchStrings) {
                    if (!name.toLowerCase().contains(searchString.toLowerCase())) {
                        matches = false;
                        break;
                    }
                }
                return matches;
            } else {
                return true;
            }
        };

        return ACAQAlgorithmRegistry.getInstance().getRegisteredAlgorithms().values().stream().filter(filterFunction)
                .sorted(Comparator.comparing(ACAQAlgorithmDeclaration::getName)).collect(Collectors.toList());
    }

    private void initializeList(JPanel listPanel) {
        algorithmList = new JList<>();
        algorithmList.setBorder(BorderFactory.createEtchedBorder());
        algorithmList.setCellRenderer(new ACAQAlgorithmDeclarationListCellRenderer());
        algorithmList.setModel(new DefaultListModel<>());
        algorithmList.addListSelectionListener(e -> {
            selectAlgorithmDeclaration(algorithmList.getSelectedValue());
        });
        listPanel.add(new JScrollPane(algorithmList), BorderLayout.CENTER);
    }

    private void selectAlgorithmDeclaration(ACAQAlgorithmDeclaration declaration) {
        if (declaration != null) {
            if (runSettings != null) {
                runSettings.getEventBus().unregister(this);
            }
            runSettings = new SingleImageJAlgorithmRun(declaration.newInstance());
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
        descriptions.setText(TooltipUtils.getAlgorithmTooltip(runSettings.getAlgorithm().getDeclaration(), false));
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
        formPanel.addGroupHeader("Algorithm parameters", UIUtils.getIconFromResources("wrench.png"));
        formPanel.addWideToForm(new ParameterPanel(this, runSettings.getAlgorithm(), null, ParameterPanel.NONE), null);

        formPanel.addVerticalGlue();
    }

    private void reloadRuntimeSettings() {
        formPanel.addGroupHeader("Runtime", UIUtils.getIconFromResources("run.png"));
        SpinnerNumberModel model = new SpinnerNumberModel(numThreads, 1, Integer.MAX_VALUE, 1);
        JSpinner spinner = new JSpinner(model);
        spinner.addChangeListener(e -> {
            setNumThreads(model.getNumber().intValue());
        });
        formPanel.addToForm(spinner, new JLabel("Number of threads"), null);
    }

    private void reloadInputSlots() {
        FormPanel.GroupHeaderPanel inputDataHeaderPanel = formPanel.addGroupHeader("Input data", UIUtils.getIconFromResources("data-types/data-type.png"));
        boolean inputSlotsAreMutable = getAlgorithm().getSlotConfiguration() instanceof ACAQMutableSlotConfiguration;
        boolean inputSlotsAreRemovable = false;
        if (inputSlotsAreMutable) {
            ACAQDefaultMutableSlotConfiguration slotConfiguration = (ACAQDefaultMutableSlotConfiguration) getAlgorithm().getSlotConfiguration();
            if (slotConfiguration.canAddInputSlot()) {
                JButton addButton = new JButton(UIUtils.getIconFromResources("add.png"));
                addButton.setToolTipText("Add new input");
                UIUtils.makeFlat25x25(addButton);
                addButton.addActionListener(e -> AddAlgorithmSlotPanel.showDialog(this, new ACAQAlgorithmGraphHistory(), getAlgorithm(), ACAQSlotType.Input));
                inputDataHeaderPanel.addColumn(addButton);
            }
            if (slotConfiguration.canModifyInputSlots()) {
                inputSlotsAreRemovable = true;
            }
        }
        for (Map.Entry<String, ImageJDatatypeImporter> entry : runSettings.getInputSlotImporters().entrySet()) {
            ImageJDatatypeImporterUI ui = ACAQUIImageJDatatypeAdapterRegistry.getInstance().getUIFor(entry.getValue());
            Component slotName;
            if (inputSlotsAreRemovable) {
                ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration) getAlgorithm().getSlotConfiguration();
                JPanel panel = new JPanel(new BorderLayout(8, 0));
                JButton removeButton = new JButton(UIUtils.getIconFromResources("close-tab.png"));
                UIUtils.makeBorderlessWithoutMargin(removeButton);
                removeButton.setToolTipText("Remove input slot");
                removeButton.addActionListener(e -> slotConfiguration.removeInputSlot(entry.getKey(), true));
                panel.add(removeButton, BorderLayout.WEST);
                panel.add(new JLabel(entry.getKey(),
                        ACAQUIDatatypeRegistry.getInstance().getIconFor(entry.getValue().getAdapter().getACAQDatatype()),
                        JLabel.LEFT), BorderLayout.CENTER);
                slotName = panel;
            } else {
                slotName = new JLabel(entry.getKey(),
                        ACAQUIDatatypeRegistry.getInstance().getIconFor(entry.getValue().getAdapter().getACAQDatatype()),
                        JLabel.LEFT);
            }
            formPanel.addToForm(ui, slotName, null);
        }
    }

    private void reloadOutputSlots() {
        FormPanel.GroupHeaderPanel outputDataHeaderPanel = formPanel.addGroupHeader("Output data", UIUtils.getIconFromResources("data-types/data-type.png"));
        boolean outputSlotsAreMutable = getAlgorithm().getSlotConfiguration() instanceof ACAQMutableSlotConfiguration;
        boolean outputSlotsAreRemovable = false;
        if (outputSlotsAreMutable) {
            ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration) getAlgorithm().getSlotConfiguration();
            if (slotConfiguration.canAddInputSlot()) {
                JButton addButton = new JButton(UIUtils.getIconFromResources("add.png"));
                addButton.setToolTipText("Add new output");
                UIUtils.makeFlat25x25(addButton);
                addButton.addActionListener(e -> AddAlgorithmSlotPanel.showDialog(this, new ACAQAlgorithmGraphHistory(), getAlgorithm(), ACAQSlotType.Input));
                outputDataHeaderPanel.addColumn(addButton);
            }
            if (slotConfiguration.canModifyInputSlots()) {
                outputSlotsAreRemovable = true;
            }
        }
        for (ACAQDataSlot outputSlot : getAlgorithm().getOutputSlots()) {
            Component slotName;
            if (outputSlotsAreRemovable) {
                ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration) getAlgorithm().getSlotConfiguration();
                JPanel panel = new JPanel(new BorderLayout(8, 0));
                JButton removeButton = new JButton(UIUtils.getIconFromResources("close-tab.png"));
                UIUtils.makeBorderlessWithoutMargin(removeButton);
                removeButton.setToolTipText("Remove output slot");
                removeButton.addActionListener(e -> slotConfiguration.removeOutputSlot(outputSlot.getName(), true));
                panel.add(removeButton, BorderLayout.WEST);
                panel.add(new JLabel(outputSlot.getName(),
                        ACAQUIDatatypeRegistry.getInstance().getIconFor(outputSlot.getAcceptedDataType()),
                        JLabel.LEFT), BorderLayout.CENTER);
                slotName = panel;
            } else {
                slotName = new JLabel(outputSlot.getName(),
                        ACAQUIDatatypeRegistry.getInstance().getIconFor(outputSlot.getAcceptedDataType()),
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
    public void onAlgorithmSlotsChanged(AlgorithmSlotsChangedEvent event) {
        reloadAlgorithmProperties();
    }

    private void initializeButtonPanel(JPanel contentPanel) {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        JButton copyCommandButton = new JButton("Copy command", UIUtils.getIconFromResources("copy.png"));
        copyCommandButton.addActionListener(e -> copyCommand());
        buttonPanel.add(copyCommandButton);

        buttonPanel.add(Box.createHorizontalStrut(8));

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("remove.png"));
        cancelButton.addActionListener(e -> {
            this.canceled = true;
            this.setVisible(false);
        });
        buttonPanel.add(cancelButton);

        JButton confirmButton = new JButton("Run", UIUtils.getIconFromResources("run.png"));
        confirmButton.addActionListener(e -> runNow());
        buttonPanel.add(confirmButton);

        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
    }

    private void copyCommand() {
        ACAQValidityReport report = new ACAQValidityReport();
        runSettings.reportValidity(report);
        if (!report.isValid()) {
            UIUtils.openValidityReportDialog(this, report, false);
            return;
        }
        String json = getAlgorithmParametersJson();
        String macro = String.format("run(\"Run ACAQ5 algorithm\", \"algorithmId=%s, threads=%d, algorithmParameters=%s\");", getAlgorithmId(), numThreads, MacroUtils.escapeString(json));
        StringSelection selection = new StringSelection(macro);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
    }

    private void runNow() {
        ACAQValidityReport report = new ACAQValidityReport();
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
        return runSettings.getAlgorithm().getDeclaration().getId();
    }

    public String getAlgorithmParametersJson() {
        try {
            return JsonUtils.getObjectMapper().writeValueAsString(runSettings);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public ACAQGraphNode getAlgorithm() {
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
    public void sendStatusBarText(String text) {
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public DocumentTabPane getDocumentTabPane() {
        return tabPane;
    }
}
