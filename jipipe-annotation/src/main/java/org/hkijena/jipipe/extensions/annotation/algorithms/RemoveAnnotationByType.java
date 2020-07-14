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
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Removes a specified annotation
 */
@JIPipeDocumentation(name = "Remove annotation by type", description = "Removes annotations of the specified types")
@JIPipeOrganization(menuPath = "Remove", algorithmCategory = JIPipeNodeCategory.Annotation)
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", inheritedSlot = "Input", autoCreate = true)
public class RemoveAnnotationByType extends JIPipeSimpleIteratingAlgorithm {

    private StringList annotationTypes = new StringList();

    /**
     * @param info algorithm info
     */
    public RemoveAnnotationByType(JIPipeNodeInfo info) {
        super(info);
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
    public void reportValidity(JIPipeValidityReport report) {
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        for (String annotationType : annotationTypes) {
            dataBatch.removeGlobalAnnotation(annotationType);
        }
        dataBatch.addOutputData(getFirstOutputSlot(), dataBatch.getInputData(getFirstInputSlot(), JIPipeData.class));
    }

    @JIPipeDocumentation(name = "Removed annotation", description = "This annotation is removed from each input data")
    @JIPipeParameter("annotation-type")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/annotation.png")
    public StringList getAnnotationTypes() {
        return annotationTypes;
    }

    @JIPipeParameter("annotation-type")
    public void setAnnotationTypes(StringList annotationTypes) {
        this.annotationTypes = annotationTypes;

    }
}
