/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop.app.customizer;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernThemeStyle;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopUIThemeBrightness;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.ThemeUtils;

import java.awt.*;
import java.nio.file.Path;

public class JIPipeDesktopThemeEditorDocument extends AbstractJIPipeParameterCollection {

    private String id;
    private Path savePath;

    private final CategoryBasics categoryBasics = new CategoryBasics();
    private final CategoryColors categoryColors = new CategoryColors();
    private final CategoryTypography categoryTypography = new CategoryTypography();
    private final CategoryLayout categoryLayout = new CategoryLayout();
    private final CategoryInteractiveElements categoryInteractiveElements = new CategoryInteractiveElements();
    private final CategorySpecializedUI categorySpecializedUI = new CategorySpecializedUI();
    private final CategoryGraphEditor categoryGraphEditor = new CategoryGraphEditor();

    public JIPipeDesktopThemeEditorDocument() {
        this(new JIPipeDesktopModernThemeStyle());
    }

    public JIPipeDesktopThemeEditorDocument(JIPipeDesktopModernThemeStyle style) {
        this.id = style.getId();
        this.savePath = style.getSavePath();

        fromStyle(style);

        registerSubParameters(categoryBasics);
        registerSubParameters(categoryColors);
        registerSubParameters(categoryTypography);
        registerSubParameters(categoryLayout);
        registerSubParameters(categoryInteractiveElements);
        registerSubParameters(categorySpecializedUI);
        registerSubParameters(categoryGraphEditor);
    }

    public String getId() {
        return id;
    }

    public Path getSavePath() {
        return savePath;
    }

    private void fromStyle(JIPipeDesktopModernThemeStyle style) {
        categoryBasics.brightness = style.getBrightness();
        categoryBasics.setName(StringUtils.orElse(StringUtils.orElse(style.getName(), style.getId()), "Unnamed"));

        categoryColors.primary = style.getPrimaryColor();
        categoryColors.secondary = style.getSecondaryColor();
        categoryColors.success = style.getSuccessColor();
        categoryColors.danger = style.getDangerColor();
        categoryColors.warning = style.getWarningColor();
        
        categoryTypography.fontSizeHuge = style.getFontSizeHuge();
        categoryTypography.fontSizeLarge = style.getFontSizeLarge();
        categoryTypography.fontSizeNormal = style.getFontSizeNormal();
        categoryTypography.fontSizeSmall = style.getFontSizeSmall();
        categoryTypography.fontSizeTiny = style.getFontSizeTiny();
        categoryTypography.textForeground = style.getTextForeground();
        categoryTypography.textForegroundSecondary = style.getTextMuted();
        categoryTypography.textForegroundInverted = style.getTextForegroundInverted();
        categoryTypography.textForegroundSecondaryInverted = style.getTextMutedInverted();
        categoryTypography.textForegroundLink = style.getTextLink();
        categoryTypography.textCaret = style.getTextCaret();
        
        categoryLayout.windowBackground = style.getWindowBackground();
        categoryLayout.panelBackground = style.getPanelBackground();
        categoryLayout.menuBackground = style.getMenuBackground();
        categoryLayout.borderColor = style.getBorderColor();
        categoryLayout.islandsCornerRadius = style.getIslandsCornerRadius();
        categoryLayout.islandsDrawBorder = style.isIslandsDrawBorder();
        categoryLayout.islandsBorderColor = style.getIslandsBorderColor();
        
        categoryInteractiveElements.buttonBackground = style.getButtonBackground();
        categoryInteractiveElements.buttonDisabledBackground = style.getButtonDisabledBackground();
        categoryInteractiveElements.buttonToggled = style.getButtonToggled();
        categoryInteractiveElements.formBackground = style.getFormBackground();
        categoryInteractiveElements.formDisabledBackground = style.getFormDisabledBackground();
        categoryInteractiveElements.formForeground = style.getFormForeground();
        categoryInteractiveElements.selectionBackground = style.getSelectionBackground();
        categoryInteractiveElements.selectionHighlight = style.getSelectionHighlight();
        categoryInteractiveElements.selectionForeground = style.getSelectionForeground();
        categoryInteractiveElements.tabSelectedBackground = style.getTabSelectedBackground();
        categoryInteractiveElements.tabSelectedHighlight = style.getTabSelectedHighlight();
        categoryInteractiveElements.listAlternateBackground = style.getListAlternateBackground();
        categoryInteractiveElements.viewportBackground = style.getViewportBackground();
        
        categorySpecializedUI.scrollbarThumb = style.getScrollBarThumb();
        categorySpecializedUI.tooltipBackground = style.getTooltipBackground();
        categorySpecializedUI.tooltipForeground = style.getTooltipForeground();
        categorySpecializedUI.categoryBackground = style.getCategoryBackground();
        categorySpecializedUI.categoryBorder = style.getCategoryBorder();
        
        categoryGraphEditor.nodeHighlightBorder = style.getNodeHighlightBorder();
        categoryGraphEditor.nodeSlotBackground = style.getNodeSlotBackground();
        categoryGraphEditor.nodeFillSaturation = style.getNodeFillSaturation();
        categoryGraphEditor.nodeFillBrightness = style.getNodeFillBrightness();
        categoryGraphEditor.nodeBorderSaturation = style.getNodeBorderSaturation();
        categoryGraphEditor.nodeBorderBrightness = style.getNodeBorderBrightness();
    }

