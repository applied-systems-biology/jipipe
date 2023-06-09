package org.hkijena.jipipe.extensions.graphannotation.nodes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.extensions.parameters.library.images.ImageParameter;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeAnnotationGraphNodeUI;
import org.hkijena.jipipe.utils.SizeFitMode;

import java.awt.*;
import java.awt.image.BufferedImage;

@JIPipeDocumentation(name = "Image box", description = "An annotation that contains an image")
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
        if(imageParameters.backgroundColor.isEnabled()) {
            g2.setColor(imageParameters.backgroundColor.getContent());
            g2.fillRect(0, 0, nodeWidth, nodeHeight);
        }
        int finalBorderThickness = (int) Math.max(1, imageParameters.borderThickness * zoom);

        // Image
        BufferedImage image = imageParameters.getImage().getImage();
        if(image != null) {
            Dimension fitted = imageParameters.getFitMode().fitSize(nodeWidth, nodeHeight, image.getWidth(), image.getHeight(), 1);
            g2.drawImage(image, nodeWidth / 2 - fitted.width / 2, nodeHeight / 2 - fitted.height / 2, fitted.width, fitted.height, null);
        }

        // Border
        g2.setColor(imageParameters.borderColor);
        g2.setStroke(new BasicStroke(finalBorderThickness));
        g2.drawRect(finalBorderThickness / 2,finalBorderThickness / 2, nodeWidth - finalBorderThickness, nodeHeight - finalBorderThickness);
    }

    @Override
    protected int getBorderThickness() {
        return imageParameters.borderThickness;
    }

    public static class ImageParameters extends AbstractJIPipeParameterCollection {
        private OptionalColorParameter backgroundColor = new OptionalColorParameter(new Color(255, 255, 204), true);
        private Color borderColor = new Color(255, 255, 204).darker();
        private int borderThickness = 1;

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
    }
}
