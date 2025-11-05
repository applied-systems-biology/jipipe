package org.hkijena.jipipe.desktop.app.customizer;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.JIPipeDesktopGraphCanvasGrid;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.JIPipeDesktopGraphCanvasResources;
import org.hkijena.jipipe.desktop.commons.components.icons.SolidColorIcon;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernThemeStyle;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopUITheme;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.ScaledIcon;

import javax.swing.*;
import java.awt.*;

public class ThemePreviewPanel extends JPanel {
    private final PreviewComponent previewTabBar = new PreviewComponent();
    private final PreviewComponent previewSplitLeft = new PreviewComponent();
    private final PreviewComponent previewSplitRight = new PreviewComponent();
    private final PreviewComponent previewTabActive = new PreviewComponent("Active tab");
    private final PreviewComponent previewTabInactive1 = new PreviewComponent("Inactive tab");
    private final PreviewComponent previewTabInactive2 = new PreviewComponent("Inactive tab");
    private final PreviewComponent previewDockActive = new PreviewComponent("Active");
    private final PreviewComponent previewDockInactive = new PreviewComponent("Inactive");
    private final JLabel previewLabelTiny = new JLabel("Tiny text");
    private final JLabel previewLabelSmall = new JLabel("Small text");
    private final JLabel previewLabelNormal = new JLabel("Normal text");
    private final JLabel previewLabelLarge = new JLabel("Large text");
    private final JLabel previewLabelHuge = new JLabel("Huge text");
    private final JLabel previewLabelColorForeground = new JLabel("Text color foreground");
    private final JLabel previewLabelColorMuted = new JLabel("Text color muted");
    private final JLabel previewLabelColorInverted = new JLabel("Text color inverted");
    private final JLabel previewLabelColorMutedInverted = new JLabel("Text color muted inverted");
    private final JLabel previewLabelColorLink = new JLabel("Text color link");
    private final JLabel previewLabelColorPrimary = new JLabel("Primary color");
    private final JLabel previewLabelColorSecondary = new JLabel("Secondary color");
    private final JLabel previewLabelColorSuccess = new JLabel("Success color");
    private final JLabel previewLabelColorDanger = new JLabel("Danger color");
    private final JLabel previewLabelColorWarning = new JLabel("Warning color");
    private final PreviewComponent previewHeaderPanelActive = new PreviewComponent("Header active");
    private final PreviewComponent previewHeaderPanelInactive = new PreviewComponent("Header inactive");
    private final PreviewComponent previewButton = new PreviewComponent("Button");
    private final PreviewComponent previewTextField = new PreviewComponent("Text field / form");
    private final PreviewComponent previewTextFieldDisabled = new PreviewComponent("Text field / form (disabled)");
    private final NodeViewportComponent previewViewport = new NodeViewportComponent(this);
    private final JPanel previewDockPanel = new JPanel();
    private JIPipeDesktopUITheme theme = JIPipeDesktopUITheme.Modern;
    private JIPipeDesktopModernThemeStyle themeStyle = new JIPipeDesktopModernThemeStyle();
    private float scale = 1;

    public ThemePreviewPanel() {
        initialize();
        updatePreview();
    }