    public JIPipeDesktopModernThemeStyle toStyle() {
        JIPipeDesktopModernThemeStyle result = new JIPipeDesktopModernThemeStyle();

        result.setBrightness(categoryBasics.getBrightness());
        result.setName(categoryBasics.getName());

        result.setPrimaryColor(categoryColors.getPrimary());
        result.setSecondaryColor(categoryColors.getSecondary());
        result.setSuccessColor(categoryColors.getSuccess());
        result.setDangerColor(categoryColors.getDanger());
        result.setWarningColor(categoryColors.getWarning());
        
        result.setFontSizeHuge(categoryTypography.getFontSizeHuge());
        result.setFontSizeLarge(categoryTypography.getFontSizeLarge());
        result.setFontSizeNormal(categoryTypography.getFontSizeNormal());
        result.setFontSizeSmall(categoryTypography.getFontSizeSmall());
        result.setFontSizeTiny(categoryTypography.getFontSizeTiny());
        result.setTextForeground(categoryTypography.getTextForeground());
        result.setTextMuted(categoryTypography.getTextForegroundSecondary());
        result.setTextForegroundInverted(categoryTypography.getTextForegroundInverted());
        result.setTextMutedInverted(categoryTypography.getTextForegroundSecondaryInverted());
        result.setTextLink(categoryTypography.getTextForegroundLink());
        result.setTextCaret(categoryTypography.getTextCaret());
        
        result.setWindowBackground(categoryLayout.getWindowBackground());
        result.setPanelBackground(categoryLayout.getPanelBackground());
        result.setMenuBackground(categoryLayout.getMenuBackground());
        result.setBorderColor(categoryLayout.getBorderColor());
        result.setIslandsCornerRadius(categoryLayout.getIslandsCornerRadius());
        result.setIslandsDrawBorder(categoryLayout.isIslandsDrawBorder());
        result.setIslandsBorderColor(categoryLayout.getIslandsBorderColor());
        
        result.setButtonBackground(categoryInteractiveElements.getButtonBackground());
        result.setButtonDisabledBackground(categoryInteractiveElements.getButtonDisabledBackground());
        result.setButtonToggled(categoryInteractiveElements.getButtonToggled());
        result.setFormBackground(categoryInteractiveElements.getFormBackground());
        result.setFormDisabledBackground(categoryInteractiveElements.getFormDisabledBackground());
        result.setFormForeground(categoryInteractiveElements.getFormForeground());
        result.setSelectionBackground(categoryInteractiveElements.getSelectionBackground());
        result.setSelectionHighlight(categoryInteractiveElements.getSelectionHighlight());
        result.setSelectionForeground(categoryInteractiveElements.getSelectionForeground());
        result.setTabSelectedBackground(categoryInteractiveElements.getTabSelectedBackground());
        result.setTabSelectedHighlight(categoryInteractiveElements.getTabSelectedHighlight());
        result.setListAlternateBackground(categoryInteractiveElements.getListAlternateBackground());
        result.setViewportBackground(categoryInteractiveElements.getViewportBackground());
        
        result.setScrollBarThumb(categorySpecializedUI.getScrollbarThumb());
        result.setTooltipBackground(categorySpecializedUI.getTooltipBackground());
        result.setTooltipForeground(categorySpecializedUI.getTooltipForeground());
        result.setCategoryBackground(categorySpecializedUI.getCategoryBackground());
        result.setCategoryBorder(categorySpecializedUI.getCategoryBorder());
        
        result.setNodeHighlightBorder(categoryGraphEditor.getNodeHighlightBorder());
        result.setNodeSlotBackground(categoryGraphEditor.getNodeSlotBackground());
        result.setNodeFillSaturation(categoryGraphEditor.getNodeFillSaturation());
        result.setNodeFillBrightness(categoryGraphEditor.getNodeFillBrightness());
        result.setNodeBorderSaturation(categoryGraphEditor.getNodeBorderSaturation());
        result.setNodeBorderBrightness(categoryGraphEditor.getNodeBorderBrightness());

        return result;
    }

