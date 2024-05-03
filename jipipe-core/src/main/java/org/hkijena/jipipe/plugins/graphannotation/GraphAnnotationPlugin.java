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

package org.hkijena.jipipe.plugins.graphannotation;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.graphannotation.nodes.ArrowAnnotationGraphNode;
import org.hkijena.jipipe.plugins.graphannotation.nodes.GroupBoxAnnotationGraphNode;
import org.hkijena.jipipe.plugins.graphannotation.nodes.ImageBoxAnnotationGraphNode;
import org.hkijena.jipipe.plugins.graphannotation.nodes.TextBoxAnnotationGraphNode;
import org.hkijena.jipipe.plugins.graphannotation.tools.ArrowAnnotationGraphNodeTool;
import org.hkijena.jipipe.plugins.graphannotation.tools.GroupBoxAnnotationGraphNodeTool;
import org.hkijena.jipipe.plugins.graphannotation.tools.ImageBoxAnnotationGraphNodeTool;
import org.hkijena.jipipe.plugins.graphannotation.tools.TextBoxAnnotationGraphNodeTool;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

@Plugin(type = JIPipeJavaPlugin.class)
public class GraphAnnotationPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "Graph annotations";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Provides the default graph annotations");
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        registerAnnotationNodeType("graph-annotation-text-box", TextBoxAnnotationGraphNode.class, TextBoxAnnotationGraphNodeTool.class, UIUtils.getIconURLFromResources("actions/insert-text-frame.png"));
        registerAnnotationNodeType("graph-annotation-group-box", GroupBoxAnnotationGraphNode.class, GroupBoxAnnotationGraphNodeTool.class, UIUtils.getIconURLFromResources("actions/object-group.png"));
        registerAnnotationNodeType("graph-annotation-arrow", ArrowAnnotationGraphNode.class, ArrowAnnotationGraphNodeTool.class, UIUtils.getIconURLFromResources("actions/draw-arrow.png"));
        registerAnnotationNodeType("graph-annotation-image", ImageBoxAnnotationGraphNode.class, ImageBoxAnnotationGraphNodeTool.class, UIUtils.getIconURLFromResources("actions/insert-image.png"));
    }

    @Override
    public String getDependencyId() {
        return "jipipe:graph-annotations";
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
    }

    @Override
    public boolean isCoreExtension() {
        return true;
    }
}
