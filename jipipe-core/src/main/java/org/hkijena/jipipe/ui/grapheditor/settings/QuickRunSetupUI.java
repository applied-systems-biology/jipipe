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

package org.hkijena.jipipe.ui.grapheditor.settings;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.components.DocumentedComponent;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.JIPipeValidityReportUI;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.UserFriendlyErrorUI;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.ui.quickrun.QuickRun;
import org.hkijena.jipipe.ui.quickrun.QuickRunSettings;
import org.hkijena.jipipe.ui.quickrun.QuickRunSetupWindow;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeResultUI;
import org.hkijena.jipipe.ui.running.JIPipeLogViewer;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.ui.running.RunUIWorkerFinishedEvent;
import org.hkijena.jipipe.ui.running.RunUIWorkerInterruptedEvent;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Consumer;

/**
 * UI for generating {@link QuickRun}
 */
public class QuickRunSetupUI extends JIPipeProjectWorkbenchPanel {

    private final JIPipeGraphNode algorithm;
    boolean showNextResults;
    private JPanel setupPanel;
    private JPanel selectionPanel;
    private JPanel validationReportPanel;
    private JIPipeValidityReportUI validationReportUI;
    private QuickRunSettings currentSettings;
    private QuickRun currentQuickRun;
    private Consumer<QuickRun> nextRunOnSuccess;

    /**
     * @param workbenchUI the workbench
     * @param algorithm   the target algorithm
     */
    public QuickRunSetupUI(JIPipeProjectWorkbench workbenchUI, JIPipeGraphNode algorithm) {
        super(workbenchUI);
        this.algorithm = algorithm;

        setLayout(new BorderLayout());
        this.validationReportUI = new JIPipeValidityReportUI(false);

        initializeValidationReportUI();
        initializeSelectionPanel();
        initializeSetupPanel();

        tryShowSelectionPanel();

        JIPipeRunnerQueue.getInstance().getEventBus().register(this);
    }

    private void initializeSelectionPanel() {
        selectionPanel = new JPanel(new BorderLayout());
        FormPanel formPanel = new FormPanel(null, FormPanel.WITH_SCROLLING);

        addSelectionPanelItem(formPanel,
                "Update cache",
                UIUtils.getIconFromResources("actions/database.png"),
                "This runs the current node and all dependencies. Results are stored into the memory cache. Intermediate results are discarded. Already cached intermediate results are used if possible.",
                () -> updateCache(false));
        addSelectionPanelItem(formPanel,
                "Cache intermediate results",
                UIUtils.getIconFromResources("actions/cache-intermediate-results.png"),
                "This runs the current node and all dependencies. Results and intermediate results are stored into the memory cache.  Already cached intermediate results are used if possible.",
                () -> updateCache(true));
        addSelectionPanelItem(formPanel,
                "Quick run",
                UIUtils.getIconFromResources("actions/player_start.png"),
                "This runs the current node and all dependencies. Results are saved to the hard drive into a temporary directory. After the run, a tab opens where you can review the results.",
                this::quickRun);
        addSelectionPanelItem(formPanel,
                "Custom quick run",
                UIUtils.getIconFromResources("actions/configure.png"),
                "Allows to customize all aspects of a quick run, including the output path an whether to utilize already cached results.",
                this::tryShowSetupPanel);

        formPanel.addVerticalGlue();

        selectionPanel.add(formPanel, BorderLayout.CENTER);
    }

    private void quickRun() {
        if (validateOrShowError()) {
            currentSettings = new QuickRunSettings();
            currentSettings.setSaveToDisk(true);
            currentSettings.setExcludeSelected(false);
            currentSettings.setLoadFromCache(true);
            currentSettings.setStoreToCache(false);
            currentSettings.setStoreIntermediateResults(true);
            generateQuickRun(true, true);
        }
    }

