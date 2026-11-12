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

package org.hkijena.jipipe.desktop.app.grapheditor.findpanel;

import java.util.Locale;
import java.util.UUID;

/**
 * A single searchable row of the Find panel: a graph node with its display
 * name, description and identity, ready to be filtered by a query string.
 * <p>
 * Plain value class: all fields are set via the constructor and exposed via
 * accessors (no setters).
 */
public class FindEntry {

    private final UUID nodeId;
    private final String name;
    private final String description;
    private final String uuidString;
    private final UUID compartmentId;
    private final String compartmentName;
    private final boolean bookmarked;

    public FindEntry(UUID nodeId, String name, String description, String uuidString, UUID compartmentId, String compartmentName, boolean bookmarked) {
        this.nodeId = nodeId;
        this.name = name;
        this.description = description;
        this.uuidString = uuidString;
        this.compartmentId = compartmentId;
        this.compartmentName = compartmentName;
        this.bookmarked = bookmarked;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getUuidString() {
        return uuidString;
    }

    public UUID getCompartmentId() {
        return compartmentId;
    }

    public String getCompartmentName() {
        return compartmentName;
    }

    public boolean isBookmarked() {
        return bookmarked;
    }

    /**
     * Returns true if this entry matches the query.
     * <p>
     * The match is a case-insensitive {@code contains} on the name, the
     * description and the UUID string. A {@code null} or empty query matches
     * every entry.
     */
    public boolean matches(String query) {
        if (query == null || query.isEmpty()) {
            return true;
        }
        String needle = query.toLowerCase(Locale.ROOT);
        return containsIgnoreCase(name, needle) || containsIgnoreCase(description, needle) || containsIgnoreCase(uuidString, needle);
    }

    private static boolean containsIgnoreCase(String text, String lowerCaseNeedle) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(lowerCaseNeedle);
    }
}
