/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.notifications;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A notification that is shown to the user
 */
public class JIPipeNotification implements Comparable<JIPipeNotification> {

    private final LocalDateTime dateTime = LocalDateTime.now();
    private String heading;
    private String description;

    public JIPipeNotification() {
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

    @Override
    public int compareTo(@NotNull JIPipeNotification o) {
        return dateTime.compareTo(o.dateTime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JIPipeNotification that = (JIPipeNotification) o;
        return Objects.equals(heading, that.heading);
    }

    @Override
    public int hashCode() {
        return Objects.hash(heading);
    }

    @Override
    public String toString() {
        return "Notification @ " + getDateTime() + ": " + getHeading();
    }
}
