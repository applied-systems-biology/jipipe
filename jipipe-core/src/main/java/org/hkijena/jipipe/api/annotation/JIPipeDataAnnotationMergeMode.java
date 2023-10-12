package org.hkijena.jipipe.api.annotation;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataItemStore;
import org.hkijena.jipipe.api.data.JIPipeDataTable;

import java.util.*;

public enum JIPipeDataAnnotationMergeMode {
    Merge,
    MergeTables,
    SkipExisting,
    OverwriteExisting,
    Discard;

    /**
     * Ensures that a list of annotations has unique names. Merges according to the strategy if needed.
     *
     * @param annotations input annotations. can have duplicate names.
     * @return annotations without duplicate names.
     */
    public List<JIPipeDataAnnotation> merge(Collection<JIPipeDataAnnotation> annotations) {
        if (this == OverwriteExisting) {
            Map<String, JIPipeDataAnnotation> dataAnnotationMap = new HashMap<>();
            for (JIPipeDataAnnotation annotation : annotations) {
                dataAnnotationMap.put(annotation.getName(), annotation);
            }
            return new ArrayList<>(dataAnnotationMap.values());
        } else if (this == SkipExisting) {
            Map<String, JIPipeDataAnnotation> dataAnnotationMap = new HashMap<>();
            for (JIPipeDataAnnotation annotation : annotations) {
                dataAnnotationMap.putIfAbsent(annotation.getName(), annotation);
            }
            return new ArrayList<>(dataAnnotationMap.values());
        } else if (this == Merge) {
            Multimap<String, JIPipeDataAnnotation> dataAnnotationMap = HashMultimap.create();
            for (JIPipeDataAnnotation annotation : annotations) {
                dataAnnotationMap.put(annotation.getName(), annotation);
            }

            List<JIPipeDataAnnotation> result = new ArrayList<>();
            for (String name : dataAnnotationMap.keySet()) {
                Collection<JIPipeDataAnnotation> values = dataAnnotationMap.get(name);
                if (values.size() <= 1) {
                    result.addAll(values);
                } else {
                    JIPipeDataTable mergedDataAnnotationsData = new JIPipeDataTable(JIPipeData.class);
                    for (JIPipeDataAnnotation value : values) {
                        mergedDataAnnotationsData.addData(value.getVirtualData(),
                                Collections.emptyList(),
                                JIPipeTextAnnotationMergeMode.OverwriteExisting,
                                new JIPipeProgressInfo());
                    }
                    result.add(new JIPipeDataAnnotation(name, mergedDataAnnotationsData));
                }
            }
            return result;
        } else if (this == MergeTables) {
            Multimap<String, JIPipeDataAnnotation> dataAnnotationMap = HashMultimap.create();
            for (JIPipeDataAnnotation annotation : annotations) {
                dataAnnotationMap.put(annotation.getName(), annotation);
            }

            List<JIPipeDataAnnotation> result = new ArrayList<>();
            for (String name : dataAnnotationMap.keySet()) {
                List<JIPipeDataItemStore> allData = new ArrayList<>();
                for (JIPipeDataAnnotation dataAnnotation : dataAnnotationMap.get(name)) {
                    if (JIPipeDataTable.class.isAssignableFrom(dataAnnotation.getDataClass())) {
                        JIPipeDataTable table = dataAnnotation.getData(JIPipeDataTable.class, new JIPipeProgressInfo());
                        for (int row = 0; row < table.getRowCount(); row++) {
                            allData.add(table.getDataItemStore(row));
                        }
                    } else {
                        allData.add(dataAnnotation.getVirtualData());
                    }
                }
                JIPipeDataTable mergedDataAnnotationsData = new JIPipeDataTable(JIPipeData.class);
                for (JIPipeDataItemStore virtualData : allData) {
                    mergedDataAnnotationsData.addData(virtualData, Collections.emptyList(), JIPipeTextAnnotationMergeMode.OverwriteExisting, new JIPipeProgressInfo());
                }
                result.add(new JIPipeDataAnnotation(name, mergedDataAnnotationsData));
            }
            return result;
        } else if (this == Discard) {
            return new ArrayList<>();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public String toString() {
        switch (this) {
            case Merge:
                return "Merge";
            case MergeTables:
                return "Merge tables";
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
