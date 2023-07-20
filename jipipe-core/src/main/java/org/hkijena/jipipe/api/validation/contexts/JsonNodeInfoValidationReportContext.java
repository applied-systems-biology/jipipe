package org.hkijena.jipipe.api.validation.contexts;

import org.hkijena.jipipe.api.grouping.JsonNodeInfo;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class JsonNodeInfoValidationReportContext extends JIPipeValidationReportContext {

    private final JsonNodeInfo nodeInfo;

    public JsonNodeInfoValidationReportContext(JsonNodeInfo nodeInfo) {
        this.nodeInfo = nodeInfo;
    }

    public JsonNodeInfoValidationReportContext(JIPipeValidationReportContext parent, JsonNodeInfo nodeInfo) {
        super(parent);
        this.nodeInfo = nodeInfo;
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
