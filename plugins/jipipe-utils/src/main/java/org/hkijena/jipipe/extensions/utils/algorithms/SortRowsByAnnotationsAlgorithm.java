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

package org.hkijena.jipipe.extensions.utils.algorithms;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeParameterSlotAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.parameters.library.pairs.StringQueryExpressionAndSortOrderPairParameter;
import org.hkijena.jipipe.extensions.parameters.library.util.SortOrder;

import java.util.*;
import java.util.stream.Collectors;

@SetJIPipeDocumentation(name = "Sort data rows by annotation", description = "Sorts the data rows by one or multiple annotations.")
@AddJIPipeInputSlot(value = JIPipeData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Sort")
public class SortRowsByAnnotationsAlgorithm extends JIPipeParameterSlotAlgorithm {
    private StringQueryExpressionAndSortOrderPairParameter.List sortOrderList = new StringQueryExpressionAndSortOrderPairParameter.List();
    private SortOrder defaultSortOrder = SortOrder.Ascending;

    public SortRowsByAnnotationsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SortRowsByAnnotationsAlgorithm(SortRowsByAnnotationsAlgorithm other) {
        super(other);
        this.sortOrderList = new StringQueryExpressionAndSortOrderPairParameter.List(other.sortOrderList);
        this.defaultSortOrder = other.defaultSortOrder;
    }

    @Override
    public void runParameterSet(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo, List<JIPipeTextAnnotation> parameterAnnotations) {
        Set<String> unMatchedAnnotationNames = new HashSet<>(getFirstInputSlot().getTextAnnotationColumnNames());
        List<String> annotationOrder = new ArrayList<>();
        Map<String, SortOrder> annotationOrderSortOrder = new HashMap<>();

        // Find the order
        for (StringQueryExpressionAndSortOrderPairParameter entry : sortOrderList) {
            String matched = entry.getKey().queryFirst(unMatchedAnnotationNames, new JIPipeExpressionVariablesMap());
            if (matched != null) {
                unMatchedAnnotationNames.remove(matched);
                annotationOrder.add(matched);
                annotationOrderSortOrder.put(matched, entry.getValue());
            }
        }
        for (String matched : unMatchedAnnotationNames.stream().sorted().collect(Collectors.toList())) {
            annotationOrder.add(matched);
            annotationOrderSortOrder.put(matched, defaultSortOrder);
        }

        // Build the sorters
        Comparator<Integer> comparator = null;
        for (String key : annotationOrder) {
            Comparator<Integer> local;
            if (annotationOrderSortOrder.get(key) == SortOrder.Ascending) {
                local = (o1, o2) -> {
                    JIPipeTextAnnotation lhs = getFirstInputSlot().getTextAnnotationOr(o1, key, new JIPipeTextAnnotation());
                    JIPipeTextAnnotation rhs = getFirstInputSlot().getTextAnnotationOr(o2, key, new JIPipeTextAnnotation());
                    return lhs.compareTo(rhs);
                };
            } else {
                local = (o1, o2) -> {
                    JIPipeTextAnnotation lhs = getFirstInputSlot().getTextAnnotationOr(o1, key, new JIPipeTextAnnotation());
                    JIPipeTextAnnotation rhs = getFirstInputSlot().getTextAnnotationOr(o2, key, new JIPipeTextAnnotation());
                    return -lhs.compareTo(rhs);
                };
            }
            if (comparator == null) {
                comparator = local;
            } else {
                comparator = comparator.thenComparing(local);
            }
        }

        // Apply sorting and exporting
        List<Integer> rows = new ArrayList<>();
        for (int row = 0; row < getFirstInputSlot().getRowCount(); row++) {
            rows.add(row);
        }
        if (comparator != null)
            rows.sort(comparator);
        List<JIPipeTextAnnotation> annotations = new ArrayList<>();
        for (Integer row : rows) {
            annotations.clear();
            annotations.addAll(getFirstInputSlot().getTextAnnotations(row));
            annotations.addAll(parameterAnnotations);
            getFirstOutputSlot().addData(getFirstInputSlot().getData(row, JIPipeData.class, progressInfo),
                    annotations,
                    JIPipeTextAnnotationMergeMode.Merge,
                    getFirstInputSlot().getDataAnnotations(row),
                    JIPipeDataAnnotationMergeMode.Merge,
                    getFirstInputSlot().getDataContext(row).branch(this),
                    progressInfo);
        }
    }

    @SetJIPipeDocumentation(name = "Sort order", description = "Defines the order and sort order for the annotation columns. " +
            "Undefined annotation names are ordered alphabetically and sorted according to the default sort order. ")
    @JIPipeParameter("sort-order")
    public StringQueryExpressionAndSortOrderPairParameter.List getSortOrderList() {
        return sortOrderList;
    }

    @JIPipeParameter("sort-order")
    public void setSortOrderList(StringQueryExpressionAndSortOrderPairParameter.List sortOrderList) {
        this.sortOrderList = sortOrderList;
    }

    @SetJIPipeDocumentation(name = "Default sort order", description = "Determines the sort order for all annotations that do not match the 'Sort order' list.")
    @JIPipeParameter("default-sort-order")
    public SortOrder getDefaultSortOrder() {
        return defaultSortOrder;
    }

    @JIPipeParameter("default-sort-order")
    public void setDefaultSortOrder(SortOrder defaultSortOrder) {
        this.defaultSortOrder = defaultSortOrder;
    }
}
