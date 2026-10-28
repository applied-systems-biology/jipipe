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

package org.hkijena.jipipe.plugins.canvasnotes.nodes;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.annotation.JIPipeGraphCanvasNote;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopAnnotationGraphNodeUI;
import org.hkijena.jipipe.plugins.parameters.library.primitives.JIPipeFontFamilySizeStyleColorParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.JIPipeFontFamilySizeStyleParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.plugins.parameters.library.roi.Anchor;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;
import java.util.Objects;

public abstract class DefaultTextBoxGraphCanvasNote extends JIPipeGraphCanvasNote {
    private final TextRenderParameters titleStyle;
    private final TextRenderParameters contentStyle;
    private final TextLocationParameters textLocation;
    private String textTitle = "";
    private String textContent = "";
    private int renderedTextWidth = 0;
    private int renderedTitleHeight = 0;
    private int renderedContentHeight = 0;
    private Font titleFont = null;
    private Font contentFont = null;
    private String renderedTitle;
    private String[] renderedContent;

    private String renderedTitleRaw;
    private String renderedContentRaw;
    private double renderedZoom;
    private int renderedGridWidth;
    private int renderedGridHeight;

    public DefaultTextBoxGraphCanvasNote(JIPipeNodeInfo info) {
        super(info);

        this.titleStyle = new TextRenderParameters();
        this.titleStyle.setFont(titleFontWithSize(18));
        this.contentStyle = new TextRenderParameters();
        this.textLocation = new TextLocationParameters();

        registerSubParameter(titleStyle);
        registerSubParameter(contentStyle);
        registerSubParameter(textLocation);
    }

    public DefaultTextBoxGraphCanvasNote(DefaultTextBoxGraphCanvasNote other) {
        super(other);

        this.titleStyle = new TextRenderParameters(other.titleStyle);
        this.contentStyle = new TextRenderParameters(other.contentStyle);
        this.textLocation = new TextLocationParameters(other.textLocation);
        this.textTitle = other.textTitle;
        this.textContent = other.textContent;
        registerSubParameter(titleStyle);
        registerSubParameter(contentStyle);

        registerSubParameter(textLocation);
    }

    @SetJIPipeDocumentation(name = "Text location", description = "The following settings determine the location of the title and content")
    @JIPipeParameter("text-location")
    public TextLocationParameters getTextLocation() {
        return textLocation;
    }

    @SetJIPipeDocumentation(name = "Title style", description = "The following parameters determine how the title is rendered")
    @JIPipeParameter("title-style")
    public TextRenderParameters getTitleStyle() {
        return titleStyle;
    }

    @SetJIPipeDocumentation(name = "Description style", description = "The following parameters determine how the description is rendered")
    @JIPipeParameter("content-style")
    public TextRenderParameters getContentStyle() {
        return contentStyle;
    }

    @SetJIPipeDocumentation(name = "Title", description = "The title of the text box")
    @JIPipeParameter(value = "text-title", important = true, uiOrder = -100)
    public String getTextTitle() {
        return textTitle;
    }

    @JIPipeParameter("text-title")
    public void setTextTitle(String textTitle) {
        this.textTitle = textTitle;
    }

    @SetJIPipeDocumentation(name = "Content", description = "The additional text content of the text box")
    @JIPipeParameter(value = "text-content", important = true, uiOrder = -90)
    @StringParameterSettings(multiline = true)
    public String getTextContent() {
        return textContent;
    }

    @JIPipeParameter("text-content")
    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    @Override
    public boolean isPaintNodeShadow() {
        return false;
    }

    public void invalidateAssets() {
        renderedTextWidth = 0;
        renderedTitleHeight = 0;
        renderedContentHeight = 0;
        renderedZoom = -1;
    }

    @Override
    public void onParameterChanged(ParameterChangedEvent event) {
        invalidateAssets();
        super.onParameterChanged(event);
    }

    @Override
    public boolean isDrawWithAntialiasing() {
        return !StringUtils.isNullOrEmpty(textTitle) || !StringUtils.isNullOrEmpty(textContent);
    }

