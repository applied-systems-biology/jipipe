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

package org.hkijena.jipipe.utils;

import ij.IJ;
import ij.text.TextPanel;
import org.hkijena.jipipe.api.JIPipeProgressInfo;

import javax.swing.*;
import java.awt.*;
import java.io.Closeable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

/**
 * Takes control of the log in {@link ij.IJ} and pumps its messages into a {@link org.hkijena.jipipe.api.JIPipeProgressInfo}
 */
public class IJLogToJIPipeProgressInfoPump implements Closeable, AutoCloseable {
    private final Timer timer;
    private final JIPipeProgressInfo target;

    private TextPanel logTextPanel;

    private int logStart = 0;

    public IJLogToJIPipeProgressInfoPump(JIPipeProgressInfo target) {
        this.timer = new Timer(250, e -> copyLog());
        timer.setRepeats(true);
        this.target = target;
        initialize();
    }

    private void initialize() {
        IJ.log(""); // Inits the log
        try {
            Field logPanelField = IJ.class.getDeclaredField("logPanel");
            logPanelField.setAccessible(true);
            logTextPanel = (TextPanel) logPanelField.get(null);
            if (logTextPanel == null) {
                target.log("Unable to hook into IJ.log(): Is null");
                return;
            }
        } catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
            target.log("Unable to hook into IJ.log(): " + e);
            return;
        }
        try {
            SwingUtilities.invokeAndWait(() -> {
                logStart = logTextPanel.getText().length();
//                target.log("IJ.Log() starts at " + logStart);
                Window windowAncestor = SwingUtilities.getWindowAncestor(logTextPanel);
                if (windowAncestor != null) {
                    windowAncestor.setVisible(false);
                }
            });
        } catch (InterruptedException | InvocationTargetException e) {
            target.log("Unable to close IJ.log() window: " + e);
            return;
        }

        timer.start();
    }

    private void copyLog() {
        if (target.isCancelled()) {
            timer.stop();
            return;
        }
        String log = logTextPanel.getText();
        if (log.length() > logStart) {
            String newText = log.substring(logStart);
            if (!StringUtils.isNullOrEmpty(newText)) {
                for (String entry : newText.split("\n")) {
                    target.log(entry);
                }
            }
            logStart = log.length();
        }
    }

    @Override
    public void close() {
        if (timer.isRunning())
            copyLog();
//        target.log("Ending IJ.log() hook");
        timer.stop();

        try {
            SwingUtilities.invokeAndWait(() -> {
                Window windowAncestor = SwingUtilities.getWindowAncestor(logTextPanel);
                if (windowAncestor != null) {
                    windowAncestor.setVisible(false);
                }
            });
        } catch (Throwable e) {
            target.log("Unable to close IJ.log() window: " + e);
        }
    }
}
