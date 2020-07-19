package org.hkijena.jipipe.api.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.hkijena.jipipe.utils.JsonUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Determines how annotations are merged
 */
public enum JIPipeAnnotationMergeStrategy {
    SkipExisting,
    OverwriteExisting,
    Merge;

    /**
     * Merges the new annotation value into the existing one according to the strategy
     *
     * @param existingValue the existing value
     * @param newValue      the value to be merged
     * @return the merged result
     */
    public String merge(String existingValue, String newValue) {
        if (this == SkipExisting) {
            if (!StringUtils.isNullOrEmpty(existingValue))
                return existingValue;
            else
                return newValue;
        } else if (this == OverwriteExisting) {
            return newValue;
        } else {
            List<String> components = Arrays.asList(extractMergedAnnotations(existingValue));
            if (!components.contains(newValue))
                components.add(newValue);
            if (components.isEmpty())
                return "";
            else if (components.size() == 1) {
                return components.get(0);
            } else {
                try {
                    return JsonUtils.getObjectMapper().writeValueAsString(components);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Extracts merged annotations
     *
     * @param merged the annotation value
     * @return the components
     */
    public static String[] extractMergedAnnotations(String merged) {
        if (StringUtils.isNullOrEmpty(merged))
            return new String[0];
        if (merged.contains("[") && merged.contains("]")) {
            try {
                return JsonUtils.getObjectMapper().readerFor(String[].class).readValue(merged);
            } catch (IOException e) {
                return new String[]{merged};
            }
        } else {
            return new String[]{merged};
        }
    }
}
