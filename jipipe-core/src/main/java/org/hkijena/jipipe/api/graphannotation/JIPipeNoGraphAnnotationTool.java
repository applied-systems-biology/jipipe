package org.hkijena.jipipe.api.graphannotation;

import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class JIPipeNoGraphAnnotationTool implements JIPipeGraphAnnotationTool {
    @Override
    public String getName() {
        return "No tool";
    }

    @Override
    public String getTooltip() {
        return "Edit the graph and move annotations around";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/followmouse.png");
    }

    @Override
    public int getPriority() {
        return -99999;
    }
}