    public void save(String id) {
        JIPipeDesktopModernThemeStyle result = ThemeUtils.saveStyle(toStyle(), id);
        this.id = result.getId();
        this.savePath = result.getSavePath();
    }

    @SetJIPipeDocumentation(name = "Basics", description = "Basic theme settings")
    @JIPipeParameter("category-basics")
    public CategoryBasics getCategoryBasics() {
        return categoryBasics;
    }

    @SetJIPipeDocumentation(name = "Colors", description = "Color theme settings")
    @JIPipeParameter("category-colors")
    public CategoryColors getCategoryColors() {
        return categoryColors;
    }

    @SetJIPipeDocumentation(name = "Typography", description = "Font size and text color settings")
    @JIPipeParameter("category-typography")
    public CategoryTypography getCategoryTypography() {
        return categoryTypography;
    }

    @SetJIPipeDocumentation(name = "Layout", description = "Window and panel layout settings")
    @JIPipeParameter("category-layout")
    public CategoryLayout getCategoryLayout() {
        return categoryLayout;
    }

    @SetJIPipeDocumentation(name = "Interactive Elements", description = "Interactive UI element colors")
    @JIPipeParameter("category-interactive-elements")
    public CategoryInteractiveElements getCategoryInteractiveElements() {
        return categoryInteractiveElements;
    }

    @SetJIPipeDocumentation(name = "Specialized UI", description = "Specialized UI element colors")
    @JIPipeParameter("category-specialized-ui")
    public CategorySpecializedUI getCategorySpecializedUI() {
        return categorySpecializedUI;
    }

    @SetJIPipeDocumentation(name = "Graph Editor", description = "Graph editor and node appearance settings")
    @JIPipeParameter("category-graph-editor")
    public CategoryGraphEditor getCategoryGraphEditor() {
        return categoryGraphEditor;
    }

    public static class CategorySpecializedUI extends AbstractJIPipeParameterCollection {
        private Color scrollbarThumb = new Color(0xd7d7d7);
        private Color tooltipBackground = new Color(0xFFFFFF);
        private Color tooltipForeground = Color.BLACK;
        private Color categoryBackground = new Color(0xe8effe);
        private Color categoryBorder = new Color(0xe8effe);

        @SetJIPipeDocumentation(name = "Scrollbar thumb", description = "Scrollbar thumb color")
        @JIPipeParameter("scrollbar-thumb")
        public Color getScrollbarThumb() {
            return scrollbarThumb;
        }

        @JIPipeParameter("scrollbar-thumb")
        public void setScrollbarThumb(Color scrollbarThumb) {
            this.scrollbarThumb = scrollbarThumb;
        }

        @SetJIPipeDocumentation(name = "Tooltip background", description = "Tooltip background color")
        @JIPipeParameter("tooltip-background")
        public Color getTooltipBackground() {
            return tooltipBackground;
        }

        @JIPipeParameter("tooltip-background")
        public void setTooltipBackground(Color tooltipBackground) {
            this.tooltipBackground = tooltipBackground;
        }

