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

package org.hkijena.jipipe.api.knowledge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One immutable, loaded KDB corpus entry (design spec 5.1). The entry key is
 * the stable, human readable identifier derived from the file path; the id is
 * a machine id generated at load time. The corpus never stores node display
 * names, parameter lists, slot signatures, or node lifecycle state: NODE_REF
 * and ENTRY_REF values are opaque strings resolved at render time.
 */
public final class KdbEntry {

    private final String entryKey;
    private final UUID id;
    private final KdbEntryKind kind;
    private final KdbDomain domain;
    private final String title;
    private final Map<String, List<Object>> slotValues;
    private final String body;
    private final List<String> aliases;
    private final String sinceVersion;
    private final String untilVersion;
    private final List<String> sourceRefs;
    private final Map<String, Object> properties;

    /**
     * Creates an entry. Called by the loader; all collections are copied into
     * unmodifiable structures.
     *
     * @param entryKey     the path derived entry key
     * @param id           the machine id generated at load time
     * @param kind         the kind (schema as code)
     * @param domain       the domain derived from the first path segment
     * @param title        the title, or null if missing (a MISSING_TITLE finding documents the defect)
     * @param slotValues   the narrowed slot values, keyed by slot name
     * @param body         the markdown prose, or null if missing
     * @param aliases      the alias match surface for lexical search
     * @param sinceVersion optional version the entry is valid from
     * @param untilVersion optional version the entry is valid until
     * @param sourceRefs   provenance references
     * @param properties   untyped escape hatch properties
     */
    public KdbEntry(String entryKey,
                    UUID id,
                    KdbEntryKind kind,
                    KdbDomain domain,
                    String title,
                    Map<String, List<Object>> slotValues,
                    String body,
                    List<String> aliases,
                    String sinceVersion,
                    String untilVersion,
                    List<String> sourceRefs,
                    Map<String, Object> properties) {
        this.entryKey = entryKey;
        this.id = id;
        this.kind = kind;
        this.domain = domain;
        this.title = title;
        Map<String, List<Object>> slotValuesCopy = new LinkedHashMap<>();
        for (Map.Entry<String, List<Object>> entry : slotValues.entrySet()) {
            slotValuesCopy.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
        }
        this.slotValues = Collections.unmodifiableMap(slotValuesCopy);
        this.body = body;
        this.aliases = Collections.unmodifiableList(new ArrayList<>(aliases));
        this.sinceVersion = sinceVersion;
        this.untilVersion = untilVersion;
        this.sourceRefs = Collections.unmodifiableList(new ArrayList<>(sourceRefs));
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
    }

    /**
     * The stable, human readable, unique entry key
     * {@code <domain>/<kind-directory>/<entry-name>}.
     *
     * @return the entry key
     */
    public String getEntryKey() {
        return entryKey;
    }

    /**
     * The machine id generated at load time.
     *
     * @return the id
     */
    public UUID getId() {
        return id;
    }

    public KdbEntryKind getKind() {
        return kind;
    }

    /**
     * The domain derived from the first path segment.
     *
     * @return the domain
     */
    public KdbDomain getDomain() {
        return domain;
    }

    /**
     * The title.
     *
     * @return the title, or null if missing
     */
    public String getTitle() {
        return title;
    }

    /**
     * The narrowed slot values, keyed by slot name. Only slots with at least
     * one successfully narrowed value are present. Values are String for
     * TEXT, NODE_REF, ENTRY_REF, and VERSION slots, and Long or Double for
     * NUMBER slots.
     *
     * @return an unmodifiable map of slot values
     */
    public Map<String, List<Object>> getSlotValues() {
        return slotValues;
    }

    /**
     * The first narrowed value of a slot.
     *
     * @param name the slot name
     * @return the first value, or null if the slot has no values
     */
    public Object getSlotValue(String name) {
        List<Object> values = slotValues.get(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    /**
     * The markdown prose (the genuinely prose part).
     *
     * @return the body, or null if missing
     */
    public String getBody() {
        return body;
    }

    /**
     * The alias match surface for lexical search.
     *
     * @return an unmodifiable list of aliases
     */
    public List<String> getAliases() {
        return aliases;
    }

    /**
     * Optional version the entry is valid from, compared against the pinned
     * corpus version.
     *
     * @return the version, or null
     */
    public String getSinceVersion() {
        return sinceVersion;
    }

    /**
     * Optional version the entry is valid until, compared against the pinned
     * corpus version.
     *
     * @return the version, or null
     */
    public String getUntilVersion() {
        return untilVersion;
    }

    /**
     * Provenance references: markdown source paths, workflow analysis report
     * sections, node database evidence.
     *
     * @return an unmodifiable list of source references
     */
    public List<String> getSourceRefs() {
        return sourceRefs;
    }

    /**
     * The untyped escape hatch properties. Keys shadowing schema slots are
     * reported as PROPERTY_SHADOWS_SLOT findings but kept.
     *
     * @return an unmodifiable map of properties
     */
    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        return "KdbEntry[" + entryKey + "]";
    }
}