    private void initialize() {
        setOpaque(true);
        setLayout(new BorderLayout(8, 8));
        add(previewTabBar, BorderLayout.NORTH);

        // Dock panel
        previewDockPanel.setOpaque(false);
        previewDockPanel.setLayout(new BoxLayout(previewDockPanel, BoxLayout.Y_AXIS));

        previewDockActive.centerLabelTopBottom();
        previewDockInactive.centerLabelTopBottom();
        previewDockPanel.add(previewDockActive);
        previewDockPanel.add(previewDockInactive);
        previewDockPanel.add(Box.createVerticalGlue());
        previewDockPanel.setBorder(UIUtils.createEmptyBorder(8));

        // Split panel (islands)
        JPanel previewSplitPanel = new JPanel(new BorderLayout(8, 8));
        previewSplitPanel.setBorder(UIUtils.createEmptyBorder(8));
        previewSplitPanel.setOpaque(false);
        previewSplitPanel.add(previewSplitLeft, BorderLayout.WEST);
        previewSplitPanel.add(previewSplitRight, BorderLayout.CENTER);
        add(UIUtils.makeNonOpaque(UIUtils.borderNSEWC(null, null, null, previewDockPanel, previewSplitPanel)), BorderLayout.CENTER);

        // Tab panel
        previewTabBar.setLayout(new BoxLayout(previewTabBar, BoxLayout.X_AXIS));
        previewTabBar.add(previewTabActive);
        previewTabBar.add(previewTabInactive1);
        previewTabBar.add(previewTabInactive2);

        // Text sizes
        previewSplitLeft.setLayout(new BoxLayout(previewSplitLeft, BoxLayout.Y_AXIS));
        previewSplitLeft.add(previewLabelTiny);
        previewSplitLeft.add(previewLabelSmall);
        previewSplitLeft.add(previewLabelNormal);
        previewSplitLeft.add(previewLabelLarge);
        previewSplitLeft.add(previewLabelHuge);

        // Text colors
        previewSplitLeft.add(Box.createVerticalStrut(32));
        previewSplitLeft.add(previewLabelColorForeground);
        previewSplitLeft.add(previewLabelColorMuted);
        previewLabelColorInverted.setOpaque(true);
        previewSplitLeft.add(previewLabelColorInverted);
        previewSplitLeft.add(previewLabelColorMutedInverted);
        previewSplitLeft.add(previewLabelColorLink);

        // Basic colors
        previewSplitLeft.add(Box.createVerticalStrut(32));
        previewSplitLeft.add(previewLabelColorPrimary);
        previewSplitLeft.add(previewLabelColorSecondary);
        previewSplitLeft.add(previewLabelColorSuccess);
        previewSplitLeft.add(previewLabelColorWarning);
        previewSplitLeft.add(previewLabelColorDanger);

        // Button & headers
        previewSplitLeft.add(Box.createVerticalStrut(32));

        previewSplitLeft.add(previewHeaderPanelInactive);
        previewHeaderPanelInactive.leftLabel();

        previewSplitLeft.add(Box.createVerticalStrut(8));

        previewSplitLeft.add(previewButton);

        // Viewport
        previewSplitRight.setLayout(new BorderLayout());
        previewSplitRight.add(previewViewport, BorderLayout.CENTER);

        // Forms below viewport
        JPanel previewSplitRightForms = new JPanel();
        previewSplitRightForms.setOpaque(false);
        previewSplitRightForms.setLayout(new BoxLayout(previewSplitRightForms, BoxLayout.Y_AXIS));

        previewSplitRightForms.add(Box.createVerticalStrut(16));

        previewSplitRightForms.add(previewHeaderPanelActive);
        previewHeaderPanelActive.leftLabel();

        previewSplitRightForms.add(Box.createVerticalStrut(8));
        previewSplitRightForms.add(previewTextField);
        previewSplitRightForms.add(Box.createVerticalStrut(8));
        previewSplitRightForms.add(previewTextFieldDisabled);


        previewSplitRight.add(previewSplitRightForms, BorderLayout.SOUTH);

    }