        @SetJIPipeDocumentation(name = "Tooltip foreground", description = "Tooltip foreground color")
        @JIPipeParameter("tooltip-foreground")
        public Color getTooltipForeground() {
            return tooltipForeground;
        }

        @JIPipeParameter("tooltip-foreground")
        public void setTooltipForeground(Color tooltipForeground) {
            this.tooltipForeground = tooltipForeground;
        }

        @SetJIPipeDocumentation(name = "Category background", description = "Category background color")
        @JIPipeParameter("category-background")
        public Color getCategoryBackground() {
            return categoryBackground;
        }

        @JIPipeParameter("category-background")
        public void setCategoryBackground(Color categoryBackground) {
            this.categoryBackground = categoryBackground;
        }

        @SetJIPipeDocumentation(name = "Category border", description = "Category border color")
        @JIPipeParameter("category-border")
        public Color getCategoryBorder() {
            return categoryBorder;
        }

        @JIPipeParameter("category-border")
        public void setCategoryBorder(Color categoryBorder) {
            this.categoryBorder = categoryBorder;
        }
    }

    public static class CategoryColors extends AbstractJIPipeParameterCollection {
        private Color primary = new Color(0xA0BDF8);
        private Color secondary = new Color(0xaa87de);
        private Color success = new Color(0x369650);
        private Color danger = new Color(0xE55765);
        private Color warning = new Color(0xE58457);

        @SetJIPipeDocumentation(name = "Primary", description = "Primary accent color")
        @JIPipeParameter("primary")
        public Color getPrimary() {
            return primary;
        }

        @JIPipeParameter("primary")
        public void setPrimary(Color primary) {
            this.primary = primary;
        }

        @SetJIPipeDocumentation(name = "Secondary", description = "Secondary accent color")
        @JIPipeParameter("secondary")
        public Color getSecondary() {
            return secondary;
        }

        @JIPipeParameter("secondary")
        public void setSecondary(Color secondary) {
            this.secondary = secondary;
        }

        @SetJIPipeDocumentation(name = "Success", description = "Success/green color")
        @JIPipeParameter("success")
        public Color getSuccess() {
            return success;
        }

        @JIPipeParameter("success")
        public void setSuccess(Color success) {
            this.success = success;
        }

        @SetJIPipeDocumentation(name = "Danger", description = "Error/red color")
        @JIPipeParameter("danger")
        public Color getDanger() {
            return danger;
        }

        @JIPipeParameter("danger")
        public void setDanger(Color danger) {
            this.danger = danger;
        }

        @SetJIPipeDocumentation(name = "Warning", description = "Warning/orange color")
        @JIPipeParameter("warning")
        public Color getWarning() {
            return warning;
        }

        @JIPipeParameter("warning")
        public void setWarning(Color warning) {
            this.warning = warning;
        }
    }

    public static class CategoryBasics extends AbstractJIPipeParameterCollection {
        private JIPipeDesktopUIThemeBrightness brightness = JIPipeDesktopUIThemeBrightness.Light;
        private String name = "";

        @SetJIPipeDocumentation(name = "Name", description = "The name of the style")
        @JIPipeParameter("name")
        public String getName() {
            return name;
        }

        @JIPipeParameter("name")
        public void setName(String name) {
            this.name = name;
        }

        @SetJIPipeDocumentation(name = "Brightness", description = "The general brightness of the theme (light/dark). " +
                "Determines which icons are used.")
        @JIPipeParameter("brightness")
        public JIPipeDesktopUIThemeBrightness getBrightness() {
            return brightness;
        }

        @JIPipeParameter("brightness")
        public void setBrightness(JIPipeDesktopUIThemeBrightness brightness) {
            this.brightness = brightness;
        }
    }

    public static class CategoryTypography extends AbstractJIPipeParameterCollection {
        private int fontSizeHuge = 16;
        private int fontSizeLarge = 14;
        private int fontSizeNormal = 12;
        private int fontSizeSmall = 11;
        private int fontSizeTiny = 10;
        private Color textForeground = new Color(0);
        private Color textForegroundSecondary = new Color(0x6c707e);
        private Color textForegroundInverted = new Color(0xFFFFFF);
        private Color textForegroundSecondaryInverted = new Color(0xebecf0);
        private Color textForegroundLink = new Color(0x67A5E0);
        private Color textCaret = new Color(0);