    @Override
    public void paintNode(Graphics2D g2, JIPipeDesktopAnnotationGraphNodeUI nodeUI, double zoom) {

        updateAssetsIfNeeded(g2, nodeUI, zoom);

        int nodeWidth = nodeUI.getWidth();
        int nodeHeight = nodeUI.getHeight();
        int finalBorderThickness = (int) (getBorderThickness() * zoom);

        paintShape(g2, nodeUI, zoom);

        int finalTextWidth = renderedTextWidth;
        int finalTextHeight = renderedTitleHeight + renderedContentHeight;
        int finalMarginTop = (int) (textLocation.marginTop * zoom);
        int finalMarginLeft = (int) (textLocation.marginLeft * zoom);
        int finalMarginRight = (int) (textLocation.marginRight * zoom);
        int finalMarginBottom = (int) (textLocation.marginBottom * zoom);

        switch (textLocation.anchor) {
            case TopLeft: {
                if (!StringUtils.isNullOrEmpty(renderedTitle)) {
                    g2.setColor(titleStyle.getColor());
                    g2.setFont(titleFont);
                    g2.drawString(renderedTitle, finalMarginLeft + finalBorderThickness, g2.getFontMetrics().getAscent() + finalMarginTop + finalBorderThickness);
                }
                if (renderedContent != null && renderedContent.length > 0) {
                    g2.setFont(contentFont);
                    g2.setColor(contentStyle.getColor());
                    int lineHeight = g2.getFontMetrics().getHeight();
                    for (int i = 0; i < renderedContent.length; i++) {
                        g2.drawString(renderedContent[i], finalMarginLeft + finalBorderThickness, renderedTitleHeight + i * lineHeight + g2.getFontMetrics().getAscent() + finalMarginTop + finalBorderThickness);
                    }
                }
            }
            break;
            case TopCenter: {
                if (!StringUtils.isNullOrEmpty(renderedTitle)) {
                    g2.setColor(titleStyle.getColor());
                    g2.setFont(titleFont);
                    g2.drawString(renderedTitle, nodeWidth / 2 - finalTextWidth / 2, g2.getFontMetrics().getAscent() + finalMarginTop + finalBorderThickness);
                }
                if (renderedContent != null && renderedContent.length > 0) {
                    g2.setFont(contentFont);
                    g2.setColor(contentStyle.getColor());
                    int lineHeight = g2.getFontMetrics().getHeight();
                    for (int i = 0; i < renderedContent.length; i++) {
                        g2.drawString(renderedContent[i], nodeWidth / 2 - finalTextWidth / 2, renderedTitleHeight + i * lineHeight + g2.getFontMetrics().getAscent() + finalMarginTop + finalBorderThickness);
                    }
                }
            }
            break;
            case TopRight: {

                if (!StringUtils.isNullOrEmpty(renderedTitle)) {
                    g2.setColor(titleStyle.getColor());
                    g2.setFont(titleFont);
                    g2.drawString(renderedTitle, nodeWidth - finalTextWidth - finalBorderThickness - finalMarginRight, g2.getFontMetrics().getAscent() + finalMarginTop + finalBorderThickness);
                }
                if (renderedContent != null && renderedContent.length > 0) {
                    g2.setFont(contentFont);
                    g2.setColor(contentStyle.getColor());
                    int lineHeight = g2.getFontMetrics().getHeight();
                    for (int i = 0; i < renderedContent.length; i++) {
                        g2.drawString(renderedContent[i], nodeWidth - finalTextWidth - finalBorderThickness - finalMarginRight, renderedTitleHeight + i * lineHeight + g2.getFontMetrics().getAscent() + finalMarginTop + finalBorderThickness);
                    }
                }
            }
            break;
            case CenterLeft: {
                if (!StringUtils.isNullOrEmpty(renderedTitle)) {
                    g2.setColor(titleStyle.getColor());
                    g2.setFont(titleFont);
                    g2.drawString(renderedTitle, finalMarginLeft + finalBorderThickness, nodeHeight / 2 - finalTextHeight / 2 + g2.getFontMetrics().getAscent() + finalMarginTop);
                }
                if (renderedContent != null && renderedContent.length > 0) {
                    g2.setFont(contentFont);
                    g2.setColor(contentStyle.getColor());
                    int lineHeight = g2.getFontMetrics().getHeight();
                    for (int i = 0; i < renderedContent.length; i++) {
                        g2.drawString(renderedContent[i], finalMarginLeft + finalBorderThickness, nodeHeight / 2 - finalTextHeight / 2 + renderedTitleHeight + i * lineHeight + g2.getFontMetrics().getAscent() + finalMarginTop);
                    }
                }
            }
            break;
            case CenterCenter: {
                if (!StringUtils.isNullOrEmpty(renderedTitle)) {
                    g2.setColor(titleStyle.getColor());
                    g2.setFont(titleFont);
                    g2.drawString(renderedTitle, nodeWidth / 2 - finalTextWidth / 2, nodeHeight / 2 - finalTextHeight / 2 + g2.getFontMetrics().getAscent() + finalMarginTop);
                }
                if (renderedContent != null && renderedContent.length > 0) {
                    g2.setFont(contentFont);
                    g2.setColor(contentStyle.getColor());
                    int lineHeight = g2.getFontMetrics().getHeight();
                    for (int i = 0; i < renderedContent.length; i++) {
                        g2.drawString(renderedContent[i], nodeWidth / 2 - finalTextWidth / 2, nodeHeight / 2 - finalTextHeight / 2 + renderedTitleHeight + i * lineHeight + g2.getFontMetrics().getAscent() + finalMarginTop);
                    }
                }
            }
            break;
            case CenterRight: {
                if (!StringUtils.isNullOrEmpty(renderedTitle)) {
                    g2.setColor(titleStyle.getColor());
                    g2.setFont(titleFont);
                    g2.drawString(renderedTitle, nodeWidth - finalTextWidth - finalBorderThickness - finalMarginRight, nodeHeight / 2 - finalTextHeight / 2 + g2.getFontMetrics().getAscent() + finalMarginTop);
                }
                if (renderedContent != null && renderedContent.length > 0) {
                    g2.setFont(contentFont);
                    g2.setColor(contentStyle.getColor());
                    int lineHeight = g2.getFontMetrics().getHeight();
                    for (int i = 0; i < renderedContent.length; i++) {
                        g2.drawString(renderedContent[i], nodeWidth - finalTextWidth - finalBorderThickness - finalMarginRight, nodeHeight / 2 - finalTextHeight / 2 + renderedTitleHeight + i * lineHeight + g2.getFontMetrics().getAscent() + finalMarginTop);
                    }
                }
            }
            break;
            case BottomLeft: {
                if (!StringUtils.isNullOrEmpty(renderedTitle)) {
                    g2.setColor(titleStyle.getColor());
                    g2.setFont(titleFont);
                    g2.drawString(renderedTitle, finalMarginLeft + finalBorderThickness, nodeHeight - finalTextHeight + g2.getFontMetrics().getAscent() - finalMarginBottom - finalBorderThickness);
                }
                if (renderedContent != null && renderedContent.length > 0) {
                    g2.setFont(contentFont);
                    g2.setColor(contentStyle.getColor());
                    int lineHeight = g2.getFontMetrics().getHeight();
                    for (int i = 0; i < renderedContent.length; i++) {
                        g2.drawString(renderedContent[i], finalMarginLeft + finalBorderThickness, nodeHeight - finalTextHeight + renderedTitleHeight + i * lineHeight + g2.getFontMetrics().getAscent() - finalMarginBottom - finalBorderThickness);
                    }
                }
            }
            break;
            case BottomCenter: {
                if (!StringUtils.isNullOrEmpty(renderedTitle)) {
                    g2.setColor(titleStyle.getColor());
                    g2.setFont(titleFont);
                    g2.drawString(renderedTitle, nodeWidth / 2 - finalTextWidth / 2, nodeHeight - finalTextHeight + g2.getFontMetrics().getAscent() - finalMarginBottom - finalBorderThickness);
                }
                if (renderedContent != null && renderedContent.length > 0) {
                    g2.setFont(contentFont);
                    g2.setColor(contentStyle.getColor());
                    int lineHeight = g2.getFontMetrics().getHeight();
                    for (int i = 0; i < renderedContent.length; i++) {
                        g2.drawString(renderedContent[i], nodeWidth / 2 - finalTextWidth / 2, nodeHeight - finalTextHeight + renderedTitleHeight + i * lineHeight + g2.getFontMetrics().getAscent() - finalMarginBottom - finalBorderThickness);
                    }
                }
            }
            break;
            case BottomRight: {
                if (!StringUtils.isNullOrEmpty(renderedTitle)) {
                    g2.setColor(titleStyle.getColor());
                    g2.setFont(titleFont);
                    g2.drawString(renderedTitle, nodeWidth - finalTextWidth - finalBorderThickness - finalMarginRight, nodeHeight - finalTextHeight + g2.getFontMetrics().getAscent() - finalMarginBottom - finalBorderThickness);
                }
                if (renderedContent != null && renderedContent.length > 0) {
                    g2.setFont(contentFont);
                    g2.setColor(contentStyle.getColor());
                    int lineHeight = g2.getFontMetrics().getHeight();
                    for (int i = 0; i < renderedContent.length; i++) {
                        g2.drawString(renderedContent[i], nodeWidth - finalTextWidth - finalBorderThickness - finalMarginRight, nodeHeight - finalTextHeight + renderedTitleHeight + i * lineHeight + g2.getFontMetrics().getAscent() - finalMarginBottom - finalBorderThickness);
                    }
                }
            }
            break;
        }

    }

