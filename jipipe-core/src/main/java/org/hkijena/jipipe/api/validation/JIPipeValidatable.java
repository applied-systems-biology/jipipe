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


import org.hkijena.jipipe.api.JIPipeProgressInfo;

/**
 * An interface about a type that reports of the validity of its internal state
 */
public interface JIPipeValidatable {

    /**
     * Generates a validity report
     *
     * @param reportContext  the report context
     * @param reportSettings the report settings
     * @param report         the report to be added to
     * @param progressInfo the progress info
     */
    void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReportSettings reportSettings, JIPipeValidationReport report, JIPipeProgressInfo progressInfo);

    /**
     * Generates a report for this object
     *
     * @param reportContext  the report context
     * @param reportSettings the report settings
     * @param progressInfo the progress info
     * @return the report
     */
    default JIPipeValidationReport generateValidityReport(JIPipeValidationReportContext reportContext, JIPipeValidationReportSettings reportSettings, JIPipeProgressInfo progressInfo) {
        JIPipeValidationReport report = new JIPipeValidationReport();
        reportValidity(reportContext, reportSettings, report, progressInfo);
        return report;
    }
}
