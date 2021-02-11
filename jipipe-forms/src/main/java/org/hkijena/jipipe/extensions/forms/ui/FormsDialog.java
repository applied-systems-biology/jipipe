package org.hkijena.jipipe.extensions.forms.ui;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.extensions.forms.datatypes.FormData;
import org.hkijena.jipipe.extensions.forms.datatypes.ParameterFormData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.cache.DataBatchTableUI;
import org.hkijena.jipipe.ui.components.DocumentTabPane;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FormsDialog extends JFrame {
    private final JIPipeWorkbench workbench;
    private final String tabAnnotation;
    private final List<JIPipeMergingDataBatch> dataBatchList;
    private final List<JIPipeDataSlot> dataBatchForms = new ArrayList<>();
    private boolean cancelled = false;
    private DataBatchTableUI dataBatchTableUI;
    private DocumentTabPane tabPane = new DocumentTabPane();

    public FormsDialog(JIPipeWorkbench workbench, List<JIPipeMergingDataBatch> dataBatchList, JIPipeDataSlot forms, String tabAnnotation) {
        this.workbench = workbench;
        this.dataBatchList = dataBatchList;
        this.tabAnnotation = tabAnnotation;

        JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
        // We need to make copies of the FormData objects, as they are mutable
        for (int i = 0; i < dataBatchList.size(); ++i) {
            JIPipeDataSlot copy = new JIPipeDataSlot(forms.getInfo(), forms.getNode());
            for (int row = 0; row < forms.getRowCount(); row++) {
                copy.addData(forms.getData(row, FormData.class, progressInfo).duplicate(),
                        forms.getAnnotations(row),
                        JIPipeAnnotationMergeStrategy.OverwriteExisting,
                        progressInfo);
            }
            dataBatchForms.add(copy);
        }

        // Initialize UI
        initialize();
        gotoNextBatch();
    }

    private void gotoNextBatch() {
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

    private void gotoPreviousBatch() {
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
            tabPane.closeAllTabs();
            if(selectedRow != -1) {
                selectedRow = dataBatchTableUI.getTable().convertRowIndexToModel(selectedRow);
                switchToDataBatchUI(selectedRow);
            }
        });
    }

    private void switchToDataBatchUI(int selectedRow) {
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
                        formPanel = new FormPanel(new MarkdownDocument("Please provide your input!"),
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

    }

    private void initializeBottomBar(JPanel contentPanel) {
        JPanel buttonBar = new JPanel();
        buttonBar.setLayout(new BoxLayout(buttonBar, BoxLayout.X_AXIS));

        buttonBar.add(Box.createHorizontalGlue());

        JButton previousButton = new JButton("Previous", UIUtils.getIconFromResources("actions/go-previous.png"));
        previousButton.addActionListener(e -> gotoPreviousBatch());
        buttonBar.add(previousButton);
        JButton nextButton = new JButton("Next", UIUtils.getIconFromResources("actions/go-next.png"));
        nextButton.addActionListener(e -> gotoNextBatch());
        buttonBar.add(nextButton);

        buttonBar.add(Box.createHorizontalStrut(8));

        JButton applyToButton = new JButton("Apply to ...", UIUtils.getIconFromResources("actions/tools-wizard.png"));
        JPopupMenu applyToMenu = UIUtils.addPopupMenuToComponent(applyToButton);

        JMenuItem applyToAllButton = new JMenuItem("All data batches", UIUtils.getIconFromResources("actions/dialog-layers.png"));
        applyToAllButton.setToolTipText("Applies the current settings to all data batches, including ones that have been already visited.");
        applyToMenu.add(applyToAllButton);

        JMenuItem applyToAllRemainingButton = new JMenuItem("All data remaining batches", UIUtils.getIconFromResources("actions/dialog-layers.png"));
        applyToAllRemainingButton.setToolTipText("Applies the current settings to all data batches, excluding ones that have been already visited.");
        applyToMenu.add(applyToAllRemainingButton);

        buttonBar.add(applyToButton);

        buttonBar.add(Box.createHorizontalStrut(8));

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/dialog-cancel.png"));
        cancelButton.addActionListener(e -> cancelDialog());
        buttonBar.add(cancelButton);

        JButton finishButton = new JButton("Finish", UIUtils.getIconFromResources("actions/dialog-apply.png"));
        finishButton.addActionListener(e -> finishDialog());
        buttonBar.add(finishButton);

        contentPanel.add(buttonBar, BorderLayout.SOUTH);
    }

    private void finishDialog() {

    }

    private void cancelDialog() {
        if(JOptionPane.showConfirmDialog(FormsDialog.this,
                "Do you really want to cancel the pipeline?",
                getTitle(),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
            cancelled = true;
            dispose();
        }
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }
}
