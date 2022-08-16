/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.annotation.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.expressions.AnnotationGeneratorExpression;
import org.hkijena.jipipe.extensions.expressions.CustomExpressionVariablesParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.parameters.library.graph.OutputSlotMapParameterCollection;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Algorithm that splits the input data by a specified annotation
 */
// Algorithm metadata
@JIPipeDocumentation(name = "Split & filter by annotation", description = "Splits the input data by a specified annotation or filters data based on the annotation value.")
@JIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "Filter")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input")
public class SplitByAnnotation extends JIPipeAlgorithm {

    private final OutputSlotMapParameterCollection targetSlots;

    private final CustomExpressionVariablesParameter customVariables;

    /**
     * @param info algorithm info
     */
    public SplitByAnnotation(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Input", "", JIPipeData.class)
                .sealInput()
                .addOutputSlot("Output", "", JIPipeData.class, "Input")
                .build());
        this.customVariables = new CustomExpressionVariablesParameter(this);
        this.targetSlots = new OutputSlotMapParameterCollection(AnnotationGeneratorExpression.class, this, null, true);
        this.targetSlots.getEventBus().register(this);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public SplitByAnnotation(SplitByAnnotation other) {
        super(other);
        this.customVariables = new CustomExpressionVariablesParameter(other.customVariables, this);
        this.targetSlots = new OutputSlotMapParameterCollection(AnnotationGeneratorExpression.class, this, null, true);
        other.targetSlots.copyTo(this.targetSlots);
        this.targetSlots.getEventBus().register(this);
    }

    @Override
    public void run(JIPipeProgressInfo progressInfo) {
        if (isPassThrough()) {
            for (JIPipeDataSlot outputSlot : getOutputSlots()) {
                outputSlot.addData(getFirstInputSlot(), progressInfo);
            }
            return;
        }
        JIPipeDataSlot inputSlot = getFirstInputSlot();
        List<String> outputSlotKeys = getOutputSlotMap().keySet().stream().sorted().collect(Collectors.toList());
        for (int row = 0; row < inputSlot.getRowCount(); ++row) {
            List<JIPipeTextAnnotation> annotations = inputSlot.getTextAnnotations(row);
            String dataString = inputSlot.getData(row, JIPipeData.class, progressInfo).toString();
            for (String outputSlotKey : outputSlotKeys) {
                AnnotationGeneratorExpression expression = targetSlots.get(outputSlotKey).get(AnnotationGeneratorExpression.class);
                ExpressionVariables variables = new ExpressionVariables();
                customVariables.writeToVariables(variables, true, "custom.", true, "custom");
                if (expression.test(annotations, dataString, variables)) {
                    getOutputSlot(outputSlotKey).addData(inputSlot.getData(row, JIPipeData.class, progressInfo), annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
                }
            }
        }
    }

    @JIPipeParameter("target-slots")
    @JIPipeDocumentation(name = "Filters", description = "One filter is created for each output slot of this node. The filter is an expression that should return a boolean value " +
            "that indicates whether a data item should be put into the corresponding output." +
            "Annotation values are available as variables. If an annotation has spaces special characters, use $ to access its value. Examples: <pre>" +
            "#Dataset CONTAINS \"Raw\" AND condition EQUALS \"mock\"</pre>" +
            "<pre>TO_NUMBER($\"my column\") < 10</pre>")
    public OutputSlotMapParameterCollection getTargetSlots() {
        return targetSlots;
    }

    @JIPipeDocumentation(name = "Custom variables", description = "Here you can add parameters that will be included into the expressions as variables <code>custom.[key]</code>. Alternatively, you can access them via <code>GET_ITEM(\"custom\", \"[key]\")</code>.")
    @JIPipeParameter(value = "custom-variables", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/insert-math-expression.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/insert-math-expression.png", persistence = JIPipeParameterPersistence.NestedCollection)
    public CustomExpressionVariablesParameter getCustomVariables() {
        return customVariables;
    }
}
