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

package org.hkijena.jipipe.api.notifications;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A notification that is shown to the user
 */
public class JIPipeNotification implements Comparable<JIPipeNotification> {

    private final LocalDateTime dateTime = LocalDateTime.now();
    private final String id;
    private JIPipeNotificationInbox inbox;
    private String heading;
    private String description;
    private List<JIPipeNotificationAction> actions = new ArrayList<>();

    public JIPipeNotification(String id) {
        this.id = id;
    }

    public JIPipeNotification(String id, String heading, String description, JIPipeNotificationAction... actions) {
        this.id = id;
        this.heading = heading;
        this.description = description;
        this.actions = new ArrayList<>(Arrays.asList(actions));
    }

    public JIPipeNotification(JIPipeNotification other) {
        this.id = other.id;
        this.inbox = other.inbox;
        this.heading = other.heading;
        this.description = other.description;
        this.actions = other.actions;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public List<JIPipeNotificationAction> getActions() {
        return actions;
    }

    public void setActions(List<JIPipeNotificationAction> actions) {
        this.actions = actions;
    }

    public void dismiss() {
        getInbox().dismiss(this);
    }

    @Override
    public int compareTo(JIPipeNotification o) {
        return dateTime.compareTo(o.dateTime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JIPipeNotification that = (JIPipeNotification) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Notification [" + id + "] @ " + getDateTime() + ": " + getHeading();
    }

    public String getId() {
        return id;
    }

    public JIPipeNotificationInbox getInbox() {
        return inbox;
    }

    public void setInbox(JIPipeNotificationInbox inbox) {
        this.inbox = inbox;
    }
}
