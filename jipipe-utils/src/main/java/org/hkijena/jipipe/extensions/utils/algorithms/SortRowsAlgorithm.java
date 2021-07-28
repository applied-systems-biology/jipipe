package org.hkijena.jipipe.extensions.utils.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeParameterSlotAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.parameters.pairs.StringQueryExpressionAndSortOrderPairParameter;
import org.hkijena.jipipe.extensions.parameters.util.SortOrder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "Sort data rows by annotation", description = "Sorts the data rows by one or multiple annotations.")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", inheritedSlot = "Input", autoCreate = true)
@JIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
public class SortRowsAlgorithm extends JIPipeParameterSlotAlgorithm {
    private StringQueryExpressionAndSortOrderPairParameter.List sortOrderList = new StringQueryExpressionAndSortOrderPairParameter.List();
    private SortOrder defaultSortOrder = SortOrder.Ascending;

    public SortRowsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SortRowsAlgorithm(SortRowsAlgorithm other) {
        super(other);
        this.sortOrderList = new StringQueryExpressionAndSortOrderPairParameter.List(other.sortOrderList);
        this.defaultSortOrder = other.defaultSortOrder;
    }

    @Override
    public void runParameterSet(JIPipeProgressInfo progressInfo, List<JIPipeAnnotation> parameterAnnotations) {
        Set<String> unMatchedAnnotationNames = new HashSet<>(getFirstInputSlot().getAnnotationColumns());
        List<String> annotationOrder = new ArrayList<>();
        Map<String, SortOrder> annotationOrderSortOrder = new HashMap<>();

        // Find the order
        for (StringQueryExpressionAndSortOrderPairParameter entry : sortOrderList) {
            String matched = entry.getKey().queryFirst(unMatchedAnnotationNames, new ExpressionVariables());
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
                    JIPipeAnnotation lhs = getFirstInputSlot().getAnnotationOr(o1, key, new JIPipeAnnotation());
                    JIPipeAnnotation rhs = getFirstInputSlot().getAnnotationOr(o2, key, new JIPipeAnnotation());
                    return lhs.compareTo(rhs);
                };
            } else {
                local = (o1, o2) -> {
                    JIPipeAnnotation lhs = getFirstInputSlot().getAnnotationOr(o1, key, new JIPipeAnnotation());
                    JIPipeAnnotation rhs = getFirstInputSlot().getAnnotationOr(o2, key, new JIPipeAnnotation());
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
        List<JIPipeAnnotation> annotations = new ArrayList<>();
        for (Integer row : rows) {
            annotations.clear();
            annotations.addAll(getFirstInputSlot().getAnnotations(row));
            annotations.addAll(parameterAnnotations);
            getFirstOutputSlot().addData(getFirstInputSlot().getData(row, JIPipeData.class, progressInfo), annotations, JIPipeAnnotationMergeStrategy.Merge, progressInfo);
        }
    }

    @JIPipeDocumentation(name = "Sort order", description = "Defines the order and sort order for the annotation columns. " +
            "Undefined annotation names are ordered alphabetically and sorted according to the default sort order. "
            + StringQueryExpression.DOCUMENTATION_DESCRIPTION)
    @JIPipeParameter("sort-order")
    public StringQueryExpressionAndSortOrderPairParameter.List getSortOrderList() {
        return sortOrderList;
    }

    @JIPipeParameter("sort-order")
    public void setSortOrderList(StringQueryExpressionAndSortOrderPairParameter.List sortOrderList) {
        this.sortOrderList = sortOrderList;
    }

    @JIPipeDocumentation(name = "Default sort order", description = "Determines the sort order for all annotations that do not match the 'Sort order' list.")
    @JIPipeParameter("default-sort-order")
    public SortOrder getDefaultSortOrder() {
        return defaultSortOrder;
    }

    @JIPipeParameter("default-sort-order")
    public void setDefaultSortOrder(SortOrder defaultSortOrder) {
        this.defaultSortOrder = defaultSortOrder;
    }
}
