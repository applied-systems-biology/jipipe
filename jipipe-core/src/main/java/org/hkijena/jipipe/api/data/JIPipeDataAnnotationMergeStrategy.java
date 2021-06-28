package org.hkijena.jipipe.api.data;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.hkijena.jipipe.api.JIPipeProgressInfo;

import java.util.*;

public enum JIPipeDataAnnotationMergeStrategy {
    Merge,
    MergeTables,
    SkipExisting,
    OverwriteExisting;

    /**
     * Ensures that a list of annotations has unique names. Merges according to the strategy if needed.
     *
     * @param annotations input annotations. can have duplicate names.
     * @return annotations without duplicate names.
     */
    public List<JIPipeDataAnnotation> merge(Collection<JIPipeDataAnnotation> annotations) {
        if(this == OverwriteExisting) {
            Map<String, JIPipeDataAnnotation> dataAnnotationMap = new HashMap<>();
            for (JIPipeDataAnnotation annotation : annotations) {
                dataAnnotationMap.put(annotation.getName(), annotation);
            }
            return new ArrayList<>(dataAnnotationMap.values());
        }
        else if(this == SkipExisting) {
            Map<String, JIPipeDataAnnotation> dataAnnotationMap = new HashMap<>();
            for (JIPipeDataAnnotation annotation : annotations) {
                dataAnnotationMap.putIfAbsent(annotation.getName(), annotation);
            }
            return new ArrayList<>(dataAnnotationMap.values());
        }
        else if(this == Merge) {
            Multimap<String, JIPipeDataAnnotation> dataAnnotationMap = HashMultimap.create();
            for (JIPipeDataAnnotation annotation : annotations) {
                dataAnnotationMap.put(annotation.getName(), annotation);
            }

            List<JIPipeDataAnnotation> result = new ArrayList<>();
            for (String name : dataAnnotationMap.keySet()) {
                Collection<JIPipeDataAnnotation> values = dataAnnotationMap.get(name);
                if(values.size() <= 1) {
                    result.addAll(values);
                }
                else {
                    JIPipeMergedDataAnnotationsData mergedDataAnnotationsData = new JIPipeMergedDataAnnotationsData(new JIPipeDataSlot(
                            new JIPipeDataSlotInfo(JIPipeData.class, JIPipeSlotType.Output, "Merged", null), null
                    ));
                    for (JIPipeDataAnnotation value : values) {
                        mergedDataAnnotationsData.getDataSlot().addData(value.getVirtualData(), Collections.emptyList(), JIPipeAnnotationMergeStrategy.OverwriteExisting);
                    }
                    result.add(new JIPipeDataAnnotation(name, mergedDataAnnotationsData));
                }
            }
            return result;
        }
        else if(this == MergeTables) {
            Multimap<String, JIPipeDataAnnotation> dataAnnotationMap = HashMultimap.create();
            for (JIPipeDataAnnotation annotation : annotations) {
                dataAnnotationMap.put(annotation.getName(), annotation);
            }

            List<JIPipeDataAnnotation> result = new ArrayList<>();
            for (String name : dataAnnotationMap.keySet()) {
                List<JIPipeVirtualData> allData = new ArrayList<>();
                for (JIPipeDataAnnotation dataAnnotation : dataAnnotationMap.get(name)) {
                    if(JIPipeMergedDataAnnotationsData.class.isAssignableFrom(dataAnnotation.getDataClass())) {
                        JIPipeMergedDataAnnotationsData table = dataAnnotation.getData(JIPipeMergedDataAnnotationsData.class, new JIPipeProgressInfo());
                        for (int row = 0; row < table.getDataSlot().getRowCount(); row++) {
                            allData.add(table.getDataSlot().getVirtualData(row));
                        }
                    }
                    else {
                        allData.add(dataAnnotation.getVirtualData());
                    }
                }
                JIPipeMergedDataAnnotationsData mergedDataAnnotationsData = new JIPipeMergedDataAnnotationsData(new JIPipeDataSlot(
                        new JIPipeDataSlotInfo(JIPipeData.class, JIPipeSlotType.Output, "Merged", null), null
                ));
                for (JIPipeVirtualData virtualData : allData) {
                    mergedDataAnnotationsData.getDataSlot().addData(virtualData, Collections.emptyList(), JIPipeAnnotationMergeStrategy.OverwriteExisting);
                }
                result.add(new JIPipeDataAnnotation(name, mergedDataAnnotationsData));
            }
            return result;
        }
        else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public String toString() {
        switch (this) {
            case Merge:
                return "Merge";
            case SkipExisting:
                return "Skip existing";
            case OverwriteExisting:
                return "Overwrite existing";
            default:
                return super.toString();
        }
    }
}