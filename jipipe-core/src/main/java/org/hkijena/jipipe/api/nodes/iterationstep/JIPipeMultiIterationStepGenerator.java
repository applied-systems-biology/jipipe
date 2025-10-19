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

package org.hkijena.jipipe.api.nodes.iterationstep;

import com.google.common.collect.Sets;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeIterationStepTextAnnotationColumMatching;
import org.hkijena.jipipe.api.nodes.JIPipeTextAnnotationMatchingMethod;
import org.hkijena.jipipe.api.nodes.iterationstep.solvers.*;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.StringQueryExpression;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class that generates a {@link JIPipeMultiIterationStep} or {@link JIPipeSingleIterationStep} instance.
 */
public class JIPipeMultiIterationStepGenerator {

    private static final Set<String> REFERENCE_COLUMN_MERGE_ALL = Sets.newHashSet("{{}}MERGE_ALL");
    private static final Set<String> REFERENCE_COLUMN_SPLIT_ALL = Sets.newHashSet("{{}}SPLIT_ALL");
    private final List<JIPipeDataSlot> slotList = new ArrayList<>();
    private final Map<String, JIPipeDataSlot> slotMap = new HashMap<>();
    private JIPipeGraphNode node;
    private Set<String> referenceColumns = new HashSet<>();
    private JIPipeTextAnnotationMergeMode annotationMergeStrategy = JIPipeTextAnnotationMergeMode.Merge;
    private JIPipeDataAnnotationMergeMode dataAnnotationMergeStrategy = JIPipeDataAnnotationMergeMode.MergeTables;
    private boolean applyMerging = true;
    private JIPipeTextAnnotationMatchingMethod annotationMatchingMethod = JIPipeTextAnnotationMatchingMethod.ExactMatch;
    private JIPipeExpressionParameter customAnnotationMatching = new JIPipeExpressionParameter("exact_match_results");
    private JIPipeMultiIterationStepGeneratorSolverPreference solverPreference = JIPipeMultiIterationStepGeneratorSolverPreference.Auto;
    private boolean forceNAIsAny = false;

    private final CompositeDictionarySolver compositeDictionarySolver = new CompositeDictionarySolver();
    private final SingleKeyDictionarySolver singleKeyDictionarySolver = new SingleKeyDictionarySolver();
    private final FlowGraphSolver flowGraphSolver = new FlowGraphSolver();
    private final MergeAllSolver  mergeAllSolver = new MergeAllSolver();
    private final SplitAllSolver splitAllSolver = new SplitAllSolver();

    public JIPipeMultiIterationStepGenerator() {

    }

    /**
     * Builds a single iteration step where each slot only can have one row
     *
     * @return the list of batched or null if none can be generated
     */
    public static List<JIPipeSingleIterationStep> convertMergingToSingleDataBatches(List<JIPipeMultiIterationStep> mergingDataBatches) {
        List<JIPipeSingleIterationStep> result = new ArrayList<>();
        for (JIPipeMultiIterationStep batch : mergingDataBatches) {
            JIPipeSingleIterationStep singleBatch = new JIPipeSingleIterationStep(batch.getNode());
            for (Map.Entry<JIPipeDataSlot, Set<Integer>> entry : batch.getInputSlotRows().entrySet()) {
                if (entry.getValue().size() > 1)
                    return null;
                if (entry.getValue().isEmpty())
                    continue;
                int targetRow = entry.getValue().iterator().next();
                singleBatch.setInputData(entry.getKey(), targetRow);
                singleBatch.setMergedTextAnnotations(batch.getMergedTextAnnotations());
                singleBatch.setMergedDataAnnotations(batch.getMergedDataAnnotations());
            }
            result.add(singleBatch);
        }
        return result;
    }

    public List<JIPipeDataSlot> getSlots() {
        return slotList;
    }

    public void setSlots(List<JIPipeInputDataSlot> slotMap) {
        this.slotMap.clear();
        this.slotList.clear();
        this.slotList.addAll(slotMap);
        for (JIPipeDataSlot slot : slotMap) {
            this.slotMap.put(slot.getName(), slot);
        }
    }

    public Set<String> getReferenceColumns() {
        return referenceColumns;
    }

    /**
     * Sets the reference columns
     * An empty list merges all data into one batch
     * Setting it to null splits all data into a separate batch
     *
     * @param referenceColumns the reference columns
     */
    public void setReferenceColumns(Set<String> referenceColumns) {
        this.referenceColumns = referenceColumns;
    }

