package org.hkijena.jipipe.extensions.forms.ui;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.extensions.forms.FormsExtension;
import org.hkijena.jipipe.extensions.forms.datatypes.FormData;
import org.hkijena.jipipe.extensions.forms.datatypes.ParameterFormData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.batchassistant.DataBatchBrowserUI;
import org.hkijena.jipipe.ui.batchassistant.DataBatchTableUI;
import org.hkijena.jipipe.ui.components.*;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.List;

public class FormsDialog extends JFrame {
    private static final String TAB_ISSUES_DETECTED = "Issues detected";
    private final JIPipeWorkbench workbench;
    private final String tabAnnotation;
    private final List<JIPipeMergingDataBatch> dataBatchList;
    private final List<JIPipeDataSlot> dataBatchForms = new ArrayList<>();
    private final JIPipeDataSlot originalForms;
    private boolean cancelled = false;
    private DataBatchTableUI dataBatchTableUI;
    private DocumentTabPane tabPane = new DocumentTabPane();
    private String lastTab = "";
    private List<DataBatchStatus> dataBatchStatuses = new ArrayList<>();
    private JLabel unvisitedLabel = new JLabel(new ColorIcon(16,16, DataBatchStatusTableCellRenderer.COLOR_UNVISITED));
    private JLabel visitedLabel = new JLabel(new ColorIcon(16,16, new Color(0xb3ef8e)));
    private JLabel invalidLabel = new JLabel(new ColorIcon(16,16, DataBatchStatusTableCellRenderer.COLOR_INVALID));
    private JToggleButton visitedButton = new JToggleButton("Reviewed", UIUtils.getIconFromResources("actions/eye.png"));
    private MarkdownDocument documentation;

    public FormsDialog(JIPipeWorkbench workbench, List<JIPipeMergingDataBatch> dataBatchList, JIPipeDataSlot originalForms, String tabAnnotation) {
        this.originalForms = originalForms;
        setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
        this.workbench = workbench;
        this.dataBatchList = dataBatchList;
        this.tabAnnotation = tabAnnotation;
        this.documentation = MarkdownDocument.fromResourceURL(FormsExtension.class.getResource("/org/hkijena/jipipe/extensions/forms/form-dialog-documentation.md"),
                true);

        JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
        // We need to make copies of the FormData objects, as they are mutable
        for (int i = 0; i < dataBatchList.size(); ++i) {
            JIPipeDataSlot copy = createFormsInstanceFor(i, progressInfo);
            dataBatchForms.add(copy);
            dataBatchStatuses.add(DataBatchStatus.Unvisited);
        }

        // Initialize UI
        initialize();
        gotoNextBatch();
    }

    private JIPipeDataSlot createFormsInstanceFor(int index, JIPipeProgressInfo progressInfo) {
        JIPipeDataSlot copy = new JIPipeDataSlot(originalForms.getInfo(), originalForms.getNode());
        for (int row = 0; row < originalForms.getRowCount(); row++) {
            FormData formCopy = (FormData) originalForms.getData(row, FormData.class, progressInfo).duplicate();
            formCopy.loadData(dataBatchList.get(index));
            copy.addData(formCopy,
                    originalForms.getAnnotations(row),
                    JIPipeAnnotationMergeStrategy.OverwriteExisting,
                    progressInfo);
        }
        return copy;
    }

    private void gotoNextBatch() {
        if(!checkCurrentBatch())
            return;
        if(dataBatchList.size() > 0) {
            dataBatchTableUI.resetSearch();
            JXTable table = dataBatchTableUI.getTable();
            int row = table.getSelectedRow();
            if(row == -1) {
                table.getSelectionModel().setSelectionInterval(0,0);
            }
            else {
                row = table.convertRowIndexToModel(row);
                row = (row + 1) % dataBatchList.size();
                row = table.convertRowIndexToView(row);
                table.getSelectionModel().setSelectionInterval(row, row);
            }
        }
    }

