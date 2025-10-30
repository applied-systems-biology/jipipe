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

package org.hkijena.jipipe.plugins.graphannotation.tools;

import org.hkijena.jipipe.api.nodes.annotation.JIPipeAnnotationGraphNodeTool;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.contextpanel.JIPipeDesktopGraphEditorContextPanelIsland;
import org.hkijena.jipipe.desktop.commons.components.color.palette.JIPipeDesktopColorPalette;
import org.hkijena.jipipe.desktop.commons.components.color.palette.JIPipeDesktopColorPaletteColor;
import org.hkijena.jipipe.plugins.graphannotation.nodes.TextBoxAnnotationGraphNode;
import org.hkijena.jipipe.plugins.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.plugins.parameters.library.roi.Anchor;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class TextBoxAnnotationGraphNodeTool extends JIPipeAnnotationGraphNodeTool<TextBoxAnnotationGraphNode> {

    private JIPipeDesktopColorPaletteColor color = JIPipeDesktopColorPalette.PASTEL[0];
    private Anchor anchor = Anchor.CenterCenter;


    public TextBoxAnnotationGraphNodeTool() {
        super(TextBoxAnnotationGraphNode.class);
    }

    @Override
    protected TextBoxAnnotationGraphNode createAndConfigureNode(Point firstPoint, Point secondPoint) {
        TextBoxAnnotationGraphNode node = super.createAndConfigureNode(firstPoint, secondPoint);
        String title = JOptionPane.showInputDialog(getDesktopWorkbench().getWindow(), "Please input the title:", "Create text box", JOptionPane.PLAIN_MESSAGE);
        if (!StringUtils.isNullOrEmpty(title)) {
            node.setTextTitle(title);
        }

        JIPipeDesktopColorPaletteColor color = getColor();
        node.getShapeParameters().setBorderColor(color.getForeground());
        node.getShapeParameters().setFillColor(new OptionalColorParameter(color.getBackground(), true));
        node.getTextLocation().setAnchor(anchor);
        node.getTextLocation().setMarginBottom(5);
        node.getTextLocation().setMarginLeft(5);
        node.getTextLocation().setMarginRight(5);
        node.getTextLocation().setMarginTop(5);

        return node;
    }

    @Override
    public KeyStroke getKeyBinding() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0);
    }

    @Override
    public int getPriority() {
        return -5000;
    }

    @Override
    public JIPipeDesktopGraphEditorContextPanelIsland createPropertiesPanel(JIPipeDesktopGraphEditorUI graphEditorUI) {
        return new TextBoxAnnotationGraphNodeToolProperties(graphEditorUI, this);
    }

    public JIPipeDesktopColorPaletteColor getColor() {
        return color;
    }

    public void setColor(JIPipeDesktopColorPaletteColor color) {
        this.color = color;
    }

    public void setAnchor(Anchor anchor) {
        this.anchor = anchor;
    }

    public Anchor getAnchor() {
        return anchor;
    }
}
