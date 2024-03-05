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

package org.hkijena.jipipe.ui.running;

import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchAccess;
import org.hkijena.jipipe.ui.components.icons.AnimatedIcon;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class JIPipeRunnableLogsButton extends JButton implements JIPipeWorkbenchAccess, JIPipeRunnableLogsCollection.LogUpdatedEventListener {
    private final JIPipeWorkbench workbench;
    private final JIPipeRunnableLogsCollection logCollection;
    private AnimatedIcon warningIcon;

    public JIPipeRunnableLogsButton(JIPipeWorkbench workbench) {
        this.workbench = workbench;
        this.logCollection = JIPipeRunnableLogsCollection.getInstance();
        initialize();
        updateStatus();

        logCollection.getLogUpdatedEventEmitter().subscribeWeak(this);
    }

    private void initialize() {
        warningIcon = new AnimatedIcon(this, UIUtils.getIconFromResources("emblems/emblem-important.png"),
                UIUtils.getIconFromResources("emblems/warning.png"),
                100, 0.05);
        setIcon(UIUtils.getIconFromResources("actions/rabbitvcs-show_log.png"));
        UIUtils.setStandardButtonBorder(this);
        addActionListener(e -> openLogs());

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(SwingUtilities.isRightMouseButton(e)) {
                    JPopupMenu menu = new JPopupMenu();
                    menu.add(UIUtils.createMenuItem("Mark all as read", "Marks all entries as read", UIUtils.getIconFromResources("actions/check-double.png"), this::markAllAsRead));
                    menu.add(UIUtils.createMenuItem("Clear", "Removes all entries", UIUtils.getIconFromResources("actions/edit-clear-history.png"), this::clear));
                }
            }

            private void clear() {
                JIPipeRunnableLogsCollection.getInstance().clear();
            }

            private void markAllAsRead() {
                JIPipeRunnableLogsCollection.getInstance().markAllAsRead();
            }

        });
    }



    private void openLogs() {
        workbench.getDocumentTabPane().selectSingletonTab(JIPipeProjectWorkbench.TAB_LOG);
    }

    private void updateStatus() {
        int count = 0;
        int countWithWarnings = 0;

        for (JIPipeRunnableLogEntry logEntry : logCollection.getLogEntries()) {
            if(!logEntry.isRead()) {
                ++count;
                if (!logEntry.getNotifications().isEmpty()) {
                    ++countWithWarnings;
                }
            }
        }

        if(count == 0) {
            setText("Logs");
            setIcon(UIUtils.getIconFromResources("actions/rabbitvcs-show_log.png"));
            warningIcon.stop();
        }
        else if(countWithWarnings > 0) {
            setText("Logs (" + count + ")");
            setIcon(warningIcon);
            warningIcon.start();
        }
        else {
            setText("Logs (" + count + ")");
            setIcon(UIUtils.getIconFromResources("emblems/emblem-important-blue.png"));
            warningIcon.stop();
        }
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }

    @Override
    public void onLogUpdated(JIPipeRunnableLogsCollection.LogUpdatedEvent event) {
        updateStatus();
    }
}
