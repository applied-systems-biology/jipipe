package org.hkijena.jipipe.extensions.utils.algorithms;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeParameterSlotAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionListTemplate;
import org.hkijena.jipipe.extensions.parameters.library.util.SortOrder;
import org.hkijena.jipipe.utils.NaturalOrderComparator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@SetJIPipeDocumentation(name = "Sort data rows (Expression)", description = "Sorts the data rows by one or multiple values that are extracted via expressions.")
@AddJIPipeInputSlot(value = JIPipeData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Sort")
public class SortRowsByExpressionAlgorithm extends JIPipeParameterSlotAlgorithm {

    private ParameterCollectionList entries = ParameterCollectionList.containingCollection(SortEntry.class);

    public SortRowsByExpressionAlgorithm(JIPipeNodeInfo info) {
        super(info);
        entries.addNewInstance();
    }

    public SortRowsByExpressionAlgorithm(SortRowsByExpressionAlgorithm other) {
        super(other);
        this.entries = new ParameterCollectionList(other.entries);
    }

    @Override
    public void runParameterSet(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo, List<JIPipeTextAnnotation> parameterAnnotations) {

        List<SortEntry> sortEntries = entries.mapToCollection(SortEntry.class);

        // Run the expression
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();

        List<List<String>> generatedValues = new ArrayList<>();
        for (int row = 0; row < getFirstInputSlot().getRowCount(); row++) {
            variables.putAnnotations(getFirstInputSlot().getTextAnnotations(row));
            getDefaultCustomExpressionVariables().writeToVariables(variables);

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

    @SetJIPipeDocumentation(name = "Sort entries", description = "Each entry contains an expression that should return a value that is compared across the data.")
    @JIPipeParameter("entries")
    @ParameterCollectionListTemplate(SortEntry.class)
    public ParameterCollectionList getEntries() {
        return entries;
    }

    @JIPipeParameter("entries")
    public void setEntries(ParameterCollectionList entries) {
        this.entries = entries;
    }

    @Override
    public boolean isEnableDefaultCustomExpressionVariables() {
        return true;
    }

    public static class SortEntry extends AbstractJIPipeParameterCollection {

        private JIPipeExpressionParameter value = new JIPipeExpressionParameter();
        private SortOrder sortOrder = SortOrder.Ascending;


        public SortEntry() {

        }

        public SortEntry(SortEntry other) {
            this.value = new JIPipeExpressionParameter(other.value);
            this.sortOrder = other.sortOrder;
        }

        @SetJIPipeDocumentation(name = "Value")
        @JIPipeParameter(value = "value", uiOrder = -100)
        @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        @JIPipeExpressionParameterVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
        @JIPipeExpressionParameterVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
        public JIPipeExpressionParameter getValue() {
            return value;
        }

        @JIPipeParameter("value")
        public void setValue(JIPipeExpressionParameter value) {
            this.value = value;
        }

        @SetJIPipeDocumentation(name = "Sort order")
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
