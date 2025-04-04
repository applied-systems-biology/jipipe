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

package org.hkijena.jipipe.plugins.forms.ui;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.CustomValidationReportContext;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.batchassistant.JIPipeDesktopDataBatchBrowserUI;
import org.hkijena.jipipe.desktop.app.datatable.JIPipeDesktopSimpleDataBatchTableUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopUserFriendlyErrorUI;
import org.hkijena.jipipe.desktop.commons.components.icons.SolidColorIcon;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.plugins.forms.FormsPlugin;
import org.hkijena.jipipe.plugins.forms.datatypes.FormData;
import org.hkijena.jipipe.plugins.forms.datatypes.ParameterFormData;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.utils.JIPipeDesktopSplitPane;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.List;

public class FormsDialog extends JFrame {
    private static final String TAB_ISSUES_DETECTED = "Issues detected";
    private final JIPipeDesktopWorkbench workbench;
    private final String tabAnnotation;
    private final List<JIPipeMultiIterationStep> iterationStepList;
    private final List<JIPipeDataSlot> iterationStepForms = new ArrayList<>();
    private final JIPipeDataSlot originalForms;
    private final JIPipeDesktopTabPane tabPane = new JIPipeDesktopTabPane(true, JIPipeDesktopTabPane.TabPlacement.Top);
    private final List<DataBatchStatus> iterationStepStatuses = new ArrayList<>();
    private final JLabel unvisitedLabel = new JLabel(new SolidColorIcon(16, 16, DataBatchStatusTableCellRenderer.getColorUnvisited()));
    private final JLabel visitedLabel = new JLabel(new SolidColorIcon(16, 16, DataBatchStatusTableCellRenderer.getColorVisited()));
    private final JLabel invalidLabel = new JLabel(new SolidColorIcon(16, 16, DataBatchStatusTableCellRenderer.getColorInvalid()));
    private final JToggleButton visitedButton = new JToggleButton("Reviewed", UIUtils.getIconFromResources("actions/eye.png"));
    private final MarkdownText documentation;
    private boolean cancelled = false;
    private JIPipeDesktopSimpleDataBatchTableUI iterationStepTableUI;
    private String lastTab = "";

    public FormsDialog(JIPipeDesktopWorkbench workbench, List<JIPipeMultiIterationStep> iterationStepList, JIPipeDataSlot originalForms, String tabAnnotation) {
        this.originalForms = originalForms;
        setIconImage(UIUtils.getJIPipeIcon128());
        this.workbench = workbench;
        this.iterationStepList = iterationStepList;
        this.tabAnnotation = tabAnnotation;
        this.documentation = MarkdownText.fromResourceURL(FormsPlugin.class.getResource("/org/hkijena/jipipe/plugins/forms/form-dialog-documentation.md"),
                true, new HashMap<>());

        JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
        // We need to make copies of the FormData objects, as they are mutable
        for (int i = 0; i < iterationStepList.size(); ++i) {
            JIPipeDataSlot copy = createFormsInstanceFor(i, progressInfo);
            iterationStepForms.add(copy);
            iterationStepStatuses.add(DataBatchStatus.Unvisited);
        }

        // Initialize UI
        initialize();
        gotoNextBatch();
    }

    @Override
    public void dispose() {
        super.dispose();
        iterationStepTableUI.dispose();
    }

    private JIPipeDataSlot createFormsInstanceFor(int index, JIPipeProgressInfo progressInfo) {
        JIPipeDataSlot copy = originalForms.getInfo().createInstance(originalForms.getNode());
        for (int row = 0; row < originalForms.getRowCount(); row++) {
            FormData formCopy = (FormData) originalForms.getData(row, FormData.class, progressInfo).duplicate(progressInfo);
            formCopy.loadData(iterationStepList.get(index));
            copy.addData(formCopy,
                    originalForms.getTextAnnotations(row),
                    JIPipeTextAnnotationMergeMode.OverwriteExisting,
                    originalForms.getDataAnnotations(row),
                    JIPipeDataAnnotationMergeMode.OverwriteExisting,
                    originalForms.getDataContext(row),
                    progressInfo);
        }
        return copy;
    }

