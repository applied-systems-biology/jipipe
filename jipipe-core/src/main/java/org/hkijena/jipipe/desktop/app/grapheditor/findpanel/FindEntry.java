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

import org.hkijena.jipipe.api.data.JIPipeData;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * A single searchable row of the Find panel: a graph node with its display
 * name, description, identity and the Add-nodes filter facets (framework,
 * tags, input/output data types), ready to be filtered by a query string and
 * the filter bar.
 * <p>
 * Plain value class: all fields are set via the constructor and exposed via
 * accessors (no setters). The facet sets are copied and unmodifiable.
 */
public class FindEntry {

    private final UUID nodeId;
    private final String name;
    private final String description;
    private final String uuidString;
    // Precomputed lowercase haystacks of the query match (matches() runs for
    // every entry on every rebuild)
    private final String nameLowerCase;
    private final String descriptionLowerCase;
    private final String uuidStringLowerCase;
    private final UUID compartmentId;
    private final String compartmentName;
    private final boolean bookmarked;
    private final String frameworkId;
    private final Set<String> tags;
    private final Set<Class<? extends JIPipeData>> inputDataTypes;
    private final Set<Class<? extends JIPipeData>> outputDataTypes;

    public FindEntry(UUID nodeId, String name, String description, String uuidString, UUID compartmentId, String compartmentName, boolean bookmarked,
                     String frameworkId, Set<String> tags, Set<Class<? extends JIPipeData>> inputDataTypes, Set<Class<? extends JIPipeData>> outputDataTypes) {
        this.nodeId = nodeId;
        this.name = name;
        this.description = description;
        this.uuidString = uuidString;
        this.nameLowerCase = lowerCase(name);
        this.descriptionLowerCase = lowerCase(description);
        this.uuidStringLowerCase = lowerCase(uuidString);
        this.compartmentId = compartmentId;
        this.compartmentName = compartmentName;
        this.bookmarked = bookmarked;
        this.frameworkId = frameworkId;
        this.tags = tags == null ? Set.of() : Set.copyOf(tags);
        this.inputDataTypes = inputDataTypes == null ? Set.of() : Set.copyOf(inputDataTypes);
        this.outputDataTypes = outputDataTypes == null ? Set.of() : Set.copyOf(outputDataTypes);
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

    public String getFrameworkId() {
        return frameworkId;
    }

    public Set<String> getTags() {
        return tags;
    }

    public Set<Class<? extends JIPipeData>> getInputDataTypes() {
        return inputDataTypes;
    }

    public Set<Class<? extends JIPipeData>> getOutputDataTypes() {
        return outputDataTypes;
    }

    /**
     * Returns true if this entry matches the query.
     * <p>
     * The match is a case-insensitive {@code contains} on the name, the
     * description and the UUID string; the lowercase haystacks are
     * precomputed in the constructor. A {@code null} or empty query matches
     * every entry.
     */
    public boolean matches(String query) {
        if (query == null || query.isEmpty()) {
            return true;
        }
        String needle = query.toLowerCase(Locale.ROOT);
        return contains(nameLowerCase, needle) || contains(descriptionLowerCase, needle) || contains(uuidStringLowerCase, needle);
    }

    /**
     * Returns true if this entry matches the active facet filters, mirroring
     * the Add-nodes filter semantics ({@code NodeBrowserModel.matches}): an
     * empty facet selection (or a {@code null} framework id) disables that
     * dimension; the tag filter passes when the entry's tags intersect the
     * selection; the input/output filters pass when at least one of the
     * entry's input/output slots has a selected data class.
     */
    public boolean matchesFacets(String frameworkId, Set<String> tags, Set<Class<? extends JIPipeData>> inputDataTypes, Set<Class<? extends JIPipeData>> outputDataTypes) {
        if (frameworkId != null && !frameworkId.equals(this.frameworkId)) {
            return false;
        }
        if (tags != null && !tags.isEmpty() && Collections.disjoint(tags, this.tags)) {
            return false;
        }
        if (inputDataTypes != null && !inputDataTypes.isEmpty() && Collections.disjoint(inputDataTypes, this.inputDataTypes)) {
            return false;
        }
        if (outputDataTypes != null && !outputDataTypes.isEmpty() && Collections.disjoint(outputDataTypes, this.outputDataTypes)) {
            return false;
        }
        return true;
    }

    private static String lowerCase(String text) {
        return text != null ? text.toLowerCase(Locale.ROOT) : null;
    }

    private static boolean contains(String lowerCaseText, String lowerCaseNeedle) {
        return lowerCaseText != null && lowerCaseText.contains(lowerCaseNeedle);
    }
}
