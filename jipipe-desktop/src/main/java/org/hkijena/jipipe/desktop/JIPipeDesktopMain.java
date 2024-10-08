/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop;

import net.imagej.ImageJ;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeGUICommand;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.hkijena.jipipe.desktop.commons.events.WindowClosedEvent;
import org.hkijena.jipipe.desktop.commons.events.WindowClosedEventListener;
import org.hkijena.jipipe.desktop.commons.events.WindowOpenedEvent;
import org.hkijena.jipipe.desktop.commons.events.WindowOpenedEventListener;

import javax.swing.*;

public class JIPipeDesktopMain {
    /**
     * @param args ignored
     */
    public static void main(final String... args) {
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

            if (windowsOpen == 0 && !JIPipe.isRestarting()) {
                JIPipe.exitLater(0);
            }
        }

        @Override
        public void onWindowOpened(WindowOpenedEvent event) {

        }
    }
}
