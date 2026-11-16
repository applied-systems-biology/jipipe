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

package org.hkijena.jipipe.api.automation.endpoint;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * GUI-side dispatch bridge for {@link AutomationInstanceEndpoint}.
 * Implementations resolve the project behind the project key and marshal the
 * registry dispatch onto the EDT.
 */
public interface AutomationEndpointHandlers {

    /**
     * Executes one operation. Implementations dispatch through the registry's
     * validating execute, so its {@link IllegalArgumentException} signals schema violations.
     */
    JsonNode execute(String operationId, String projectKey, JsonNode params) throws Exception;

    boolean isRegistered(String operationId);

    boolean isHidden(String operationId);
}
