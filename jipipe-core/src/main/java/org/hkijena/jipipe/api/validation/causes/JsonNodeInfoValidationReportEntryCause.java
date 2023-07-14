package org.hkijena.jipipe.api.validation.causes;

import org.hkijena.jipipe.api.grouping.JsonNodeInfo;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryCause;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class JsonNodeInfoValidationReportEntryCause extends JIPipeValidationReportEntryCause {

    private final JsonNodeInfo nodeInfo;

    public JsonNodeInfoValidationReportEntryCause(JsonNodeInfo nodeInfo) {
        this.nodeInfo = nodeInfo;
    }

    public JsonNodeInfoValidationReportEntryCause(JIPipeValidationReportEntryCause parent, JsonNodeInfo nodeInfo) {
        super(parent);
        this.nodeInfo = nodeInfo;
    }

    @Override
    public boolean canNavigate(JIPipeWorkbench workbench) {
        return false;
    }

    @Override
    public void navigate(JIPipeWorkbench workbench) {

    }

    @Override
    public String renderName() {
        return "Node type '" + nodeInfo.getName() + "' [" + nodeInfo.getId() + "]";
    }

    @Override
    public Icon renderIcon() {
        return UIUtils.getIconFromResources("actions/graph-node.png");
    }
}
