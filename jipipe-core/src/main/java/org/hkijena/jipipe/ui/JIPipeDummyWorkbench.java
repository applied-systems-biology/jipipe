package org.hkijena.jipipe.ui;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.ui.components.DocumentTabPane;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;

/**
 * A dummy implementation of {@link JIPipeWorkbench} that will trigger no errors, but will not work for various functions.
 * Use at own discretion.
 */
public class JIPipeDummyWorkbench implements JIPipeWorkbench {

    private final JFrame frame = new JFrame();
    private final DocumentTabPane tabPane = new DocumentTabPane();

    @Override
    public Window getWindow() {
        return frame;
    }

    @Override
    public void sendStatusBarText(String text) {

    }

    @Override
    public boolean isProjectModified() {
        return false;
    }

    @Override
    public void setProjectModified(boolean modified) {

    }

    @Override
    public Context getContext() {
        if (JIPipe.getInstance() != null)
            return JIPipe.getInstance().getContext();
        else
            return new Context();
    }

    @Override
    public DocumentTabPane getDocumentTabPane() {
        return tabPane;
    }
}
