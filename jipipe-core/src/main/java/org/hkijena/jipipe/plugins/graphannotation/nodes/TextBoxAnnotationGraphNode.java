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

package org.hkijena.jipipe.plugins.graphannotation.nodes;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.categories.GraphAnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopAnnotationGraphNodeUI;

import java.awt.*;

@SetJIPipeDocumentation(name = "Text box", description = "An annotation that contains text")
@ConfigureJIPipeNode(nodeTypeCategory = GraphAnnotationsNodeTypeCategory.class)
public class TextBoxAnnotationGraphNode extends AbstractTextBoxAnnotationGraphNode {
    private final ShapeParameters shapeParameters;

    public TextBoxAnnotationGraphNode(JIPipeNodeInfo info) {
        super(info);
        this.shapeParameters = new ShapeParameters();
        this.setTextTitle("Text box");
        registerSubParameter(shapeParameters);
    }

    public TextBoxAnnotationGraphNode(TextBoxAnnotationGraphNode other) {
        super(other);
        this.shapeParameters = new ShapeParameters(other.shapeParameters);
        registerSubParameter(shapeParameters);
    }

    @SetJIPipeDocumentation(name = "Shape", description = "The following settings allow to modify the text box shape")
    @JIPipeParameter(value = "shape-parameters", uiOrder = -10)
    public ShapeParameters getShapeParameters() {
        return shapeParameters;
    }

    @Override
    protected void paintShape(Graphics2D g2, JIPipeDesktopAnnotationGraphNodeUI nodeUI, double zoom) {
        int nodeWidth = nodeUI.getWidth();
        int nodeHeight = nodeUI.getHeight();
        if (shapeParameters.fillColor.isEnabled()) {
            g2.setColor(shapeParameters.fillColor.getContent());
            g2.fillRect(0, 0, nodeWidth, nodeHeight);
        }
        int finalBorderThickness = (int) Math.max(1, shapeParameters.borderThickness * zoom);
        g2.setColor(shapeParameters.borderColor);
        g2.setStroke(new BasicStroke(finalBorderThickness));
        g2.drawRect(finalBorderThickness / 2, finalBorderThickness / 2, nodeWidth - finalBorderThickness, nodeHeight - finalBorderThickness);
    }

    @Override
    protected int getBorderThickness() {
        return shapeParameters.borderThickness;
    }

    public static class ShapeParameters extends AbstractJIPipeParameterCollection {
        private OptionalColorParameter fillColor = new OptionalColorParameter(new Color(255, 255, 204), true);
        private Color borderColor = new Color(255, 255, 204).darker();
        private int borderThickness = 1;

        public ShapeParameters() {

        }

        public ShapeParameters(ShapeParameters other) {
            this.fillColor = new OptionalColorParameter(other.fillColor);
            this.borderColor = other.borderColor;
            this.borderThickness = other.borderThickness;
        }

        @SetJIPipeDocumentation(name = "Fill color", description = "The fill color of this node")
        @JIPipeParameter("fill-color")
        public OptionalColorParameter getFillColor() {
            return fillColor;
        }

        @JIPipeParameter("fill-color")
        public void setFillColor(OptionalColorParameter fillColor) {
            this.fillColor = fillColor;
        }

        @SetJIPipeDocumentation(name = "Border color", description = "The border color of this node")
        @JIPipeParameter("border-color")
        public Color getBorderColor() {
            return borderColor;
        }

        @JIPipeParameter("border-color")
        public void setBorderColor(Color borderColor) {
            this.borderColor = borderColor;
        }

        @SetJIPipeDocumentation(name = "Border thickness", description = "The thickness of the border")
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
