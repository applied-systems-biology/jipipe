package org.hkijena.jipipe.desktop.commons.theme;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class JIPipeDesktopModernThemeStyle {

    @JsonProperty("name")
    private String name = "Light";

    @JsonProperty("brightness")
    private JIPipeDesktopUIThemeBrightness brightness = JIPipeDesktopUIThemeBrightness.Light;

    /*
     * Basic colors
     */

    @JsonProperty("primary")
    private Color primaryColor = new Color(0x8EBFEF);
    @JsonProperty("secondary")
    private Color secondaryColor = new Color(0xaa87de);
    @JsonProperty("success")
    private Color successColor = new Color(0x369650);
    @JsonProperty("danger")
    private Color dangerColor = new Color(0xE55765);
    @JsonProperty("warning")
    private Color warningColor = new Color(0xE58457);

    /*
     * Text colors
     */

    @JsonProperty("foreground")
    private Color textForeground = new Color(0);
    @JsonProperty("foreground-secondary")
    private Color textMuted = new Color(0x6c707e);
    @JsonProperty("foreground-inverted")
    private Color textForegroundInverted = new Color(0xFFFFFF);
    @JsonProperty("foreground-secondary-inverted")
    private Color textMutedInverted = new Color(0xebecf0);
    @JsonProperty("caret")
    private Color caret = new Color(0);

    /*
     * Panels/windows
     */
    @JsonProperty("window-background")
    private Color windowBackground = new Color(0xEBECF0);
    @JsonProperty("panel-background")
    private Color panelBackground = new Color(0xFFFFFF);
    @JsonProperty("menu-background")
    private Color menuBackground = new Color(0xFFFFFF);
    @JsonProperty("border-color")
    private Color borderColor = new Color(0xdedee2);

    /*
     * Scroll bar
     */
    @JsonProperty("scrollbar-thumb")
    private Color scrollBarThumb = new Color(0xd7d7d7);

    /*
     * Tab panel
     */
    @JsonProperty("tab-selected-background")
    private Color tabSelectedBackground = new Color(0xE1EAFF);
    @JsonProperty("tab-selected-highlight")
    private Color tabSelectedHighlight = new Color(0xa0bdf8);

    /*
     * Selection
     */
    @JsonProperty("selection-background")
    private Color selectionBackground = new Color(0xDFE1E5);
    @JsonProperty("selection-highlight")
    private Color selectionHighlight = new Color(0xE1EAFF);
    @JsonProperty("selection-foreground")
    private Color selectionForeground = Color.BLACK;

    /*
     * Lists/tables
     */
    @JsonProperty("list-alternate-background")
    private Color listAlternateBackground = new Color(0xF5F8FE);

    /*
     * Buttons
     */
    @JsonProperty("button-background")
    private Color buttonBackground = Color.WHITE;

    @JsonProperty("button-disabled-background")
    private Color buttonDisabledBackground = new Color(0xF7F8FA);

    @JsonProperty("button-toggled")
    private Color buttonToggled = new Color(0xCFCCD5);

    /*
     * Forms/fields
     */
    @JsonProperty("form-background")
    private Color formBackground = Color.WHITE;
    @JsonProperty("form-disabled-background")
    private Color formDisabledBackground = new Color(0xF7F8FA);
    @JsonProperty("form-foreground")
    private Color formForeground = Color.BLACK;

    /*
     * Viewports
     */
    @JsonProperty("viewport-background")
    private Color viewportBackground = Color.WHITE;

    /*
     * Tooltips
     */

    @JsonProperty("tooltip-background")
    private Color tooltipBackground = new Color(0xFFFFFF);
    @JsonProperty("tooltip-foreground")
    private Color tooltipForeground = Color.BLACK;

    /*
     * Categories
     */
    @JsonProperty("category-background")
    private Color categoryBackground = new Color(0xe8effe);

    @JsonProperty("category-border")
    private Color categoryBorder = new Color(0xe8effe);


    public Color getViewportBackground() {
        return viewportBackground;
    }

    public void setViewportBackground(Color viewportBackground) {
        this.viewportBackground = viewportBackground;
    }

    public Color getSelectionHighlight() {
        return selectionHighlight;
    }

    public void setSelectionHighlight(Color selectionHighlight) {
        this.selectionHighlight = selectionHighlight;
    }

    public Color getTabSelectedBackground() {
        return tabSelectedBackground;
    }

    public void setTabSelectedBackground(Color tabSelectedBackground) {
        this.tabSelectedBackground = tabSelectedBackground;
    }

    public Color getTabSelectedHighlight() {
        return tabSelectedHighlight;
    }

    public void setTabSelectedHighlight(Color tabSelectedHighlight) {
        this.tabSelectedHighlight = tabSelectedHighlight;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Color getMenuBackground() {
        return menuBackground;
    }

    public void setMenuBackground(Color menuBackground) {
        this.menuBackground = menuBackground;
    }

    public JIPipeDesktopUIThemeBrightness getBrightness() {
        return brightness;
    }

    public void setBrightness(JIPipeDesktopUIThemeBrightness brightness) {
        this.brightness = brightness;
    }

    public Color getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(Color primaryColor) {
        this.primaryColor = primaryColor;
    }

    public Color getSecondaryColor() {
        return secondaryColor;
    }

    public void setSecondaryColor(Color secondaryColor) {
        this.secondaryColor = secondaryColor;
    }

    public Color getSuccessColor() {
        return successColor;
    }

    public void setSuccessColor(Color successColor) {
        this.successColor = successColor;
    }

    public Color getDangerColor() {
        return dangerColor;
    }

    public void setDangerColor(Color dangerColor) {
        this.dangerColor = dangerColor;
    }

    public Color getWarningColor() {
        return warningColor;
    }

    public void setWarningColor(Color warningColor) {
        this.warningColor = warningColor;
    }

    public Color getTextForeground() {
        return textForeground;
    }

    public void setTextForeground(Color textForeground) {
        this.textForeground = textForeground;
    }

    public Color getTextMuted() {
        return textMuted;
    }

    public void setTextMuted(Color textMuted) {
        this.textMuted = textMuted;
    }

    public Color getWindowBackground() {
        return windowBackground;
    }

    public void setWindowBackground(Color windowBackground) {
        this.windowBackground = windowBackground;
    }

    public Color getPanelBackground() {
        return panelBackground;
    }

    public void setPanelBackground(Color panelBackground) {
        this.panelBackground = panelBackground;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
    }

    public Color getScrollBarThumb() {
        return scrollBarThumb;
    }

    public void setScrollBarThumb(Color scrollBarThumb) {
        this.scrollBarThumb = scrollBarThumb;
    }

    public Color getSelectionBackground() {
        return selectionBackground;
    }

    public void setSelectionBackground(Color selectionBackground) {
        this.selectionBackground = selectionBackground;
    }

    public Color getSelectionForeground() {
        return selectionForeground;
    }

    public void setSelectionForeground(Color selectionForeground) {
        this.selectionForeground = selectionForeground;
    }

    public Color getButtonBackground() {
        return buttonBackground;
    }

    public void setButtonBackground(Color buttonBackground) {
        this.buttonBackground = buttonBackground;
    }

    public Color getButtonToggled() {
        return buttonToggled;
    }

    public void setButtonToggled(Color buttonToggled) {
        this.buttonToggled = buttonToggled;
    }

    public Color getFormBackground() {
        return formBackground;
    }

    public void setFormBackground(Color formBackground) {
        this.formBackground = formBackground;
    }

    public Color getFormForeground() {
        return formForeground;
    }

    public void setFormForeground(Color formForeground) {
        this.formForeground = formForeground;
    }

    public Color getTooltipBackground() {
        return tooltipBackground;
    }

    public void setTooltipBackground(Color tooltipBackground) {
        this.tooltipBackground = tooltipBackground;
    }

    public Color getTooltipForeground() {
        return tooltipForeground;
    }

    public void setTooltipForeground(Color tooltipForeground) {
        this.tooltipForeground = tooltipForeground;
    }

    public Color getTextForegroundInverted() {
        return textForegroundInverted;
    }

    public void setTextForegroundInverted(Color textForegroundInverted) {
        this.textForegroundInverted = textForegroundInverted;
    }

    public Color getTextMutedInverted() {
        return textMutedInverted;
    }

    public void setTextMutedInverted(Color textMutedInverted) {
        this.textMutedInverted = textMutedInverted;
    }

    public Color getListAlternateBackground() {
        return listAlternateBackground;
    }

    public void setListAlternateBackground(Color listAlternateBackground) {
        this.listAlternateBackground = listAlternateBackground;
    }

    public Color getButtonDisabledBackground() {
        return buttonDisabledBackground;
    }

    public void setButtonDisabledBackground(Color buttonDisabledBackground) {
        this.buttonDisabledBackground = buttonDisabledBackground;
    }

    public Color getCaret() {
        return caret;
    }

    public void setCaret(Color caret) {
        this.caret = caret;
    }

    public Color getFormDisabledBackground() {
        return formDisabledBackground;
    }

    public void setFormDisabledBackground(Color formDisabledBackground) {
        this.formDisabledBackground = formDisabledBackground;
    }

    public Color getIconBaseColor() {
        if(brightness == JIPipeDesktopUIThemeBrightness.Light) {
            return new Color(0x6c707e);
        }
        else {
            return new Color(0xced0d6);
        }
    }

    public Color getCategoryBackground() {
        return categoryBackground;
    }

    public void setCategoryBackground(Color categoryBackground) {
        this.categoryBackground = categoryBackground;
    }

    public Color getCategoryBorder() {
        return categoryBorder;
    }

    public void setCategoryBorder(Color categoryBorder) {
        this.categoryBorder = categoryBorder;
    }
}
