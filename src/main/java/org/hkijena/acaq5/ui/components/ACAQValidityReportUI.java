package org.hkijena.acaq5.ui.components;

import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.hkijena.acaq5.utils.StringUtils;
import org.hkijena.acaq5.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Map;

public class ACAQValidityReportUI extends JPanel {
    private JSplitPane splitPane;
    private ACAQValidityReport report;
    private JXTable table;
    private JPanel tablePanel;
    private boolean withHelp;
    private JPanel everythingValidPanel;

    public ACAQValidityReportUI(boolean withHelp) {
        this.withHelp = withHelp;
        initialize();
        switchToEverythingValid();
    }

    private void switchToTable() {
        if (withHelp) {
            splitPane.setLeftComponent(tablePanel);
        } else {
            removeAll();
            add(tablePanel, BorderLayout.CENTER);
            revalidate();
            repaint();
        }
    }

    private void switchToEverythingValid() {
        if (withHelp) {
            splitPane.setLeftComponent(everythingValidPanel);
        } else {
            removeAll();
            add(everythingValidPanel, BorderLayout.CENTER);
            revalidate();
            repaint();
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());

        // Create table panel
        tablePanel = new JPanel(new BorderLayout());
        table = new JXTable() {
            @Override
            public boolean isCellEditable(int i, int i1) {
                return false;
            }
        };
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(Color.WHITE);
        tablePanel.add(scrollPane, BorderLayout.CENTER);
        tablePanel.add(table.getTableHeader(), BorderLayout.NORTH);

        // Create alternative panel
        everythingValidPanel = new JPanel(new BorderLayout());
        {
            JLabel label = new JLabel("No issues found", UIUtils.getIconFromResources("check-circle-green-64.png"), JLabel.LEFT);
            label.setFont(label.getFont().deriveFont(26.0f));
            everythingValidPanel.add(label, BorderLayout.CENTER);
        }

        // Create split pane if needed
        if (withHelp) {
            splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JLabel(),
                    new MarkdownReader(false, MarkdownDocument.fromPluginResource("documentation/validation.md")));
            splitPane.setDividerSize(3);
            splitPane.setResizeWeight(0.66);
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    super.componentResized(e);
                    splitPane.setDividerLocation(0.66);
                }
            });
            add(splitPane, BorderLayout.CENTER);
        }
    }

    private void refreshUI() {
        if (report == null || report.isValid()) {
            switchToEverythingValid();
        } else {
            DefaultTableModel model = new DefaultTableModel();
            model.addColumn("Location");
            model.addColumn("Message");
            Map<String, String> messages = report.getMessages();
            for (String key : report.getInvalidResponses()) {

                model.addRow(new Object[]{
                        StringUtils.createIconTextHTMLTable(key.replace("/", " ⏵ "), ResourceUtils.getPluginResource("icons/error.png")),
                        StringUtils.wordWrappedInHTML(messages.getOrDefault(key, ""), 50)
                });
            }
            table.setModel(model);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            UIUtils.fitRowHeights(table);
            table.packAll();
            switchToTable();
        }
    }

    public void setReport(ACAQValidityReport report) {
        this.report = report;
        refreshUI();
    }
}
