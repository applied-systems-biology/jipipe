package org.hkijena.jipipe.extensions.core.nodes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryCause;
import org.hkijena.jipipe.extensions.parameters.library.references.IconRef;

import java.awt.*;

@JIPipeDocumentation(name = "Comment", description = "Allows you to comment an input or output slot. You can customize the color and icon of this node. " +
        "This nodes has no workload attached to it and will connect to any data type. It requires no input and produces no output.")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Comment", autoCreate = true, optional = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Comment", autoCreate = true)
@JIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
public class JIPipeCommentNode extends JIPipeGraphNode {

    private Color backgroundColor = new Color(255, 255, 204);
    private Color textColor = Color.BLACK;
    private IconRef icon = new IconRef("actions/edit-comment.png");

    public JIPipeCommentNode(JIPipeNodeInfo info) {
        super(info);
    }

    public JIPipeCommentNode(JIPipeCommentNode other) {
        super(other);
        this.backgroundColor = other.backgroundColor;
        this.textColor = other.textColor;
        this.icon = new IconRef(other.getIcon());
    }

    @Override
    public void reportValidity(JIPipeValidationReportEntryCause parentCause, JIPipeValidationReport report) {

    }

    @Override
    public void run(JIPipeProgressInfo progressInfo) {

    }

    @JIPipeDocumentation(name = "Background color", description = "Defines the background color of this comment node.")
    @JIPipeParameter("background-color")
    public Color getBackgroundColor() {
        return backgroundColor;
    }

    @JIPipeParameter("background-color")
    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    @JIPipeDocumentation(name = "Text color", description = "Defines the text color of this comment node.")
    @JIPipeParameter("text-color")
    public Color getTextColor() {
        return textColor;
    }

    @JIPipeParameter("text-color")
    public void setTextColor(Color textColor) {
        this.textColor = textColor;
    }

    @JIPipeDocumentation(name = "Icon", description = "Defines the icon of this comment node.")
    @JIPipeParameter("icon")
    public IconRef getIcon() {
        if (icon == null) {
            icon = new IconRef("actions/edit-comment.png");
        }
        return icon;
    }

    @JIPipeParameter("icon")
    public void setIcon(IconRef icon) {
        this.icon = icon;
    }
}
