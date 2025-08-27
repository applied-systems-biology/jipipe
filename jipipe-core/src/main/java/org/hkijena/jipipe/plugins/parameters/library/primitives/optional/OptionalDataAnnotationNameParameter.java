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

package org.hkijena.jipipe.plugins.parameters.library.primitives.optional;

import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.validation.JIPipeValidatable;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.Collection;

/**
 * Parameter used for creating annotations
 */
@StringParameterSettings(monospace = true, icon = "data-types/data-annotation.png")
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
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReportSettings reportSettings, JIPipeValidationReport report) {
        if (isEnabled()) {
            if (!StringUtils.isNullOrEmpty(getContent())) {
                reportContext.warning().title("The annotation name is empty!").explanation("Annotation names cannot be empty!").report(report);
            }
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
