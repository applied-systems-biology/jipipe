package org.hkijena.jipipe.plugins.statistics.ui;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.service.components.JIPipeStatisticsServiceComponent;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.panels.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.settings.JIPipeStatisticsApplicationSettings;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;
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
    private JPanel headerPanel;
    private JPanel cardsContainer;
    private JTextField machineIdField;

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
    }

    private JPanel createHeaderPanel() {
        JIPipeStatisticsServiceComponent service = JIPipe.getInstance().getStatistics();
        JIPipeStatisticsApplicationSettings settings = JIPipeStatisticsApplicationSettings.getInstance();

        JPanel header = new JPanel();
        header.setBorder(BorderFactory::getCompoundBorder);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 0, 16, 0),
                BorderFactory.createMatteBorder(1, 0, 1, 0, ThemeUtils.getCurrentStyle().getBorderColor())));
        header.setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());
        header.setLayout(new BorderLayout());
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 150));

        JIPipeDesktopFormPanel leftPanel = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.TRANSPARENT_BACKGROUND);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));

        JTextField titleField = UIUtils.createReadonlyBorderlessTextField("Statistics");
        titleField.setOpaque(false);
        titleField.setFont(new Font(Font.DIALOG, Font.PLAIN, ThemeUtils.getCurrentStyle().getFontSizeHuge()));
        titleField.setBorder(UIUtils.createEmptyBorder(4));

        machineIdField = UIUtils.createReadonlyBorderlessTextField(StringUtils.nullToEmpty(service.getMachineId()));
        machineIdField.setOpaque(false);
        machineIdField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, ThemeUtils.getCurrentStyle().getFontSizeSmall()));

        JButton rerollButton = UIUtils.createButton("", JIPipe.RESOURCES.getIcon16("actions/dice-one.png"), () -> {
            service.rerollMachineId();
            machineIdField.setText(StringUtils.nullToEmpty(service.getMachineId()));
            refresh();
        });
        rerollButton.setToolTipText("Generate a new machine ID");

        leftPanel.addWideToForm(UIUtils.makeNonOpaque(UIUtils.boxHorizontal(titleField)), null);
        leftPanel.addWideToForm(UIUtils.makeNonOpaque(UIUtils.boxHorizontal(machineIdField,
                UIUtils.makeButtonTransparent(rerollButton))), null);

        LocalDateTime idCreated = service.getMachineIdCreatedTimestamp();
        JLabel idDateLabel = new JLabel("ID created: " + (idCreated != null ? idCreated.format(FORMATTER) : "Unknown"));
        idDateLabel.setFont(idDateLabel.getFont().deriveFont(Font.ITALIC, ThemeUtils.getCurrentStyle().getFontSizeSmall()));
        idDateLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        idDateLabel.setBorder(UIUtils.createEmptyBorder(4));
        leftPanel.addWideToForm(UIUtils.makeNonOpaque(idDateLabel), null);

        leftPanel.addVerticalGlue();
        header.add(leftPanel, BorderLayout.WEST);

        JIPipeDesktopFormPanel rightPanel = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.TRANSPARENT_BACKGROUND);

        LocalDateTime firstLaunch = service.getFirstLaunchTimestamp();
        JTextField firstLaunchField = UIUtils.createReadonlyBorderlessTextField(firstLaunch != null ? firstLaunch.format(FORMATTER) : "Unknown");
        rightPanel.addToForm(firstLaunchField, new JLabel("First launch"), null);

        LocalDateTime lastSent = service.getLastSentTimestamp();
        JTextField lastSentField = UIUtils.createReadonlyBorderlessTextField(lastSent != null ? lastSent.format(FORMATTER) : "Never");
        rightPanel.addToForm(lastSentField, new JLabel("Last sent"), null);

        JTextField privacyField = UIUtils.createReadonlyBorderlessTextField(settings.getPrivacyLevel().toString());
        rightPanel.addToForm(privacyField, new JLabel("Privacy level"), null);

        rightPanel.addVerticalGlue();
        header.add(rightPanel, BorderLayout.EAST);

        initializeHeaderToolbar(header);

        return header;
    }

    private void initializeHeaderToolbar(JPanel header) {
        JPanel toolBar = new JPanel();
        toolBar.setBorder(UIUtils.createEmptyBorder(4));
        toolBar.setLayout(new BoxLayout(toolBar, BoxLayout.X_AXIS));
        toolBar.setOpaque(false);

        JButton reloadButton = new JButton("Reload", JIPipe.RESOURCES.getIcon16("actions/view-refresh.png"));
        reloadButton.addActionListener(e -> refresh());
        reloadButton.setOpaque(false);
        reloadButton.setBackground(new Color(0, 0, 0, 0));
        toolBar.add(reloadButton);
        toolBar.add(Box.createHorizontalStrut(4));

        JButton configureButton = new JButton("Configure", JIPipe.RESOURCES.getIcon16("actions/configure.png"));
        configureButton.addActionListener(e -> {
            JIPipeDesktopStatisticsConfigurationUI dialog = new JIPipeDesktopStatisticsConfigurationUI(getDesktopProjectWorkbench().getWindow());
            dialog.setVisible(true);
            if (dialog.isConfigured()) {
                refresh();
            }
        });
        configureButton.setOpaque(false);
        configureButton.setBackground(new Color(0, 0, 0, 0));
        toolBar.add(configureButton);
        toolBar.add(Box.createHorizontalStrut(4));

        header.add(toolBar, BorderLayout.SOUTH);
    }

    private JComponent createCenterPanel() {
        JPanel islandPanel = new JPanel(new BorderLayout());
        islandPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8, 8, 8, 8),
                new RoundedLineBorder(UIUtils.getControlBorderColor(), 1, 4)
        ));

        JLabel titleLabel = new JLabel("Collected statistics", JIPipe.RESOURCES.getIcon32("status/starred.png"), JLabel.LEFT);
        titleLabel.setBorder(UIUtils.createEmptyBorder(8));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, ThemeUtils.getCurrentStyle().getFontSizeLarge()));

        JToolBar titleBar = new JToolBar();
        titleBar.setFloatable(false);
        titleBar.add(titleLabel);
        titleBar.add(Box.createHorizontalGlue());
        titleBar.add(Box.createHorizontalStrut(8));
        titleBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIUtils.getControlBorderColor()));
        titleBar.setBackground(ColorUtils.mix(ThemeUtils.getCurrentStyle().getPrimaryColor(),
                ColorUtils.scaleHSV(UIManager.getColor("Panel.background"), 1, 1, 0.98f), 0.92));

        islandPanel.add(titleBar, BorderLayout.NORTH);

        cardsContainer = new JPanel();
        cardsContainer.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 8));
        cardsContainer.setBackground(UIManager.getColor("Panel.background"));

        JScrollPane scrollPane = new JScrollPane(cardsContainer);
        scrollPane.setBorder(BorderFactory::getEmptyBorder);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        islandPanel.add(scrollPane, BorderLayout.CENTER);

        return islandPanel;
    }

    public void refresh() {
        remove(headerPanel);
        headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        cardsContainer.removeAll();

        JIPipeStatisticsServiceComponent service = JIPipe.getInstance().getStatistics();
        for (JIPipeStatisticsItem item : service.getRegistry().getItems()) {
            JPanel card = createCard(item, service);
            cardsContainer.add(card);
        }

        cardsContainer.revalidate();
        cardsContainer.repaint();
        revalidate();
        repaint();
    }

    private JPanel createCard(JIPipeStatisticsItem item, JIPipeStatisticsServiceComponent service) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(new RoundedLineBorder(UIUtils.getControlBorderColor(), 1, 4));

        JLabel titleLabel = new JLabel(item.getName());
        titleLabel.setIcon(JIPipe.RESOURCES.getIcon32("status/starred.png"));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, ThemeUtils.getCurrentStyle().getFontSizeLarge()));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        card.add(titleLabel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);

        JsonNode serialized = item.serialize();
        String valueStr = formatValue(serialized);
        JLabel valueLabel = new JLabel("<html>" + valueStr + "</html>");
        contentPanel.add(valueLabel, BorderLayout.NORTH);

        if (item.isTimeTracked()) {
            ChartPanel chartPanel = createHistoryChart(item, service);
            if (chartPanel != null) {
                chartPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
                contentPanel.add(chartPanel, BorderLayout.CENTER);
            }
        }

        card.add(contentPanel, BorderLayout.CENTER);

        card.setPreferredSize(new Dimension(300, 250));
        card.setMaximumSize(new Dimension(300, 300));

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
                if (sb.length() > 0) sb.append("<br>");
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
        panel.setPreferredSize(new Dimension(260, 80));
        panel.setMinimumDrawWidth(0);
        panel.setMaximumDrawWidth(Integer.MAX_VALUE);
        panel.setMinimumDrawHeight(0);
        panel.setMaximumDrawHeight(Integer.MAX_VALUE);
        return panel;
    }
}
