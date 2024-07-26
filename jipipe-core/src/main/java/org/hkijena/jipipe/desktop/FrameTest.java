package org.hkijena.jipipe.desktop;

import org.hkijena.jipipe.utils.ui.FloatingDockPanel;

import javax.swing.*;
import java.awt.*;

public class FrameTest {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Floating JSplitPane Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1980, 1024);

        // Create the text editor
        JTextPane textPane = new JTextPane();
        textPane.setText("This is a text editor.\nYou can still type here.");
        textPane.setBackground(Color.gray);
        textPane.setBorder(BorderFactory.createLineBorder(Color.RED));

        FloatingDockPanel dockPanel = new FloatingDockPanel();
        frame.setContentPane(dockPanel);
        dockPanel.setBackgroundComponent(textPane);

        frame.setVisible(true);
    }
}
