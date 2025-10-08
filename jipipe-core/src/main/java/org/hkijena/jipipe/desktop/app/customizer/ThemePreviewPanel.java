package org.hkijena.jipipe.desktop.app.customizer;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernThemeStyle;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopUITheme;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class ThemePreviewPanel extends JPanel {
    private JIPipeDesktopUITheme theme = JIPipeDesktopUITheme.Modern;
    private JIPipeDesktopModernThemeStyle themeStyle = new  JIPipeDesktopModernThemeStyle();
    private float scale = 1;

    private final PreviewComponent previewTabBar = new PreviewComponent();
    private final PreviewComponent previewSplitLeft = new PreviewComponent();
    private final PreviewComponent previewSplitRight = new PreviewComponent();
    private final PreviewComponent previewTabActive = new PreviewComponent("Active tab");
    private final PreviewComponent previewTabInactive1 = new PreviewComponent("Inactive tab");
    private final PreviewComponent previewTabInactive2 = new PreviewComponent("Inactive tab");

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

    public ThemePreviewPanel() {
        initialize();
        updatePreview();
    }

    private void initialize() {
        setOpaque(true);
        setLayout(new BorderLayout(8,8));
        add(previewTabBar, BorderLayout.NORTH);
        JPanel previewSplitPanel = new JPanel(new BorderLayout(8,8));
        previewSplitPanel.setBorder(UIUtils.createEmptyBorder(8));
        previewSplitPanel.setOpaque(false);
        previewSplitPanel.add(previewSplitLeft, BorderLayout.WEST);
        previewSplitPanel.add(previewSplitRight, BorderLayout.CENTER);
        add(previewSplitPanel, BorderLayout.CENTER);

        previewTabBar.setLayout(new BoxLayout(previewTabBar, BoxLayout.X_AXIS));
        previewTabBar.add(previewTabActive);
        previewTabBar.add(previewTabInactive1);
        previewTabBar.add(previewTabInactive2);

        previewSplitLeft.setLayout(new BoxLayout(previewSplitLeft, BoxLayout.Y_AXIS));
        previewSplitLeft.add(previewLabelTiny);
        previewSplitLeft.add(previewLabelSmall);
        previewSplitLeft.add(previewLabelNormal);
        previewSplitLeft.add(previewLabelLarge);
        previewSplitLeft.add(previewLabelHuge);
        previewSplitLeft.add(Box.createVerticalStrut(32));
        previewSplitLeft.add(previewLabelColorForeground);
        previewSplitLeft.add(previewLabelColorMuted);
        previewSplitLeft.add(previewLabelColorInverted);
        previewSplitLeft.add(previewLabelColorMutedInverted);
        previewSplitLeft.add(previewLabelColorLink);
    }

    private void updatePreview() {
        setBackground(themeStyle.getWindowBackground());

        // Update scales
        previewTabActive.setScale(scale);
        previewTabInactive1.setScale(scale);
        previewTabInactive2.setScale(scale);

        // Update fonts
        previewLabelTiny.setFont(new Font(Font.DIALOG, Font.PLAIN, (int) (themeStyle.getFontSizeTiny() * scale)));
        previewLabelSmall.setFont(new Font(Font.DIALOG, Font.PLAIN, (int) (themeStyle.getFontSizeSmall() * scale)));
        previewLabelNormal.setFont(new Font(Font.DIALOG, Font.PLAIN, (int) (themeStyle.getFontSizeNormal() * scale)));
        previewLabelLarge.setFont(new Font(Font.DIALOG, Font.PLAIN, (int) (themeStyle.getFontSizeLarge() * scale)));
        previewLabelHuge.setFont(new Font(Font.DIALOG, Font.PLAIN, (int) (themeStyle.getFontSizeHuge() * scale)));

        previewLabelColorForeground.setFont(new Font(Font.DIALOG, Font.PLAIN, (int) (themeStyle.getFontSizeNormal() * scale)));
        previewLabelColorInverted.setFont(new Font(Font.DIALOG, Font.PLAIN, (int) (themeStyle.getFontSizeNormal() * scale)));
        previewLabelColorLink.setFont(new Font(Font.DIALOG, Font.PLAIN, (int) (themeStyle.getFontSizeNormal() * scale)));
        previewLabelColorMuted.setFont(new Font(Font.DIALOG, Font.PLAIN, (int) (themeStyle.getFontSizeNormal() * scale)));
        previewLabelColorMutedInverted.setFont(new Font(Font.DIALOG, Font.PLAIN, (int) (themeStyle.getFontSizeNormal() * scale)));

        // Update panel previews
        previewSplitLeft.setBackgroundColor(themeStyle.getPanelBackground());
        previewSplitRight.setBackgroundColor(themeStyle.getPanelBackground());

        // Update tab designs
        previewTabBar.setBackgroundColor(themeStyle.getWindowBackground());
        previewTabActive.setBackgroundColor(themeStyle.getTabSelectedBackground());
        previewTabActive.setBorderColor(themeStyle.getTabSelectedHighlight());

        previewTabActive.getLabel().setIcon(getIcon16("actions/configure.png"));
        previewTabInactive1.getLabel().setIcon(getIcon16("actions/help-info.png"));
        previewTabInactive2.getLabel().setIcon(getIcon16("actions/graph-compartments.png"));

        revalidate();
        repaint(50);
    }

    private Icon getIcon16(String name) {
        if(themeStyle.getBrightness() != ThemeUtils.getCurrentStyle().getBrightness()) {
            return JIPipe.RESOURCES.getIcon16Inverted(name);
        }
        else {
            return JIPipe.RESOURCES.getIcon16(name);
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
        if(theme != JIPipeDesktopUITheme.Modern) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.drawString("No preview available", 0,0);
        }
        else {
            super.paint(g);
        }
    }

    public static class PreviewComponent extends JPanel {

        private final JLabel label = new JLabel();
        private int cornerRadius = 8;
        private Color borderColor;
        private Color backgroundColor;

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

           if(backgroundColor != null) {
               g.setColor(backgroundColor);
               g.fillRoundRect(0, 0, getWidth() -1, getHeight() -1, cornerRadius, cornerRadius);
           }
           if(borderColor != null) {
               g.setColor(borderColor);
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
    }
}
