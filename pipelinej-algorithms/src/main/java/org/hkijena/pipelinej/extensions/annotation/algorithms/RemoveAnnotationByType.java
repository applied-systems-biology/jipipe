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

package org.hkijena.pipelinej.extensions.annotation.algorithms;

import org.hkijena.pipelinej.api.ACAQDocumentation;
import org.hkijena.pipelinej.api.ACAQOrganization;
import org.hkijena.pipelinej.api.ACAQRunnerSubStatus;
import org.hkijena.pipelinej.api.ACAQValidityReport;
import org.hkijena.pipelinej.api.algorithm.*;
import org.hkijena.pipelinej.api.data.ACAQData;
import org.hkijena.pipelinej.api.parameters.ACAQParameter;
import org.hkijena.pipelinej.extensions.parameters.primitives.StringList;
import org.hkijena.pipelinej.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.pipelinej.utils.ResourceUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Removes a specified annotation
 */
@ACAQDocumentation(name = "Remove annotation by type", description = "Removes annotations of the specified types")
@ACAQOrganization(menuPath = "Remove", algorithmCategory = ACAQAlgorithmCategory.Annotation)
@AlgorithmInputSlot(value = ACAQData.class, slotName = "Input", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQData.class, slotName = "Output", inheritedSlot = "Input", autoCreate = true)
public class RemoveAnnotationByType extends ACAQSimpleIteratingAlgorithm {

    private StringList annotationTypes = new StringList();

    /**
     * @param declaration algorithm declaration
     */
    public RemoveAnnotationByType(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public RemoveAnnotationByType(RemoveAnnotationByType other) {
        super(other);
        this.annotationTypes = other.annotationTypes;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        for (String annotationType : annotationTypes) {
            dataInterface.removeGlobalAnnotation(annotationType);
        }
        dataInterface.addOutputData(getFirstOutputSlot(), dataInterface.getInputData(getFirstInputSlot(), ACAQData.class));
    }

    @ACAQDocumentation(name = "Removed annotation", description = "This annotation is removed from each input data")
    @ACAQParameter("annotation-type")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/annotation.png")
    public StringList getAnnotationTypes() {
        return annotationTypes;
    }

    @ACAQParameter("annotation-type")
    public void setAnnotationTypes(StringList annotationTypes) {
        this.annotationTypes = annotationTypes;

    }
}
