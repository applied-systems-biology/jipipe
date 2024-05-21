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

package org.hkijena.jipipe.desktop.app.grapheditor.pipeline.properties;

import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.run.JIPipeGraphRun;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableLogEntry;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.quickrun.JIPipeDesktopQuickRun;
import org.hkijena.jipipe.desktop.app.quickrun.JIPipeDesktopQuickRunSettings;
import org.hkijena.jipipe.desktop.app.resultanalysis.JIPipeDesktopResultUI;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopLogViewer;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunExecuteUI;
import org.hkijena.jipipe.desktop.commons.components.*;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.settings.JIPipeRuntimeApplicationSettings;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Consumer;

/**
 * UI for generating {@link JIPipeDesktopQuickRun}
 */
public class JIPipeDesktopQuickRunSetupUI extends JIPipeDesktopProjectWorkbenchPanel implements JIPipeRunnable.InterruptedEventListener, JIPipeRunnable.FinishedEventListener {

    private final JIPipeGraphNode algorithm;
    boolean showNextResults;
    private JPanel setupPanel;
    private JPanel selectionPanel;
    private JPanel validationReportPanel;
    private JIPipeDesktopValidityReportUI validationReportUI;
    private JIPipeDesktopQuickRunSettings currentSettings;
    private JIPipeDesktopQuickRun currentQuickRun;
    private Consumer<JIPipeDesktopQuickRun> nextRunOnSuccess;

    /**
     * @param workbenchUI the workbench
     * @param algorithm   the target algorithm
     */
    public JIPipeDesktopQuickRunSetupUI(JIPipeDesktopProjectWorkbench workbenchUI, JIPipeGraphNode algorithm) {
        super(workbenchUI);
        this.algorithm = algorithm;

        setLayout(new BorderLayout());
        this.validationReportUI = new JIPipeDesktopValidityReportUI(workbenchUI, false);

        initializeValidationReportUI();
        initializeSelectionPanel();
        initializeSetupPanel();

        tryShowSelectionPanel();

        JIPipeRunnableQueue.getInstance().getInterruptedEventEmitter().subscribeWeak(this);
        JIPipeRunnableQueue.getInstance().getFinishedEventEmitter().subscribeWeak(this);
    }

    private void initializeSelectionPanel() {
        selectionPanel = new JPanel(new BorderLayout());
        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.WITH_SCROLLING);

        formPanel.addGroupHeader("Store to cache", "The following operations will store all results to the memory cache. " +
                "Please note that for larger amounts of data, you might run into memory limitations.", UIUtils.getIconFromResources("actions/database.png"));
        addSelectionPanelItem(formPanel,
                "Update cache",
                UIUtils.getIconFromResources("actions/database.png"),
                "Runs the pipeline up until this algorithm and caches the results. Nothing is written to disk.",
                () -> updateCache(false, false),
                true);
        addSelectionPanelItem(formPanel,
                "Cache intermediate results",
                UIUtils.getIconFromResources("actions/cache-intermediate-results.png"),
                "This runs the current node and all dependencies. Results and intermediate results are stored into the memory cache.  Already cached intermediate results are used if possible.",
                () -> updateCache(true, false),
                true);
        addSelectionPanelItem(formPanel,
                "Update predecessor caches",
                UIUtils.getIconFromResources("actions/cache-predecessors.png"),
                "Runs the pipeline up until the predecessors of the selected node. Nothing is written to disk.",
                () -> updateCache(true, true),
                false);

        formPanel.addGroupHeader("Store to disk", "The following operations will store all results to the hard drive.", UIUtils.getIconFromResources("devices/drive-harddisk.png"));

        addSelectionPanelItem(formPanel,
                "Run & Show results",
                UIUtils.getIconFromResources("actions/play.png"),
                "Runs the pipeline up until this algorithm and shows the results. " +
                        "The results will be stored on the hard drive.",
                () -> quickRun(false),
                false);

