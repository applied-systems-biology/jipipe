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
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.pairs.StringAndStringPredicatePair;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Removes a specified annotation
 */
@JIPipeDocumentation(name = "Remove annotation by value", description = "Removes annotations that match a filter value")
@JIPipeOrganization(menuPath = "Remove", algorithmCategory = JIPipeAlgorithmCategory.Annotation)
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", inheritedSlot = "Input", autoCreate = true)
public class RemoveAnnotationByValue extends JIPipeSimpleIteratingAlgorithm {

    private StringAndStringPredicatePair.List filters = new StringAndStringPredicatePair.List();

    /**
     * @param declaration algorithm declaration
     */
    public RemoveAnnotationByValue(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public RemoveAnnotationByValue(RemoveAnnotationByValue other) {
        super(other);
        this.filters = other.filters;
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        for (StringAndStringPredicatePair filter : filters) {
            JIPipeAnnotation instance = dataInterface.getAnnotationOfType(filter.getKey());
            if (instance != null) {
                if (filter.getValue().test(instance.getValue())) {
                    dataInterface.removeGlobalAnnotation(filter.getKey());
                }
            }
        }
        dataInterface.addOutputData(getFirstOutputSlot(), dataInterface.getInputData(getFirstInputSlot(), JIPipeData.class));
    }

    @JIPipeDocumentation(name = "Removed annotation", description = "This annotation is removed from each input data")
    @JIPipeParameter("filters")
    @StringParameterSettings(monospace = true)
    public StringAndStringPredicatePair.List getFilters() {
        return filters;
    }

    @JIPipeParameter("filters")
    public void setFilters(StringAndStringPredicatePair.List annotationTypes) {
        this.filters = annotationTypes;

    }

}
