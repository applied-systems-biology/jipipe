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

package org.hkijena.jipipe.api.automation.permissions;

import org.hkijena.jipipe.api.automation.AutomationOperation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves the {@link AutomationPermissionState} of automation operations.
 * <p>
 * Resolution order in {@link #resolve}: "Allow always" grant for (operation id, project key),
 * then CLI override, then user state, then the category default
 * ({@code operation.isReadOnly() ? Allow : Ask}).
 * <p>
 * All storage is backed by concurrent maps: {@link #resolve} may be called from HTTP threads
 * while states and grants are changed from other threads. No additional locking is required.
 * <p>
 * Grants are in-memory only and are not persisted.
 */
public class AutomationPermissionService {

    /**
     * A persistent "Allow always" decision for one operation in one project.
     */
    public record AutomationGrant(String operationId, String projectKey) {
    }

    /**
     * Fail-closed check order for CLI overrides: if an operation id is registered for
     * multiple override states, the most restrictive state wins.
     */
    private static final List<AutomationPermissionState> OVERRIDE_CHECK_ORDER = List.of(
            AutomationPermissionState.Deny,
            AutomationPermissionState.Hide,
            AutomationPermissionState.Ask,
            AutomationPermissionState.Allow);

    private final ConcurrentHashMap<String, AutomationPermissionState> userStates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<AutomationPermissionState, Set<String>> cliOverrides = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<AutomationGrant, Boolean> grants = new ConcurrentHashMap<>();

    /**
     * Sets the user-chosen state for one operation id.
     * A later call for the same id replaces the earlier one; a null state clears it.
     */
    public void setUserState(String operationId, AutomationPermissionState state) {
        if (state == null) {
            userStates.remove(operationId);
        } else {
            userStates.put(operationId, state);
        }
    }

    /**
     * Registers the operation ids that are overridden via CLI arguments.
     * Replaces the id set registered for this state (repeated calls are idempotent);
     * overrides of other states are kept. A null or empty collection clears this state's override.
     */
    public void setCliOverride(AutomationPermissionState state, Collection<String> operationIds) {
        if (operationIds == null || operationIds.isEmpty()) {
            cliOverrides.remove(state);
        } else {
            cliOverrides.put(state, Set.copyOf(operationIds));
        }
    }

    /**
     * Resolves the permission state of an operation within a project.
     */
    public AutomationPermissionState resolve(AutomationOperation operation, String projectKey) {
        String operationId = operation.getId();
        if (projectKey != null && grants.containsKey(new AutomationGrant(operationId, projectKey))) {
            return AutomationPermissionState.Allow;
        }
        for (AutomationPermissionState state : OVERRIDE_CHECK_ORDER) {
            Set<String> ids = cliOverrides.get(state);
            if (ids != null && ids.contains(operationId)) {
                return state;
            }
        }
        AutomationPermissionState userState = userStates.get(operationId);
        if (userState != null) {
            return userState;
        }
        return operation.isReadOnly() ? AutomationPermissionState.Allow : AutomationPermissionState.Ask;
    }

    /**
     * Grants "Allow always" for one operation id in one project (in-memory only).
     */
    public void grantAlways(String operationId, String projectKey) {
        grants.put(new AutomationGrant(operationId, projectKey), Boolean.TRUE);
    }

    /**
     * Removes all grants.
     */
    public void revokeAllGrants() {
        grants.clear();
    }

    /**
     * Removes one grant (no-op when the grant does not exist).
     */
    public void revokeGrant(AutomationGrant grant) {
        if (grant != null) {
            grants.remove(grant);
        }
    }

    /**
     * Returns a sorted snapshot of all grants.
     */
    public List<AutomationGrant> getGrants() {
        List<AutomationGrant> snapshot = new ArrayList<>(grants.keySet());
        snapshot.sort(Comparator.comparing(AutomationGrant::operationId).thenComparing(AutomationGrant::projectKey));
        return List.copyOf(snapshot);
    }
}
