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

package org.hkijena.jipipe.extensions.graphannotation.nodes;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.annotation.JIPipeAnnotationGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.FontFamilyParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.FontStyleParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.roi.Anchor;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopAnnotationGraphNodeUI;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;
import java.util.Objects;

public abstract class AbstractTextBoxAnnotationGraphNode extends JIPipeAnnotationGraphNode {
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

    public AbstractTextBoxAnnotationGraphNode(JIPipeNodeInfo info) {
        super(info);

        this.titleStyle = new TextRenderParameters();
        this.titleStyle.fontSize = 18;
        this.contentStyle = new TextRenderParameters();
        this.textLocation = new TextLocationParameters();

        registerSubParameter(titleStyle);
        registerSubParameter(contentStyle);
        registerSubParameter(textLocation);
    }

    public AbstractTextBoxAnnotationGraphNode(AbstractTextBoxAnnotationGraphNode other) {
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
                titleFont = new Font(this.titleStyle.fontFamily.getValue(), this.titleStyle.fontStyle.getNativeValue(), (int) Math.max(1, this.titleStyle.fontSize * zoom));
                FontMetrics fontMetrics = g2.getFontMetrics(titleFont);
                renderedTitle = StringUtils.limitWithEllipsis(textTitle, availableWidth, fontMetrics);
                renderedTextWidth = Math.max(renderedTextWidth, fontMetrics.stringWidth(renderedTitle));
                renderedTitleHeight = fontMetrics.getHeight();
            } else {
                renderedTitle = "";
            }

            if (!StringUtils.isNullOrEmpty(textContent)) {
                contentFont = new Font(this.contentStyle.fontFamily.getValue(), this.contentStyle.fontStyle.getNativeValue(), (int) Math.max(1, this.contentStyle.fontSize * zoom));
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

    public static class TextLocationParameters extends AbstractJIPipeParameterCollection {
        private Anchor anchor = Anchor.CenterCenter;
        private int marginLeft;
        private int marginRight;
        private int marginTop;
        private int marginBottom;

        public TextLocationParameters() {
        }

        public TextLocationParameters(TextLocationParameters other) {
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
        private FontFamilyParameter fontFamily = new FontFamilyParameter();
        private FontStyleParameter fontStyle = FontStyleParameter.Plain;
        private int fontSize = 12;
        private Color color = Color.BLACK;

        public TextRenderParameters() {
        }

        public TextRenderParameters(TextRenderParameters other) {
            this.fontFamily = new FontFamilyParameter(other.fontFamily);
            this.fontStyle = other.fontStyle;
            this.fontSize = other.fontSize;
            this.color = other.color;
        }

        @SetJIPipeDocumentation(name = "Color", description = "The color of the text")
        @JIPipeParameter("color")
        public Color getColor() {
            return color;
        }

        @JIPipeParameter("color")
        public void setColor(Color color) {
            this.color = color;
        }

        @SetJIPipeDocumentation(name = "Font", description = "The font family")
        @JIPipeParameter("font-family")
        public FontFamilyParameter getFontFamily() {
            return fontFamily;
        }

        @JIPipeParameter("font-family")
        public void setFontFamily(FontFamilyParameter fontFamily) {
            this.fontFamily = fontFamily;
        }

        @SetJIPipeDocumentation(name = "Font style", description = "The font style")
        @JIPipeParameter("font-style")
        public FontStyleParameter getFontStyle() {
            return fontStyle;
        }

        @JIPipeParameter("font-style")
        public void setFontStyle(FontStyleParameter fontStyle) {
            this.fontStyle = fontStyle;
        }

        @SetJIPipeDocumentation(name = "Font size", description = "The font size")
        @JIPipeParameter("font-size")
        public int getFontSize() {
            return fontSize;
        }

        @JIPipeParameter("font-size")
        public void setFontSize(int fontSize) {
            this.fontSize = fontSize;
        }
    }
}
