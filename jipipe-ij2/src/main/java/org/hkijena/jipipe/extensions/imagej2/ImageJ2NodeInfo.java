package org.hkijena.jipipe.extensions.imagej2;

import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;

import java.util.List;
import java.util.Set;

/**
 * A {@link JIPipeNodeInfo} implementation that reads its information from an ImageJ2 {@link org.scijava.command.Command}
 */
public class ImageJ2NodeInfo implements JIPipeNodeInfo {
    @Override
    public String getId() {
        return null;
    }

    @Override
    public Class<? extends JIPipeGraphNode> getInstanceClass() {
        return null;
    }

    @Override
    public JIPipeGraphNode newInstance() {
        return null;
    }

    @Override
    public JIPipeGraphNode duplicate(JIPipeGraphNode algorithm) {
        return null;
    }

    @Override
    public String getName() {
        return null;
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
}