    private void updatePreview() {
        setBackground(themeStyle.getWindowBackground());

        // Update scales
        previewTabActive.setScale(scale);
        previewTabInactive1.setScale(scale);
        previewTabInactive2.setScale(scale);
        previewTabActive.getLabel().setForeground(themeStyle.getTextForeground());
        previewTabInactive1.getLabel().setForeground(themeStyle.getTextForeground());
        previewTabInactive2.getLabel().setForeground(themeStyle.getTextForeground());

        // Update fonts preview
        previewLabelTiny.setFont(createScaledTinyFont());
        previewLabelSmall.setFont(createScaledSmallFont());
        previewLabelNormal.setFont(createScaledNormalFont());
        previewLabelLarge.setFont(createScaledLargeFont());
        previewLabelHuge.setFont(createScaledHugeFont());

        // Update text colors preview
        setLabelsForeground(themeStyle.getTextForeground(),
                previewLabelTiny,
                previewLabelSmall,
                previewLabelNormal,
                previewLabelLarge,
                previewLabelHuge);

        previewLabelColorForeground.setFont(createScaledNormalFont());
        previewLabelColorInverted.setFont(createScaledNormalFont());
        previewLabelColorLink.setFont(createScaledNormalFont());
        previewLabelColorMuted.setFont(createScaledNormalFont());
        previewLabelColorMutedInverted.setFont(createScaledNormalFont());

        previewLabelColorForeground.setForeground(themeStyle.getTextForeground());
        previewLabelColorInverted.setBackground(themeStyle.getWindowBackground());
        previewLabelColorInverted.setForeground(themeStyle.getTextForegroundInverted());
        previewLabelColorLink.setForeground(themeStyle.getTextLink());
        previewLabelColorMuted.setForeground(themeStyle.getTextMuted());
        previewLabelColorMutedInverted.setForeground(themeStyle.getTextMutedInverted());

        // Update basic colors preview
        setLabelsForeground(themeStyle.getTextForeground(),
                previewLabelColorPrimary,
                previewLabelColorSecondary,
                previewLabelColorSuccess,
                previewLabelColorWarning,
                previewLabelColorDanger);
        previewLabelColorPrimary.setIcon(createColorIcon(themeStyle.getPrimaryColor(), 16));
        previewLabelColorSecondary.setIcon(createColorIcon(themeStyle.getSecondaryColor(), 16));
        previewLabelColorSuccess.setIcon(createColorIcon(themeStyle.getSuccessColor(), 16));
        previewLabelColorWarning.setIcon(createColorIcon(themeStyle.getWarningColor(), 16));
        previewLabelColorDanger.setIcon(createColorIcon(themeStyle.getDangerColor(), 16));


        // Update panel previews
        previewSplitLeft.setCornerRadius((int) (themeStyle.getIslandsCornerRadius() * scale));
        previewSplitRight.setCornerRadius((int) (themeStyle.getIslandsCornerRadius() * scale));
        previewSplitLeft.setBackgroundColor(themeStyle.getPanelBackground());
        previewSplitRight.setBackgroundColor(themeStyle.getPanelBackground());
        previewSplitLeft.setBorderColor(themeStyle.isIslandsDrawBorder() ? themeStyle.getIslandsBorderColor() : null);
        previewSplitRight.setBorderColor(themeStyle.isIslandsDrawBorder() ? themeStyle.getIslandsBorderColor() : null);

        // Update tab designs
        previewTabBar.setBackgroundColor(themeStyle.getWindowBackground());
        previewTabActive.setBackgroundColor(themeStyle.getTabSelectedBackground());
        previewTabActive.setBorderColor(themeStyle.getTabSelectedHighlight());

        previewTabActive.getLabel().setIcon(getIcon16("actions/configure.png"));
        previewTabInactive1.getLabel().setIcon(getIcon16("actions/help-info.png"));
        previewTabInactive2.getLabel().setIcon(getIcon16("actions/graph-compartments.png"));

        // Update dock panel
        previewDockPanel.setMinimumSize(new Dimension((int) (92 * scale), 32));
        previewDockPanel.setPreferredSize(new Dimension((int) (92 * scale), 32));
        previewDockPanel.setMaximumSize(new Dimension((int) (92 * scale), 32));

        previewDockActive.setBackgroundColor(themeStyle.getButtonToggled());
        previewDockActive.getLabel().setForeground(themeStyle.getIconBaseColor());
        previewDockActive.getLabel().setFont(createScaledTinyFont());
        previewDockActive.getLabel().setIcon(getIcon24("actions/configure.png"));
        previewDockActive.setSizeMinMaxPreferred(90, 64, scale);

        previewDockInactive.setBackgroundColor(themeStyle.getWindowBackground());
        previewDockInactive.getLabel().setForeground(themeStyle.getIconBaseColor());
        previewDockInactive.getLabel().setFont(createScaledTinyFont());
        previewDockInactive.getLabel().setIcon(getIcon24("actions/configure.png"));
        previewDockInactive.setSizeMinMaxPreferred(90, 64, scale);

        // Update header panels
        previewHeaderPanelActive.getLabel().setIcon(getIcon16("actions/configure.png"));
        previewHeaderPanelActive.getLabel().setFont(new Font(Font.DIALOG, Font.BOLD, (int) (scale * themeStyle.getFontSizeNormal())));
        previewHeaderPanelActive.setCornerRadius((int) (4 * scale));
        previewHeaderPanelActive.setBackgroundColor(themeStyle.getCategoryBackground());
        previewHeaderPanelActive.setBorderColor(themeStyle.getCategoryBorder());
        previewHeaderPanelActive.setSizeMinMaxHeightPreferred(32, scale);
        previewHeaderPanelActive.getLabel().setForeground(themeStyle.getTextForeground());

        previewHeaderPanelInactive.getLabel().setIcon(getIcon16("actions/configure.png"));
        previewHeaderPanelInactive.getLabel().setFont(new Font(Font.DIALOG, Font.BOLD, (int) (scale * themeStyle.getFontSizeNormal())));
        previewHeaderPanelInactive.setCornerRadius((int) (4 * scale));
        previewHeaderPanelInactive.setBorderColor(themeStyle.getCategoryBorder());
        previewHeaderPanelInactive.setSizeMinMaxHeightPreferred(32, scale);
        previewHeaderPanelInactive.getLabel().setForeground(themeStyle.getTextForeground());

        // Update button
        previewButton.getLabel().setIcon(getIcon16("actions/dialog-ok.png"));
        previewButton.setScale(scale);
        previewButton.setCornerRadius((int) (scale * 5));
        previewButton.setBorderColor(themeStyle.getBorderColor());
        previewButton.setSizeMinMaxHeightPreferred(42, scale);
        previewButton.getLabel().setForeground(themeStyle.getTextForeground());

        // Update text field/form
        previewTextField.setScale(scale);
        previewTextField.setBackgroundColor(themeStyle.getFormBackground());
        previewTextField.getLabel().setForeground(themeStyle.getFormForeground());
        previewTextField.setBorderColor(themeStyle.getBorderColor());
        previewTextField.setCornerRadius((int) (scale * 5));

        previewTextFieldDisabled.setScale(scale);
        previewTextFieldDisabled.setBackgroundColor(themeStyle.getFormDisabledBackground());
        previewTextFieldDisabled.getLabel().setForeground(themeStyle.getFormForeground());
        previewTextFieldDisabled.setBorderColor(themeStyle.getBorderColor());
        previewTextFieldDisabled.setCornerRadius((int) (scale * 5));

        revalidate();
        repaint(50);
    }

