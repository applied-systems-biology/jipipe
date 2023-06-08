package org.hkijena.jipipe.extensions.graphannotation.nodes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.nodes.JIPipeAnnotationGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.categories.GraphAnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.FontFamilyParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.FontStyleParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.roi.Anchor;
import org.hkijena.jipipe.extensions.parameters.library.roi.Margin;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeAnnotationGraphNodeUI;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Objects;

@JIPipeDocumentation(name = "Text box", description = "A box with a title and optional text")
@JIPipeNode(nodeTypeCategory = GraphAnnotationsNodeTypeCategory.class)
public class TextBoxAnnotationGraphNode extends JIPipeAnnotationGraphNode {

    private final ShapeParameters shapeParameters;
    private final TextRenderParameters titleStyle;
    private final TextRenderParameters contentStyle;
    private Anchor textLocation = Anchor.CenterCenter;
    private String textTitle = "Text box";
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



    public TextBoxAnnotationGraphNode(JIPipeNodeInfo info) {
        super(info);
        this.shapeParameters = new ShapeParameters();
        this.titleStyle = new TextRenderParameters();
        this.titleStyle.fontSize = 18;
        this.contentStyle = new TextRenderParameters();
        registerSubParameter(shapeParameters);
        registerSubParameter(titleStyle);
        registerSubParameter(contentStyle);
    }

    public TextBoxAnnotationGraphNode(TextBoxAnnotationGraphNode other) {
        super(other);
        this.shapeParameters = new ShapeParameters(other.shapeParameters);
        this.titleStyle = new TextRenderParameters(other.titleStyle);
        this.contentStyle = new TextRenderParameters(other.contentStyle);
        this.textLocation = other.textLocation;
        this.textTitle = other.textTitle;
        this.textContent = other.textContent;
        registerSubParameter(titleStyle);
        registerSubParameter(contentStyle);
        registerSubParameter(shapeParameters);
    }



    @JIPipeDocumentation(name = "Title style", description = "The following parameters determine how the title is rendered")
    @JIPipeParameter("title-style")
    public TextRenderParameters getTitleStyle() {
        return titleStyle;
    }

    @JIPipeDocumentation(name = "Text location", description = "Determines the location of the text")
    @JIPipeParameter("text-location")
    public Anchor getTextLocation() {
        return textLocation;
    }

    @JIPipeParameter("text-location")
    public void setTextLocation(Anchor textLocation) {
        this.textLocation = textLocation;
    }

    @JIPipeDocumentation(name = "Description style", description = "The following parameters determine how the description is rendered")
    @JIPipeParameter("content-style")
    public TextRenderParameters getContentStyle() {
        return contentStyle;
    }

    @JIPipeDocumentation(name = "Shape", description = "The following settings allow to modify the text box shape")
    @JIPipeParameter("shape-parameters")
    public ShapeParameters getShapeParameters() {
        return shapeParameters;
    }

    @JIPipeDocumentation(name = "Title", description = "The title of the text box")
    @JIPipeParameter(value = "text-title", important = true, uiOrder = -100)
    public String getTextTitle() {
        return textTitle;
    }

    @JIPipeParameter("text-title")
    public void setTextTitle(String textTitle) {
        this.textTitle = textTitle;
    }

    @JIPipeDocumentation(name = "Content", description = "The additional text content of the text box")
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
    public void paintNode(Graphics2D g2, JIPipeAnnotationGraphNodeUI nodeUI, double zoom) {

        updateAssetsIfNeeded(g2, nodeUI, zoom);

        if(shapeParameters.fillColor.isEnabled()) {
            g2.setColor(shapeParameters.fillColor.getContent());
            g2.fillRect(0, 0, nodeUI.getWidth(), nodeUI.getHeight());
        }
        int finalBorderThickness = (int) Math.max(1, shapeParameters.borderThickness * zoom);
        g2.setColor(shapeParameters.borderColor);
        g2.setStroke(new BasicStroke(finalBorderThickness));
        g2.drawRect(finalBorderThickness / 2,finalBorderThickness / 2, nodeUI.getWidth() - finalBorderThickness, nodeUI.getHeight() - finalBorderThickness);

        int finalTextWidth = renderedTextWidth;
        int finalTextHeight = renderedTitleHeight + renderedContentHeight;

        switch (textLocation) {
            case CenterCenter: {
                if(!StringUtils.isNullOrEmpty(renderedTitle)) {
                    g2.setColor(titleStyle.getColor());
                    g2.setFont(titleFont);
                    g2.drawString(renderedTitle, nodeUI.getWidth() / 2 - finalTextWidth / 2, nodeUI.getHeight() / 2 - finalTextHeight / 2 + g2.getFontMetrics().getAscent());
                }
                if(renderedContent != null && renderedContent.length > 0) {
                    g2.setFont(contentFont);
                    int lineHeight = g2.getFontMetrics().getHeight();
                    for (int i = 0; i < renderedContent.length; i++) {
                        g2.drawString(renderedContent[i], nodeUI.getWidth() / 2 - finalTextWidth / 2, nodeUI.getHeight() / 2 - finalTextHeight / 2 + renderedTitleHeight + i * lineHeight + g2.getFontMetrics().getAscent());
                    }
                }
            }
        }

    }

