package org.hkijena.jipipe.extensions.nodetemplate;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

@Plugin(type = JIPipeJavaExtension.class)
public class NodeTemplateBoxToolExtension extends JIPipePrepackagedDefaultJavaExtension {
    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "Node templates";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Provides a tool that allows to use node templates");
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:node-templates";
    }

    @Override
    public String getDependencyVersion() {
        return "1.70.1";
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        registerContextMenuAction(new AddTemplateContextMenuAction());
    }
}
