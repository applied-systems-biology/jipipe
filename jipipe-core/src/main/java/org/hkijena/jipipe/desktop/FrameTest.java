package org.hkijena.jipipe.desktop;

import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopUITheme;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;

import javax.swing.*;
import java.awt.*;

public class FrameTest {
    public static void main(String[] args) {

        JIPipeDesktopUITheme.ModernLight.install();

        JFrame frame = new JFrame("Floating JSplitPane Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1980, 1024);

        // Create the text editor
        JTextPane textPane = new JTextPane();
        textPane.setText("This is a text editor.\nYou can still type here.");
        textPane.setBackground(Color.gray);
        textPane.setBorder(BorderFactory.createLineBorder(Color.RED));

        JIPipeDesktopDockPanel dockPanel = new JIPipeDesktopDockPanel();
        frame.setContentPane(dockPanel);
        dockPanel.setBackgroundComponent(textPane);

        dockPanel.addDockPanel("P1", "Panel 1", UIUtils.getIcon32FromResources("actions/1.png"), JIPipeDesktopDockPanel.PanelLocation.TopLeft, false, 0, new JTextArea("Panel 1"));
        dockPanel.addDockPanel("P2", "Panel 2", UIUtils.getIcon32FromResources("actions/2.png"), JIPipeDesktopDockPanel.PanelLocation.TopLeft, false, 0, new JTextArea("Panel 2"));
        dockPanel.addDockPanel("P3", "Panel 3", UIUtils.getIcon32FromResources("actions/3.png"), JIPipeDesktopDockPanel.PanelLocation.BottomLeft, false, 0, new JTextArea("Panel 3"));
        dockPanel.addDockPanel("P4", "Panel 4", UIUtils.getIcon32FromResources("actions/4.png"), JIPipeDesktopDockPanel.PanelLocation.TopRight, false, 0, new JTextArea("Panel 4"));
        dockPanel.addDockPanel("P5", "Panel 5", UIUtils.getIcon32FromResources("actions/5.png"), JIPipeDesktopDockPanel.PanelLocation.BottomRight, false, 0, new JTextArea("Panel 5"));

        frame.setVisible(true);
    }
}