    protected abstract void paintShape(Graphics2D g2, JIPipeDesktopAnnotationGraphNodeUI nodeUI, double zoom);

    private void updateAssetsIfNeeded(Graphics2D g2, JIPipeDesktopAnnotationGraphNodeUI nodeUI, double zoom) {
        if (!Objects.equals(renderedContentRaw, textContent) || !Objects.equals(renderedTitleRaw, textTitle) || renderedZoom != zoom || renderedGridWidth != getGridWidth() || renderedGridHeight != getGridHeight()) {

            int finalBorderThickness = (int) Math.max(1, getBorderThickness() * zoom);
            int finalMarginLeft = (int) (textLocation.marginLeft * zoom);
            int finalMarginRight = (int) (textLocation.marginRight * zoom);

            int availableWidth = nodeUI.getWidth()
                    - finalBorderThickness * 2
                    - finalMarginLeft
                    - finalMarginRight;

            renderedTextWidth = 0;
            renderedTitleHeight = 0;
            renderedContentHeight = 0;
            renderedGridWidth = 0;
            renderedGridHeight = 0;

            if (!StringUtils.isNullOrEmpty(textTitle)) {
                titleFont = scaledFont(titleStyle.getFont().toFont(), zoom);
                FontMetrics fontMetrics = g2.getFontMetrics(titleFont);
                renderedTitle = StringUtils.limitWithEllipsis(textTitle, availableWidth, fontMetrics);
                renderedTextWidth = Math.max(renderedTextWidth, fontMetrics.stringWidth(renderedTitle));
                renderedTitleHeight = fontMetrics.getHeight();
            } else {
                renderedTitle = "";
            }

            if (!StringUtils.isNullOrEmpty(textContent)) {
                contentFont = scaledFont(contentStyle.getFont().toFont(), zoom);
                FontMetrics fontMetrics = g2.getFontMetrics(contentFont);
                renderedContent = textContent.split("\n");
                for (int i = 0; i < renderedContent.length; i++) {
                    renderedContent[i] = StringUtils.limitWithEllipsis(renderedContent[i], availableWidth, fontMetrics);
                    renderedTextWidth = Math.max(renderedTextWidth, fontMetrics.stringWidth(renderedContent[i]));
                    renderedContentHeight += fontMetrics.getHeight();
                }
            } else {
                renderedContent = null;
            }

            renderedContentRaw = textContent;
            renderedTitleRaw = textTitle;
            renderedZoom = zoom;
            renderedGridWidth = getGridWidth();
            renderedGridHeight = getGridHeight();
        }
    }

