package org.hkijena.jipipe.launcher.commands;

import net.imagej.ImageJ;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeGUICommand;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.hkijena.jipipe.desktop.commons.events.WindowClosedEvent;
import org.hkijena.jipipe.desktop.commons.events.WindowClosedEventListener;
import org.hkijena.jipipe.desktop.commons.events.WindowOpenedEvent;
import org.hkijena.jipipe.desktop.commons.events.WindowOpenedEventListener;

import javax.swing.*;
import java.util.List;

public class GuiCommand {
    public static void startGui(List<String> argsList) {
        if(!argsList.isEmpty()) {
            JIPipeDesktop.tryAddOpenProjectOnLoad(argsList.getLast());
        }

        final ImageJ ij = new ImageJ();
        final WindowWatcher windowWatcher = new WindowWatcher(); //JIPipeGUICommand
        SwingUtilities.invokeLater(() -> ij.command().run(JIPipeGUICommand.class, true));
    }

    public static class WindowWatcher implements WindowOpenedEventListener, WindowClosedEventListener {
        public WindowWatcher() {
            JIPipeDesktopProjectWindow.WINDOW_OPENED_EVENT_EMITTER.subscribe(this);
            JIPipeDesktopProjectWindow.WINDOW_CLOSED_EVENT_EMITTER.subscribe(this);
        }

        @Override
        public void onWindowClosed(WindowClosedEvent event) {
            int windowsOpen = 0;
            windowsOpen += JIPipeDesktopProjectWindow.getOpenWindows().size();

            if (windowsOpen == 0) {
                JIPipe.exitLater(0);
            }
        }

        @Override
        public void onWindowOpened(WindowOpenedEvent event) {

        }
    }
}