    private Icon createColorIcon(Color color, int size) {
        return new SolidColorIcon((int) (scale * size), (int) (scale * size), color, color);
    }

    private void setLabelsForeground(Color foreground, JLabel... labels) {
        for (JLabel label : labels) {
            label.setForeground(foreground);
        }
    }

    private Icon getIcon24(String name) {
        if (themeStyle.getBrightness() != ThemeUtils.getCurrentStyle().getBrightness()) {
            return new ScaledIcon(JIPipe.RESOURCES.getIcon24Inverted(name), scale);
        } else {
            return new ScaledIcon(JIPipe.RESOURCES.getIcon24(name), scale);
        }
    }

    private Font createScaledHugeFont() {
        return new Font(Font.DIALOG, Font.PLAIN, (int) (themeStyle.getFontSizeHuge() * scale));
    }

    private Font createScaledLargeFont() {
        return new Font(Font.DIALOG, Font.PLAIN, (int) (themeStyle.getFontSizeLarge() * scale));
    }

    private Font createScaledNormalFont() {
        return new Font(Font.DIALOG, Font.PLAIN, (int) (themeStyle.getFontSizeNormal() * scale));
    }

    private Font createScaledTinyFont() {
        return new Font(Font.DIALOG, Font.PLAIN, (int) (themeStyle.getFontSizeTiny() * scale));
    }

