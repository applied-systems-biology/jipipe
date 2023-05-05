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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Report about the validity of an object, usually a {@link JIPipeValidatable}
 */
public class JIPipeIssueReport {
    private final List<String> pathComponents = new ArrayList<>();
    private Multimap<String, Issue> issues = HashMultimap.create();

    /**
     * Creates a new report instance
     */
    public JIPipeIssueReport() {
    }

    /**
     * Clears all the issues in this report and all related reports
     */
    public void clearAll() {
        issues.clear();
    }

    /**
     * Clears issues of the current path
     */
    public void clearCurrentPath() {
        issues.removeAll(getPath());
    }

    /**
     * Gets the map of all issues
     *
     * @return the issues (modifiable)
     */
    public Multimap<String, Issue> getIssues() {
        return issues;
    }

    /**
     * Returns true if there are no issues
     *
     * @return if there are no issues
     */
    public boolean isValid() {
        return issues.isEmpty();
    }

    /**
     * Returns true if there is at least one issue
     *
     * @return if there are issues
     */
    public boolean isInvalid() {
        return !issues.isEmpty();
    }

    /**
     * Gets the current path components
     *
     * @return the path components
     */
    public List<String> getPathComponents() {
        return pathComponents;
    }

    /**
     * Gets the current path (formatted)
     *
     * @return the path
     */
    public String getPath() {
        return String.join("/", pathComponents);
    }

    /**
     * Returns a report for the specified category
     *
     * @param pathComponent the category
     * @return the sub-report
     */
    public JIPipeIssueReport resolve(String pathComponent) {
        JIPipeIssueReport result = new JIPipeIssueReport();
        result.pathComponents.addAll(pathComponents);
        result.pathComponents.add(pathComponent);
        result.issues = issues;
        return result;
    }

    public void mergeWith(JIPipeIssueReport other) {
        for (Map.Entry<String, Issue> entry : other.issues.entries()) {
            issues.put(getPath() + "/" + entry.getKey(), entry.getValue());
        }
    }

    /**
     * Reports an issue
     *
     * @param issue the issue
     */
    public void report(Issue issue) {
        issues.put(getPath(), issue);
    }

    /**
     * Reports a {@link JIPipeValidatable} into this report
     *
     * @param validatable the validatable
     */
    public void report(JIPipeValidatable validatable) {
        validatable.reportValidity(this);
    }

    /**
     * Reports that this report is invalid
     *
     * @param what   explanation what happened
     * @param why    explanation why it happened
     * @param how    explanation how to solve the issue
     * @param source the source that triggers the check. passed to details
     */
    public void reportIsInvalid(String what, String why, String how, Object source) {
        String message = null;
        try {
            message = ReflectionToStringBuilder.toString(source, ToStringStyle.MULTI_LINE_STYLE);
        } catch (Exception | Error e) {
            message = "Unable to generate report due to following issue: " + e;
        }
        report(new Issue(what,
                why,
                how,
                message));
    }

    /**
     * Reports as invalid if the value is not within the limits
     *
     * @param source     the source that triggers the check. passed to details
     * @param value      the value
     * @param min        interval start
     * @param max        interval end
     * @param includeMin if the interval start is included
     * @param includeMax if the interval end is included
     */
    public void checkIfWithin(Object source, double value, double min, double max, boolean includeMin, boolean includeMax) {
        if ((includeMin && value < min) || (!includeMin && value <= min) || (includeMax && value > max) || (!includeMax && value >= max)) {
            reportIsInvalid("Invalid value!", "Numeric values must be within an allowed range.",
                    "Please provide a value within " + (includeMin ? "[" : "(") + min + " and " + (includeMax ? "]" : ")") + max,
                    source);
        }
    }

    /**
     * Reports as invalid if the value is null
     *
     * @param value  the value
     * @param source the source that triggers the check. passed to details
     */
    public void checkNonNull(Object value, Object source) {
        if (value == null) {
            reportIsInvalid("No value provided!", "Dependent methods require that a value is set.", "Please provide a valid value.",
                    source);
        }
    }

    /**
     * Reports as invalid if the string value is null or empty
     *
     * @param value  the value
     * @param source the source that triggers the check. passed to details
     */
    public void checkNonEmpty(Object value, Object source) {
        if (StringUtils.isNullOrEmpty(value)) {
            reportIsInvalid("No value provided!", "Dependent methods require that a value is set.", "Please provide a valid value.",
                    source);
        }
    }

    /**
     * Prints messages to the standard error
     */
    public void print() {
        for (Map.Entry<String, Issue> entry : issues.entries()) {
            System.err.println();
            System.err.println("At " + entry.getKey() + ":");
            System.err.println("\tWhat: " + entry.getValue().getUserWhat());
            System.err.println("\tWhy: " + entry.getValue().getUserWhy());
            System.err.println("\tHow to solve: " + entry.getValue().getUserHow());
            System.err.println("\tDetails: " + entry.getValue().getDetails());
        }
    }

    /**
     * A validity report message
     */
    public static class Issue {
        private final String userWhat;
        private final String userWhy;
        private final String userHow;
        private final String details;

        /**
         * @param userWhat explanation what happened
         * @param userWhy  explanation why it happened
         * @param userHow  explanation how to solve the issue
         * @param details  optional details
         */
        public Issue(String userWhat, String userWhy, String userHow, String details) {
            this.userWhat = userWhat;
            this.userWhy = userWhy;
            this.userHow = userHow;
            this.details = details;
        }

        public String getUserWhat() {
            return userWhat;
        }

        public String getUserWhy() {
            return userWhy;
        }

        public String getUserHow() {
            return userHow;
        }

        @Override
        public String toString() {
            return userWhat + " " + userWhy + " " + userHow;
        }

        public String getDetails() {
            return details;
        }
    }
}