        @SetJIPipeDocumentation(name = "Extra large font size", description = "Font size for extra large text")
        @JIPipeParameter("font-size-huge")
        public int getFontSizeHuge() {
            return fontSizeHuge;
        }

        @JIPipeParameter("font-size-huge")
        public void setFontSizeHuge(int fontSizeHuge) {
            this.fontSizeHuge = fontSizeHuge;
        }

        @SetJIPipeDocumentation(name = "Large font size", description = "Font size for large text")
        @JIPipeParameter("font-size-large")
        public int getFontSizeLarge() {
            return fontSizeLarge;
        }

        @JIPipeParameter("font-size-large")
        public void setFontSizeLarge(int fontSizeLarge) {
            this.fontSizeLarge = fontSizeLarge;
        }

        @SetJIPipeDocumentation(name = "Normal font size", description = "Font size for normal text")
        @JIPipeParameter("font-size-normal")
        public int getFontSizeNormal() {
            return fontSizeNormal;
        }

        @JIPipeParameter("font-size-normal")
        public void setFontSizeNormal(int fontSizeNormal) {
            this.fontSizeNormal = fontSizeNormal;
        }

        @SetJIPipeDocumentation(name = "Small font size", description = "Font size for small text")
        @JIPipeParameter("font-size-small")
        public int getFontSizeSmall() {
            return fontSizeSmall;
        }

        @JIPipeParameter("font-size-small")
        public void setFontSizeSmall(int fontSizeSmall) {
            this.fontSizeSmall = fontSizeSmall;
        }

        @SetJIPipeDocumentation(name = "Tiny font size", description = "Font size for tiny text")
        @JIPipeParameter("font-size-tiny")
        public int getFontSizeTiny() {
            return fontSizeTiny;
        }

        @JIPipeParameter("font-size-tiny")
        public void setFontSizeTiny(int fontSizeTiny) {
            this.fontSizeTiny = fontSizeTiny;
        }

        @SetJIPipeDocumentation(name = "Main text color", description = "Color for main text content")
        @JIPipeParameter("text-foreground")
        public Color getTextForeground() {
            return textForeground;
        }

        @JIPipeParameter("text-foreground")
        public void setTextForeground(Color textForeground) {
            this.textForeground = textForeground;
        }

        @SetJIPipeDocumentation(name = "Secondary text color", description = "Color for secondary/muted text")
        @JIPipeParameter("text-foreground-secondary")
        public Color getTextForegroundSecondary() {
            return textForegroundSecondary;
        }

        @JIPipeParameter("text-foreground-secondary")
        public void setTextForegroundSecondary(Color textForegroundSecondary) {
            this.textForegroundSecondary = textForegroundSecondary;
        }

        @SetJIPipeDocumentation(name = "Inverted text color", description = "Color for text on dark backgrounds")
        @JIPipeParameter("text-foreground-inverted")
        public Color getTextForegroundInverted() {
            return textForegroundInverted;
        }

        @JIPipeParameter("text-foreground-inverted")
        public void setTextForegroundInverted(Color textForegroundInverted) {
            this.textForegroundInverted = textForegroundInverted;
        }

        @SetJIPipeDocumentation(name = "Inverted secondary text color", description = "Color for secondary text on dark backgrounds")
        @JIPipeParameter("text-foreground-secondary-inverted")
        public Color getTextForegroundSecondaryInverted() {
            return textForegroundSecondaryInverted;
        }

        @JIPipeParameter("text-foreground-secondary-inverted")
        public void setTextForegroundSecondaryInverted(Color textForegroundSecondaryInverted) {
            this.textForegroundSecondaryInverted = textForegroundSecondaryInverted;
        }

        @SetJIPipeDocumentation(name = "Link text color", description = "Color for hyperlinks")
        @JIPipeParameter("text-foreground-link")
        public Color getTextForegroundLink() {
            return textForegroundLink;
        }