    private Font createScaledSmallFont() {
        return new Font(Font.DIALOG, Font.PLAIN, (int) (themeStyle.getFontSizeTiny() * scale));
    }

    private Icon getIcon16(String name) {
        if (themeStyle.getBrightness() != ThemeUtils.getCurrentStyle().getBrightness()) {
            return new ScaledIcon(JIPipe.RESOURCES.getIcon16Inverted(name), scale);
        } else {
            return new ScaledIcon(JIPipe.RESOURCES.getIcon16(name), scale);
        }
    }

    public JIPipeDesktopUITheme getTheme() {
        return theme;
    }

    public void setTheme(JIPipeDesktopUITheme theme) {
        this.theme = theme;
        updatePreview();
    }

    public JIPipeDesktopModernThemeStyle getThemeStyle() {
        return themeStyle;
    }

    public void setThemeStyle(JIPipeDesktopModernThemeStyle themeStyle) {
        this.themeStyle = themeStyle;
        updatePreview();
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
        updatePreview();
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (theme != JIPipeDesktopUITheme.Modern) {
            g2d.setFont(new Font(Font.DIALOG, Font.PLAIN, (int) (themeStyle.getFontSizeHuge() * scale)));
            FontMetrics fontMetrics = g2d.getFontMetrics();
            String text = "No preview available for this theme";
            int stringWidth = fontMetrics.stringWidth(text);
            UIUtils.drawStringVerticallyCentered(g2d, text, getWidth() / 2 - stringWidth / 2, getHeight() / 2, fontMetrics);
        } else {
            super.paint(g);
        }
    }

    public static class NodeViewportComponent extends JPanel {
        private final ThemePreviewPanel themePreviewPanel;

        public NodeViewportComponent(ThemePreviewPanel themePreviewPanel) {
            this.themePreviewPanel = themePreviewPanel;
        }

