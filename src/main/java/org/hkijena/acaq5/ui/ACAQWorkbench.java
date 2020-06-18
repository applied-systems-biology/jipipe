package org.hkijena.acaq5.ui;

import org.hkijena.acaq5.ACAQGUICommand;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.scijava.Context;

/**
 * Interface shared by all workbench UIs
 */
public interface ACAQWorkbench {
    /**
     * Sends a text to the status bar
     *
     * @param text The text
     */
    void sendStatusBarText(String text);

    /**
     * @return SciJava context
     */
    Context getContext();

    /**
     * @return The tab pane
     */
    DocumentTabPane getDocumentTabPane();
}
