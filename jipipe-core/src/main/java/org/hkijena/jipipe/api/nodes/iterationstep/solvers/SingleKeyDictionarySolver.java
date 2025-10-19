package org.hkijena.jipipe.api.nodes.iterationstep.solvers;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.iterationstep.IterationStepSolver;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStepGenerator;

import java.util.*;

public class SingleKeyDictionarySolver implements IterationStepSolver {
    @Override
    public List<JIPipeMultiIterationStep> solve(JIPipeMultiIterationStepGenerator generator, JIPipeProgressInfo progressInfo) {
        final List<JIPipeDataSlot> slotList = generator.getSlots();
        final String annotationKey = generator.getReferenceColumns().iterator().next();
        Set<String> allKeys = new HashSet<>();
        Map<JIPipeDataSlot, Multimap<String, Integer>> matchedRows = new HashMap<>();

        // Find groups of rows
        progressInfo.log("Grouping rows");
        for (JIPipeDataSlot slot : slotList) {
            progressInfo.resolve("Grouping rows").log("Slot " + slot.getName());
            Multimap<String, Integer> slotMap = HashMultimap.create();
            for (int row = 0; row < slot.getRowCount(); row++) {
                JIPipeTextAnnotation annotation = slot.getTextAnnotationOr(row, annotationKey, null);
                String value = annotation != null ? annotation.getValue() : "";
                slotMap.put(value, row);
                allKeys.add(value);
            }
            matchedRows.put(slot, slotMap);
        }
        if (progressInfo.isCancelled())
            return null;

        // Apply "empty" to all other keys if we have any other ones (distribute case - only for multiple inputs)
        if (allKeys.contains("") && (generator.isForceNAIsAny() || slotList.size() > 1) && allKeys.size() > 1) {

            // Expand the empty key to all other keys
            for (String key : allKeys) {
                for (JIPipeDataSlot slot : slotList) {
                    Multimap<String, Integer> slotMatchedRows = matchedRows.get(slot);
                    if (slotMatchedRows.keySet().contains("")) {
                        slotMatchedRows.putAll(key, slotMatchedRows.get(""));
                    }
                }
            }
            for (JIPipeDataSlot slot : slotList) {
                Multimap<String, Integer> slotMatchedRows = matchedRows.get(slot);
                slotMatchedRows.removeAll("");
            }
            allKeys.remove("");
        }

        List<JIPipeMultiIterationStep> iterationSteps = new ArrayList<>();
        if (generator.isApplyMerging()) {
            // We are done. Directly convert to iteration steps
            for (String key : allKeys) {
                JIPipeMultiIterationStep iterationStep = new JIPipeMultiIterationStep(generator.getNode());
                for (JIPipeDataSlot slot : slotList) {
                    Multimap<String, Integer> slotMap = matchedRows.get(slot);
                    iterationStep.addInputData(slot, slotMap.get(key));
                    iterationStep.addMergedTextAnnotations(slot.getTextAnnotations(slotMap.get(key)), generator.getAnnotationMergeStrategy());
                    iterationStep.addMergedDataAnnotations(slot.getDataAnnotations(slotMap.get(key)), generator.getDataAnnotationMergeStrategy());
                }
                iterationSteps.add(iterationStep);
            }
        } else {
            // Split apart into single batches
            progressInfo.log("Splitting batches (" + allKeys.size() + " keys)");
            for (String key : allKeys) {
                List<JIPipeMultiIterationStep> keyBatches = new ArrayList<>();
                List<JIPipeMultiIterationStep> tempKeyBatches = new ArrayList<>();
                for (JIPipeDataSlot slot : slotList) {
                    Collection<Integer> rows = matchedRows.get(slot).get(key);
                    if (keyBatches.isEmpty()) {
                        // No iteration steps -> Create initial batch set
                        for (Integer row : rows) {
                            JIPipeMultiIterationStep iterationStep = new JIPipeMultiIterationStep(generator.getNode());
                            iterationStep.addInputData(slot, row);
                            tempKeyBatches.add(iterationStep);
                        }
                    } else {
                        // Add the row into copies of all key batches
                        if (!rows.isEmpty()) {
                            for (Integer row : rows) {
                                for (JIPipeMultiIterationStep iterationStep : keyBatches) {
                                    JIPipeMultiIterationStep copy = new JIPipeMultiIterationStep(iterationStep);
                                    copy.addInputData(slot, row);
                                    tempKeyBatches.add(copy);
                                }
                            }
                        } else {
                            tempKeyBatches.addAll(keyBatches);
                        }
                    }
                    keyBatches.clear();
                    keyBatches.addAll(tempKeyBatches);
                    tempKeyBatches.clear();
                }
                iterationSteps.addAll(keyBatches);
            }
            // Resolve annotations
            progressInfo.log("Resolving annotations");
            for (JIPipeMultiIterationStep iterationStep : iterationSteps) {
                List<JIPipeTextAnnotation> annotations = new ArrayList<>();
                List<JIPipeDataAnnotation> dataAnnotations = new ArrayList<>();
                for (JIPipeDataSlot slot : slotList) {
                    annotations.addAll(slot.getTextAnnotations(iterationStep.getInputRows(slot)));
                    dataAnnotations.addAll(slot.getDataAnnotations(iterationStep.getInputRows(slot)));
                }
                iterationStep.addMergedTextAnnotations(annotations, generator.getAnnotationMergeStrategy());
                iterationStep.addMergedDataAnnotations(dataAnnotations, generator.getDataAnnotationMergeStrategy());
            }
        }
        // Ensure that all slots are known to the iteration step builder
        for (JIPipeMultiIterationStep iterationStep : iterationSteps) {
            for (JIPipeDataSlot slot : slotList) {
                iterationStep.addEmptySlot(slot);
            }
        }
        return iterationSteps;
    }
}
