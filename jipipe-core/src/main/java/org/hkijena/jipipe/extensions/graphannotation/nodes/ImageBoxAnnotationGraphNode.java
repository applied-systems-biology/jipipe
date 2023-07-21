package org.hkijena.jipipe.extensions.graphannotation.nodes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.categories.GraphAnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.extensions.parameters.library.images.ImageParameter;
import org.hkijena.jipipe.extensions.parameters.library.roi.Anchor;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeAnnotationGraphNodeUI;
import org.hkijena.jipipe.utils.SizeFitMode;

import java.awt.*;
import java.awt.image.BufferedImage;

@JIPipeDocumentation(name = "Image box", description = "An annotation that contains an image")
@JIPipeNode(nodeTypeCategory = GraphAnnotationsNodeTypeCategory.class)
public class ImageBoxAnnotationGraphNode extends AbstractTextBoxAnnotationGraphNode {
    private final ImageParameters imageParameters;

    public ImageBoxAnnotationGraphNode(JIPipeNodeInfo info) {
        super(info);
        this.imageParameters = new ImageParameters();
        registerSubParameter(imageParameters);
    }

    public ImageBoxAnnotationGraphNode(ImageBoxAnnotationGraphNode other) {
        super(other);
        this.imageParameters = new ImageParameters(other.imageParameters);
        registerSubParameter(imageParameters);
    }

    @JIPipeDocumentation(name = "Image", description = "The following settings allow to modify the shape/image")
    @JIPipeParameter(value = "image-parameters", uiOrder = -10)
    public ImageParameters getImageParameters() {
        return imageParameters;
    }

    @Override
    protected void paintShape(Graphics2D g2, JIPipeAnnotationGraphNodeUI nodeUI, double zoom) {
        int nodeWidth = nodeUI.getWidth();
        int nodeHeight = nodeUI.getHeight();
        if (imageParameters.backgroundColor.isEnabled()) {
            g2.setColor(imageParameters.backgroundColor.getContent());
            g2.fillRect(0, 0, nodeWidth, nodeHeight);
        }
        int finalBorderThickness = (int) Math.max(1, imageParameters.borderThickness * zoom);

        // Image
        BufferedImage image = imageParameters.getImage().getImage();
        if (image != null) {
            Dimension fitted = imageParameters.getFitMode().fitSize(nodeWidth, nodeHeight, image.getWidth(), image.getHeight(), 1);
            int fittedWidth = (int) (fitted.width * Math.max(0.01, imageParameters.scaleX));
            int fittedHeight = (int) (fitted.height * Math.max(0.01, imageParameters.scaleY));
            int centerX = nodeWidth / 2 - fittedWidth / 2;
            int centerY = nodeHeight / 2 - fittedHeight / 2;
            int finalMarginTop = (int) (imageParameters.marginTop * zoom);
            int finalMarginLeft = (int) (imageParameters.marginLeft * zoom);
            int finalMarginRight = (int) (imageParameters.marginRight * zoom);
            int finalMarginBottom = (int) (imageParameters.marginBottom * zoom);
            switch (imageParameters.anchor) {
                case TopLeft:
                    g2.drawImage(image, finalBorderThickness + finalMarginLeft, finalBorderThickness + finalMarginTop, fittedWidth, fittedHeight, null);
                    break;
                case TopCenter:
                    g2.drawImage(image, centerX, finalBorderThickness + finalMarginTop, fittedWidth, fittedHeight, null);
                    break;
                case TopRight:
                    g2.drawImage(image, nodeWidth - finalBorderThickness - fittedWidth - finalMarginRight, finalBorderThickness + finalMarginTop, fittedWidth, fittedHeight, null);
                    break;
                case CenterLeft:
                    g2.drawImage(image, finalBorderThickness + finalMarginLeft, centerY, fittedWidth, fittedHeight, null);
                    break;
                case CenterCenter:
                    g2.drawImage(image, centerX, centerY, fittedWidth, fittedHeight, null);
                    break;
                case CenterRight:
                    g2.drawImage(image, nodeWidth - finalBorderThickness - fittedWidth - finalMarginRight, centerY, fittedWidth, fittedHeight, null);
                    break;
                case BottomLeft:
                    g2.drawImage(image, finalBorderThickness + finalMarginLeft, nodeHeight - finalBorderThickness - fittedHeight - finalMarginBottom, fittedWidth, fittedHeight, null);
                    break;
                case BottomCenter:
                    g2.drawImage(image, centerX, nodeHeight - finalBorderThickness - fittedHeight - finalMarginBottom, fittedWidth, fittedHeight, null);
                    break;
                case BottomRight:
                    g2.drawImage(image, nodeWidth - finalBorderThickness - fittedWidth - finalMarginRight, nodeHeight - finalBorderThickness - fittedHeight - finalMarginBottom, fittedWidth, fittedHeight, null);
                    break;
            }
        }

        // Border
        g2.setColor(imageParameters.borderColor);
        g2.setStroke(new BasicStroke(finalBorderThickness));
        g2.drawRect(finalBorderThickness / 2, finalBorderThickness / 2, nodeWidth - finalBorderThickness, nodeHeight - finalBorderThickness);
    }