        addSelectionPanelItem(formPanel,
                "Run & Show intermediate results",
                UIUtils.getIconFromResources("actions/rabbitvcs-update.png"),
                "Runs the pipeline up until this algorithm and shows the results (including intermediate results). " +
                        "The results will be stored on the hard drive.",
                () -> quickRun(true), false);

        formPanel.addGroupHeader("Miscellaneous", UIUtils.getIconFromResources("actions/configure.png"));
        addSelectionPanelItem(formPanel,
                "Custom run",
                UIUtils.getIconFromResources("actions/configure.png"),
                "Setup a run with custom settings.",
                this::tryShowSetupPanel,
                false);

        formPanel.addVerticalGlue();

        selectionPanel.add(formPanel, BorderLayout.CENTER);
    }

    private void quickRun(boolean storeIntermediates) {
        if (validateOrShowError()) {
            currentSettings = new JIPipeDesktopQuickRunSettings();
            currentSettings.setSaveToDisk(true);
            currentSettings.setExcludeSelected(false);
            currentSettings.setLoadFromCache(true);
            currentSettings.setStoreToCache(false);
            currentSettings.setStoreIntermediateResults(storeIntermediates);
            generateQuickRun(true);
        }
    }

    private void addSelectionPanelItem(JIPipeDesktopFormPanel formPanel, String name, ImageIcon icon, String description, Runnable action, boolean suggested) {
        JTextArea descriptionArea = UIUtils.createReadonlyBorderlessTextArea(description);
        descriptionArea.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JButton actionButton = new JButton(name, icon);
        actionButton.addActionListener(e -> action.run());
        if (suggested) {
            UIUtils.makeButtonHighlightedSuccess(actionButton);
        }

        formPanel.addToForm(descriptionArea, actionButton);

    }

    private void updateCache(boolean cacheIntermediateResults, boolean excludeSelected) {
        if (validateOrShowError()) {
            currentSettings = new JIPipeDesktopQuickRunSettings();
            currentSettings.setSaveToDisk(false);
            currentSettings.setExcludeSelected(excludeSelected);
            currentSettings.setLoadFromCache(true);
            currentSettings.setStoreToCache(true);
            currentSettings.setStoreIntermediateResults(cacheIntermediateResults);
            generateQuickRun(false);
        }
    }

    /**
     * Attempts to setup and run the quick run automatically and run a function when finished
     *
     * @param showResults show results after successful run
     * @param settings    settings
     * @param onSuccess   called if successful
     * @return if the initial validation failed
     */
    public boolean tryAutoRun(boolean showResults, JIPipeDesktopQuickRunSettings settings, Consumer<JIPipeDesktopQuickRun> onSuccess) {
        if (!validateOrShowError())
            return false;
        currentSettings = settings;
        nextRunOnSuccess = onSuccess;
        generateQuickRun(showResults);
        return true;
    }

    private void initializeValidationReportUI() {
        validationReportPanel = new JPanel();
        validationReportPanel.setLayout(new BorderLayout());
        validationReportUI = new JIPipeDesktopValidityReportUI(getDesktopWorkbench(), false);
        JIPipeDesktopDocumentedComponent pane = new JIPipeDesktopDocumentedComponent(true,
                MarkdownText.fromPluginResource("documentation/testbench.md", new HashMap<>()),
                validationReportUI);
        validationReportPanel.add(pane, BorderLayout.CENTER);

        JButton refreshButton = new JButton("Retry", UIUtils.getIconFromResources("actions/view-refresh.png"));
        refreshButton.addActionListener(e -> tryShowSelectionPanel());
        validationReportUI.getErrorToolbar().add(refreshButton);
    }

    private void initializeSetupPanel() {
        setupPanel = new JPanel();
        setupPanel.setLayout(new BorderLayout());

        currentSettings = new JIPipeDesktopQuickRunSettings();
        JIPipeDesktopParameterPanel formPanel = new JIPipeDesktopParameterPanel(getDesktopWorkbench(), currentSettings,
                MarkdownText.fromPluginResource("documentation/testbench.md", new HashMap<>()), JIPipeDesktopParameterPanel.WITH_SCROLLING |
                JIPipeDesktopParameterPanel.WITH_DOCUMENTATION | JIPipeDesktopParameterPanel.DOCUMENTATION_BELOW);
        setupPanel.add(formPanel, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton backButton = new JButton("Back", UIUtils.getIconFromResources("actions/back.png"));
        backButton.addActionListener(e -> tryShowSelectionPanel());
        toolBar.add(backButton);

        toolBar.add(Box.createHorizontalGlue());

        JButton runOnly = new JButton("Run", UIUtils.getIconFromResources("actions/run-build.png"));
        runOnly.addActionListener(e -> generateQuickRun(false));
        toolBar.add(runOnly);

        JButton runAndOpen = new JButton("Run & open results", UIUtils.getIconFromResources("actions/run-build.png"));
        runAndOpen.addActionListener(e -> generateQuickRun(true));
        toolBar.add(runAndOpen);

        setupPanel.add(toolBar, BorderLayout.NORTH);
    }

    private boolean validateOrShowError() {
        JIPipeValidationReport report = new JIPipeValidationReport();
        getProject().reportValidity(new UnspecifiedValidationReportContext(), report, algorithm);

        Set<JIPipeGraphNode> algorithmsWithMissingInput = getProject().getGraph().getDeactivatedNodes(true);
        if (algorithmsWithMissingInput.contains(algorithm)) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new GraphNodeValidationReportContext(algorithm),
                    "Selected node is deactivated or missing inputs!",
                    "The selected node would not be executed, as it is deactivated or missing input data. " +
                            "You have to ensure that all input slots are assigned for the selected algorithm and its dependencies.",
                    "Please check if the parameter 'Enabled' is checked. Please check if all input slots are assigned. Also check all dependency algorithms.",
                    JsonUtils.toPrettyJsonString(algorithm)));
        }
        if (report.isEmpty())
            return true;

        // Replace by error UI
        removeAll();
        add(validationReportPanel, BorderLayout.CENTER);
        validationReportUI.setReport(report);
        revalidate();
        repaint();
        return false;
    }

    private void tryShowSetupPanel() {
        if (validateOrShowError()) {
            removeAll();
            add(setupPanel, BorderLayout.CENTER);
            revalidate();
            repaint();
        }
    }

    private void tryShowSelectionPanel() {
        if (validateOrShowError()) {
            removeAll();
            add(selectionPanel, BorderLayout.CENTER);
            revalidate();
            repaint();
        }
    }

    private void openError(Throwable exception) {
        removeAll();

        JIPipeDesktopUserFriendlyErrorUI errorUI = new JIPipeDesktopUserFriendlyErrorUI(getDesktopWorkbench(), null, JIPipeDesktopUserFriendlyErrorUI.WITH_SCROLLING);
        errorUI.displayErrors(exception);
        errorUI.addVerticalGlue();

        JPanel errorPanel = new JPanel(new BorderLayout());
        errorPanel.add(errorUI, BorderLayout.CENTER);

        JButton openLogButton = new JButton("Open log", UIUtils.getIconFromResources("actions/show_log.png"));
        JPopupMenu openLogMenu = UIUtils.addPopupMenuToButton(openLogButton);

        JMenuItem openLogInJIPipeItem = new JMenuItem("Open log in JIPipe", UIUtils.getIconFromResources("apps/jipipe.png"));
        openLogInJIPipeItem.addActionListener(e -> openLogInJIPipe());
        openLogMenu.add(openLogInJIPipeItem);

        JMenuItem openLogInExternalEditorItem = new JMenuItem("Open log in external editor", UIUtils.getIconFromResources("actions/document-open-folder.png"));
        openLogInExternalEditorItem.addActionListener(e -> openLogInExternalEditor());
        openLogMenu.add(openLogInExternalEditorItem);

        errorUI.getToolBar().add(openLogButton);

        JButton refreshButton = new JButton("Retry", UIUtils.getIconFromResources("actions/view-refresh.png"));
        refreshButton.addActionListener(e -> tryShowSelectionPanel());
        errorUI.getToolBar().add(refreshButton);

        add(errorPanel, BorderLayout.CENTER);

        revalidate();
    }

    private void openLogInExternalEditor() {
        if (currentQuickRun != null) {
            Path tempFile = JIPipeRuntimeApplicationSettings.generateTempFile("log", ".txt");
            try {
                Files.write(tempFile, currentQuickRun.getProgressInfo().getLog().toString().getBytes(StandardCharsets.UTF_8));
                Desktop.getDesktop().open(tempFile.toFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            JOptionPane.showMessageDialog(this, "The log is unavailable for this run.", "Open log", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openLogInJIPipe() {
        if (currentQuickRun != null) {
            JIPipeDesktopTabPane.DocumentTab tab = getDesktopProjectWorkbench().getDocumentTabPane().selectSingletonTab(JIPipeDesktopProjectWorkbench.TAB_LOG);
            JIPipeDesktopLogViewer viewer = (JIPipeDesktopLogViewer) tab.getContent();
            viewer.showLog(new JIPipeRunnableLogEntry("Run", LocalDateTime.now(), currentQuickRun.getProgressInfo().getLog().toString(), currentQuickRun.getProgressInfo().getNotifications(), true));
        } else {
            JOptionPane.showMessageDialog(this, "The log is unavailable for this run.", "Open log", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void generateQuickRun(boolean showResults) {

        JIPipeValidationReport report = new JIPipeValidationReport();
        getProject().reportValidity(new UnspecifiedValidationReportContext(), report, algorithm);
        if (!report.isEmpty()) {
            tryShowSelectionPanel();
            return;
        }

        currentQuickRun = new JIPipeDesktopQuickRun(getProject(), algorithm, currentSettings);
        JIPipeRuntimeApplicationSettings.getInstance().setDefaultQuickRunThreads(currentSettings.getNumThreads());
        showNextResults = showResults;

        removeAll();
        JIPipeDesktopRunExecuteUI executerUI = new JIPipeDesktopRunExecuteUI(getDesktopWorkbench(), currentQuickRun);
        add(executerUI, BorderLayout.CENTER);
        revalidate();
        repaint();
        executerUI.startRun();
    }

    /**
     * Triggered when a worker is finished
     *
     * @param event Generated event
     */
    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if (event.getRun() == currentQuickRun) {
            tryShowSelectionPanel();

            if (showNextResults) {
                try {
                    JIPipeGraphRun run = currentQuickRun.getRun();
                    JIPipeDesktopResultUI resultUI = new JIPipeDesktopResultUI(getDesktopProjectWorkbench(), run.getProject(), run.getConfiguration().getOutputPath());
                    String name = "Run: " + algorithm.getName();
                    getDesktopProjectWorkbench().getDocumentTabPane().addTab(name, UIUtils.getIconFromResources("actions/testbench.png"),
                            resultUI, JIPipeDesktopTabPane.CloseMode.withAskOnCloseButton, true);
                    getDesktopProjectWorkbench().getDocumentTabPane().switchToLastTab();
                    currentQuickRun = null;
                } catch (Exception e) {
                    openError(e);
                }
            } else {
                if (nextRunOnSuccess != null) {
                    nextRunOnSuccess.accept(currentQuickRun);
                    nextRunOnSuccess = null;
                }
                currentQuickRun = null;
            }
        }
    }

    /**
     * Triggered when a worker is interrupted
     *
     * @param event Generated event
     */
    @Override
    public void onRunnableInterrupted(JIPipeRunnable.InterruptedEvent event) {
        if (event.getRun() == currentQuickRun) {
            openError(event.getException());
        }
    }
}
