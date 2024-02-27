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

package org.hkijena.jipipe.api.annotation;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.util.*;

/**
 * Determines how annotations are merged
 */
public enum JIPipeTextAnnotationMergeMode {
    SkipExisting,
    OverwriteExisting,
    Merge,
    MergeLists,
    Discard;

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

    /**
     * Ensures that a list of annotations has unique names. Merges according to the strategy if needed.
     *
     * @param annotations input annotations. can have duplicate names.
     * @return annotations without duplicate names.
     */
    public List<JIPipeTextAnnotation> merge(Collection<JIPipeTextAnnotation> annotations) {
        if (this == Discard) {
            return new ArrayList<>();
        }
        Map<String, String> map = new HashMap<>();
        for (JIPipeTextAnnotation annotation : annotations) {
            map.put(annotation.getName(), merge(map.getOrDefault(annotation.getName(), ""), annotation.getValue()));
        }
        List<JIPipeTextAnnotation> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            result.add(new JIPipeTextAnnotation(entry.getKey(), entry.getValue()));
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
    public void mergeInto(Map<String, JIPipeTextAnnotation> target, Collection<JIPipeTextAnnotation> annotations) {
        if (this == Discard) {
            target.clear();
            return;
        }
        Map<String, String> map = new HashMap<>();
        for (Map.Entry<String, JIPipeTextAnnotation> entry : target.entrySet()) {
            if (entry.getValue() != null) {
                map.put(entry.getKey(), entry.getValue().getValue());
            }
        }
        for (JIPipeTextAnnotation annotation : annotations) {
            map.put(annotation.getName(), merge(map.getOrDefault(annotation.getName(), ""), annotation.getValue()));
        }
        for (Map.Entry<String, String> entry : map.entrySet()) {
            target.put(entry.getKey(), new JIPipeTextAnnotation(entry.getKey(), entry.getValue()));
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


}
