package org.hkijena.jipipe.extensions.forms.ui;

import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.extensions.forms.datatypes.FormData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.cache.DataBatchTableModel;
import org.hkijena.jipipe.ui.cache.DataBatchTableUI;
import org.hkijena.jipipe.ui.components.DocumentTabPane;
import org.hkijena.jipipe.ui.components.JIPipeComponentCellRenderer;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

public class FormsDialog extends JFrame {
    private final JIPipeWorkbench workbench;
    private final List<JIPipeMergingDataBatch> dataBatchList;
    private final List<List<FormData>> dataBatchForms = new ArrayList<>();
    private boolean cancelled = false;
    private DataBatchTableUI dataBatchTableUI;
    private DocumentTabPane tabPane = new DocumentTabPane();

    public FormsDialog(JIPipeWorkbench workbench, List<JIPipeMergingDataBatch> dataBatchList, List<FormData> forms) {
        this.workbench = workbench;
        this.dataBatchList = dataBatchList;

        // We need to make copies of the FormData objects, as they are mutable
        for (JIPipeMergingDataBatch dataBatch : dataBatchList) {
            List<FormData> dataBatchFormList = new ArrayList<>();
            for (FormData form : forms) {
                dataBatchFormList.add((FormData) form.duplicate());
            }
            dataBatchForms.add(dataBatchFormList);
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
