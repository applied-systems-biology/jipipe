package org.hkijena.jipipe.extensions.utils.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeParameterSlotAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.expressions.CustomExpressionVariablesParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionListTemplate;
import org.hkijena.jipipe.extensions.parameters.library.util.SortOrder;
import org.hkijena.jipipe.utils.NaturalOrderComparator;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@JIPipeDocumentation(name = "Sort data rows (Expression)", description = "Sorts the data rows by one or multiple values that are extracted via expressions.")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", autoCreate = true)
@JIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Sort")
public class SortRowsByExpressionAlgorithm extends JIPipeParameterSlotAlgorithm {

    private final CustomExpressionVariablesParameter customExpressionVariables;
    private ParameterCollectionList entries = ParameterCollectionList.containingCollection(SortEntry.class);

    public SortRowsByExpressionAlgorithm(JIPipeNodeInfo info) {
        super(info);
        customExpressionVariables = new CustomExpressionVariablesParameter(this);
        entries.addNewInstance();
    }

    public SortRowsByExpressionAlgorithm(SortRowsByExpressionAlgorithm other) {
        super(other);
        this.customExpressionVariables = new CustomExpressionVariablesParameter(other.customExpressionVariables, this);
        this.entries = new ParameterCollectionList(other.entries);
    }

    @Override
    public void runParameterSet(JIPipeProgressInfo progressInfo, List<JIPipeTextAnnotation> parameterAnnotations) {

        List<SortEntry> sortEntries = entries.mapToCollection(SortEntry.class);

        // Run the expression
        ExpressionVariables variables = new ExpressionVariables();

        List<List<String>> generatedValues = new ArrayList<>();
        for (int row = 0; row < getFirstInputSlot().getRowCount(); row++) {
            variables.putAnnotations(getFirstInputSlot().getTextAnnotations(row));
            customExpressionVariables.writeToVariables(variables, true, "custom.", true, "custom");

            List<String> values = new ArrayList<>();
            for (SortEntry entry : sortEntries) {
                values.add(entry.getValue().evaluateToString(variables));
            }
            generatedValues.add(values);
        }

        // Comparator between two row indices
        Comparator<Integer> comparator = null;
        if (!entries.isEmpty()) {
            SortEntry entry = sortEntries.get(0);
            if (entry.getSortOrder() == SortOrder.Ascending) {
                comparator = Comparator.comparing((Integer row) -> generatedValues.get(row).get(0), NaturalOrderComparator.INSTANCE);
            } else {
                comparator = Comparator.comparing((Integer row) -> generatedValues.get(row).get(0), NaturalOrderComparator.INSTANCE.reversed());
            }
        }
        for (int entryIndex = 1; entryIndex < sortEntries.size(); entryIndex++) {
            SortEntry entry = sortEntries.get(entryIndex);
            if (entry.getSortOrder() == SortOrder.Ascending) {
                int finalEntryIndex = entryIndex;
                comparator = comparator.thenComparing((Integer row) -> generatedValues.get(row).get(finalEntryIndex), NaturalOrderComparator.INSTANCE);
            } else {
                int finalEntryIndex = entryIndex;
                comparator = comparator.thenComparing((Integer row) -> generatedValues.get(row).get(finalEntryIndex), NaturalOrderComparator.INSTANCE.reversed());
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

    @JIPipeDocumentation(name = "Sort entries", description = "Each entry contains an expression that should return a value that is compared across the data.")
    @JIPipeParameter("entries")
    @ParameterCollectionListTemplate(SortEntry.class)
    public ParameterCollectionList getEntries() {
        return entries;
    }

    @JIPipeParameter("entries")
    public void setEntries(ParameterCollectionList entries) {
        this.entries = entries;
    }

    @JIPipeDocumentation(name = "Custom expression variables", description = "Here you can add parameters that will be included into the expression as variables <code>custom.[key]</code>. Alternatively, you can access them via <code>GET_ITEM(custom, \"[key]\")</code>.")
    @JIPipeParameter(value = "custom-expression-variables", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/insert-math-expression.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/insert-math-expression.png", persistence = JIPipeParameterPersistence.NestedCollection)
    public CustomExpressionVariablesParameter getCustomExpressionVariables() {
        return customExpressionVariables;
    }

    public static class SortEntry extends AbstractJIPipeParameterCollection {

        private DefaultExpressionParameter value = new DefaultExpressionParameter();
        private SortOrder sortOrder = SortOrder.Ascending;


        public SortEntry() {

        }

        public SortEntry(SortEntry other) {
            this.value = new DefaultExpressionParameter(other.value);
            this.sortOrder = other.sortOrder;
        }

        @JIPipeDocumentation(name = "Value")
        @JIPipeParameter(value = "value", uiOrder = -100)
        @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
        @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
        @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
        public DefaultExpressionParameter getValue() {
            return value;
        }

        @JIPipeParameter("value")
        public void setValue(DefaultExpressionParameter value) {
            this.value = value;
        }

        @JIPipeDocumentation(name = "Sort order")
        @JIPipeParameter("sort-order")
        public SortOrder getSortOrder() {
            return sortOrder;
        }

        @JIPipeParameter("sort-order")
        public void setSortOrder(SortOrder sortOrder) {
            this.sortOrder = sortOrder;
        }
    }
}