        private static void paintNode(float scale, JIPipeDesktopModernThemeStyle style, Graphics2D g2d, int nodeX, int nodeY, int nodeWidth, float hue, boolean selected) {
            int nodeCellHeight = (int) (JIPipeDesktopGraphCanvasGrid.GRID_HEIGHT * scale);
            int nodeHeight = 3 * nodeCellHeight;

            Color nodeBorderColor = Color.getHSBColor(hue, style.getNodeBorderSaturation(), style.getNodeBorderBrightness());
            Color nodeFillColor = Color.getHSBColor(hue, style.getNodeFillSaturation(), style.getNodeFillBrightness());
            Color nodeSlotColor = style.getNodeSlotBackground();

            JIPipeDesktopGraphCanvasResources.DROP_SHADOW_BORDER.paint(g2d,
                    nodeX - 3,
                    nodeY - 3,
                    nodeWidth + 8,
                    nodeHeight + 8);

            g2d.setPaint(nodeFillColor);
            g2d.fillRect(nodeX, nodeY, nodeWidth, nodeHeight);

            g2d.setPaint(nodeSlotColor);
            g2d.fillRect(nodeX, nodeY, nodeWidth, nodeCellHeight);

            g2d.setPaint(nodeSlotColor);
            g2d.fillRect(nodeX, nodeY + nodeCellHeight * 2, nodeWidth, nodeCellHeight);

            g2d.setPaint(nodeBorderColor);
            g2d.drawRect(nodeX, nodeY, nodeWidth, nodeCellHeight);
            g2d.drawRect(nodeX, nodeY + nodeCellHeight * 2, nodeWidth, nodeCellHeight);
            g2d.drawRect(nodeX, nodeY, nodeWidth, nodeHeight);

            Image dataTypeIconImage = JIPipe.RESOURCES.getIcon16("data-types/data-type.png").getImage();
            int iconStart = (int) ((JIPipeDesktopGraphCanvasGrid.GRID_HEIGHT * scale) / 2 - (16.0 / 2) * scale);
            g2d.drawImage(dataTypeIconImage, nodeX + iconStart, nodeY + iconStart, (int) (scale * 16), (int) (scale * 16), null);
            g2d.drawImage(dataTypeIconImage, nodeX + iconStart, nodeY + nodeCellHeight * 2 + iconStart, (int) (scale * 16), (int) (scale * 16), null);

            g2d.setPaint(style.getTextForeground());

            Font mainFont = new Font(Font.DIALOG, Font.PLAIN, (int) (scale * style.getFontSizeNormal()));
            FontMetrics mainFontMetrics = g2d.getFontMetrics(mainFont);
            Font secondaryFont = new Font(Font.DIALOG, Font.PLAIN, (int) (scale * style.getFontSizeSmall()));
            FontMetrics secondaryFontMetrics = g2d.getFontMetrics(secondaryFont);

            g2d.setFont(secondaryFont);
            UIUtils.drawStringVerticallyCentered(g2d, "Input", (int) (nodeX + scale * 25), nodeY + nodeCellHeight / 2, secondaryFontMetrics);
            UIUtils.drawStringVerticallyCentered(g2d, "Output", (int) (nodeX + scale * 25), nodeY + nodeCellHeight * 2 + nodeCellHeight / 2, secondaryFontMetrics);

            g2d.drawImage(dataTypeIconImage, (int) (nodeX + 32 * scale), nodeY + nodeCellHeight + iconStart, (int) (scale * 16), (int) (scale * 16), null);
            g2d.setFont(mainFont);
            UIUtils.drawStringVerticallyCentered(g2d, "Node", (int) (nodeX + scale * 55), nodeY + nodeCellHeight + nodeCellHeight / 2, mainFontMetrics);

            if (selected) {
                g2d.setStroke(JIPipeDesktopGraphCanvasResources.STROKE_SELECTION);
                g2d.setColor(style.getNodeHighlightBorder());
                g2d.drawRect(nodeX - 4, nodeY - 4, nodeWidth + 8, nodeHeight + 8);
            }
        }

        @Override
        public void paint(Graphics g) {
            final JIPipeDesktopModernThemeStyle style = themePreviewPanel.themeStyle;
            final float scale = themePreviewPanel.scale;
            Graphics2D g2d = (Graphics2D) g;

            // Viewport
            g2d.setPaint(style.getViewportBackground());
            g2d.fillRect(0, 0, getWidth() - 1, getHeight() - 1);


            // Grid
            int nodeCellHeight = (int) (JIPipeDesktopGraphCanvasGrid.GRID_HEIGHT * scale);

            // Edge
            paintEdge(g2d, scale, nodeCellHeight * 2, 0, nodeCellHeight * 2, nodeCellHeight * 3, style.getEdgeBorderColorConvert());
            paintEdge(g2d, scale, nodeCellHeight * 4, nodeCellHeight * 3, nodeCellHeight * 4, nodeCellHeight * 8, style.getEdgeBorderColorDefault());
            g2d.setStroke(JIPipeDesktopGraphCanvasResources.STROKE_UNIT);

            // Nodes
            paintNode(scale, style, g2d, 25, nodeCellHeight, (int) (150 * scale), 186.0f / 360.0f, false);
            paintNode(scale, style, g2d, 50, nodeCellHeight + nodeCellHeight * 3 + nodeCellHeight * 3, (int) (150 * scale), 0, true);

            // Scrollbar
            int scrollBarSize = (int) (12 * scale);
            int verticalScrollBarStart = getHeight() / 6;
            int verticalScrollBarEnd = Math.max(verticalScrollBarStart, getHeight() - 2 * verticalScrollBarStart);
            g2d.setPaint(style.getScrollBarThumb());

            g2d.fillRect(getWidth() - scrollBarSize - 1, verticalScrollBarStart, scrollBarSize, verticalScrollBarEnd - verticalScrollBarStart);
        }