    private void addSelectionPanelItem(FormPanel formPanel, String name, ImageIcon icon, String description, Runnable action) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1, true));

        JLabel nameLabel = new JLabel(name);
        nameLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        nameLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
        panel.add(nameLabel, BorderLayout.NORTH);

        JTextArea descriptionArea = UIUtils.makeReadonlyBorderlessTextArea(description);
        descriptionArea.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        panel.add(descriptionArea, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new BorderLayout(4, 4));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        JButton actionButton = new JButton(name, icon);
        actionButton.setPreferredSize(new Dimension(200, 32));
        actionButton.addActionListener(e -> action.run());
        buttonPanel.add(actionButton, BorderLayout.EAST);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        formPanel.addWideToForm(panel, null);
    }

    private void updateCache(boolean cacheIntermediateResults) {
        if (validateOrShowError()) {
            currentSettings = new QuickRunSettings();
            currentSettings.setSaveToDisk(false);
            currentSettings.setExcludeSelected(false);
            currentSettings.setLoadFromCache(true);
            currentSettings.setStoreToCache(true);
            currentSettings.setStoreIntermediateResults(cacheIntermediateResults);
            generateQuickRun(false, true);
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
    public boolean tryAutoRun(boolean showResults, QuickRunSettings settings, Consumer<QuickRun> onSuccess) {
        if (!validateOrShowError())
            return false;
        currentSettings = settings;
        nextRunOnSuccess = onSuccess;
        generateQuickRun(showResults, true);
        return true;
    }

    private void initializeValidationReportUI() {
        validationReportPanel = new JPanel();
        validationReportPanel.setLayout(new BorderLayout());
        validationReportUI = new JIPipeValidityReportUI(false);
        DocumentedComponent pane = new DocumentedComponent(true,
                MarkdownDocument.fromPluginResource("documentation/testbench.md", new HashMap<>()),
                validationReportUI);
        validationReportPanel.add(pane, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(Box.createHorizontalGlue());

        JButton refreshButton = new JButton("Retry", UIUtils.getIconFromResources("actions/view-refresh.png"));
        refreshButton.addActionListener(e -> tryShowSelectionPanel());
        toolBar.add(refreshButton);

        validationReportPanel.add(toolBar, BorderLayout.NORTH);
    }

    private void initializeSetupPanel() {
        setupPanel = new JPanel();
        setupPanel.setLayout(new BorderLayout());

        currentSettings = new QuickRunSettings();
        ParameterPanel formPanel = new ParameterPanel(getWorkbench(), currentSettings,
                MarkdownDocument.fromPluginResource("documentation/testbench.md", new HashMap<>()), ParameterPanel.WITH_SCROLLING |
                ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.DOCUMENTATION_BELOW);
        setupPanel.add(formPanel, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton backButton = new JButton("Back", UIUtils.getIconFromResources("actions/back.png"));
        backButton.addActionListener(e -> tryShowSelectionPanel());
        toolBar.add(backButton);

        toolBar.add(Box.createHorizontalGlue());

        JButton runOnly = new JButton("Run", UIUtils.getIconFromResources("actions/run-build.png"));
        runOnly.addActionListener(e -> generateQuickRun(false, false));
        toolBar.add(runOnly);

        JButton runAndOpen = new JButton("Run & open results", UIUtils.getIconFromResources("actions/run-build.png"));
        runAndOpen.addActionListener(e -> generateQuickRun(true, false));
        toolBar.add(runAndOpen);

        setupPanel.add(toolBar, BorderLayout.NORTH);
    }

    private boolean validateOrShowError() {
        JIPipeIssueReport report = new JIPipeIssueReport();
        getProject().reportValidity(report, algorithm);

        Set<JIPipeGraphNode> algorithmsWithMissingInput = getProject().getGraph().getDeactivatedAlgorithms(true);
        if (algorithmsWithMissingInput.contains(algorithm)) {
            report.resolve("Test Bench").reportIsInvalid(
                    "Selected algorithm is deactivated or missing inputs!",
                    "The selected algorithm would not be executed, as it is deactivated or missing input data. " +
                            "You have to ensure that all input slots are assigned for the selected algorithm and its dependencies.",
                    "Please check if the parameter 'Enabled' is checked. Please check if all input slots are assigned. Also check all dependency algorithms.",
                    algorithm
            );
        }
        if (report.isValid())
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

        UserFriendlyErrorUI errorUI = new UserFriendlyErrorUI(null, UserFriendlyErrorUI.WITH_SCROLLING);
        errorUI.displayErrors(exception);
        errorUI.addVerticalGlue();

        JPanel errorPanel = new JPanel(new BorderLayout());
        errorPanel.add(errorUI, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(Box.createHorizontalGlue());

        JButton openLogButton = new JButton("Open log", UIUtils.getIconFromResources("actions/show_log.png"));
        JPopupMenu openLogMenu = UIUtils.addPopupMenuToComponent(openLogButton);

        JMenuItem openLogInJIPipeItem = new JMenuItem("Open log in JIPipe", UIUtils.getIconFromResources("apps/jipipe.png"));
        openLogInJIPipeItem.addActionListener(e -> openLogInJIPipe());
        openLogMenu.add(openLogInJIPipeItem);

        JMenuItem openLogInExternalEditorItem = new JMenuItem("Open log in external editor", UIUtils.getIconFromResources("actions/document-open-folder.png"));
        openLogInExternalEditorItem.addActionListener(e -> openLogInExternalEditor());
        openLogMenu.add(openLogInExternalEditorItem);

        toolBar.add(openLogButton);

        JButton refreshButton = new JButton("Retry", UIUtils.getIconFromResources("actions/view-refresh.png"));
        refreshButton.addActionListener(e -> tryShowSelectionPanel());
        toolBar.add(refreshButton);
        errorPanel.add(toolBar, BorderLayout.NORTH);

        add(errorPanel, BorderLayout.CENTER);

        revalidate();
    }

    private void openLogInExternalEditor() {
        if (currentQuickRun != null) {
            Path tempFile = RuntimeSettings.generateTempFile("log", ".txt");
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
            DocumentTabPane.DocumentTab tab = getProjectWorkbench().getDocumentTabPane().selectSingletonTab(JIPipeProjectWorkbench.TAB_LOG);
            JIPipeLogViewer viewer = (JIPipeLogViewer) tab.getContent();
            viewer.showLog(currentQuickRun.getProgressInfo().getLog().toString());
        } else {
            JOptionPane.showMessageDialog(this, "The log is unavailable for this run.", "Open log", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void generateQuickRun(boolean showResults, boolean showSetupPanel) {

        JIPipeIssueReport report = new JIPipeIssueReport();
        getProject().reportValidity(report, algorithm);
        if (!report.isValid()) {
            tryShowSelectionPanel();
            return;
        }

        // Setup panel
        if (showSetupPanel && RuntimeSettings.getInstance().isShowQuickRunSetupWindow()) {
            QuickRunSetupWindow window = new QuickRunSetupWindow(getWorkbench());
            window.revalidate();
            window.repaint();
            window.setLocationRelativeTo(getProjectWorkbench().getWindow());
            window.setVisible(true);
            if (window.isCancelled()) {
                tryShowSelectionPanel();
                return;
            }
        }
        if (!showSetupPanel) {
            RuntimeSettings.getInstance().setShowQuickRunSetupWindow(false);
            RuntimeSettings.getInstance().triggerParameterChange("show-quick-run-setup-window");
        }

        currentQuickRun = new QuickRun(getProject(), algorithm, currentSettings);
        RuntimeSettings.getInstance().setDefaultQuickRunThreads(currentSettings.getNumThreads());
        showNextResults = showResults;

        removeAll();
        JIPipeRunExecuterUI executerUI = new JIPipeRunExecuterUI(currentQuickRun);
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
    @Subscribe
    public void onWorkerFinished(RunUIWorkerFinishedEvent event) {
        if (event.getRun() == currentQuickRun) {
            tryShowSelectionPanel();

            if (showNextResults) {
                try {
                    JIPipeResultUI resultUI = new JIPipeResultUI(getProjectWorkbench(), currentQuickRun.getRun());
                    String name = "Quick run: " + algorithm.getName();
                    getProjectWorkbench().getDocumentTabPane().addTab(name, UIUtils.getIconFromResources("actions/testbench.png"),
                            resultUI, DocumentTabPane.CloseMode.withAskOnCloseButton, true);
                    getProjectWorkbench().getDocumentTabPane().switchToLastTab();
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
    @Subscribe
    public void onWorkerInterrupted(RunUIWorkerInterruptedEvent event) {
        if (event.getRun() == currentQuickRun) {
            openError(event.getException());
        }
    }
}
