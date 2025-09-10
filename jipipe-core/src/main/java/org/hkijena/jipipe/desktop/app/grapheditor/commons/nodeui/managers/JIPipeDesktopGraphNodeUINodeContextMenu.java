package org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.managers;

import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.GraphInteractiveObjectUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Collections;

import static org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI.RUN_NODE_CONTEXT_MENU_ENTRIES;

public class JIPipeDesktopGraphNodeUINodeContextMenu {
    private final JIPipeDesktopGraphNodeUI nodeUI;

    public JIPipeDesktopGraphNodeUINodeContextMenu(JIPipeDesktopGraphNodeUI nodeUI) {
        this.nodeUI = nodeUI;
    }


    public void openRunNodeMenu(MouseEvent event) {
        JPopupMenu menu = new JPopupMenu();
        for (GraphInteractiveObjectUIContextAction entry : RUN_NODE_CONTEXT_MENU_ENTRIES) {
            if (entry == null)
                UIUtils.addSeparatorIfNeeded(menu);
            else {
                JMenuItem item = new JMenuItem(entry.getName(), entry.getIcon());
                item.setToolTipText(entry.getDescription());
                item.setAccelerator(entry.getKeyboardShortcut());
                item.addActionListener(e -> {
                    if (entry.matches(Collections.singleton(nodeUI))) {
                        entry.run(nodeUI.getGraphCanvasUI(), Collections.singleton(nodeUI));
                    } else {
                        JOptionPane.showMessageDialog(nodeUI.getDesktopWorkbench().getWindow(),
                                "Could not run this operation",
                                entry.getName(),
                                JOptionPane.ERROR_MESSAGE);
                    }
                });
                menu.add(item);
            }
        }
        MouseEvent convertMouseEvent = SwingUtilities.convertMouseEvent(nodeUI.getGraphCanvasUI(), event, nodeUI);
        Point mousePosition = convertMouseEvent.getPoint();
        menu.show(nodeUI, mousePosition.x, mousePosition.y);
    }
}
