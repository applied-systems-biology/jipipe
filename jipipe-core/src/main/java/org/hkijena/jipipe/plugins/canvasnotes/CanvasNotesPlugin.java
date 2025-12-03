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

package org.hkijena.jipipe.plugins.canvasnotes;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.canvasnotes.nodes.ArrowGraphCanvasNote;
import org.hkijena.jipipe.plugins.canvasnotes.nodes.ImageBoxGraphCanvasNote;
import org.hkijena.jipipe.plugins.canvasnotes.nodes.TextBoxGraphCanvasNote;
import org.hkijena.jipipe.plugins.canvasnotes.tools.ArrowNoteGraphNodeTool;
import org.hkijena.jipipe.plugins.canvasnotes.tools.EditNotesGraphNodeTool;
import org.hkijena.jipipe.plugins.canvasnotes.tools.ImageBoxNoteGraphNodeTool;
import org.hkijena.jipipe.plugins.canvasnotes.tools.TextBoxNoteGraphNodeTool;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

@Plugin(type = JIPipeJavaPlugin.class)
public class CanvasNotesPlugin extends JIPipePrepackagedDefaultJavaPlugin {

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
        registerAnnotationNodeType("graph-annotation-text-box", TextBoxGraphCanvasNote.class, TextBoxNoteGraphNodeTool.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-text-frame.png"));
        registerAnnotationNodeType("graph-annotation-arrow", ArrowGraphCanvasNote.class, ArrowNoteGraphNodeTool.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-arrow.png"));
        registerAnnotationNodeType("graph-annotation-image", ImageBoxGraphCanvasNote.class, ImageBoxNoteGraphNodeTool.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-image.png"));
        registerGraphEditorTool(EditNotesGraphNodeTool.class);
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