    private void gotoNextBatch() {
        if (!checkCurrentBatch())
            return;
        if (iterationStepList.size() > 0) {
            iterationStepTableUI.resetSearch();
            JXTable table = iterationStepTableUI.getTable();
            int row = table.getSelectedRow();
            if (row == -1) {
                table.getSelectionModel().setSelectionInterval(0, 0);
            } else {
                row = table.convertRowIndexToModel(row);
                row = (row + 1) % iterationStepList.size();
                row = table.convertRowIndexToView(row);
                table.getSelectionModel().setSelectionInterval(row, row);
            }
        }
    }

    private boolean checkCurrentBatch() {
        JXTable table = iterationStepTableUI.getTable();
        int row = table.getSelectedRow();
        if (row == -1) {
            return true;
        }
        JIPipeProgressInfo info = new JIPipeProgressInfo();
        JIPipeValidationReport report = getReportForDataBatch(row, info);
        if (!report.isValid()) {
            int result = JOptionPane.showOptionDialog(this,
                    "The current batch has settings that are not fully valid. Do you want to continue, anyway?",
                    "Issues found",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new Object[]{"Yes", "No", "Show errors"},
                    "Show errors");
            if (result == JOptionPane.YES_OPTION)
                return true;
            else if (result == JOptionPane.NO_OPTION)
                return false;
            else {
                tabPane.closeAllTabs(true);
                lastTab = TAB_ISSUES_DETECTED;
                switchToDataBatchUI(row);
                return false;
            }
        }
        return true;
    }

    private void gotoPreviousBatch() {
        if (!checkCurrentBatch())
            return;
        if (iterationStepList.size() > 0) {
            iterationStepTableUI.resetSearch();
            JXTable table = iterationStepTableUI.getTable();
            int row = table.getSelectedRow();
            if (row == -1) {
                table.getSelectionModel().setSelectionInterval(0, 0);
            } else {
                row = table.convertRowIndexToModel(row);
                --row;
                if (row < 0)
                    row = iterationStepList.size() - 1;
                row = table.convertRowIndexToView(row);
                table.getSelectionModel().setSelectionInterval(row, row);
            }
        }
    }

