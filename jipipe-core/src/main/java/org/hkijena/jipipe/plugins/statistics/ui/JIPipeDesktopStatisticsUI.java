package org.hkijena.jipipe.plugins.statistics.ui;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.service.components.JIPipeStatisticsServiceComponent;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.settings.JIPipeStatisticsApplicationSettings;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;

import com.fasterxml.jackson.databind.JsonNode;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Map;

public class JIPipeDesktopStatisticsUI extends JIPipeDesktopProjectWorkbenchPanel {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private JPanel cardsPanel;
    private JPanel headerPanel;

    public JIPipeDesktopStatisticsUI(JIPipeDesktopProjectWorkbench workbench) {
        super(workbench);
        initialize();
        refresh();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);
        add(createCenterPanel(), BorderLayout.CENTER);
        add(createToolbar(), BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JIPipeStatisticsServiceComponent service = JIPipe.getInstance().getStatistics();
        JIPipeStatisticsApplicationSettings settings = JIPipeStatisticsApplicationSettings.getInstance();

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 0, 16, 0),
                BorderFactory.createMatteBorder(1, 0, 1, 0, ThemeUtils.getCurrentStyle().getBorderColor())));
        headerPanel.setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());
        headerPanel.setPreferredSize(new Dimension(headerPanel.getPreferredSize().width, 120));

        JLabel titleLabel = new JLabel("Statistics");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, ThemeUtils.getCurrentStyle().getFontSizeHuge()));
        titleLabel.setBorder(UIUtils.createEmptyBorder(8));
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 8, 2, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        infoPanel.add(new JLabel("Machine ID:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        JTextField machineIdField = new JTextField(StringUtils.nullToEmpty(service.getMachineId()));
        machineIdField.setEditable(false);
        infoPanel.add(machineIdField, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        JButton rerollButton = UIUtils.createButton("Re-roll", JIPipe.RESOURCES.getIcon16("actions/reload.png"), () -> {
            service.rerollMachineId();
            machineIdField.setText(StringUtils.nullToEmpty(service.getMachineId()));
        });
        infoPanel.add(rerollButton, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        infoPanel.add(new JLabel("First launch:"), gbc);
        gbc.gridx = 1;
        LocalDateTime firstLaunch = service.getFirstLaunchTimestamp();
        infoPanel.add(new JLabel(firstLaunch != null ? firstLaunch.format(FORMATTER) : "Unknown"), gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        infoPanel.add(new JLabel("Last sent:"), gbc);
        gbc.gridx = 1;
        LocalDateTime lastSent = service.getLastSentTimestamp();
        infoPanel.add(new JLabel(lastSent != null ? lastSent.format(FORMATTER) : "Never"), gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        infoPanel.add(new JLabel("Privacy level:"), gbc);
        gbc.gridx = 1;
        infoPanel.add(new JLabel(settings.getPrivacyLevel().toString()), gbc);

        headerPanel.add(infoPanel, BorderLayout.CENTER);

        return headerPanel;
    }

    private JComponent createCenterPanel() {
        cardsPanel = new JPanel();
        cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.Y_AXIS));
        cardsPanel.setBackground(UIManager.getColor("Panel.background"));

        JScrollPane scrollPane = new JScrollPane(cardsPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        return scrollPane;
    }

    private JToolBar createToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(UIUtils.createButton("Refresh", JIPipe.RESOURCES.getIcon16("actions/stock_refresh.png"), this::refresh));
        return toolBar;
    }

    public void refresh() {
        remove(headerPanel);
        headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        cardsPanel.removeAll();
        cardsPanel.add(Box.createVerticalStrut(8));

        JIPipeStatisticsServiceComponent service = JIPipe.getInstance().getStatistics();
        for (JIPipeStatisticsItem item : service.getRegistry().getItems()) {
            JPanel card = createCard(item, service);
            card.setAlignmentX(Component.LEFT_ALIGNMENT);
            cardsPanel.add(card);
        }
        cardsPanel.add(Box.createVerticalGlue());
        cardsPanel.revalidate();
        cardsPanel.repaint();
        revalidate();
        repaint();
    }

    private JPanel createCard(JIPipeStatisticsItem item, JIPipeStatisticsServiceComponent service) {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 8, 8, 8),
                new RoundedLineBorder(UIUtils.getControlBorderColor(), 1, 4)
        ));
        card.setBackground(UIManager.getColor("Panel.background"));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, item.isTimeTracked() ? 220 : 100));

        JLabel titleLabel = new JLabel(item.getName());
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, ThemeUtils.getCurrentStyle().getFontSizeLarge()));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        card.add(titleLabel, BorderLayout.NORTH);

        JsonNode serialized = item.serialize();
        String valueStr = formatValue(serialized);
        JLabel valueLabel = new JLabel(valueStr);
        valueLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 4, 8));
        valueLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        card.add(valueLabel, BorderLayout.CENTER);

        if (item.isTimeTracked()) {
            ChartPanel chartPanel = createHistoryChart(item, service);
            if (chartPanel != null) {
                chartPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
                card.add(chartPanel, BorderLayout.SOUTH);
            }
        }

        return card;
    }

    private String formatValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return "N/A";
        }
        if (node.isNumber()) {
            return node.toString();
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isObject()) {
            StringBuilder sb = new StringBuilder();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                if (sb.length() > 0) sb.append(", ");
                sb.append(entry.getKey()).append(": ").append(entry.getValue().toString());
            }
            return sb.toString();
        }
        if (node.isArray()) {
            return node.size() + " entries";
        }
        return node.toString();
    }

    private ChartPanel createHistoryChart(JIPipeStatisticsItem item, JIPipeStatisticsServiceComponent service) {
        JsonNode historyNode = service.getHistory();
        if (historyNode == null) {
            return null;
        }
        JsonNode history = historyNode.get(item.getId());
        if (history == null || !history.isArray() || history.isEmpty()) {
            return null;
        }

        XYSeries series = new XYSeries(item.getName());
        boolean hasNumeric = false;
        for (int i = 0; i < history.size(); i++) {
            JsonNode point = history.get(i);
            JsonNode valueNode = point.get("value");
            if (valueNode != null && valueNode.isNumber()) {
                series.add(i, valueNode.asDouble());
                hasNumeric = true;
            }
        }
        if (!hasNumeric) {
            return null;
        }

        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart(null, null, null, dataset);
        chart.removeLegend();
        chart.setBackgroundPaint(UIManager.getColor("Panel.background"));

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(UIManager.getColor("Panel.background"));
        plot.setOutlineVisible(false);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(false);
        plot.getDomainAxis().setVisible(false);
        plot.getRangeAxis().setVisible(false);
        plot.setInsets(new org.jfree.chart.ui.RectangleInsets(2, 2, 2, 2));

        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        renderer.setDefaultStroke(new BasicStroke(2f));
        renderer.setSeriesPaint(0, ThemeUtils.getCurrentStyle().getPrimaryColor());
        renderer.setDefaultShapesVisible(false);

        ChartPanel panel = new ChartPanel(chart);
        panel.setPreferredSize(new Dimension(280, 100));
        panel.setMinimumDrawWidth(0);
        panel.setMaximumDrawWidth(Integer.MAX_VALUE);
        panel.setMinimumDrawHeight(0);
        panel.setMaximumDrawHeight(Integer.MAX_VALUE);
        return panel;
    }
}
