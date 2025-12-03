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

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.LabelAsJIPipeHidden;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.categories.GraphAnnotationsNodeTypeCategory;
import org.hkijena.jipipe.plugins.parameters.library.roi.Anchor;

@SetJIPipeDocumentation(name = "Group box canvas note", description = "A text box canvas note pre-configured to contain a title at the top left")
@ConfigureJIPipeNode(nodeTypeCategory = GraphAnnotationsNodeTypeCategory.class)
@LabelAsJIPipeHidden
public class GroupBoxGraphCanvasNote extends TextBoxGraphCanvasNote {
    public GroupBoxGraphCanvasNote(JIPipeNodeInfo info) {
        super(info);
        setTextTitle("Group");
        getShapeParameters().getFillColor().setEnabled(false);
        getTextLocation().setAnchor(Anchor.TopLeft);
        getTextLocation().setMarginLeft(8);
        getTextLocation().setMarginTop(8);
        getTextLocation().setMarginRight(8);
        getTextLocation().setMarginBottom(8);
    }

    public GroupBoxGraphCanvasNote(TextBoxGraphCanvasNote other) {
        super(other);
    }
}