    private void initialize() {
        JPanel contentPanel = new JPanel(new BorderLayout());

        iterationStepTableUI = new JIPipeDesktopSimpleDataBatchTableUI(iterationStepList);
        iterationStepTableUI.getTable().setDefaultRenderer(Integer.class, new DataBatchStatusTableCellRenderer(iterationStepStatuses));
        iterationStepTableUI.getTable().setDefaultRenderer(String.class, new DataBatchStatusTableCellRenderer(iterationStepStatuses));
        JSplitPane splitPane = new JIPipeDesktopSplitPane(JSplitPane.HORIZONTAL_SPLIT, iterationStepTableUI, tabPane, JIPipeDesktopSplitPane.RATIO_1_TO_3);
        contentPanel.add(splitPane, BorderLayout.CENTER);

        initializeBottomBar(contentPanel);

        setContentPane(contentPanel);
        // Catch the closing event
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancelDialog();
            }
        });

        iterationStepTableUI.getTable().getSelectionModel().addListSelectionListener(e -> {
            int selectedRow = iterationStepTableUI.getTable().getSelectedRow();
            closeAllTabsAndRememberLast();
            if (selectedRow != -1) {
                selectedRow = iterationStepTableUI.getTable().convertRowIndexToModel(selectedRow);
                switchToDataBatchUI(selectedRow);
            }
        });
    }

    private void closeAllTabsAndRememberLast() {
        if (tabPane.getTabCount() > 0 && tabPane.getCurrentContent() != null) {
            lastTab = tabPane.getTabContainingContent(tabPane.getCurrentContent()).getTitle();
        }
        tabPane.closeAllTabs(true);
    }

    private void switchToDataBatchUI(int selectedRow) {

        boolean wasVisitedBefore = iterationStepStatuses.get(selectedRow) != DataBatchStatus.Unvisited;
        visitedButton.setSelected(true);

        updateVisitedStatuses();

        // Set the status
        iterationStepStatuses.set(selectedRow, DataBatchStatus.Visited);
        iterationStepTableUI.getTable().repaint();
        updateBottomBarStats();

        // Create preview tab
        tabPane.addTab("View data",
                UIUtils.getIconFromResources("actions/zoom.png"),
                new JIPipeDesktopDataBatchBrowserUI(getWorkbench(), iterationStepList.get(selectedRow)),
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton,
                false);

        // If invalid, create report
        if (wasVisitedBefore) {
            JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
            JIPipeValidationReport report = getReportForDataBatch(selectedRow, progressInfo);
            if (!report.isValid()) {
                JIPipeDesktopUserFriendlyErrorUI errorUI = new JIPipeDesktopUserFriendlyErrorUI(workbench, null, JIPipeDesktopUserFriendlyErrorUI.WITH_SCROLLING);
                errorUI.displayErrors(report);
                errorUI.addVerticalGlue();
                tabPane.addTab(TAB_ISSUES_DETECTED,
                        UIUtils.getIconFromResources("actions/dialog-warning.png"),
                        errorUI,
                        JIPipeDesktopTabPane.CloseMode.withSilentCloseButton,
                        false);
            }
        }

        // Create settings tabs
        Map<String, List<Integer>> groupedByTabName = new HashMap<>();
        JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
        JIPipeDataSlot formsForRow = iterationStepForms.get(selectedRow);
        for (int row = 0; row < formsForRow.getRowCount(); row++) {
            String tab = formsForRow.getTextAnnotationOr(row, tabAnnotation, new JIPipeTextAnnotation(tabAnnotation, "General")).getValue();
            List<Integer> rowList = groupedByTabName.getOrDefault(tab, null);
            if (rowList == null) {
                rowList = new ArrayList<>();
                groupedByTabName.put(tab, rowList);
            }
            rowList.add(row);
        }
        Map<String, JIPipeDesktopFormPanel> formPanelsForTab = new HashMap<>();
        for (Map.Entry<String, List<Integer>> entry : groupedByTabName.entrySet()) {
            String tab = entry.getKey();
            for (Integer row : entry.getValue()) {
                FormData formData = formsForRow.getData(row, FormData.class, progressInfo);
                if (formData instanceof ParameterFormData) {
                    // Add to form panel
                    JIPipeDesktopFormPanel formPanel = formPanelsForTab.getOrDefault(tab, null);
                    if (formPanel == null) {
                        formPanel = new JIPipeDesktopFormPanel(documentation,
                                JIPipeDesktopFormPanel.WITH_DOCUMENTATION | JIPipeDesktopFormPanel.DOCUMENTATION_BELOW | JIPipeDesktopFormPanel.WITH_SCROLLING);
                        tabPane.addTab(tab,
                                UIUtils.getIconFromResources("actions/settings.png"),
                                formPanel,
                                JIPipeDesktopTabPane.CloseMode.withoutCloseButton,
                                false);
                        formPanelsForTab.put(tab, formPanel);
                    }
                    ParameterFormData parameterFormData = (ParameterFormData) formData;
                    if (parameterFormData.isShowName()) {
                        formPanel.addToForm(formData.getEditor(workbench),
                                new JLabel(parameterFormData.getName()),
                                parameterFormData.getDescription().toMarkdown());
                    } else {
                        formPanel.addWideToForm(formData.getEditor(workbench),
                                parameterFormData.getDescription().toMarkdown());
                    }
                } else {
                    // Create a separate GUI tab
                    tabPane.addTab(tab,
                            UIUtils.getIconFromResources("actions/settings.png"),
                            formData.getEditor(getWorkbench()),
                            JIPipeDesktopTabPane.CloseMode.withoutCloseButton,
                            false);
                }
            }
        }
        for (Map.Entry<String, JIPipeDesktopFormPanel> entry : formPanelsForTab.entrySet()) {
            entry.getValue().addVerticalGlue();
        }

        // Switch to the last tab for consistency
        tabPane.getTabs().stream().filter(tab -> Objects.equals(lastTab, tab.getTitle())).findFirst()
                .ifPresent(documentTab -> tabPane.switchToContent(documentTab.getContent()));
    }

    private JIPipeValidationReport getReportForDataBatch(int selectedRow, JIPipeProgressInfo progressInfo) {
        JIPipeValidationReport report = new JIPipeValidationReport();
        JIPipeDataSlot formsForRow = iterationStepForms.get(selectedRow);
        for (int row = 0; row < formsForRow.getRowCount(); row++) {
            FormData formData = formsForRow.getData(row, FormData.class, progressInfo);
            String name = formData.toString();
            String tab = formsForRow.getTextAnnotationOr(row, tabAnnotation, new JIPipeTextAnnotation(tabAnnotation, "General")).getValue();
            if (formData instanceof ParameterFormData) {
                name = ((ParameterFormData) formData).getName();
            }
            formData.reportValidity(new CustomValidationReportContext(tab + " -> " + name + " (#" + row + ")"), report);
        }
        return report;
    }

    private void updateVisitedStatuses() {
        JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
        for (int i = 0; i < iterationStepList.size(); i++) {
            if (iterationStepStatuses.get(i) == DataBatchStatus.Visited) {
                JIPipeValidationReport report = new JIPipeValidationReport();
                for (int row = 0; row < iterationStepForms.get(i).getRowCount(); row++) {
                    FormData formData = iterationStepForms.get(i).getData(row, FormData.class, progressInfo);
                    formData.reportValidity(new CustomValidationReportContext("Form " + row), report);
                    if (!report.isValid()) {
                        iterationStepStatuses.set(i, DataBatchStatus.Invalid);
                        break;
                    }
                }
                if (report.isValid()) {
                    iterationStepStatuses.set(i, DataBatchStatus.Visited);
                }
            }
        }
        iterationStepTableUI.getTable().repaint();
    }

    private void updateBottomBarStats() {
        int visited = 0;
        int unvisited = 0;
        int invalid = 0;
        for (DataBatchStatus status : iterationStepStatuses) {
            switch (status) {
                case Invalid:
                    ++invalid;
                    break;
                case Visited:
                    ++visited;
                    break;
                case Unvisited:
                    ++unvisited;
                    break;
            }
        }
        invalidLabel.setVisible(invalid > 0);
        unvisitedLabel.setVisible(unvisited > 0);
        visitedLabel.setVisible(visited > 0);
        invalidLabel.setText(invalid + " invalid");
        unvisitedLabel.setText(unvisited + " to review");
        visitedLabel.setText(visited + " already reviewed");
    }

    private void initializeBottomBar(JPanel contentPanel) {
        JPanel buttonBar = new JPanel();
        buttonBar.setLayout(new BoxLayout(buttonBar, BoxLayout.X_AXIS));

        visitedLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        buttonBar.add(visitedLabel);
        unvisitedLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        buttonBar.add(unvisitedLabel);
        invalidLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        buttonBar.add(invalidLabel);

        buttonBar.add(Box.createHorizontalGlue());

        JButton previousButton = new JButton("Previous", UIUtils.getIconFromResources("actions/go-previous.png"));
        previousButton.addActionListener(e -> gotoPreviousBatch());
        buttonBar.add(previousButton);
        visitedButton.setToolTipText("Mark/un-mark the current entry as reviewed.");
        visitedButton.addActionListener(e -> toggleBatchVisited());
        buttonBar.add(visitedButton);
        JButton nextButton = new JButton("Next", UIUtils.getIconFromResources("actions/go-next.png"));
        nextButton.addActionListener(e -> gotoNextBatch());
        buttonBar.add(nextButton);

        buttonBar.add(Box.createHorizontalStrut(8));

        JButton applyToButton = new JButton("Apply to ...", UIUtils.getIconFromResources("actions/tools-wizard.png"));
        JPopupMenu applyToMenu = UIUtils.addPopupMenuToButton(applyToButton);

        JMenuItem applyToAllButton = new JMenuItem("All iteration steps", UIUtils.getIconFromResources("actions/dialog-layers.png"));
        applyToAllButton.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this,
                    "Do you really want to copy the current settings to all other batches?\n" +
                            "This will replace all existing values.",
                    "Apply to all iteration steps",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                applyCurrentSettingsToAll(true);
            }
        });
        applyToAllButton.setToolTipText("Applies the current settings to all iteration steps, including ones that have been already visited.");
        applyToMenu.add(applyToAllButton);

        JMenuItem applyToAllRemainingButton = new JMenuItem("All data remaining batches", UIUtils.getIconFromResources("actions/dialog-layers.png"));
        applyToAllRemainingButton.addActionListener(e -> {
            if (iterationStepStatuses.stream().noneMatch(iterationStepStatus -> iterationStepStatus == DataBatchStatus.Unvisited)) {
                JOptionPane.showMessageDialog(this,
                        "There are no remaining unvisited iteration steps.",
                        "Apply to all remaining batches",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (JOptionPane.showConfirmDialog(this,
                    "Do you really want to copy the current settings to all remaining batches?",
                    "Apply to all remaining iteration steps",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                applyCurrentSettingsToAll(false);
            }
        });
        applyToAllRemainingButton.setToolTipText("Applies the current settings to all iteration steps, excluding ones that have been already visited.");
        applyToMenu.add(applyToAllRemainingButton);

        buttonBar.add(applyToButton);

        JButton resetButton = new JButton("Reset ...", UIUtils.getIconFromResources("actions/clear-brush.png"));
        JPopupMenu resetMenu = UIUtils.addPopupMenuToButton(resetButton);

        JMenuItem resetVisitedItem = new JMenuItem("'Reviewed' status only", UIUtils.getIconFromResources("actions/eye-slash.png"));
        resetVisitedItem.setToolTipText("Marks all iteration steps as not reviewed. This will not change any settings.");
        resetVisitedItem.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this,
                    "Do you want to set all batches to 'not reviewed'?\n" +
                            "This will not change any settings already made.",
                    "Reset 'Reviewed' status only",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                for (int i = 0; i < iterationStepStatuses.size(); i++) {
                    iterationStepStatuses.set(i, DataBatchStatus.Unvisited);
                }
                visitedButton.setSelected(false);
                iterationStepTableUI.getTable().repaint();
                updateBottomBarStats();
            }
        });
        resetMenu.add(resetVisitedItem);
        resetMenu.addSeparator();

        JMenuItem resetCurrentItem = new JMenuItem("Current batch", UIUtils.getIconFromResources("actions/clear-brush.png"));
        resetCurrentItem.setToolTipText("Resets the settings of the currently viewed iteration step.");
        resetCurrentItem.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this,
                    "Do you want to reset all settings of the current batch?",
                    "Reset current batch",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                resetCurrentBatch();
            }
        });
        resetMenu.add(resetCurrentItem);

        JMenuItem resetAllItem = new JMenuItem("All batches", UIUtils.getIconFromResources("actions/clear-brush.png"));
        resetAllItem.setToolTipText("Resets the settings of all iteration steps.");
        resetAllItem.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this,
                    "Do you want to reset all settings of all batches?",
                    "Reset all batches",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                resetAllBatches(false);
            }
        });
        resetMenu.add(resetAllItem);

        JMenuItem resetUnvisitedItem = new JMenuItem("Non-reviewed batches", UIUtils.getIconFromResources("actions/clear-brush.png"));
        resetUnvisitedItem.setToolTipText("Resets the settings of all iteration steps that are not reviewed.");
        resetUnvisitedItem.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this,
                    "Do you want to reset all settings of all non-reviewed batches?",
                    "Reset non-reviewed batches",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                resetAllBatches(true);
            }
        });
        resetMenu.add(resetUnvisitedItem);

        buttonBar.add(resetButton);

        buttonBar.add(Box.createHorizontalStrut(8));

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/dialog-cancel.png"));
        cancelButton.addActionListener(e -> cancelDialog());
        buttonBar.add(cancelButton);

        JButton finishButton = new JButton("Finish", UIUtils.getIconFromResources("actions/dialog-apply.png"));
        finishButton.addActionListener(e -> finishDialog());
        buttonBar.add(finishButton);

        contentPanel.add(buttonBar, BorderLayout.SOUTH);
    }

    private void resetAllBatches(boolean onlyUnvisited) {
        for (int i = 0; i < iterationStepList.size(); ++i) {
            if (onlyUnvisited && iterationStepStatuses.get(i) != DataBatchStatus.Unvisited)
                continue;
            resetDataBatch(i);
        }
        updateVisitedStatuses();
        updateBottomBarStats();
        iterationStepTableUI.getTable().repaint();
    }

    private void resetDataBatch(int i) {
        JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
        JIPipeDataSlot tmpCopy = createFormsInstanceFor(i, progressInfo);
        JIPipeDataSlot copy = originalForms.getInfo().createInstance(originalForms.getNode());
        for (int row = 0; row < tmpCopy.getRowCount(); row++) {
            FormData src = tmpCopy.getData(row, FormData.class, progressInfo);
            FormData target = iterationStepForms.get(i).getData(row, FormData.class, progressInfo);
            if (target.isUsingCustomReset()) {
                target.customReset();
                copy.addData(target,
                        tmpCopy.getTextAnnotations(row),
                        JIPipeTextAnnotationMergeMode.OverwriteExisting,
                        tmpCopy.getDataAnnotations(row),
                        JIPipeDataAnnotationMergeMode.OverwriteExisting,
                        tmpCopy.getDataContext(row),
                        progressInfo);
            } else {
                copy.addData(src,
                        tmpCopy.getTextAnnotations(row),
                        JIPipeTextAnnotationMergeMode.OverwriteExisting,
                        tmpCopy.getDataAnnotations(row),
                        JIPipeDataAnnotationMergeMode.OverwriteExisting,
                        tmpCopy.getDataContext(row),
                        progressInfo);
            }
        }
        iterationStepForms.set(i, copy);
        iterationStepStatuses.set(i, DataBatchStatus.Unvisited);
    }

    private void resetCurrentBatch() {
        int selectedRow = iterationStepTableUI.getTable().getSelectedRow();
        if (selectedRow != -1) {
            selectedRow = iterationStepTableUI.getTable().convertRowIndexToModel(selectedRow);
        } else {
            return;
        }
        resetDataBatch(selectedRow);
        updateBottomBarStats();
        iterationStepTableUI.getTable().repaint();
        closeAllTabsAndRememberLast();
        switchToDataBatchUI(selectedRow);
    }

    private void applyCurrentSettingsToAll(boolean includingVisited) {
        if (!checkCurrentBatch())
            return;
        int selectedRow = iterationStepTableUI.getTable().getSelectedRow();
        if (selectedRow != -1) {
            selectedRow = iterationStepTableUI.getTable().convertRowIndexToModel(selectedRow);
        } else {
            return;
        }

        boolean encounteredImmutable = false;
        JIPipeValidationReport report = new JIPipeValidationReport();

        JIPipeDataSlot forms = iterationStepForms.get(selectedRow);
        JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
        for (int i = 0; i < iterationStepList.size(); i++) {
            if (i == selectedRow)
                continue;
            if (!includingVisited && iterationStepStatuses.get(i) != DataBatchStatus.Unvisited)
                continue;

            // Just copy the form
            JIPipeDataSlot copy = forms.getInfo().createInstance(forms.getNode());
            for (int row = 0; row < forms.getRowCount(); row++) {
                FormData srcData = forms.getData(row, FormData.class, progressInfo);
                FormData targetData = iterationStepForms.get(i).getData(row, FormData.class, progressInfo);
                if (!targetData.isImmutable()) {
                    if (targetData.isUsingCustomCopy())
                        targetData.customCopy(srcData, new CustomValidationReportContext("Item " + (i + 1)), report);
                    else
                        targetData = (FormData) srcData.duplicate(progressInfo);
                } else {
                    encounteredImmutable = true;
                }
                copy.addData(targetData,
                        forms.getTextAnnotations(row),
                        JIPipeTextAnnotationMergeMode.OverwriteExisting,
                        progressInfo);
            }
            iterationStepForms.set(i, copy);
            iterationStepStatuses.set(i, DataBatchStatus.Visited);
        }
        updateVisitedStatuses();
        updateBottomBarStats();

        if (encounteredImmutable) {
            JOptionPane.showMessageDialog(this, "Some settings could not be copied, as " +
                    "they are marked immutable.", "Copy settings", JOptionPane.WARNING_MESSAGE);
        }
        if (!report.isValid()) {
            UIUtils.showValidityReportDialog(workbench, this, report, "Errors while copying settings", "The following issues were detected while copying the data:", true);
        }
    }

    private void toggleBatchVisited() {
        int selectedRow = iterationStepTableUI.getTable().getSelectedRow();
        if (selectedRow != -1) {
            selectedRow = iterationStepTableUI.getTable().convertRowIndexToModel(selectedRow);
            if (visitedButton.isSelected()) {
                iterationStepStatuses.set(selectedRow, DataBatchStatus.Visited);
            } else {
                iterationStepStatuses.set(selectedRow, DataBatchStatus.Unvisited);
            }
            updateBottomBarStats();
            iterationStepTableUI.getTable().repaint();
        }
    }

    private void finishDialog() {
        updateVisitedStatuses();
        long unvisited = iterationStepStatuses.stream().filter(iterationStepStatus -> iterationStepStatus == DataBatchStatus.Unvisited).count();
        long invalid = iterationStepStatuses.stream().filter(iterationStepStatus -> iterationStepStatus == DataBatchStatus.Invalid).count();
        if (unvisited > 0) {
            if (JOptionPane.showConfirmDialog(FormsDialog.this,
                    "There are " + unvisited + " iteration steps that are not marked as reviewed. Do you want to continue, anyway?",
                    getTitle(),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) {
                return;
            }
        }
        if (invalid > 0) {
            if (JOptionPane.showConfirmDialog(FormsDialog.this,
                    "There are " + invalid + " iteration steps that report issues. Do you want to continue, anyway?",
                    getTitle(),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) {
                return;
            }
        }
        cancelled = false;
        dispose();
    }

    private void cancelDialog() {
        updateVisitedStatuses();
        if (iterationStepStatuses.stream().allMatch(iterationStepStatus -> iterationStepStatus == DataBatchStatus.Visited)) {
            cancelled = false;
            dispose();
        } else {
            if (JOptionPane.showConfirmDialog(FormsDialog.this,
                    "Do you really want to cancel the pipeline?",
                    getTitle(),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                cancelled = true;
                dispose();
            }
        }
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public JIPipeDesktopWorkbench getWorkbench() {
        return workbench;
    }

    public List<JIPipeDataSlot> getDataBatchForms() {
        return iterationStepForms;
    }
}
