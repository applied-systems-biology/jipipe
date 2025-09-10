package org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.managers;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationStepGenerationSettingsVisualization;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.pipeline.JIPipeDesktopPipelineGraphEditorUI;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.ViewOnlyMenuItem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

public class JIPipeDesktopGraphNodeUIInputConfigContextMenu {
    private final JIPipeDesktopGraphNodeUI nodeUI;

    public JIPipeDesktopGraphNodeUIInputConfigContextMenu(JIPipeDesktopGraphNodeUI nodeUI) {
        this.nodeUI = nodeUI;
    }

    public void openInputConfigMenu(MouseEvent event) {
        JPopupMenu menu = new JPopupMenu();

        menu.add(UIUtils.createMenuItem("Configure ...", "Opens the input manager", JIPipe.RESOURCES.getIcon16("actions/configure.png"), () -> {
            nodeUI.getGraphCanvasUI().getSelectionManager().selectOnly(nodeUI);
            nodeUI.getGraphCanvasUI().getGraphEditorUI().getDockPanel().activatePanel(JIPipeDesktopPipelineGraphEditorUI.DOCK_NODE_CONTEXT_INPUT_MANAGER, true);
        }));

        if (!nodeUI.getIterationStepGenerationSettingsVisualization().getReportEntries().isEmpty()) {
            menu.addSeparator();
        }

        for (JIPipeIterationStepGenerationSettingsVisualization.ReportEntry reportEntry : nodeUI.getIterationStepGenerationSettingsVisualization().getReportEntries()) {
            ViewOnlyMenuItem infoItem = new ViewOnlyMenuItem("<html>" + reportEntry.name() + "<br><small>" + reportEntry.message() + "</small></html>",
                    JIPipe.RESOURCES.getIcon16("actions/configure_toolbars.png"));
            menu.add(infoItem);
        }

        MouseEvent convertMouseEvent = SwingUtilities.convertMouseEvent(nodeUI.getGraphCanvasUI(), event, nodeUI);
        Point mousePosition = convertMouseEvent.getPoint();
        menu.show(nodeUI, mousePosition.x, mousePosition.y);
    }
}
