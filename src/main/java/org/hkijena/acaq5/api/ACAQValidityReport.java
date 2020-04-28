/*
 * Copyright by Ruman Gerst
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * This code is licensed under BSD 2-Clause
 * See the LICENSE file provided with this code for the full license.
 */

package org.hkijena.acaq5.api;

import java.util.*;

/**
 * Report about the validity of an object
 */
public class ACAQValidityReport {
    private final List<String> categories = new ArrayList<>();
    private Map<String, Response> responses = new HashMap<>();
    private Map<String, Message> messages = new HashMap<>();

    /**
     * Creates a new report instance
     */
    public ACAQValidityReport() {
    }

    /**
     * Clears the report
     */
    public void clear() {
        responses.clear();
        messages.clear();
    }

    /**
     * Gets the response keys that are invalid
     * @return the response keys
     */
    public List<String> getInvalidResponses() {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Response> entry : responses.entrySet()) {
            if (entry.getValue() == Response.Invalid)
                result.add(entry.getKey());
        }
        return result;
    }

    public Map<String, Message> getMessages() {
        return Collections.unmodifiableMap(messages);
    }

    public boolean isValid() {
        return responses.values().stream().allMatch(r -> r == Response.Valid);
    }

    public List<String> getCategories() {
        return categories;
    }

    /**
     * Returns a report for the specified category
     *
     * @param category the category
     * @return the sub-report
     */
    public ACAQValidityReport forCategory(String category) {
        ACAQValidityReport result = new ACAQValidityReport();
        result.categories.addAll(categories);
        result.categories.add(category);
        result.responses = responses;
        result.messages = messages;
        return result;
    }

    /**
     * Reports a response
     *
     * @param response the response
     * @param message  the message
     */
    public void report(Response response, Message message) {
        String key = String.join("/", categories);
        responses.put(key, response);
        messages.put(key, message);
    }

    /**
     * Passes the report to another {@link ACAQValidatable}
     *
     * @param validatable the target
     */
    public void report(ACAQValidatable validatable) {
        validatable.reportValidity(this);
    }

    /**
     * Reports validity or invalidity
     *
     * @param valid   if the report is valid
     * @param message the message
     */
    public void report(boolean valid, Message message) {
        report(valid ? Response.Valid : Response.Invalid, message);
    }

    /**
     * Report that is report is valid
     */
    public void reportIsValid() {
        report(true, null);
    }

    /**
     * Reports that this report is invalid
     *
     * @param message The message
     */
    public void reportIsInvalid(Message message) {
        report(false, message);
    }

    /**
     * Reports that this report is invalid
     *
     * @param what explanation what happened
     * @param why explanation why it happened
     * @param how explanation how to solve the issue
     */
    public void reportIsInvalid(String what, String why, String how) {
        report(false, new Message(what, why, how));
    }

    /**
     * Reports as invalid if the value is not within the limits
     *
     * @param value      the value
     * @param min        interval start
     * @param max        interval end
     * @param includeMin if the interval start is included
     * @param includeMax if the interval end is included
     */
    public void checkIfWithin(double value, double min, double max, boolean includeMin, boolean includeMax) {
        if ((includeMin && value < min) || (!includeMin && value <= min) || (includeMax && value > max) || (!includeMax && value >= max)) {
            reportIsInvalid("Invalid value!", "Numeric values must be within an allowed range.",
                    "Please provide a value within " + (includeMin ? "[" : "(") + min + " and " + (includeMax ? "]" : ")") + max);
        }
    }

    /**
     * Reports as invalid if the value is null
     *
     * @param value the value
     */
    public void checkNonNull(Object value) {
        if (value == null) {
            reportIsInvalid("No value provided!", "Dependent methods require that a value is set.", "Please provide a valid value.");
        }
    }

    /**
     * The response type
     */
    public enum Response {
        Valid,
        Invalid
    }

    /**
     * A validity report message
     */
    public static class Message {
        private String userWhat;
        private String userWhy;
        private String userHow;

        /**
         * @param userWhat explanation what happened
         * @param userWhy explanation why it happened
         * @param userHow explanation how to solve the issue
         */
        public Message(String userWhat, String userWhy, String userHow) {
            this.userWhat = userWhat;
            this.userWhy = userWhy;
            this.userHow = userHow;
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
    }
}
