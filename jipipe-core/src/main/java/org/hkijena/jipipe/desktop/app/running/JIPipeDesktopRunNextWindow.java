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

package org.hkijena.jipipe.desktop.app.running;

import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartmentOutput;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.run.JIPipeProjectRunSet;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchAccess;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.quickrun.JIPipeDesktopQuickRun;
import org.hkijena.jipipe.desktop.app.quickrun.JIPipeDesktopQuickRunSettings;
import org.hkijena.jipipe.desktop.app.resultanalysis.JIPipeDesktopResultUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopMessagePanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopPathEditorComponent;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopToggleButtonPanel;
import org.hkijena.jipipe.desktop.commons.components.icons.SolidColorIcon;
import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopHTMLEditorKit;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernMetalTheme;
import org.hkijena.jipipe.plugins.settings.JIPipeRuntimeApplicationSettings;
import org.hkijena.jipipe.utils.*;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Next-generation run dialog
 */
public class JIPipeDesktopRunNextWindow extends JFrame implements JIPipeDesktopProjectWorkbenchAccess, JIPipeRunnable.FinishedEventListener, JIPipeRunnable.InterruptedEventListener {
    private final JIPipeDesktopProjectWorkbench workbench;
    private final JPanel startingPage = new JPanel(new BorderLayout(8, 8));
    private final JList<Object> startingPageRunOptions = new JList<>();
    private final JIPipeDesktopFormPanel startingPageOptionsPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);

    private final JIPipeDesktopToggleButtonPanel<StartPageStorageMode> startingPageStorageModeSelection = new JIPipeDesktopToggleButtonPanel<>();
    private final JIPipeDesktopToggleButtonPanel<Boolean> startingPageIntermediateResultsModeSelection = new JIPipeDesktopToggleButtonPanel<>();
    private final JIPipeDesktopPathEditorComponent startingPageOutputPath;

    private JIPipeDesktopQuickRun run;
    private List<JIPipeAlgorithm> nodes;
    private boolean shouldAskOnClose = false;

    public JIPipeDesktopRunNextWindow(JIPipeDesktopProjectWorkbench workbench) {
        this.workbench = workbench;
        this.startingPageOutputPath = new JIPipeDesktopPathEditorComponent(this.workbench, PathIOMode.Save, PathType.DirectoriesOnly);
        this.startingPageOutputPath.setPath(workbench.getProject().newTemporaryDirectory());

        initialize();
        initializeStartingPage();
        postInitialize();
        activatePage(startingPage);

        initializeRunOptions();
        initializeEvents();

        JIPipeRunnableQueue.getInstance().getFinishedEventEmitter().subscribe(this);
        JIPipeRunnableQueue.getInstance().getInterruptedEventEmitter().subscribe(this);
    }

    private void initializeEvents() {
        // Rebuild options if this changes
        startingPageStorageModeSelection.addActionListener(e -> onRunOptionSelectionChanged());
    }

    private void initializeRunOptions() {
        DefaultListModel<Object> items = new DefaultListModel<>();
        for (JIPipeProjectRunSet runSet : getProject().getRunSetsConfiguration().getRunSets()) {
            items.addElement(runSet);
        }
        for (JIPipeGraphNode graphNode : getProject().getCompartmentGraph().traverse()) {
            if (graphNode instanceof JIPipeProjectCompartment) {
                for (JIPipeProjectCompartmentOutput outputNode : ((JIPipeProjectCompartment) graphNode).getSortedOutputNodes()) {
                    items.addElement(outputNode);
                }
            }
        }
        items.addElement(null);
        startingPageRunOptions.setModel(items);
        startingPageRunOptions.setSelectedIndex(0);
    }

    private void activatePage(JPanel page) {
        setContentPane(page);
        revalidate();
        repaint(50);
    }

    private void postInitialize() {
        pack();
        setSize(1024, 768);
        setLocationRelativeTo(getDesktopWorkbench().getWindow());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    private void initialize() {
        setIconImage(UIUtils.getJIPipeIcon128());
        Path projectSavePath = getDesktopProjectWorkbench().getProjectWindow().getProjectSavePath();
        if (projectSavePath == null) {
            setTitle("JIPipe - New project - Run");
        } else {
            setTitle("JIPipe - " + projectSavePath + " - Run");
        }
        UIUtils.setToAskOnClose(this, "Do you really want to close this window?", "Close run setup", () -> shouldAskOnClose);
    }

    private void initializeStartingPage() {

        startingPage.setBorder(UIUtils.createEmptyBorder(8));

        // Warning about saving the project
        Path projectSavePath = getDesktopProjectWorkbench().getProjectWindow().getProjectSavePath();
        if (projectSavePath == null) {
            JIPipeDesktopMessagePanel messagePanel = new JIPipeDesktopMessagePanel();
            messagePanel.addMessage(JIPipeDesktopMessagePanel.MessageType.InfoLight, "We recommend to save the project before running it.", true, true);
            startingPage.add(messagePanel, BorderLayout.NORTH);
        }

        // Center panel
        startingPageRunOptions.setOpaque(false);
        startingPageRunOptions.setCellRenderer(new RunOptionListCellRenderer());
        startingPage.add(new JIPipeDesktopSplitPane(JIPipeDesktopSplitPane.LEFT_RIGHT,
                UIUtils.borderNorthCenter(UIUtils.createInfoLabel("Select the run configuration", "You can run the whole projects or only specific parts."),
                        new JScrollPane(startingPageRunOptions)),
                startingPageOptionsPanel,
                new JIPipeDesktopSplitPane.DynamicSidebarRatio(300, true)));
        startingPageRunOptions.addListSelectionListener(e -> onRunOptionSelectionChanged());


        // Options
        startingPageStorageModeSelection.addToggleButton("Custom", "Results are exported via exporter nodes", UIUtils.getIcon32FromResources("actions/document-export.png"), StartPageStorageMode.Discard);
        startingPageStorageModeSelection.addToggleButton("Cache", "Results are stored in memory", UIUtils.getIcon32FromResources("actions/database.png"), StartPageStorageMode.Cache);
        startingPageStorageModeSelection.addToggleButton("Filesystem", "Results are stored on the hard drive (in JIPipe format)", UIUtils.getIcon32FromResources("devices/drive-harddisk.png"), StartPageStorageMode.Filesystem);

        startingPageIntermediateResultsModeSelection.addToggleButton("Discard", "Intermediate results are discarded", UIUtils.getIcon32FromResources("actions/edit-delete-remove.png"), false);
        startingPageIntermediateResultsModeSelection.addToggleButton("Keep", "Intermediate results are also saved", UIUtils.getIcon32FromResources("actions/filesave.png"), true);

        if (projectHasExporterNodes()) {
            startingPageStorageModeSelection.setSelectedValue(StartPageStorageMode.Discard);
        } else {
            startingPageStorageModeSelection.setSelectedValue(StartPageStorageMode.Cache);
        }
        startingPageIntermediateResultsModeSelection.setSelectedValue(false);

        // Create the button panel
        JPanel buttonPanel = UIUtils.boxHorizontal(
                UIUtils.setFontSize(UIUtils.createButton("Cancel", UIUtils.getIconFromResources("actions/dialog-cancel.png"), () -> setVisible(false)), 16),
                Box.createHorizontalGlue(),
                UIUtils.setFontSize(UIUtils.makeButtonHighlightedSuccess(UIUtils.createButton("Next", UIUtils.getIconFromResources("actions/check.png"), () -> confirmStartingPage())), 16)
        );
        startingPage.add(buttonPanel, BorderLayout.SOUTH);

    }

    private boolean projectHasExporterNodes() {
        for (JIPipeGraphNode graphNode : getProject().getGraph().getGraphNodes()) {
            if (graphNode.getInfo().getCategory() instanceof ExportNodeTypeCategory) {
                return true;
            }
        }
        return false;
    }

    private void confirmStartingPage() {
        Object selectedValue = startingPageRunOptions.getSelectedValue();
        if (selectedValue instanceof JIPipeProjectRunSet || selectedValue instanceof JIPipeProjectCompartmentOutput) {
            if (selectedValue instanceof JIPipeProjectRunSet) {
                nodes = JIPipeUtils.filterAlgorithmsList(((JIPipeProjectRunSet) selectedValue).resolveNodes(getProject()));
            } else if (selectedValue instanceof JIPipeProjectCompartmentOutput) {
                nodes = Collections.singletonList((JIPipeAlgorithm) selectedValue);
            }

            run(
                    startingPageStorageModeSelection.getSelectedValue() == StartPageStorageMode.Cache,
                    startingPageStorageModeSelection.getSelectedValue() == StartPageStorageMode.Filesystem,
                    startingPageIntermediateResultsModeSelection.getSelectedValue(),
                    false
            );

        } else {
            // Show ask on close now
            shouldAskOnClose = true;

            // Switch to a full run setup dialog
            JIPipeDesktopCustomRunSettingsUI ui = new JIPipeDesktopCustomRunSettingsUI(workbench);
            activatePage(ui);
        }
    }

    private void onRunOptionSelectionChanged() {
        Object selectedValue = startingPageRunOptions.getSelectedValue();
        startingPageOptionsPanel.clear();

        if (selectedValue instanceof JIPipeProjectRunSet || selectedValue instanceof JIPipeProjectCompartmentOutput) {
            if (selectedValue instanceof JIPipeProjectRunSet) {
                // General Description
                addDescriptionPanelToStartingOptionsPanel("The selected option will run a set of predefined nodes (run set)", "Run predefined set of nodes");

                // Description if available
                if (!((JIPipeProjectRunSet) selectedValue).getDescription().isEmpty()) {
                    addDescriptionPanelToStartingOptionsPanel(((JIPipeProjectRunSet) selectedValue).getDescription().getHtml(), "Description");
                }
            } else if (selectedValue instanceof JIPipeProjectCompartmentOutput) {
                // General Description
                addDescriptionPanelToStartingOptionsPanel("The selected option will the output of a project compartment.", "Run compartment output");

                // Description if available
                if (!((JIPipeProjectCompartmentOutput) selectedValue).getCustomDescription().isEmpty()) {
                    addDescriptionPanelToStartingOptionsPanel(((JIPipeProjectCompartmentOutput) selectedValue).getCustomDescription().getHtml(), "Description");
                }
            }

            // Add options
            startingPageStorageModeSelection.setBorder(UIUtils.createEmptyBorder(8));
            addPanelToStartingPageOptionsPanel(UIUtils.getIcon32FromResources("actions/filesave.png"), "Select where to automatically store the results", startingPageStorageModeSelection);

            startingPageIntermediateResultsModeSelection.setBorder(UIUtils.createEmptyBorder(8));
            if (startingPageStorageModeSelection.getSelectedValue() != StartPageStorageMode.Discard) {
                addPanelToStartingPageOptionsPanel(UIUtils.getIcon32FromResources("actions/document-save-all.png"), "Select how to handle intermediate results", startingPageIntermediateResultsModeSelection);
            }

            String outputPathTitle;
            switch (startingPageStorageModeSelection.getSelectedValue()) {
                case Discard:
                case Cache:
                    outputPathTitle = "(Optional) Change the work directory for the run";
                    break;
                default:
                    outputPathTitle = "Select the output directory";
                    break;
            }
            addPanelToStartingPageOptionsPanel(UIUtils.getIcon32FromResources("actions/document-open-folder.png"), outputPathTitle, UIUtils.wrapInEmptyBorder(startingPageOutputPath, 8));

        } else {
            // Description
            addDescriptionPanelToStartingOptionsPanel("This option allows you to run the whole pipeline and change a variety of additional settings regarding where and how results are stored and which nodes should be executed. " +
                    "If you are used to older JIPipe versions, this opens the same 'Run' interface you are already familiar with.", "Whole project/Customize");
        }
    }

    private void addDescriptionPanelToStartingOptionsPanel(String text, String title) {
        JTextPane descriptionReader = new JTextPane();
        descriptionReader.setContentType("text/html");
        descriptionReader.setEditorKit(new JIPipeDesktopHTMLEditorKit());
        descriptionReader.setEditable(false);
        descriptionReader.setText(text);
        UIUtils.registerHyperlinkHandler(descriptionReader);
        descriptionReader.setBorder(UIUtils.createEmptyBorder(8));
        addPanelToStartingPageOptionsPanel(
                UIUtils.getIcon32FromResources("status/messagebox_info.png"),
                title,
                descriptionReader
        );
    }

    private void addPanelToStartingPageOptionsPanel(Icon icon, String title, Component center, Component... titleBarComponents) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(16, 16, 16, 16),
                new RoundedLineBorder(UIUtils.getControlBorderColor(), 1, 4)
        ));

        JLabel titleLabel = new JLabel(title, icon, JLabel.LEFT);
        titleLabel.setBorder(UIUtils.createEmptyBorder(8));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16));

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(titleLabel);
        toolBar.add(Box.createHorizontalGlue());
        for (Component titleBarComponent : titleBarComponents) {
            toolBar.add(titleBarComponent);
        }
        toolBar.add(Box.createHorizontalStrut(8));
        toolBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIUtils.getControlBorderColor()));
        toolBar.setBackground(ColorUtils.mix(JIPipeDesktopModernMetalTheme.PRIMARY5, ColorUtils.scaleHSV(UIManager.getColor("Panel.background"), 1, 1, 0.98f), 0.92));

        panel.add(toolBar, BorderLayout.NORTH);
        panel.add(center, BorderLayout.CENTER);

        startingPageOptionsPanel.addWideToForm(panel);
    }

    @Override
    public JIPipeDesktopWorkbench getDesktopWorkbench() {
        return workbench;
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }

    public void run(boolean saveToCache, boolean saveToDisk, boolean storeIntermediateResults, boolean excludeSelected) {
        // Validation step
        JIPipeValidationReport report = new JIPipeValidationReport();
        createValidationReport(report);
        if (!report.isEmpty()) {
            UIUtils.showValidityReportDialog(workbench, this, report, "Unable to run workflow", "JIPipe detected issues with the workflow and cannot run it", true);
            return;
        }

        // Show ask on close now
        shouldAskOnClose = true;

        // Create an enqueue the run
        run = createRun(saveToCache, saveToDisk, storeIntermediateResults, excludeSelected);

        JIPipeDesktopRunExecuteUI executeUI = new JIPipeDesktopRunExecuteUI(workbench, run);
        activatePage(executeUI);

        JIPipeRunnableQueue.getInstance().enqueue(run);
    }

    private JIPipeDesktopQuickRun createRun(boolean saveToCache, boolean saveToDisk, boolean storeIntermediateResults, boolean excludeSelected) {

        // Generate settings
        JIPipeDesktopQuickRunSettings settings = new JIPipeDesktopQuickRunSettings(getProject());
        settings.setSaveToDisk(saveToDisk);
        settings.setExcludeSelected(excludeSelected);
        settings.setLoadFromCache(true);
        settings.setStoreToCache(saveToCache);
        settings.setStoreIntermediateResults(storeIntermediateResults);

        // Run
        JIPipeDesktopQuickRun run = new JIPipeDesktopQuickRun(getProject(), new ArrayList<>(nodes), settings);
        JIPipeRuntimeApplicationSettings.getInstance().setDefaultQuickRunThreads(settings.getNumThreads());
        return run;
    }

    private void createValidationReport(JIPipeValidationReport report) {
        for (JIPipeAlgorithm node : nodes) {
            node.reportValidity(new UnspecifiedValidationReportContext(), report);
        }
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if(event.getRun() == run) {
            if(run.getSettings().isSaveToDisk()) {
                // Ensure ask on close
                shouldAskOnClose = true;

                JIPipeDesktopResultUI resultUI = new JIPipeDesktopResultUI(getDesktopProjectWorkbench(), run.getProject(), run.getSettings().getOutputPath());
                activatePage(resultUI);
            }
            else {
                // Can close safely
                shouldAskOnClose = false;

                JPanel panel = new JPanel(new BorderLayout());
                panel.add(UIUtils.createInfoLabel("Workflow finished", "The run finished successfully. You can now close this window.", UIUtils.getIcon64FromResources("check-circle-green.png")), BorderLayout.CENTER);
                JPanel buttonPanel = UIUtils.boxHorizontal(
                        Box.createHorizontalGlue(),
                        UIUtils.setFontSize(UIUtils.makeButtonHighlightedSuccess(UIUtils.createButton("Close", UIUtils.getIconFromResources("actions/message-close.png"), () -> setVisible(false))), 16)
                );
                panel.add(buttonPanel, BorderLayout.SOUTH);

                activatePage(panel);
            }
        }
    }

    @Override
    public void onRunnableInterrupted(JIPipeRunnable.InterruptedEvent event) {

    }

    public enum StartPageStorageMode {
        Discard,
        Cache,
        Filesystem
    }

    public static class RunOptionListCellRenderer extends JPanel implements ListCellRenderer<Object> {

        private final SolidColorIcon colorIcon = new SolidColorIcon(8, 32);
        private final JLabel nameLabel = new JLabel();

        public RunOptionListCellRenderer() {
            setLayout(new BorderLayout(16, 0));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(4, 4, 4, 4),
                    new RoundedLineBorder(UIUtils.getControlBorderColor(), 1, 4)
            ));

            add(new JLabel(colorIcon), BorderLayout.WEST);
            add(nameLabel, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Color color = null;
            if (value instanceof JIPipeProjectRunSet) {
                nameLabel.setIcon(UIUtils.getIconFromResources("actions/debug-run.png"));
                nameLabel.setText(((JIPipeProjectRunSet) value).getDisplayName());
            } else if (value instanceof JIPipeProjectCompartmentOutput) {
                nameLabel.setIcon(UIUtils.getIconFromResources("actions/graph-compartment.png"));
                nameLabel.setText(((JIPipeProjectCompartmentOutput) value).getDisplayName());
            } else {
                nameLabel.setIcon(UIUtils.getIconFromResources("actions/configure.png"));
                nameLabel.setText("Whole project/Customize");
            }

            if (color != null) {
                colorIcon.setFillColor(color);
                colorIcon.setBorderColor(UIUtils.getControlBorderColor());
            } else {
                colorIcon.setFillColor(UIManager.getColor("Panel.background"));
                colorIcon.setBorderColor(UIUtils.getControlBorderColor());
            }

            if (isSelected) {
                setBorder(UIUtils.createControlBorder(UIUtils.COLOR_SUCCESS));
            } else {
                setBorder(UIUtils.createControlBorder());
            }
            return this;
        }
    }
}
