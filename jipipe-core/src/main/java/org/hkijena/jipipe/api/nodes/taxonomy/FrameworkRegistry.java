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

package org.hkijena.jipipe.api.nodes.taxonomy;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * In-memory registry of {@link JIPipeNodeTaxonomyFramework}s.
 * <p>
 * Frameworks form a flat set: each framework is the origin of the algorithm
 * nodes it implements. The registry is not thread-safe; it is expected to be
 * fully populated during startup before being exposed to the UI.
 */
public class FrameworkRegistry {

    private final Map<String, JIPipeNodeTaxonomyFramework> byId = new HashMap<>();
    /**
     * The display names registered per id, in registration order. The
     * {@link #byId} map collapses duplicate ids on lookup, so this list is
     * what makes duplicates detectable for {@link #validate()}.
     */
    private final Map<String, List<String>> namesPerId = new HashMap<>();

    /**
     * Registers a framework. Re-registering an existing id replaces the previous framework
     * for lookup purposes; the duplicate is still reported by {@link #validate()}.
     *
     * @param framework the framework to register
     */
    public void register(JIPipeNodeTaxonomyFramework framework) {
        byId.put(framework.getId(), framework);
        namesPerId.computeIfAbsent(framework.getId(), k -> new ArrayList<>()).add(framework.getName());
    }

    /**
     * Unregisters a framework. Does nothing if the id is not registered.
     *
     * @param id the id of the framework to remove
     * @return the removed framework, or {@code null} if none was registered under that id
     */
    @Nullable
    public JIPipeNodeTaxonomyFramework unregister(String id) {
        JIPipeNodeTaxonomyFramework removed = byId.remove(id);
        namesPerId.remove(id);
        return removed;
    }

    /**
     * @param id the id of the framework to look up
     * @return the registered framework with that id, or {@code null} if none is registered
     */
    @Nullable
    public JIPipeNodeTaxonomyFramework byId(String id) {
        return byId.get(id);
    }

    /**
     * @return an unmodifiable snapshot of all registered frameworks, one entry per id
     */
    public Set<JIPipeNodeTaxonomyFramework> all() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(byId.values()));
    }

    /**
     * Validates the registered frameworks.
     * <p>
     * Reported errors:
     * <ul>
     * <li>duplicate ids (listing the frameworks that registered the same id),</li>
     * <li>abbreviations longer than 4 characters,</li>
     * <li>empty abbreviations.</li>
     * </ul>
     *
     * @return human-readable error strings; empty if the registry is valid
     */
    public Set<String> validate() {
        Set<String> errors = new LinkedHashSet<>();

        for (Map.Entry<String, List<String>> entry : namesPerId.entrySet()) {
            if (entry.getValue().size() > 1) {
                errors.add("Duplicate taxonomy framework id " + entry.getKey()
                    + " in frameworks: " + String.join(", ", entry.getValue()));
            }
        }

        for (JIPipeNodeTaxonomyFramework framework : byId.values()) {
            String abbreviation = framework.getAbbreviation();
            if (abbreviation.isEmpty()) {
                errors.add("Taxonomy framework " + framework.getId() + " has an empty abbreviation");
            } else if (abbreviation.length() > 4) {
                errors.add("Taxonomy framework " + framework.getId()
                    + " has an abbreviation longer than 4 characters: " + abbreviation);
            }
        }
        return errors;
    }
}
