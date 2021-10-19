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

package org.hkijena.jipipe.extensions.parameters.primitives;

import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeValidatable;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataAnnotation;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.util.Collection;

/**
 * Parameter used for creating annotations
 */
@StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/data-annotation.png")
public class OptionalDataAnnotationNameParameter extends OptionalStringParameter implements JIPipeValidatable {
    public OptionalDataAnnotationNameParameter() {
    }

    public OptionalDataAnnotationNameParameter(OptionalStringParameter other) {
        super(other);
    }

    public OptionalDataAnnotationNameParameter(String value, boolean enabled) {
        super(value, enabled);
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        if (isEnabled()) {
            report.resolve("Value").checkNonEmpty(getContent(), this);
        }
    }

    /**
     * Creates a new annotation with the defined name and value
     *
     * @param value the value of the annotation
     * @return annotation
     */
    public JIPipeDataAnnotation createAnnotation(JIPipeData value) {
        return new JIPipeDataAnnotation(getContent(), value);
    }

    /**
     * Adds the annotation with given value of the parameter is enabled
     *
     * @param annotations list of annotations
     * @param value       the value
     */
    public void addAnnotationIfEnabled(Collection<JIPipeDataAnnotation> annotations, JIPipeData value) {
        if (isEnabled()) {
            annotations.add(createAnnotation(value));
        }
    }
}
