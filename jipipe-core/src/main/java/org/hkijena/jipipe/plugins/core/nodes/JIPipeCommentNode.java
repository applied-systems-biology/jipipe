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

package org.hkijena.jipipe.plugins.core.nodes;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.plugins.parameters.library.references.IconRef;

import java.awt.*;

@SetJIPipeDocumentation(name = "Comment", description = "Allows you to comment an input or output slot. You can customize the color and icon of this node. " +
        "This nodes has no workload attached to it and will connect to any data type. It requires no input and produces no output.")
@AddJIPipeInputSlot(value = JIPipeData.class, slotName = "Comment", create = true, optional = true)
@AddJIPipeOutputSlot(value = JIPipeData.class, slotName = "Comment", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
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
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {

    }

    @Override
    public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {

    }

    @SetJIPipeDocumentation(name = "Background color", description = "Defines the background color of this comment node.")
    @JIPipeParameter("background-color")
    public Color getBackgroundColor() {
        return backgroundColor;
    }

    @JIPipeParameter("background-color")
    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    @SetJIPipeDocumentation(name = "Text color", description = "Defines the text color of this comment node.")
    @JIPipeParameter("text-color")
    public Color getTextColor() {
        return textColor;
    }

    @JIPipeParameter("text-color")
    public void setTextColor(Color textColor) {
        this.textColor = textColor;
    }

    @SetJIPipeDocumentation(name = "Icon", description = "Defines the icon of this comment node.")
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
