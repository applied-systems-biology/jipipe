package org.hkijena.jipipe.launcher.commands;

import net.imagej.ImageJ;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeGUICommand;
import org.hkijena.jipipe.api.instrumentation.InstrumentationApplicationSettings;
import org.hkijena.jipipe.api.instrumentation.InstrumentationServer;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.hkijena.jipipe.desktop.commons.events.WindowClosedEvent;
import org.hkijena.jipipe.desktop.commons.events.WindowClosedEventListener;
import org.hkijena.jipipe.desktop.commons.events.WindowOpenedEvent;
import org.hkijena.jipipe.desktop.commons.events.WindowOpenedEventListener;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class GuiCommand {

    private static InstrumentationServer instrumentationServer;
    private static int pendingInstrumentationPort = -1;
    private static boolean instrumentationFlag = false;

    public static InstrumentationServer getInstrumentationServer() {
        return instrumentationServer;
    }

    public static void startGui(List<String> argsList) {
        List<String> remainingArgs = new ArrayList<>();
        for (int i = 0; i < argsList.size(); i++) {
            if ("--instrumentation".equals(argsList.get(i))) {
                instrumentationFlag = true;
                if (i + 1 < argsList.size()) {
                    try {
                        pendingInstrumentationPort = Integer.parseInt(argsList.get(i + 1));
                        i++;
                    } catch (NumberFormatException e) {
                        // Port will be resolved from settings in onWindowOpened()
                    }
                }
                // else: port will be resolved from settings in onWindowOpened()
            } else {
                remainingArgs.add(argsList.get(i));
            }
        }

        if (!remainingArgs.isEmpty()) {
            JIPipeDesktop.tryAddOpenProjectOnLoad(remainingArgs.getLast());
        }

        final ImageJ ij = new ImageJ();
        final WindowWatcher windowWatcher = new WindowWatcher();
        SwingUtilities.invokeLater(() -> ij.command().run(JIPipeGUICommand.class, true));
    }

    public static class WindowWatcher implements WindowOpenedEventListener, WindowClosedEventListener {
        public WindowWatcher() {
            JIPipeDesktopProjectWindow.WINDOW_OPENED_EVENT_EMITTER.subscribe(this);
            JIPipeDesktopProjectWindow.WINDOW_CLOSED_EVENT_EMITTER.subscribe(this);
        }

        @Override
        public void onWindowClosed(WindowClosedEvent event) {
            if (instrumentationServer != null) {
                SwingUtilities.invokeLater(() -> instrumentationServer.onProjectWindowsChanged());
            }
            int windowsOpen = JIPipeDesktopProjectWindow.getOpenWindows().size();
            if (windowsOpen == 0) {
                if (instrumentationServer != null) {
                    try {
                        instrumentationServer.stop(0);
                    } catch (Exception ignored) { }
                }
                JIPipe.exitLater(0);
            }
        }

        @Override
        public void onWindowOpened(WindowOpenedEvent event) {
            if (instrumentationServer == null) {
                int port = -1;
                if (pendingInstrumentationPort > 0) {
                    port = pendingInstrumentationPort;
                } else if (instrumentationFlag || InstrumentationApplicationSettings.getInstance().isAutoStart()) {
                    port = InstrumentationApplicationSettings.getInstance().getPort();
                }
                if (port > 0) {
                    instrumentationServer = new InstrumentationServer(port);
                    try {
                        instrumentationServer.start();
                        System.err.println("Instrumentation server started on port " + port);
                    } catch (Exception e) {
                        System.err.println("Failed to start instrumentation server: " + e.getMessage());
                        instrumentationServer = null;
                    }
                    pendingInstrumentationPort = -1;
                }
            }
            if (instrumentationServer != null) {
                SwingUtilities.invokeLater(() -> instrumentationServer.onProjectWindowsChanged());
            }
        }
    }
}