    private void updateAssetsIfNeeded(Graphics2D g2, JIPipeAnnotationGraphNodeUI nodeUI, double zoom)  {
        if(!Objects.equals(renderedContentRaw, textContent) || !Objects.equals(renderedTitleRaw, textTitle) || renderedZoom != zoom) {

            int finalBorderThickness = (int) Math.max(1, shapeParameters.borderThickness * zoom);
            renderedTextWidth = 0;
            renderedTitleHeight = 0;
            renderedContentHeight = 0;

            if(!StringUtils.isNullOrEmpty(textTitle)) {
                titleFont = new Font(this.titleStyle.fontFamily.getValue(), this.titleStyle.fontStyle.getNativeValue(), (int) Math.max(1, this.titleStyle.fontSize * zoom));
                FontMetrics fontMetrics = g2.getFontMetrics(titleFont);
                renderedTitle = StringUtils.limitWithEllipsis(textTitle, nodeUI.getWidth() - finalBorderThickness * 2, fontMetrics);
                renderedTextWidth = Math.max(renderedTextWidth, fontMetrics.stringWidth(renderedTitle));
                renderedTitleHeight = fontMetrics.getHeight();
            }
            else {
                renderedTitle = "";
            }

            if(!StringUtils.isNullOrEmpty(textContent)) {
                contentFont = new Font(this.contentStyle.fontFamily.getValue(), this.contentStyle.fontStyle.getNativeValue(), (int) Math.max(1, this.contentStyle.fontSize * zoom));
                FontMetrics fontMetrics = g2.getFontMetrics(contentFont);
                renderedContent = textContent.split("\n");
                for (int i = 0; i < renderedContent.length; i++) {
                    renderedContent[i] = StringUtils.limitWithEllipsis(renderedContent[i], nodeUI.getWidth() - finalBorderThickness * 2, fontMetrics);
                    renderedTextWidth = Math.max(renderedTextWidth, fontMetrics.stringWidth(renderedContent[i]));
                    renderedContentHeight += fontMetrics.getHeight();
                }
            }
            else {
                renderedContent = null;
            }

            renderedContentRaw = textContent;
            renderedTitleRaw = textTitle;
            renderedZoom = zoom;
        }
    }

    public static class ShapeParameters extends AbstractJIPipeParameterCollection {
        private OptionalColorParameter fillColor = new OptionalColorParameter(new Color(255, 255, 204), false);
        private Color borderColor = new Color(255, 255, 204).darker();
        private int borderThickness = 1;

        public ShapeParameters() {

        }

        public ShapeParameters(ShapeParameters other) {
            this.fillColor = new OptionalColorParameter(other.fillColor);
            this.borderColor = other.borderColor;
            this.borderThickness = other.borderThickness;
        }

        @JIPipeDocumentation(name = "Fill color", description = "The fill color of this node")
        @JIPipeParameter("fill-color")
        public OptionalColorParameter getFillColor() {
            return fillColor;
        }

        @JIPipeParameter("fill-color")
        public void setFillColor(OptionalColorParameter fillColor) {
            this.fillColor = fillColor;
        }

        @JIPipeDocumentation(name = "Border color", description = "The border color of this node")
        @JIPipeParameter("border-color")
        public Color getBorderColor() {
            return borderColor;
        }

        @JIPipeParameter("border-color")
        public void setBorderColor(Color borderColor) {
            this.borderColor = borderColor;
        }

        @JIPipeDocumentation(name = "Border thickness", description = "The thickness of the border")
        @JIPipeParameter("border-thickness")
        public int getBorderThickness() {
            return borderThickness;
        }

        @JIPipeParameter("border-thickness")
        public void setBorderThickness(int borderThickness) {
            this.borderThickness = borderThickness;
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

        @JIPipeDocumentation(name = "Color", description = "The color of the text")
        @JIPipeParameter("color")
        public Color getColor() {
            return color;
        }

        @JIPipeParameter("color")
        public void setColor(Color color) {
            this.color = color;
        }

        @JIPipeDocumentation(name = "Font", description = "The font family")
        @JIPipeParameter("font-family")
        public FontFamilyParameter getFontFamily() {
            return fontFamily;
        }

        @JIPipeParameter("font-family")
        public void setFontFamily(FontFamilyParameter fontFamily) {
            this.fontFamily = fontFamily;
        }

        @JIPipeDocumentation(name = "Font style", description = "The font style")
        @JIPipeParameter("font-style")
        public FontStyleParameter getFontStyle() {
            return fontStyle;
        }

        @JIPipeParameter("font-style")
        public void setFontStyle(FontStyleParameter fontStyle) {
            this.fontStyle = fontStyle;
        }

        @JIPipeDocumentation(name = "Font size", description = "The font size")
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
