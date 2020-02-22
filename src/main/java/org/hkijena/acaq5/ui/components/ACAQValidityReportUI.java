package org.hkijena.acaq5.ui.components;

import com.google.common.base.Joiner;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;

public class ACAQValidityReportUI extends JPanel {
    private ACAQValidityReport report;
    private JXTable table;

    public ACAQValidityReportUI() {
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        table = new JXTable() {
            @Override
            public boolean isCellEditable(int i, int i1) {
                return false;
            }
        };
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);
        add(table.getTableHeader(), BorderLayout.NORTH);
    }

    private void refreshUI() {
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return getValueAt(0, columnIndex).getClass();
            }
        };
        if(report != null) {
            model.addColumn("");
            model.addColumn("Location");
            model.addColumn("Message");
            ImageIcon icon = UIUtils.getIconFromResources("error.png");
            Map<String, String> messages = report.getMessages();
            for(String key : report.getInvalidResponses()) {
                model.addRow(new Object[]{
                        icon,
                        key,
                        messages.getOrDefault(key, "")
                });
            }
        }
        table.setModel(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.packAll();
    }

    public void setReport(ACAQValidityReport report) {
        this.report = report;
        refreshUI();
    }
}