        private void paintEdge(Graphics2D g2d, float scale, int x1, int y1, int x2, int y2, Color borderColor) {
            final JIPipeDesktopModernThemeStyle style = themePreviewPanel.themeStyle;
            BasicStroke strokeBorder = new BasicStroke((int) Math.max(1, scale * 4) + 2, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER);
            BasicStroke strokeFill = new BasicStroke((int) Math.max(1, scale * 4), BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER);

            g2d.setPaint(borderColor);
            g2d.setStroke(strokeBorder);
            g2d.drawLine(x1, y1, x2, y2);

            g2d.setPaint(style.getPanelBackground());
            g2d.setStroke(strokeFill);
            g2d.drawLine(x1, y1, x2, y2);
        }
    }

    public static class PreviewComponent extends JPanel {

        private final JLabel label = new JLabel();
        private int cornerRadius = 8;
        private Color borderColor;
        private Color backgroundColor;
        private Stroke borderStroke = new BasicStroke(1);

        public PreviewComponent() {
            initialize();
        }

        public PreviewComponent(String text) {
            initialize();
            add(label, BorderLayout.CENTER);
            label.setText(text);
        }

        private void initialize() {
            setOpaque(false);
            setLayout(new BorderLayout());
            setBorder(UIUtils.createEmptyBorder(8));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;

            if (backgroundColor != null) {
                g.setColor(backgroundColor);
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);
            }
            if (borderColor != null) {
                g.setColor(borderColor);
                g2d.setStroke(borderStroke);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);
            }
        }

        public JLabel getLabel() {
            return label;
        }

        public void setScale(float scale) {
            label.setFont(new Font(Font.DIALOG, Font.PLAIN, (int) (scale * 12)));
        }

        public int getCornerRadius() {
            return cornerRadius;
        }

        public void setCornerRadius(int cornerRadius) {
            this.cornerRadius = cornerRadius;
        }

        public Color getBorderColor() {
            return borderColor;
        }

        public void setBorderColor(Color borderColor) {
            this.borderColor = borderColor;
        }

        public Color getBackgroundColor() {
            return backgroundColor;
        }

        public void setBackgroundColor(Color backgroundColor) {
            this.backgroundColor = backgroundColor;
        }

        public void centerLabel() {
            label.setHorizontalAlignment(JLabel.CENTER);
            label.setVerticalAlignment(JLabel.CENTER);
            label.setHorizontalTextPosition(JLabel.CENTER);
            label.setVerticalTextPosition(JLabel.BOTTOM);
        }

        public void centerLabelTopBottom() {
            label.setHorizontalAlignment(JLabel.CENTER);
            label.setVerticalAlignment(JLabel.CENTER);
            label.setHorizontalTextPosition(JLabel.CENTER);
            label.setVerticalTextPosition(JLabel.BOTTOM);
        }

        public void setSizeMinMaxPreferred(int width, int height, float scale) {
            setPreferredSize(new Dimension((int) (width * scale), (int) (height * scale)));
            setMinimumSize(new Dimension((int) (width * scale), (int) (height * scale)));
            setMaximumSize(new Dimension((int) (width * scale), (int) (height * scale)));
        }

        public void setSizeMinMaxHeightPreferred(int height, float scale) {
//            setPreferredSize(new Dimension(Short.MAX_VALUE, (int) (height * scale)));
            setMinimumSize(new Dimension(32, (int) (height * scale)));
            setMaximumSize(new Dimension(Short.MAX_VALUE, (int) (height * scale)));
        }

        public void leftLabel() {
            label.setHorizontalAlignment(JLabel.LEFT);
            label.setVerticalAlignment(JLabel.CENTER);
        }

        public Stroke getBorderStroke() {
            return borderStroke;
        }

        public void setBorderStroke(Stroke borderStroke) {
            this.borderStroke = borderStroke;
        }
    }
}