    @Override
    protected int getBorderThickness() {
        return imageParameters.borderThickness;
    }

    public static class ImageParameters extends AbstractJIPipeParameterCollection {
        private OptionalColorParameter backgroundColor = new OptionalColorParameter(new Color(255, 255, 204), true);
        private Color borderColor = new Color(255, 255, 204).darker();
        private int borderThickness = 1;

        private Anchor anchor = Anchor.CenterCenter;

        private double scaleX = 1;
        private double scaleY = 1;

        private int marginLeft;
        private int marginRight;
        private int marginTop;
        private int marginBottom;

        private ImageParameter image = new ImageParameter();

        private SizeFitMode fitMode = SizeFitMode.Fit;

        public ImageParameters() {

        }

        public ImageParameters(ImageParameters other) {
            this.backgroundColor = new OptionalColorParameter(other.backgroundColor);
            this.borderColor = other.borderColor;
            this.borderThickness = other.borderThickness;
            this.image = new ImageParameter(other.image);
            this.fitMode = other.fitMode;
            this.anchor = other.anchor;
            this.scaleX = other.scaleX;
            this.scaleY = other.scaleY;
            this.marginLeft = other.marginLeft;
            this.marginRight = other.marginRight;
            this.marginTop = other.marginTop;
            this.marginBottom = other.marginBottom;
        }

        @JIPipeDocumentation(name = "Image", description = "The image to be displayed")
        @JIPipeParameter("image")
        public ImageParameter getImage() {
            return image;
        }

        @JIPipeParameter("image")
        public void setImage(ImageParameter image) {
            this.image = image;
        }

        @JIPipeDocumentation(name = "Fit image", description = "How the image is fit into the node")
        @JIPipeParameter("fit-mode")
        public SizeFitMode getFitMode() {
            return fitMode;
        }

        @JIPipeParameter("fit-mode")
        public void setFitMode(SizeFitMode fitMode) {
            this.fitMode = fitMode;
        }

        @JIPipeDocumentation(name = "Background color", description = "The background color of this node")
        @JIPipeParameter("fill-color")
        public OptionalColorParameter getBackgroundColor() {
            return backgroundColor;
        }

        @JIPipeParameter("fill-color")
        public void setBackgroundColor(OptionalColorParameter backgroundColor) {
            this.backgroundColor = backgroundColor;
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

        @JIPipeDocumentation(name = "Anchor", description = "Determines to which anchor location the image is attached to.")
        @JIPipeParameter("anchor")
        public Anchor getAnchor() {
            return anchor;
        }

        @JIPipeParameter("anchor")
        public void setAnchor(Anchor anchor) {
            this.anchor = anchor;
        }

        @JIPipeDocumentation(name = "Scale (X)", description = "Custom image scale in X direction")
        @JIPipeParameter("scale-x")
        public double getScaleX() {
            return scaleX;
        }

        @JIPipeParameter("scale-x")
        public void setScaleX(double scaleX) {
            this.scaleX = scaleX;
        }

        @JIPipeDocumentation(name = "Scale (Y)", description = "Custom image scale in Y direction")
        @JIPipeParameter("scale-y")
        public double getScaleY() {
            return scaleY;
        }

        @JIPipeParameter("scale-y")
        public void setScaleY(double scaleY) {
            this.scaleY = scaleY;
        }

        @JIPipeDocumentation(name = "Margin left", description = "The left margin of the available text area relative to the node border")
        @JIPipeParameter("margin-left")
        public int getMarginLeft() {
            return marginLeft;
        }

        @JIPipeParameter("margin-left")
        public void setMarginLeft(int marginLeft) {
            this.marginLeft = marginLeft;
        }

        @JIPipeDocumentation(name = "Margin right", description = "The right margin of the available text area relative to the node border")
        @JIPipeParameter("margin-right")
        public int getMarginRight() {
            return marginRight;
        }

        @JIPipeParameter("margin-right")
        public void setMarginRight(int marginRight) {
            this.marginRight = marginRight;
        }

        @JIPipeDocumentation(name = "Margin top", description = "The top margin of the available text area relative to the node border")
        @JIPipeParameter("margin-top")
        public int getMarginTop() {
            return marginTop;
        }

        @JIPipeParameter("margin-top")
        public void setMarginTop(int marginTop) {
            this.marginTop = marginTop;
        }

        @JIPipeDocumentation(name = "Margin bottom", description = "The bottom margin of the available text area relative to the node border")
        @JIPipeParameter("margin-bottom")
        public int getMarginBottom() {
            return marginBottom;
        }

        @JIPipeParameter("margin-bottom")
        public void setMarginBottom(int marginBottom) {
            this.marginBottom = marginBottom;
        }
    }
}