    protected abstract int getBorderThickness();

    /**
     * @return the given font scaled by {@code zoom} (minimum size 1)
     */
    private static Font scaledFont(Font font, double zoom) {
        return font.deriveFont((float) Math.max(1, font.getSize() * zoom));
    }

    /**
     * @return a title font (the default family, plain, black) at the given size
     */
    private static JIPipeFontFamilySizeStyleColorParameter titleFontWithSize(int size) {
        JIPipeFontFamilySizeStyleColorParameter f = new JIPipeFontFamilySizeStyleColorParameter();
        f.migrateFrom(JIPipeFontFamilySizeStyleParameter.defaultFamily(), size, Font.PLAIN, Color.BLACK);
        return f;
    }

    /**
     * #1339: migrate the legacy split title/content font parameters
     * (font-family, font-style, font-size, color) into the single
     * {@code font} parameter of each style when a pre-migration project is
     * loaded. The nested collections are loaded via flat slash-keys, so the
     * keys are prefixed with {@code title-style/} / {@code content-style/}.
     * Idempotent: after the next save the legacy keys are gone.
     */
    @Override
    protected void onDeserialized(JsonNode node, JIPipeValidationReport issues, JIPipeNotificationInbox notifications) {
        String[] prefixes = {"title-style/", "content-style/"};
        for (String p : prefixes) {
            if (node.has(p + "font-family") || node.has(p + "font-size")
                    || node.has(p + "font-style") || node.has(p + "color")) {
                TextRenderParameters target = p.equals("title-style/") ? titleStyle : contentStyle;
                target.setFont(JIPipeFontFamilySizeStyleColorParameter.fromLegacyKeys(node,
                        p + "font-family", p + "font-size", p + "font-style", p + "color",
                        p.equals("title-style/") ? 18 : 12));
            }
        }
    }