        @JIPipeParameter("text-foreground-link")
        public void setTextForegroundLink(Color textForegroundLink) {
            this.textForegroundLink = textForegroundLink;
        }

        @SetJIPipeDocumentation(name = "Text caret color", description = "Color for text input caret")
        @JIPipeParameter("text-caret")
        public Color getTextCaret() {
            return textCaret;
        }

        @JIPipeParameter("text-caret")
        public void setTextCaret(Color textCaret) {
            this.textCaret = textCaret;
        }
    }

    public static class CategoryLayout extends AbstractJIPipeParameterCollection {
        private Color windowBackground = new Color(0xEBECF0);
        private Color panelBackground = new Color(0xFFFFFF);
        private Color menuBackground = new Color(0xFFFFFF);
        private Color borderColor = new Color(0xdedee2);
        private int islandsCornerRadius = 15;
        private boolean islandsDrawBorder;
        private Color islandsBorderColor;

        @SetJIPipeDocumentation(name = "Window background", description = "Main window background color")
        @JIPipeParameter("window-background")
        public Color getWindowBackground() {
            return windowBackground;
        }

        @JIPipeParameter("window-background")
        public void setWindowBackground(Color windowBackground) {
            this.windowBackground = windowBackground;
        }

        @SetJIPipeDocumentation(name = "Panel background", description = "Panel background color")
        @JIPipeParameter("panel-background")
        public Color getPanelBackground() {
            return panelBackground;
        }

        @JIPipeParameter("panel-background")
        public void setPanelBackground(Color panelBackground) {
            this.panelBackground = panelBackground;
        }

        @SetJIPipeDocumentation(name = "Menu background", description = "Menu background color")
        @JIPipeParameter("menu-background")
        public Color getMenuBackground() {
            return menuBackground;
        }

        @JIPipeParameter("menu-background")
        public void setMenuBackground(Color menuBackground) {
            this.menuBackground = menuBackground;
        }

        @SetJIPipeDocumentation(name = "Border color", description = "Border color for UI elements")
        @JIPipeParameter("border-color")
        public Color getBorderColor() {
            return borderColor;
        }

        @JIPipeParameter("border-color")
        public void setBorderColor(Color borderColor) {
            this.borderColor = borderColor;
        }

        @SetJIPipeDocumentation(name = "Islands corner radius", description = "Split pane islands and UI corner radius (0-32)")
        @JIPipeParameter("corner-radius")
        public int getIslandsCornerRadius() {
            return islandsCornerRadius;
        }

        @JIPipeParameter("corner-radius")
        public void setIslandsCornerRadius(int islandsCornerRadius) {
            this.islandsCornerRadius = islandsCornerRadius;
        }

        @SetJIPipeDocumentation(name = "Draw islands border", description = "If enabled, draw a border around split pane islands")
        @JIPipeParameter("islands-draw-border")
        public boolean isIslandsDrawBorder() {
            return islandsDrawBorder;
        }

        @JIPipeParameter("islands-draw-border")
        public void setIslandsDrawBorder(boolean islandsDrawBorder) {
            this.islandsDrawBorder = islandsDrawBorder;
        }

        @SetJIPipeDocumentation(name = "Islands border color", description = "The color of island borders if enabled")
        @JIPipeParameter("islands-border-color")
        public Color getIslandsBorderColor() {
            return islandsBorderColor;
        }

        @JIPipeParameter("islands-border-color")
        public void setIslandsBorderColor(Color islandsBorderColor) {
            this.islandsBorderColor = islandsBorderColor;
        }
    }

    public static class CategoryInteractiveElements extends AbstractJIPipeParameterCollection {
        private Color buttonBackground = Color.WHITE;
        private Color buttonDisabledBackground = new Color(0xF7F8FA);
        private Color buttonToggled = new Color(0xCFCCD5);
        private Color formBackground = Color.WHITE;
        private Color formDisabledBackground = new Color(0xF7F8FA);
        private Color formForeground = Color.BLACK;
        private Color selectionBackground = new Color(0xDFE1E5);
        private Color selectionHighlight = new Color(0xE1EAFF);
        private Color selectionForeground = Color.BLACK;
        private Color tabSelectedBackground = new Color(0xE1EAFF);
        private Color tabSelectedHighlight = new Color(0xa0bdf8);
        private Color listAlternateBackground = new Color(0xF5F8FE);
        private Color viewportBackground = Color.WHITE;

