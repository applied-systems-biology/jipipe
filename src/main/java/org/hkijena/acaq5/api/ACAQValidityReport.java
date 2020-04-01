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
    private List<String> categories = new ArrayList<>();
    private Map<String, Response> responses = new HashMap<>();
    private Map<String, String> messages = new HashMap<>();

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

    public List<String> getInvalidResponses() {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Response> entry : responses.entrySet()) {
            if (entry.getValue() == Response.Invalid)
                result.add(entry.getKey());
        }
        return result;
    }

    public Map<String, String> getMessages() {
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
    public void report(Response response, String message) {
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
    public void report(boolean valid, String message) {
        report(valid ? Response.Valid : Response.Invalid, message);
    }

    /**
     * Report that is report is valid
     */
    public void reportIsValid() {
        report(true, "");
    }

    /**
     * Reports that this report is invalid
     *
     * @param message The message
     */
    public void reportIsInvalid(String message) {
        report(false, message);
    }

    /**
     * The response type
     */
    public enum Response {
        Valid,
        Invalid
    }
}
