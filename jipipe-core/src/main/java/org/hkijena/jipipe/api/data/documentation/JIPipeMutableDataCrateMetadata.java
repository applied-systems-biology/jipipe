package org.hkijena.jipipe.api.data.documentation;

import java.util.*;

/**
 * A description of a JIPipe data that follows a subset of the <a href="https://www.researchobject.org/ro-crate/specification/1.2/index.html">RO-Crate Metadata Specification 1.2</a>.
 * Contains a set of unique {@link JIPipeDataCrateMetadataEntry}.
 */
public class JIPipeMutableDataCrateMetadata implements JIPipeDataCrateMetadata {
    private Map<String, JIPipeDataCrateMetadataEntry> entries = new LinkedHashMap<>();

    public JIPipeMutableDataCrateMetadata() {
    }

    @Override
    public Map<String, JIPipeDataCrateMetadataEntry> getEntriesMap() {
        return Collections.unmodifiableMap(entries);
    }

    @Override
    public List<JIPipeDataCrateMetadataEntry> getEntries() {
        return entries.values().stream().sorted(Comparator.comparing(JIPipeDataCrateMetadataEntry::getId)).toList();
    }

    public void setEntries(Map<String, JIPipeDataCrateMetadataEntry> entries) {
        this.entries = entries;
    }

    @Override
    public boolean isValid() {
        for (JIPipeDataCrateMetadataEntry value : entries.values()) {
            if (!value.isValid()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public void put(JIPipeDataCrateMetadataEntry entry) {
        if (!entry.isValid()) {
            throw new IllegalStateException("Invalid entry: " + entry);
        }
        entries.put(entry.getId(), entry);
    }
}