        @SetJIPipeDocumentation(name = "Button background", description = "Button background color")
        @JIPipeParameter("button-background")
        public Color getButtonBackground() {
            return buttonBackground;
        }

        @JIPipeParameter("button-background")
        public void setButtonBackground(Color buttonBackground) {
            this.buttonBackground = buttonBackground;
        }

        @SetJIPipeDocumentation(name = "Button disabled background", description = "Button disabled background color")
        @JIPipeParameter("button-disabled-background")
        public Color getButtonDisabledBackground() {
            return buttonDisabledBackground;
        }

        @JIPipeParameter("button-disabled-background")
        public void setButtonDisabledBackground(Color buttonDisabledBackground) {
            this.buttonDisabledBackground = buttonDisabledBackground;
        }

        @SetJIPipeDocumentation(name = "Button toggled", description = "Button toggled color")
        @JIPipeParameter("button-toggled")
        public Color getButtonToggled() {
            return buttonToggled;
        }

        @JIPipeParameter("button-toggled")
        public void setButtonToggled(Color buttonToggled) {
            this.buttonToggled = buttonToggled;
        }

        @SetJIPipeDocumentation(name = "Form background", description = "Form background color")
        @JIPipeParameter("form-background")
        public Color getFormBackground() {
            return formBackground;
        }

        @JIPipeParameter("form-background")
        public void setFormBackground(Color formBackground) {
            this.formBackground = formBackground;
        }

        @SetJIPipeDocumentation(name = "Form disabled background", description = "Form disabled background color")
        @JIPipeParameter("form-disabled-background")
        public Color getFormDisabledBackground() {
            return formDisabledBackground;
        }

        @JIPipeParameter("form-disabled-background")
        public void setFormDisabledBackground(Color formDisabledBackground) {
            this.formDisabledBackground = formDisabledBackground;
        }

        @SetJIPipeDocumentation(name = "Form foreground", description = "Form foreground color")
        @JIPipeParameter("form-foreground")
        public Color getFormForeground() {
            return formForeground;
        }

        @JIPipeParameter("form-foreground")
        public void setFormForeground(Color formForeground) {
            this.formForeground = formForeground;
        }

        @SetJIPipeDocumentation(name = "Selection background", description = "Selection background color")
        @JIPipeParameter("selection-background")
        public Color getSelectionBackground() {
            return selectionBackground;
        }

        @JIPipeParameter("selection-background")
        public void setSelectionBackground(Color selectionBackground) {
            this.selectionBackground = selectionBackground;
        }

        @SetJIPipeDocumentation(name = "Selection highlight", description = "Selection highlight color")
        @JIPipeParameter("selection-highlight")
        public Color getSelectionHighlight() {
            return selectionHighlight;
        }

        @JIPipeParameter("selection-highlight")
        public void setSelectionHighlight(Color selectionHighlight) {
            this.selectionHighlight = selectionHighlight;
        }

        @SetJIPipeDocumentation(name = "Selection foreground", description = "Selection foreground color")
        @JIPipeParameter("selection-foreground")
        public Color getSelectionForeground() {
            return selectionForeground;
        }

        @JIPipeParameter("selection-foreground")
        public void setSelectionForeground(Color selectionForeground) {
            this.selectionForeground = selectionForeground;
        }

        @SetJIPipeDocumentation(name = "Tab selected background", description = "Tab selected background color")
        @JIPipeParameter("tab-selected-background")
        public Color getTabSelectedBackground() {
            return tabSelectedBackground;
        }

        @JIPipeParameter("tab-selected-background")
        public void setTabSelectedBackground(Color tabSelectedBackground) {
            this.tabSelectedBackground = tabSelectedBackground;
        }

        @SetJIPipeDocumentation(name = "Tab selected highlight", description = "Tab selected highlight color")
        @JIPipeParameter("tab-selected-highlight")
        public Color getTabSelectedHighlight() {
            return tabSelectedHighlight;
        }

