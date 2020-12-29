package org.hkijena.jipipe.extensions.nodetoolboxtool;

import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
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
    public String getDescription() {
        return "Provides a tool that allows dragging node types into a graph";
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:node-toolbox";
    }

    @Override
    public String getDependencyVersion() {
        return "2020.12";
    }

    @Override
    public void register() {
        registerMenuExtension(NodeToolBoxMenuExtension.class);
    }
}
