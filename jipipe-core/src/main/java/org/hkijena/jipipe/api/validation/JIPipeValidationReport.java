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

package org.hkijena.jipipe.api.validation;

import java.util.ArrayList;

/**
 * Report about the validity of an object, usually a {@link JIPipeValidatable}
 *
 */
public class JIPipeValidationReport extends ArrayList<JIPipeValidationReportEntry> {

    /**
     * Creates a new report instance
     */
    public JIPipeValidationReport() {
    }

    public int getNumberOf(JIPipeValidationReportEntryLevel level) {
        return (int) stream().filter(entry -> entry.getLevel() == level).count();
    }

    /**
     * Reports an issue
     *
     * @param issue the issue
     */
    public void report(JIPipeValidationReportEntry issue) {
        add(issue);
    }

    /**
     * Reports a {@link JIPipeValidatable} into this report
     *
     * @param context the context
     * @param validatable the validatable
     */
    public void report(JIPipeValidationReportContext context, JIPipeValidatable validatable) {
        validatable.reportValidity(context, this);
    }

    /**
     * Prints messages to the standard error
     */
    public void print() {
        for (JIPipeValidationReportEntry entry : this) {
            System.err.println(entry.toReport());
        }
    }
        
    public boolean isValid() {
        return getNumberOf(JIPipeValidationReportEntryLevel.Error) == 0;
    }
}
