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

package org.hkijena.jipipe.api.validation;


/**
 * An interface about a type that reports of the validity of its internal state
 */
public interface JIPipeValidatable {

    /**
     * Generates a validity report
     *
     * @param reportContext the report context
     * @param report        the report to be added to
     */
    void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report);

    /**
     * Generates a report for this object
     *
     * @param context the report context
     * @return the report
     */
    default JIPipeValidationReport generateValidityReport(JIPipeValidationReportContext context) {
        JIPipeValidationReport report = new JIPipeValidationReport();
        reportValidity(context, report);
        return report;
    }
}
