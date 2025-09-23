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
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.graphannotation.nodes.ArrowAnnotationGraphNode;
import org.hkijena.jipipe.plugins.graphannotation.nodes.GroupBoxAnnotationGraphNode;
import org.hkijena.jipipe.plugins.graphannotation.nodes.ImageBoxAnnotationGraphNode;
import org.hkijena.jipipe.plugins.graphannotation.nodes.TextBoxAnnotationGraphNode;
import org.hkijena.jipipe.plugins.graphannotation.tools.*;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
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
    public void register(JIPipeService service, Context context, JIPipeProgressInfo progressInfo) {
        registerAnnotationNodeType("graph-annotation-text-box", TextBoxAnnotationGraphNode.class, TextBoxAnnotationGraphNodeTool.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-text-frame.png"));
        registerAnnotationNodeType("graph-annotation-group-box", GroupBoxAnnotationGraphNode.class, GroupBoxAnnotationGraphNodeTool.class, JIPipe.RESOURCES.getIcon16URL("actions/object-group.png"));
        registerAnnotationNodeType("graph-annotation-arrow", ArrowAnnotationGraphNode.class, ArrowAnnotationGraphNodeTool.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-arrow.png"));
        registerAnnotationNodeType("graph-annotation-image", ImageBoxAnnotationGraphNode.class, ImageBoxAnnotationGraphNodeTool.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-image.png"));
        registerGraphEditorTool(EditAnnotationGraphNodeTool.class);
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
    public boolean isCorePlugin() {
        return true;
    }
}
