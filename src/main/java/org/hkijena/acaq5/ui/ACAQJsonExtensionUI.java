package org.hkijena.acaq5.ui;

import org.hkijena.acaq5.ACAQGUICommand;
import org.hkijena.acaq5.api.ACAQJsonExtensionProject;

import javax.swing.*;

public class ACAQJsonExtensionUI extends JPanel {
    private final ACAQJsonExtensionWindow window;
    private final ACAQGUICommand command;
    private final ACAQJsonExtensionProject project;

    public ACAQJsonExtensionUI(ACAQJsonExtensionWindow window, ACAQGUICommand command, ACAQJsonExtensionProject project) {
        this.window = window;
        this.command = command;
        this.project = project;
    }

    public void sendStatusBarText(String message) {

    }
}