    public void setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching columnGrouping, StringQueryExpression customColumns) {
        if (slotMap.isEmpty())
            System.err.println("Warning: Trying to calculate reference columns with empty slot list!");
        switch (columnGrouping) {
            case Custom:
                referenceColumns = getInputAnnotationByFilter(customColumns);
                break;
            case Union:
                referenceColumns = getInputAnnotationColumnUnion("");
                break;
            case Intersection:
                referenceColumns = getInputAnnotationColumnIntersection("");
                break;
            case PrefixHashUnion:
                referenceColumns = getInputAnnotationColumnUnion("#");
                break;
            case PrefixHashIntersection:
                referenceColumns = getInputAnnotationColumnIntersection("#");
                break;
            case MergeAll:
                referenceColumns = REFERENCE_COLUMN_MERGE_ALL;
                break;
            case SplitAll:
                referenceColumns = REFERENCE_COLUMN_SPLIT_ALL;
                break;
            case None:
                referenceColumns = Collections.emptySet();
                break;
            default:
                throw new UnsupportedOperationException("Unknown column matching strategy: " + columnGrouping);
        }
    }

    public Set<String> getInputAnnotationByFilter(StringQueryExpression expression) {
        Set<String> result = new HashSet<>();
        for (JIPipeDataSlot slot : slotMap.values()) {
            result.addAll(slot.getTextAnnotationColumnNames());
        }
        return new HashSet<>(expression.queryAll(result, new JIPipeExpressionVariablesMap()));
    }

    public Set<String> getInputAnnotationColumnIntersection(String prefix) {
        Set<String> result = new HashSet<>();
        for (JIPipeDataSlot inputSlot : slotMap.values()) {
            Set<String> filtered = inputSlot.getTextAnnotationColumnNames().stream().filter(s -> s.startsWith(prefix)).collect(Collectors.toSet());
            if (result.isEmpty()) {
                result.addAll(filtered);
            } else {
                result.retainAll(filtered);
            }
        }
        return result;
    }

    public Set<String> getInputAnnotationColumnUnion(String prefix) {
        Set<String> result = new HashSet<>();
        for (JIPipeDataSlot inputSlot : slotMap.values()) {
            Set<String> filtered = inputSlot.getTextAnnotationColumnNames().stream().filter(s -> s.startsWith(prefix)).collect(Collectors.toSet());
            result.addAll(filtered);
        }
        return result;
    }

    public List<JIPipeMultiIterationStep> build(JIPipeProgressInfo progressInfo) {

        if(slotList.isEmpty()) {
            return new ArrayList<>();
        }

        // Special case: Merge all
        if (getReferenceColumns() == REFERENCE_COLUMN_MERGE_ALL) {
            return mergeAllSolver.solve(this, progressInfo.resolveAndLog("Merge into one batch"));
        }
        // Special case: Split all
        if (getReferenceColumns() == REFERENCE_COLUMN_SPLIT_ALL) {
            return splitAllSolver.solve(this, progressInfo.resolveAndLog("Split into batches"));
        }

        if (solverPreference.isAllowSingleDictionary() && referenceColumns.size() == 1 && annotationMatchingMethod == JIPipeTextAnnotationMatchingMethod.ExactMatch) {
            return singleKeyDictionarySolver.solve(this, progressInfo.resolveAndLog("Single-key dictionary solver"));
        }
        if (solverPreference.isAllowCompositeDictionary() && annotationMatchingMethod == JIPipeTextAnnotationMatchingMethod.ExactMatch) {
            return compositeDictionarySolver.solve(this, progressInfo.resolveAndLog("Composite dictionary solver"));
        }

        // No easy solution: Use flow graph solver
        return flowGraphSolver.solve(this, progressInfo.resolveAndLog("Flow graph solver"));
    }



    public boolean isApplyMerging() {
        return applyMerging;
    }

    public void setApplyMerging(boolean applyMerging) {
        this.applyMerging = applyMerging;
    }

    public JIPipeGraphNode getNode() {
        return node;
    }

    public void setNode(JIPipeGraphNode node) {
        this.node = node;
    }

    public JIPipeTextAnnotationMergeMode getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    public void setAnnotationMergeStrategy(JIPipeTextAnnotationMergeMode annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }

    public JIPipeDataAnnotationMergeMode getDataAnnotationMergeStrategy() {
        return dataAnnotationMergeStrategy;
    }

    public void setDataAnnotationMergeStrategy(JIPipeDataAnnotationMergeMode dataAnnotationMergeStrategy) {
        this.dataAnnotationMergeStrategy = dataAnnotationMergeStrategy;
    }

    public JIPipeTextAnnotationMatchingMethod getAnnotationMatchingMethod() {
        return annotationMatchingMethod;
    }

    public void setAnnotationMatchingMethod(JIPipeTextAnnotationMatchingMethod annotationMatchingMethod) {
        this.annotationMatchingMethod = annotationMatchingMethod;
    }

    public JIPipeExpressionParameter getCustomAnnotationMatching() {
        return customAnnotationMatching;
    }

    public void setCustomAnnotationMatching(JIPipeExpressionParameter customAnnotationMatching) {
        this.customAnnotationMatching = customAnnotationMatching;
    }

    public boolean isForceNAIsAny() {
        return forceNAIsAny;
    }

    public void setForceNAIsAny(boolean forceNAIsAny) {
        this.forceNAIsAny = forceNAIsAny;
    }

    public JIPipeMultiIterationStepGeneratorSolverPreference getSolverPreference() {
        return solverPreference;
    }

    public void setSolverPreference(JIPipeMultiIterationStepGeneratorSolverPreference solverPreference) {
        this.solverPreference = solverPreference;
    }
}
