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
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.collections.OutputSlotMapParameterCollection;
import org.hkijena.jipipe.extensions.parameters.expressions.AnnotationGeneratorExpression;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Algorithm that splits the input data by a specified annotation
 */
// Algorithm metadata
@JIPipeDocumentation(name = "Split & filter by annotation", description = "Splits the input data by a specified annotation or filters data based on the annotation value.")
@JIPipeOrganization(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "Filter")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input")
public class SplitByAnnotation extends JIPipeAlgorithm {

    private OutputSlotMapParameterCollection targetSlots;

    /**
     * @param info algorithm info
     */
    public SplitByAnnotation(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Input", JIPipeData.class)
                .sealInput()
                .build());
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
        this.targetSlots = new OutputSlotMapParameterCollection(AnnotationGeneratorExpression.class, this, null, false);
        other.targetSlots.copyTo(this.targetSlots);
        this.targetSlots.getEventBus().register(this);
    }

    @Override
    public void run(JIPipeProgressInfo progressInfo) {
        JIPipeDataSlot inputSlot = getFirstInputSlot();
        List<String> outputSlotKeys = getOutputSlotMap().keySet().stream().sorted().collect(Collectors.toList());
        for (int row = 0; row < inputSlot.getRowCount(); ++row) {
            List<JIPipeAnnotation> annotations = inputSlot.getAnnotations(row);
            String dataString = inputSlot.getData(row, JIPipeData.class, progressInfo).toString();
            for (String outputSlotKey : outputSlotKeys) {
                AnnotationGeneratorExpression expression = targetSlots.get(outputSlotKey).get(AnnotationGeneratorExpression.class);
                if (expression.test(annotations, dataString)) {
                    getOutputSlot(outputSlotKey).addData(inputSlot.getData(row, JIPipeData.class, progressInfo), annotations, JIPipeAnnotationMergeStrategy.Merge, progressInfo);
                }
            }
        }
    }

    @JIPipeParameter("target-slots")
    @JIPipeDocumentation(name = "Target slots", description = "Data that matches the filter on the right-hand side are redirected to the data slot on the left-hand side. " +
            "Annotation values are available as variables. If an annotation has spaces special characters, use $ to access its value. Examples: <pre>" +
            "#Dataset CONTAINS \"Raw\" AND condition EQUALS \"mock\"</pre>" +
            "<pre>NUMBER($\"my column\") < 10</pre>")
    public OutputSlotMapParameterCollection getTargetSlots() {
        return targetSlots;
    }
}
