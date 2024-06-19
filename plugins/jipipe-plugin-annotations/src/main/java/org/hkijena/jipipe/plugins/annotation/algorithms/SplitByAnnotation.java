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

package org.hkijena.jipipe.plugins.annotation.algorithms;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.parameters.library.graph.OutputSlotMapParameterCollection;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Algorithm that splits the input data by a specified annotation
 */
// Algorithm metadata
@SetJIPipeDocumentation(name = "Split & filter by annotation", description = "Splits the input data by a specified annotation or filters data based on the annotation value.")
@ConfigureJIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "Filter")
@AddJIPipeInputSlot(value = JIPipeData.class, name = "Input")
public class SplitByAnnotation extends JIPipeAlgorithm {

    private final OutputSlotMapParameterCollection targetSlots;

    /**
     * @param info algorithm info
     */
    public SplitByAnnotation(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Input", "", JIPipeData.class)
                .sealInput()
                .addOutputSlot("Output", "", JIPipeData.class)
                .build());
        this.targetSlots = new OutputSlotMapParameterCollection(AnnotationFilterExpression.class, this, null, true);
        registerSubParameter(targetSlots);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public SplitByAnnotation(SplitByAnnotation other) {
        super(other);
        this.targetSlots = new OutputSlotMapParameterCollection(AnnotationFilterExpression.class, this, null, true);
        other.targetSlots.copyTo(this.targetSlots);
        registerSubParameter(targetSlots);
    }

    @Override
    public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        if (isPassThrough()) {
            for (JIPipeDataSlot outputSlot : getOutputSlots()) {
                outputSlot.addDataFromSlot(getFirstInputSlot(), progressInfo);
            }
            return;
        }
        JIPipeDataSlot inputSlot = getFirstInputSlot();
        List<String> outputSlotKeys = getOutputSlotMap().keySet().stream().sorted().collect(Collectors.toList());
        for (int row = 0; row < inputSlot.getRowCount(); ++row) {
            List<JIPipeTextAnnotation> annotations = inputSlot.getTextAnnotations(row);
            String dataString = inputSlot.getData(row, JIPipeData.class, progressInfo).toString();
            for (String outputSlotKey : outputSlotKeys) {
                AnnotationFilterExpression expression = targetSlots.get(outputSlotKey).get(AnnotationFilterExpression.class);
                JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
                getDefaultCustomExpressionVariables().writeToVariables(variables);
                if (expression.test(annotations, dataString, variables)) {
                    getOutputSlot(outputSlotKey).addData(inputSlot.getData(row, JIPipeData.class, progressInfo),
                            annotations,
                            JIPipeTextAnnotationMergeMode.Merge,
                            inputSlot.getDataAnnotations(row),
                            JIPipeDataAnnotationMergeMode.Merge,
                            inputSlot.getDataContext(row).branch(this),
                            progressInfo);
                }
            }
        }
    }

    @JIPipeParameter("target-slots")
    @SetJIPipeDocumentation(name = "Filters", description = "One filter is created for each output slot of this node. The filter is an expression that should return a boolean value " +
            "that indicates whether a data item should be put into the corresponding output." +
            "Annotation values are available as variables. If an annotation has spaces special characters, use $ to access its value. Examples: <pre>" +
            "#Dataset CONTAINS \"Raw\" AND condition EQUALS \"mock\"</pre>" +
            "<pre>TO_NUMBER($\"my column\") < 10</pre>")
    public OutputSlotMapParameterCollection getTargetSlots() {
        return targetSlots;
    }

    @Override
    public boolean isEnableDefaultCustomExpressionVariables() {
        return true;
    }

}
