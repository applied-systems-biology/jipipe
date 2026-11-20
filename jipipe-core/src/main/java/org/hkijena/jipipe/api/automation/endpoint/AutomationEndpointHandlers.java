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
     * <p>
     * {@code projectKey} is the EFFECTIVE key: the endpoint resolved a null or blank
     * forwarded key through {@link #resolveProjectKey} before the permission gate, so the
     * key received here is already the one the permission resolution, the Ask prompt, and
     * the call log used. A null key (nothing to resolve: no default window) still reaches
     * this method: registry-global operations (list_projects, list_algorithms, get_node_docs)
     * are routed without a project and must run with a null-project context (E2E ruling
     * 2026-11-16). Project-bound operations reached with a null key fail with their own
     * "No project loaded" style error results.
     */
    JsonNode execute(String operationId, String projectKey, JsonNode params) throws Exception;

    /**
     * Resolves the effective project key of one dispatch, called ONCE per dispatch BEFORE
     * the permission gate: the returned key is used for the permission resolution, the Ask
     * prompt, the execution, and the call log, so Ask prompts and Allow-always grants
     * attribute to the same window the dispatch executes against (review 2026-11-20: a null
     * key under default routing used to reach the permission gate unresolved, which stored
     * Allow-always grants under null where {@code AutomationPermissionService.resolve} can
     * never consult them).
     * <p>
     * The default is the identity (the key is used exactly as forwarded), so implementations
     * that need no resolution (tests, non-GUI wiring) are unaffected. Implementations may
     * resolve a null or blank key to a concrete default (the production GUI wiring resolves
     * it to the focused project window of the instance, WI #1383). Throwing fails the whole
     * dispatch: the message becomes the error envelope the agent receives, before any
     * permission state is consulted.
     *
     * @param projectKey the raw forwarded key (may be null or blank)
     * @return the effective project key of the dispatch
     * @throws Exception when the key cannot be resolved; the dispatch fails with the message
     */
    default String resolveProjectKey(String projectKey) throws Exception {
        return projectKey;
    }

    boolean isRegistered(String operationId);

    boolean isHidden(String operationId);
}
