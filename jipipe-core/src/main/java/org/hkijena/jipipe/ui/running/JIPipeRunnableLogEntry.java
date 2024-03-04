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

import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;

import java.time.LocalDateTime;

public class JIPipeRunnableLogEntry {
    private final String name;
    private final LocalDateTime dateTime;
    private final String log;
    private final JIPipeNotificationInbox notifications;
    private final boolean success;
    private boolean read = false;

    public JIPipeRunnableLogEntry(String name, LocalDateTime dateTime, String log, JIPipeNotificationInbox notifications, boolean success) {
        this.name = name;
        this.dateTime = dateTime;
        this.log = log;
        this.notifications = notifications;
        this.success = success;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public String getLog() {
        return log;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getName() {
        return name;
    }

    public JIPipeNotificationInbox getNotifications() {
        return notifications;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}
