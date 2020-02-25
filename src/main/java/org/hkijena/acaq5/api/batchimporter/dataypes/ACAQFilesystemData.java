package org.hkijena.acaq5.api.batchimporter.dataypes;

import org.hkijena.acaq5.api.data.ACAQData;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for all filesystem data.
 * The data can hold annotations
 */
public interface ACAQFilesystemData extends ACAQData {

    Map<String, Object> getAnnotations();

    /**
     * Finds an annotation in this entry, or in a parent entry
     * @param key
     * @return null if it was not found
     */
    Object findAnnotation(String key);

    void annotate(String key, Object value);
}
