package org.hkijena.jipipe.ui.grapheditor.contextmenu;

import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.JIPipeNodeUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

public class SetNodeHotkeyContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeNodeUI> selection) {
        return selection.size() == 1;
    }

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeNodeUI> selection) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(canvasUI), "Set quick access hotkey");
        dialog.setModal(true);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(new JLabel("Select/Unselect which numeric key should select the node."), BorderLayout.NORTH);

        JPanel dialPanel = new JPanel(new GridLayout(1,9));
        int[] keys = {KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3,
                KeyEvent.VK_4, KeyEvent.VK_5, KeyEvent.VK_6, KeyEvent.VK_7,
                KeyEvent.VK_8, KeyEvent.VK_9, KeyEvent.VK_0};
        int[] numPadKeys = {KeyEvent.VK_NUMPAD1, KeyEvent.VK_NUMPAD2, KeyEvent.VK_NUMPAD3,
                KeyEvent.VK_NUMPAD4,
                KeyEvent.VK_NUMPAD5, KeyEvent.VK_NUMPAD6, KeyEvent.VK_NUMPAD7,
                KeyEvent.VK_NUMPAD8, KeyEvent.VK_NUMPAD9, KeyEvent.VK_NUMPAD0};
        for (int i = 0; i < 10; i++) {
            int number = (i + 1) % 10;
            JToggleButton button = new JToggleButton("" + number);
            dialPanel.add(button);
            dialog.getRootPane().registerKeyboardAction(e -> button.doClick(),
                    KeyStroke.getKeyStroke(keys[i], 0),
                    JComponent.WHEN_IN_FOCUSED_WINDOW);
            dialog.getRootPane().registerKeyboardAction(e -> button.doClick(),
                    KeyStroke.getKeyStroke(numPadKeys[i], 0),
                    JComponent.WHEN_IN_FOCUSED_WINDOW);
        }

        contentPanel.add(dialPanel, BorderLayout.CENTER);
        dialog.setContentPane(contentPanel);
        dialog.setSize(400,100);
        dialog.setLocationRelativeTo(null);
        UIUtils.addEscapeListener(dialog);
        dialog.setVisible(true);
    }

    @Override
    public String getName() {
        return "Set quick access hotkey";
    }

    @Override
    public String getDescription() {
        return "Allows to set a hotkey (numbers 1-9) to quickly select the node";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/key-enter.png");
    }

    @Override
    public boolean isShowingInOverhang() {
        return false;
    }
}
