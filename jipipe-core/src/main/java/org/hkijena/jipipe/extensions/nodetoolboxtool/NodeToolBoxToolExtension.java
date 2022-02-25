package org.hkijena.jipipe.extensions.nodetoolboxtool;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

@Plugin(type = JIPipeJavaExtension.class)
public class NodeToolBoxToolExtension extends JIPipePrepackagedDefaultJavaExtension {
    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "Node toolbox";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Provides a tool that allows dragging node types into a graph");
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:node-toolbox";
    }

    @Override
    public String getDependencyVersion() {
        return "1.65.0";
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        registerGraphEditorToolBarButtonExtension(NodeToolBoxMenuExtension.class);
    }
}