        @JIPipeParameter("tab-selected-highlight")
        public void setTabSelectedHighlight(Color tabSelectedHighlight) {
            this.tabSelectedHighlight = tabSelectedHighlight;
        }

        @SetJIPipeDocumentation(name = "List alternate background", description = "List alternate background color")
        @JIPipeParameter("list-alternate-background")
        public Color getListAlternateBackground() {
            return listAlternateBackground;
        }

        @JIPipeParameter("list-alternate-background")
        public void setListAlternateBackground(Color listAlternateBackground) {
            this.listAlternateBackground = listAlternateBackground;
        }

        @SetJIPipeDocumentation(name = "Viewport background", description = "Viewport background color")
        @JIPipeParameter("viewport-background")
        public Color getViewportBackground() {
            return viewportBackground;
        }

        @JIPipeParameter("viewport-background")
        public void setViewportBackground(Color viewportBackground) {
            this.viewportBackground = viewportBackground;
        }
    
    }

    public static class CategoryGraphEditor extends AbstractJIPipeParameterCollection {
        private Color nodeHighlightBorder = new Color(0x737880);
        private Color nodeSlotBackground = new Color(0xFAFAFA);
        private float nodeFillSaturation = 0.1f;
        private float nodeFillBrightness = 0.9f;
        private float nodeBorderSaturation = 0.1f;
        private float nodeBorderBrightness = 0.5f;

        @SetJIPipeDocumentation(name = "Node highlight border", description = "Node highlight border color")
        @JIPipeParameter("node-highlight-border")
        public Color getNodeHighlightBorder() {
            return nodeHighlightBorder;
        }

        @JIPipeParameter("node-highlight-border")
        public void setNodeHighlightBorder(Color nodeHighlightBorder) {
            this.nodeHighlightBorder = nodeHighlightBorder;
        }

        @SetJIPipeDocumentation(name = "Node slot background", description = "Node slot background color")
        @JIPipeParameter("node-slot-background")
        public Color getNodeSlotBackground() {
            return nodeSlotBackground;
        }

        @JIPipeParameter("node-slot-background")
        public void setNodeSlotBackground(Color nodeSlotBackground) {
            this.nodeSlotBackground = nodeSlotBackground;
        }

        @SetJIPipeDocumentation(name = "Node fill saturation", description = "Node fill color saturation (0.0-1.0)")
        @JIPipeParameter("node-fill-saturation")
        public float getNodeFillSaturation() {
            return nodeFillSaturation;
        }

        @JIPipeParameter("node-fill-saturation")
        public void setNodeFillSaturation(float nodeFillSaturation) {
            this.nodeFillSaturation = nodeFillSaturation;
        }

        @SetJIPipeDocumentation(name = "Node fill brightness", description = "Node fill color brightness (0.0-1.0)")
        @JIPipeParameter("node-fill-brightness")
        public float getNodeFillBrightness() {
            return nodeFillBrightness;
        }

        @JIPipeParameter("node-fill-brightness")
        public void setNodeFillBrightness(float nodeFillBrightness) {
            this.nodeFillBrightness = nodeFillBrightness;
        }

        @SetJIPipeDocumentation(name = "Node border saturation", description = "Node border color saturation (0.0-1.0)")
        @JIPipeParameter("node-border-saturation")
        public float getNodeBorderSaturation() {
            return nodeBorderSaturation;
        }

        @JIPipeParameter("node-border-saturation")
        public void setNodeBorderSaturation(float nodeBorderSaturation) {
            this.nodeBorderSaturation = nodeBorderSaturation;
        }

        @SetJIPipeDocumentation(name = "Node border brightness", description = "Node border color brightness (0.0-1.0)")
        @JIPipeParameter("node-border-brightness")
        public float getNodeBorderBrightness() {
            return nodeBorderBrightness;
        }

        @JIPipeParameter("node-border-brightness")
        public void setNodeBorderBrightness(float nodeBorderBrightness) {
            this.nodeBorderBrightness = nodeBorderBrightness;
        }
    }
}
