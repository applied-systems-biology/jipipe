package org.hkijena.jipipe.api.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Determines how annotations are merged
 */
public enum JIPipeAnnotationMergeStrategy {
    SkipExisting,
    OverwriteExisting,
    Merge,
    MergeLists,
    Discard;

    /**
     * Ensures that a list of annotations has unique names. Merges according to the strategy if needed.
     *
     * @param annotations input annotations. can have duplicate names.
     * @return annotations without duplicate names.
     */
    public List<JIPipeAnnotation> merge(Collection<JIPipeAnnotation> annotations) {
        if (this == Discard) {
            return new ArrayList<>();
        }
        Map<String, String> map = new HashMap<>();
        for (JIPipeAnnotation annotation : annotations) {
            map.put(annotation.getName(), merge(map.getOrDefault(annotation.getName(), ""), annotation.getValue()));
        }
        List<JIPipeAnnotation> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            result.add(new JIPipeAnnotation(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    /**
     * Ensures that a list of annotations has unique names. Merges according to the strategy if needed.
     * If the mode is Discard, removes all annotations
     *
     * @param target      the target list
     * @param annotations input annotations. can have duplicate names.
     */
    public void mergeInto(Map<String, JIPipeAnnotation> target, Collection<JIPipeAnnotation> annotations) {
        if (this == Discard) {
            target.clear();
            return;
        }
        Map<String, String> map = new HashMap<>();
        for (Map.Entry<String, JIPipeAnnotation> entry : target.entrySet()) {
            if (entry.getValue() != null) {
                map.put(entry.getKey(), entry.getValue().getValue());
            }
        }
        for (JIPipeAnnotation annotation : annotations) {
            map.put(annotation.getName(), merge(map.getOrDefault(annotation.getName(), ""), annotation.getValue()));
        }
        for (Map.Entry<String, String> entry : map.entrySet()) {
            target.put(entry.getKey(), new JIPipeAnnotation(entry.getKey(), entry.getValue()));
        }
    }

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
        } else if (this == Discard) {
            return ""; // Empty = Discard
        } else {
            List<String> components = new ArrayList<>(Arrays.asList(extractMergedAnnotations(existingValue)));
            if (this == MergeLists) {
                String[] newValues = extractMergedAnnotations(newValue);
                if (newValues.length > 1) {
                    components.addAll(Arrays.asList(newValues));
                } else {
                    if (!components.contains(newValue))
                        components.add(newValue);
                }
            } else {
                if (!components.contains(newValue))
                    components.add(newValue);
            }
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

    @Override
    public String toString() {
        switch (this) {
            case Merge:
                return "Merge";
            case MergeLists:
                return "Merge lists";
            case SkipExisting:
                return "Skip existing";
            case OverwriteExisting:
                return "Overwrite existing";
            case Discard:
                return "Discard annotations";
            default:
                return super.toString();
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