    public static class TextLocationParameters extends AbstractJIPipeParameterCollection {
        private Anchor anchor = Anchor.CenterCenter;
        private int marginLeft;
        private int marginRight;
        private int marginTop;
        private int marginBottom;

        public TextLocationParameters() {
        }

        public TextLocationParameters(TextLocationParameters other) {
            setTo(other);
        }

        public void setTo(TextLocationParameters other) {
            this.anchor = other.anchor;
            this.marginLeft = other.marginLeft;
            this.marginRight = other.marginRight;
            this.marginTop = other.marginTop;
            this.marginBottom = other.marginBottom;
        }

        @SetJIPipeDocumentation(name = "Anchor", description = "Determines to which anchor location the text is attached to.")
        @JIPipeParameter("anchor")
        public Anchor getAnchor() {
            return anchor;
        }

        @JIPipeParameter("anchor")
        public void setAnchor(Anchor anchor) {
            this.anchor = anchor;
        }

        @SetJIPipeDocumentation(name = "Margin left", description = "The left margin of the available text area relative to the node border")
        @JIPipeParameter("margin-left")
        public int getMarginLeft() {
            return marginLeft;
        }

        @JIPipeParameter("margin-left")
        public void setMarginLeft(int marginLeft) {
            this.marginLeft = marginLeft;
        }

        @SetJIPipeDocumentation(name = "Margin right", description = "The right margin of the available text area relative to the node border")
        @JIPipeParameter("margin-right")
        public int getMarginRight() {
            return marginRight;
        }

        @JIPipeParameter("margin-right")
        public void setMarginRight(int marginRight) {
            this.marginRight = marginRight;
        }

        @SetJIPipeDocumentation(name = "Margin top", description = "The top margin of the available text area relative to the node border")
        @JIPipeParameter("margin-top")
        public int getMarginTop() {
            return marginTop;
        }

        @JIPipeParameter("margin-top")
        public void setMarginTop(int marginTop) {
            this.marginTop = marginTop;
        }

        @SetJIPipeDocumentation(name = "Margin bottom", description = "The bottom margin of the available text area relative to the node border")
        @JIPipeParameter("margin-bottom")
        public int getMarginBottom() {
            return marginBottom;
        }

        @JIPipeParameter("margin-bottom")
        public void setMarginBottom(int marginBottom) {
            this.marginBottom = marginBottom;
        }
    }


    public static class TextRenderParameters extends AbstractJIPipeParameterCollection {
        // #1339: the legacy color + font-family + font-style + font-size are
        // folded into one font parameter (the text color is the font's color).
        private JIPipeFontFamilySizeStyleColorParameter font = new JIPipeFontFamilySizeStyleColorParameter();

        public TextRenderParameters() {
            font.migrateFrom(JIPipeFontFamilySizeStyleParameter.defaultFamily(), 12, Font.PLAIN, Color.BLACK);
        }

        public TextRenderParameters(TextRenderParameters other) {
            setTo(other);
        }

        public void setTo(TextRenderParameters other) {
            this.font = new JIPipeFontFamilySizeStyleColorParameter(other.font);
        }

        /**
         * The text color (the font's color). Kept as a plain convenience for
         * the rendering code — the color is persisted inside the {@code font}
         * parameter, not separately (#1339).
         */
        public Color getColor() {
            return font.getColor();
        }

        @SetJIPipeDocumentation(name = "Font", description = "The font (family, size, style and color) of the text")
        @JIPipeParameter("font")
        public JIPipeFontFamilySizeStyleColorParameter getFont() {
            return font;
        }

        @JIPipeParameter("font")
        public void setFont(JIPipeFontFamilySizeStyleColorParameter font) {
            this.font = font;
        }
    }
}
