package org.hkijena.jipipe.extensions.imagej2;

import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.scijava.command.Command;
import org.scijava.plugin.PluginInfo;

import java.util.List;
import java.util.Set;

/**
 * A {@link JIPipeNodeInfo} implementation that reads its information from an ImageJ2 {@link org.scijava.command.Command}
 */
public class ImageJ2NodeInfo implements JIPipeNodeInfo {

    private final PluginInfo<Command> pluginInfo;
    private final String id;

    public ImageJ2NodeInfo(PluginInfo<Command> pluginInfo) {
        this.pluginInfo = pluginInfo;
        this.id = "ij2:" + pluginInfo.getIdentifier();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Class<? extends JIPipeGraphNode> getInstanceClass() {
        return ImageJ2Algorithm.class;
    }

    @Override
    public JIPipeGraphNode newInstance() {
        return new ImageJ2Algorithm(this);
    }

    @Override
    public JIPipeGraphNode duplicate(JIPipeGraphNode algorithm) {
        return new ImageJ2Algorithm((ImageJ2Algorithm) algorithm);
    }

    @Override
    public String getName() {
        return "Imagej2 " + pluginInfo.getName();
    }

    @Override
    public HTMLText getDescription() {
        return null;
    }

    @Override
    public String getMenuPath() {
        return null;
    }

    @Override
    public JIPipeNodeTypeCategory getCategory() {
        return null;
    }

    @Override
    public List<JIPipeInputSlot> getInputSlots() {
        return null;
    }

    @Override
    public List<JIPipeOutputSlot> getOutputSlots() {
        return null;
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return null;
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    public PluginInfo<Command> getPluginInfo() {
        return pluginInfo;
    }
}
