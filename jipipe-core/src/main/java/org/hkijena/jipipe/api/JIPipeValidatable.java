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

package org.hkijena.jipipe.api;


/**
 * An interface about a type that reports of the validity of its internal state
 */
public interface JIPipeValidatable {

    /**
     * Generates a validity report
     *
     * @param report the report to be added to
     */
    void reportValidity(JIPipeIssueReport report);

    /**
     * Generates a report for this object
     *
     * @return the report
     */
    default JIPipeIssueReport generateValidityReport() {
        JIPipeIssueReport report = new JIPipeIssueReport();
        reportValidity(report);
        return report;
    }

//    @Override
//    boolean isValid();
//
//    @Override
//    default List<ValidityProblem> getProblems() {
//        JIPipeIssueReport report = new JIPipeIssueReport();
//        reportValidity(report);
//        List<ValidityProblem> list = new ArrayList<>();
//        for (Map.Entry<String, JIPipeIssueReport.Issue> entry : report.getIssues().entries()) {
//            list.add(new ValidityProblem(entry.getKey() + ": " + entry.getValue().toString()));
//        }
//        return list;
//    }
}
