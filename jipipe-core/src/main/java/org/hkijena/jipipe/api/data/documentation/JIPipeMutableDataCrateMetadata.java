package org.hkijena.jipipe.api.data.documentation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A description of a JIPipe data that follows a subset of the <a href="https://www.researchobject.org/ro-crate/specification/1.2/index.html">RO-Crate Metadata Specification 1.2</a>.
 * Contains a set of unique {@link JIPipeDataCrateMetadataEntry}.
 */
public class JIPipeMutableDataCrateMetadata implements JIPipeDataCrateMetadata {
    private Map<String, JIPipeDataCrateMetadataEntry> entries = new LinkedHashMap<>();

    public JIPipeMutableDataCrateMetadata() {
    }

    @Override
    public Map<String, JIPipeDataCrateMetadataEntry> getEntries() {
        return Collections.unmodifiableMap(entries);
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

    public void setEntries(Map<String, JIPipeDataCrateMetadataEntry> entries) {
        this.entries = entries;
    }

    public void put(JIPipeDataCrateMetadataEntry entry) {
        if (!entry.isValid()) {
            throw new IllegalStateException("Invalid entry: " + entry);
        }
        entries.put(entry.getId(), entry);
    }
}