    private boolean checkCurrentBatch() {
        JXTable table = dataBatchTableUI.getTable();
        int row = table.getSelectedRow();
        if(row == -1) {
            return true;
        }
        JIPipeProgressInfo info = new JIPipeProgressInfo();
        JIPipeValidityReport report = getReportForDataBatch(row, info);
        if(!report.isValid()) {
            int result = JOptionPane.showOptionDialog(this,
                    "The current batch has settings that are not fully valid. Do you want to continue, anyways?",
                    "Issues found",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new Object[]{"Yes", "No", "Show errors"},
                    "Show errors");
            if(result == JOptionPane.YES_OPTION)
                return true;
            else if (result == JOptionPane.NO_OPTION)
                return false;
            else {
                tabPane.closeAllTabs();
                lastTab = TAB_ISSUES_DETECTED;
                switchToDataBatchUI(row);
                return false;
            }
        }
        return true;
    }

    private void gotoPreviousBatch() {
        if(!checkCurrentBatch())
            return;
        if(dataBatchList.size() > 0) {
            dataBatchTableUI.resetSearch();
            JXTable table = dataBatchTableUI.getTable();
            int row = table.getSelectedRow();
            if(row == -1) {
                table.getSelectionModel().setSelectionInterval(0,0);
            }
            else {
                row = table.convertRowIndexToModel(row);
                --row;
                if(row < 0)
                    row = dataBatchList.size() - 1;
                row = table.convertRowIndexToView(row);
                table.getSelectionModel().setSelectionInterval(row, row);
            }
        }
    }

    private void initialize() {
        JPanel contentPanel = new JPanel(new BorderLayout());

        dataBatchTableUI = new DataBatchTableUI(dataBatchList);
        dataBatchTableUI.getTable().setDefaultRenderer(Integer.class, new DataBatchStatusTableCellRenderer(dataBatchStatuses));
        dataBatchTableUI.getTable().setDefaultRenderer(String.class, new DataBatchStatusTableCellRenderer(dataBatchStatuses));
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, dataBatchTableUI, tabPane);
        splitPane.setDividerSize(3);
        splitPane.setResizeWeight(0.33);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                splitPane.setDividerLocation(0.33);
            }
        });
        splitPane.setDividerLocation(0.33);
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

        dataBatchTableUI.getTable().getSelectionModel().addListSelectionListener(e -> {
            int selectedRow = dataBatchTableUI.getTable().getSelectedRow();
            closeAllTabsAndRememberLast();
            if(selectedRow != -1) {
                selectedRow = dataBatchTableUI.getTable().convertRowIndexToModel(selectedRow);
                switchToDataBatchUI(selectedRow);
            }
        });
    }

    private void closeAllTabsAndRememberLast() {
        if(tabPane.getTabCount() > 0 && tabPane.getCurrentContent() != null) {
            lastTab = tabPane.getTabContaining(tabPane.getCurrentContent()).getTitle();
        }
        tabPane.closeAllTabs();
    }

    private void switchToDataBatchUI(int selectedRow) {

        boolean wasVisitedBefore = dataBatchStatuses.get(selectedRow) != DataBatchStatus.Unvisited;
        visitedButton.setSelected(true);

        updateVisitedStatuses();

        // Set the status
        dataBatchStatuses.set(selectedRow, DataBatchStatus.Visited);
        dataBatchTableUI.getTable().repaint();
        updateBottomBarStats();

        // Create preview tab
        tabPane.addTab("View data",
                UIUtils.getIconFromResources("actions/zoom.png"),
                new DataBatchBrowserUI(getWorkbench(), dataBatchList.get(selectedRow)),
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);

        // If invalid, create report
        if(wasVisitedBefore) {
            JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
            JIPipeValidityReport report = getReportForDataBatch(selectedRow, progressInfo);
            if(!report.isValid()) {
                UserFriendlyErrorUI errorUI = new UserFriendlyErrorUI(null, UserFriendlyErrorUI.WITH_SCROLLING);
                errorUI.displayErrors(report);
                errorUI.addVerticalGlue();
                tabPane.addTab(TAB_ISSUES_DETECTED,
                        UIUtils.getIconFromResources("actions/dialog-warning.png"),
                        errorUI,
                        DocumentTabPane.CloseMode.withSilentCloseButton,
                        false);
            }
        }

        // Create settings tabs
        Map<String, List<Integer>> groupedByTabName = new HashMap<>();
        JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
        JIPipeDataSlot formsForRow = dataBatchForms.get(selectedRow);
        for (int row = 0; row < formsForRow.getRowCount(); row++) {
            String tab = formsForRow.getAnnotationOr(row, tabAnnotation, new JIPipeAnnotation(tabAnnotation, "General")).getValue();
            List<Integer> rowList = groupedByTabName.getOrDefault(tab, null);
            if(rowList == null) {
                rowList = new ArrayList<>();
                groupedByTabName.put(tab, rowList);
            }
            rowList.add(row);
        }
        Map<String, FormPanel> formPanelsForTab = new HashMap<>();
        for (Map.Entry<String, List<Integer>> entry : groupedByTabName.entrySet()) {
            String tab = entry.getKey();
            for (Integer row : entry.getValue()) {
                FormData formData = formsForRow.getData(row, FormData.class, progressInfo);
                if(formData instanceof ParameterFormData) {
                    // Add to form panel
                    FormPanel formPanel = formPanelsForTab.getOrDefault(tab, null);
                    if(formPanel == null) {
                        formPanel = new FormPanel(documentation,
                                FormPanel.WITH_DOCUMENTATION | FormPanel.DOCUMENTATION_BELOW | FormPanel.WITH_SCROLLING);
                        tabPane.addTab(tab,
                                UIUtils.getIconFromResources("actions/settings.png"),
                                formPanel,
                                DocumentTabPane.CloseMode.withoutCloseButton,
                                false);
                        formPanelsForTab.put(tab, formPanel);
                    }
                    ParameterFormData parameterFormData = (ParameterFormData) formData;
                    if(parameterFormData.isShowName()) {
                        formPanel.addToForm(formData.getEditor(workbench),
                                new JLabel(parameterFormData.getName()),
                                parameterFormData.getDescription().toMarkdown());
                    }
                    else {
                        formPanel.addWideToForm(formData.getEditor(workbench),
                                parameterFormData.getDescription().toMarkdown());
                    }
                }
                else {
                    // Create a separate GUI tab
                    tabPane.addTab(tab,
                            UIUtils.getIconFromResources("actions/settings.png"),
                            formData.getEditor(getWorkbench()),
                            DocumentTabPane.CloseMode.withoutCloseButton,
                            false);
                }
            }
        }
        for (Map.Entry<String, FormPanel> entry : formPanelsForTab.entrySet()) {
            entry.getValue().addVerticalGlue();
        }

        // Switch to the last tab for consistency
        tabPane.getTabs().stream().filter(tab -> Objects.equals(lastTab, tab.getTitle())).findFirst()
                .ifPresent(documentTab -> tabPane.switchToContent(documentTab.getContent()));
    }

    @NotNull
    private JIPipeValidityReport getReportForDataBatch(int selectedRow, JIPipeProgressInfo progressInfo) {
        JIPipeValidityReport report = new JIPipeValidityReport();
        JIPipeDataSlot formsForRow = dataBatchForms.get(selectedRow);
        for (int row = 0; row < formsForRow.getRowCount(); row++) {
            FormData formData = formsForRow.getData(row, FormData.class, progressInfo);
            String name = formData.toString();
            String tab = formsForRow.getAnnotationOr(row, tabAnnotation, new JIPipeAnnotation(tabAnnotation, "General")).getValue();
            if(formData instanceof ParameterFormData) {
                name = ((ParameterFormData) formData).getName();
            }
            formData.reportValidity(report.forCategory(tab).forCategory(name + " (#" + row + ")"));
        }
        return report;
    }

    private void updateVisitedStatuses() {
        JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
        for (int i = 0; i < dataBatchList.size(); i++) {
            if(dataBatchStatuses.get(i) == DataBatchStatus.Visited) {
                JIPipeValidityReport report = new JIPipeValidityReport();
                for (int row = 0; row < dataBatchForms.get(i).getRowCount(); row++) {
                    FormData formData = dataBatchForms.get(i).getData(row, FormData.class, progressInfo);
                    formData.reportValidity(report.forCategory("Form " + row));
                    if(!report.isValid()) {
                        dataBatchStatuses.set(i, DataBatchStatus.Invalid);
                        break;
                    }
                }
                if(report.isValid()) {
                    dataBatchStatuses.set(i, DataBatchStatus.Visited);
                }
            }
        }
        dataBatchTableUI.getTable().repaint();
    }

    private void updateBottomBarStats() {
        int visited = 0;
        int unvisited = 0;
        int invalid = 0;
        for (DataBatchStatus status : dataBatchStatuses) {
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

        visitedLabel.setBorder(BorderFactory.createEmptyBorder(0,4,0,4));
        buttonBar.add(visitedLabel);
        unvisitedLabel.setBorder(BorderFactory.createEmptyBorder(0,4,0,4));
        buttonBar.add(unvisitedLabel);
        invalidLabel.setBorder(BorderFactory.createEmptyBorder(0,4,0,4));
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
        JPopupMenu applyToMenu = UIUtils.addPopupMenuToComponent(applyToButton);

        JMenuItem applyToAllButton = new JMenuItem("All data batches", UIUtils.getIconFromResources("actions/dialog-layers.png"));
        applyToAllButton.addActionListener(e -> {
            if(JOptionPane.showConfirmDialog(this,
                    "Do you really want to copy the current settings to all other batches?\n" +
                            "This will replace all existing values.",
                    "Apply to all data batches",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                applyCurrentSettingsToAll(true);
            }
        });
        applyToAllButton.setToolTipText("Applies the current settings to all data batches, including ones that have been already visited.");
        applyToMenu.add(applyToAllButton);

        JMenuItem applyToAllRemainingButton = new JMenuItem("All data remaining batches", UIUtils.getIconFromResources("actions/dialog-layers.png"));
        applyToAllRemainingButton.addActionListener(e -> {
            if(dataBatchStatuses.stream().noneMatch(dataBatchStatus -> dataBatchStatus == DataBatchStatus.Unvisited)) {
                JOptionPane.showMessageDialog(this,
                        "There are no remaining unvisited data batches.",
                        "Apply to all remaining batches",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(JOptionPane.showConfirmDialog(this,
                    "Do you really want to copy the current settings to all remaining batches?",
                    "Apply to all remaining data batches",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                applyCurrentSettingsToAll(false);
            }
        });
        applyToAllRemainingButton.setToolTipText("Applies the current settings to all data batches, excluding ones that have been already visited.");
        applyToMenu.add(applyToAllRemainingButton);

        buttonBar.add(applyToButton);

        JButton resetButton = new JButton("Reset ...", UIUtils.getIconFromResources("actions/clear-brush.png"));
        JPopupMenu resetMenu = UIUtils.addPopupMenuToComponent(resetButton);

        JMenuItem resetVisitedItem = new JMenuItem("'Reviewed' status only", UIUtils.getIconFromResources("actions/eye-slash.png"));
        resetVisitedItem.setToolTipText("Marks all data batches as not reviewed. This will not change any settings.");
        resetVisitedItem.addActionListener(e -> {
            if(JOptionPane.showConfirmDialog(this,
                    "Do you want to set all batches to 'not reviewed'?\n" +
                            "This will not change any settings already made.",
                    "Reset 'Reviewed' status only",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                for (int i = 0; i < dataBatchStatuses.size(); i++) {
                    dataBatchStatuses.set(i, DataBatchStatus.Unvisited);
                }
                visitedButton.setSelected(false);
                dataBatchTableUI.getTable().repaint();
                updateBottomBarStats();
            }
        });
        resetMenu.add(resetVisitedItem);
        resetMenu.addSeparator();

        JMenuItem resetCurrentItem = new JMenuItem("Current batch", UIUtils.getIconFromResources("actions/clear-brush.png"));
        resetCurrentItem.setToolTipText("Resets the settings of the currently viewed data batch.");
        resetCurrentItem.addActionListener(e -> {
            if(JOptionPane.showConfirmDialog(this,
                    "Do you want to reset all settings of the current batch?",
                    "Reset current batch",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
               resetCurrentBatch();
            }
        });
        resetMenu.add(resetCurrentItem);

        JMenuItem resetAllItem = new JMenuItem("All batches", UIUtils.getIconFromResources("actions/clear-brush.png"));
        resetAllItem.setToolTipText("Resets the settings of all data batches.");
        resetAllItem.addActionListener(e -> {
            if(JOptionPane.showConfirmDialog(this,
                    "Do you want to reset all settings of all batches?",
                    "Reset all batches",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                resetAllBatches(false);
            }
        });
        resetMenu.add(resetAllItem);

        JMenuItem resetUnvisitedItem = new JMenuItem("Non-reviewed batches", UIUtils.getIconFromResources("actions/clear-brush.png"));
        resetUnvisitedItem.setToolTipText("Resets the settings of all data batches that are not reviewed.");
        resetUnvisitedItem.addActionListener(e -> {
            if(JOptionPane.showConfirmDialog(this,
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
        JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
        for (int i = 0; i < dataBatchList.size(); ++i) {
            if(onlyUnvisited && dataBatchStatuses.get(i) != DataBatchStatus.Unvisited)
                continue;
            JIPipeDataSlot copy = createFormsInstanceFor(i, progressInfo);
            dataBatchForms.set(i, copy);
            dataBatchStatuses.set(i, DataBatchStatus.Unvisited);
        }
        updateVisitedStatuses();
        updateBottomBarStats();
        dataBatchTableUI.getTable().repaint();
    }

    private void resetCurrentBatch() {
        int selectedRow = dataBatchTableUI.getTable().getSelectedRow();
        if(selectedRow != -1) {
            selectedRow = dataBatchTableUI.getTable().convertRowIndexToModel(selectedRow);
        }
        else {
            return;
        }
        JIPipeDataSlot copy = createFormsInstanceFor(selectedRow, new JIPipeProgressInfo());
        dataBatchForms.set(selectedRow, copy);
        dataBatchStatuses.set(selectedRow, DataBatchStatus.Unvisited);
        updateBottomBarStats();
        dataBatchTableUI.getTable().repaint();
        closeAllTabsAndRememberLast();
        switchToDataBatchUI(selectedRow);
    }

    private void applyCurrentSettingsToAll(boolean includingVisited) {
        if(!checkCurrentBatch())
            return;
        int selectedRow = dataBatchTableUI.getTable().getSelectedRow();
        if(selectedRow != -1) {
            selectedRow = dataBatchTableUI.getTable().convertRowIndexToModel(selectedRow);
        }
        else {
            return;
        }

        JIPipeDataSlot forms = dataBatchForms.get(selectedRow);
        JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
        for (int i = 0; i < dataBatchList.size(); i++) {
            if(i == selectedRow)
                continue;
            if(!includingVisited && dataBatchStatuses.get(i) != DataBatchStatus.Unvisited)
                continue;

            // Just copy the form
            JIPipeDataSlot copy = new JIPipeDataSlot(forms.getInfo(), forms.getNode());
            for (int row = 0; row < forms.getRowCount(); row++) {
                copy.addData(forms.getData(row, FormData.class, progressInfo).duplicate(),
                        forms.getAnnotations(row),
                        JIPipeAnnotationMergeStrategy.OverwriteExisting,
                        progressInfo);
            }
            dataBatchForms.set(i, copy);
            dataBatchStatuses.set(i, DataBatchStatus.Visited);
        }
        updateVisitedStatuses();
        updateBottomBarStats();
    }

    private void toggleBatchVisited() {
        int selectedRow = dataBatchTableUI.getTable().getSelectedRow();
        if(selectedRow != -1) {
            selectedRow = dataBatchTableUI.getTable().convertRowIndexToModel(selectedRow);
            if(visitedButton.isSelected()) {
                dataBatchStatuses.set(selectedRow, DataBatchStatus.Visited);
            }
            else {
                dataBatchStatuses.set(selectedRow, DataBatchStatus.Unvisited);
            }
            updateBottomBarStats();
            dataBatchTableUI.getTable().repaint();
        }
    }

    private void finishDialog() {
        updateVisitedStatuses();
        long unvisited = dataBatchStatuses.stream().filter(dataBatchStatus -> dataBatchStatus == DataBatchStatus.Unvisited).count();
        long invalid = dataBatchStatuses.stream().filter(dataBatchStatus -> dataBatchStatus == DataBatchStatus.Invalid).count();
        if(unvisited > 0) {
            if (JOptionPane.showConfirmDialog(FormsDialog.this,
                    "There are " + unvisited + " data batches that are not marked as reviewed. Do you want to continue, anyways?",
                    getTitle(),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) {
                return;
            }
        }
        if(invalid > 0) {
            if (JOptionPane.showConfirmDialog(FormsDialog.this,
                    "There are " + invalid + " data batches that report issues. Do you want to continue, anyways?",
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
        if(dataBatchStatuses.stream().allMatch(dataBatchStatus -> dataBatchStatus == DataBatchStatus.Visited)) {
            cancelled = false;
            dispose();
        }
        else {
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

    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }

    public List<JIPipeDataSlot> getDataBatchForms() {
        return dataBatchForms;
    }
}
